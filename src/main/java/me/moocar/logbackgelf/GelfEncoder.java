package me.moocar.logbackgelf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.json.simple.JSONValue;

/**
 * Responsible for converting a GelfMessage into packets.
 * The GelfMessage is converted to json, gzipped and then converted into 1 or more packets (a.k.a. chunks).
 */
final class GelfEncoder
{
  private static final String ID_NAME = "id";
  private static final String GELF_VERSION = "1.0";

  private static final int PROTOCOL_VERSION_1_0_ID_LENGTH = 8;

  private static final int PROTOCOL_VERSION_1_0_SEQUENCE_LENGTH = 1;

  private static final byte[] CHUNKED_GELF_ID = new byte[]{ 0x1e, 0x0f };

  @SuppressWarnings( "PointlessArithmeticExpression" )
  private static final int PROTOCOL_VERSION_1_0_HEADER_SIZE =
    CHUNKED_GELF_ID.length + PROTOCOL_VERSION_1_0_ID_LENGTH + PROTOCOL_VERSION_1_0_SEQUENCE_LENGTH * 2;

  public static final int MAX_PACKET_SIZE = 1024;
  private static final int PROTOCOL_VERSION_1_0_PAYLOAD_THRESHOLD = MAX_PACKET_SIZE - PROTOCOL_VERSION_1_0_HEADER_SIZE;

  public static final int MAX_SEQ_NUMBER = Byte.MAX_VALUE;

  private static final BigDecimal TIME_DIVISOR = new BigDecimal( 1000 );
  public static final String DEFAULT_FACILITY = "GELF";


  private final MessageDigest _messageDigest;
  private final String _hostname;

  GelfEncoder( final MessageDigest messageDigest, final String hostname )
  {
    _messageDigest = messageDigest;
    _hostname = hostname;
  }

  List<byte[]> encode( final GelfMessage message )
  {
    final Map<String, Object> map = new HashMap<String, Object>();

    map.put( "version", GELF_VERSION );
    final String shortMessage = message.getShortMessage();
    if( null == shortMessage )
    {
      //A message with no short message can not be validly encoded
      return null;
    }
    map.put( "short_message", shortMessage );
    final String fullMessage = message.getFullMessage();
    if( null != fullMessage )
    {
      map.put( "full_message", fullMessage );
    }

    final Long timestamp = message.getJavaTimestamp();
    map.put( "timestamp", encodeTimestamp( null != timestamp ? timestamp : System.currentTimeMillis() ) );
    final String facility = message.getFacility();
    map.put( "facility", null != facility ? facility : DEFAULT_FACILITY );

    final SyslogLevel level = message.getLevel();
    if( null != level )
    {
      map.put( "level", level.ordinal() );
    }

    final String file = message.getFile();
    if( null != file )
    {
      map.put( "file", file );
    }
    final Long line = message.getLine();
    if( null != line )
    {
      map.put( "line", line );
    }


    final String hostname = message.getHostname();
    map.put( "host", null == hostname ? _hostname : hostname );

    final Map<String, Object> fields = message.getAdditionalFields();
    for( final Map.Entry<String, Object> entry : fields.entrySet() )
    {
      final String key = entry.getKey();
      if( !key.equals( ID_NAME ) )
      {
        map.put( "_" + key, entry.getValue() );
      }
    }

    return encode( JSONValue.toJSONString( map ) );
  }

  private String encodeTimestamp( final long time )
  {
    return new BigDecimal( time ).divide( TIME_DIVISOR ).toPlainString();
  }

  /**
   * Converts a GELF message into a number of GELF chunks.
   *
   * @param message The GELF Message (JSON)
   * @return A list of chunks that should be sent to the graylog2 server, or null if failed to encode
   */
  List<byte[]> encode( final String message )
  {
    final byte[] encodedPayload = gzip( message );
    return null == encodedPayload ? null : createChunks( generateMessageID(), splitPayload( encodedPayload ) );
  }

  /**
   * Converts a payload into a number of full GELF chunks.
   *
   * @param payload The original gzipped payload
   * @return A list of chunks that should be sent to the graylog2 server
   *         TODO: Remove me
   */
  @Deprecated
  List<byte[]> chunkIt( final byte[] payload )
  {
    return createChunks( generateMessageID(), splitPayload( payload ) );
  }

  /**
   * Creates a message id that should be unique on every call. The message ID needs to be unique for every message. If
   * a message is chunked, then each chunk in a message needs the same message ID.
   *
   * @return unique message ID
   */
  private byte[] generateMessageID()
  {
    // Uniqueness is guaranteed by combining the hostname and the current nano second, hashing the result, and
    // selecting the first x bytes of the result
    final String timestamp = String.valueOf( System.nanoTime() );
    final byte[] digestString = ( _hostname + timestamp ).getBytes();
    return Arrays.copyOf( _messageDigest.digest( digestString ), PROTOCOL_VERSION_1_0_ID_LENGTH );
  }

  /**
   * Converts each subPayload into a full GELF chunk
   *
   * @param messageId   The unique messageId that all the produced chunks will use
   * @param subPayloads The raw sub payload data.
   * @return A list of each subPayload wrapped in a GELF chunk
   */
  private List<byte[]> createChunks( final byte[] messageId, final List<byte[]> subPayloads )
  {
    final List<byte[]> chunks = new ArrayList<byte[]>();

    byte seqNum = 0;
    for( final byte[] subPayload : subPayloads )
    {
      if( MAX_SEQ_NUMBER == seqNum )
      {
        break;
      }

      chunks.add( create( messageId, seqNum++, (byte) ( subPayloads.size() ), subPayload ) );
    }

    return chunks;
  }

  /**
   * Splits the payload data into chunks
   *
   * @param payload The full payload
   * @return A list of subPayloads which when added together, make up the full payload
   */
  private List<byte[]> splitPayload( byte[] payload )
  {
    final List<byte[]> subPayloads = new ArrayList<byte[]>();

    final int numFullSubs = payload.length / PROTOCOL_VERSION_1_0_PAYLOAD_THRESHOLD;
    final int lastSubLength = payload.length % PROTOCOL_VERSION_1_0_PAYLOAD_THRESHOLD;

    for( int subPayload = 0; subPayload < numFullSubs; subPayload++ )
    {
      subPayloads.add( extractSubPayload( payload, subPayload ) );
    }
    if( lastSubLength > 0 )
    {
      subPayloads.add( extractSubPayload( payload, numFullSubs ) );
    }
    return subPayloads;
  }

  /**
   * Extracts a chunk of bytes from another byte array based on a chunkNumber/Position
   *
   * @param payload   The main array
   * @param subPaylod The chunk number to extract.
   * @return The extracted chunk
   */
  private byte[] extractSubPayload( final byte[] payload, final int subPaylod )
  {
    return Arrays.copyOfRange( payload,
                               subPaylod * PROTOCOL_VERSION_1_0_PAYLOAD_THRESHOLD,
                               ( subPaylod + 1 ) * PROTOCOL_VERSION_1_0_PAYLOAD_THRESHOLD );
  }


  /**
   * Concatenates everything into a GELF Chunk header and then appends the sub Payload
   *
   * @param messageId  The message ID of the message (remains same for all chunks)
   * @param seqNum     The sequence number of the chunk
   * @param numChunks  The number of chunks that will be sent
   * @param subPayload The actual data that will be sent in this chunk
   * @return The complete chunk that wraps the sub pay load
   */
  public byte[] create( final byte[] messageId, final byte seqNum, final byte numChunks, final byte[] subPayload )
  {
    return concatArrays( concatArrays( concatArrays( CHUNKED_GELF_ID, messageId ), new byte[]{ seqNum, numChunks } ), subPayload );
  }

  /**
   * Returns array1 concatenated with array2 (non-destructive)
   *
   * @param array1 The first array
   * @param array2 The array to append to the end of the first
   * @return Array1 + array2
   */
  private byte[] concatArrays( final byte[] array1, final byte[] array2 )
  {
    byte[] finalArray = Arrays.copyOf( array1, array2.length + array1.length );
    System.arraycopy( array2, 0, finalArray, array1.length, array2.length );
    return finalArray;
  }

  /**
   * Compresses a string using the GZIP compression scheme.
   *
   * @param message The message to compress.
   * @return The encoded message.
   */
  private byte[] gzip( final String message )
  {
    GZIPOutputStream zipStream = null;
    try
    {
      final ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
      zipStream = new GZIPOutputStream( targetStream );
      zipStream.write( message.getBytes() );
      zipStream.close();
      final byte[] zipped = targetStream.toByteArray();
      targetStream.close();
      return zipped;
    }
    catch( final IOException ioe )
    {
      return null;
    }
    finally
    {
      try
      {
        if( null != zipStream )
        {
          zipStream.close();
        }
      }
      catch( final IOException ioe )
      {
        //Ignore..
      }
    }
  }
}
