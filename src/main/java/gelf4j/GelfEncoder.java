package gelf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
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
  static final String ID_NAME = "id";
  static final String GELF_VERSION = "1.0";
  static final byte[] CHUNKED_GELF_ID = new byte[]{ 0x1e, 0x0f };

  static final int MESSAGE_ID_LENGTH = 32;
  static final int SEQUENCE_LENGTH = 2;
  static final int COMPRESSED_MESSAGE_ID_LENGTH = 8;
  static final int COMPRESSED_SEQUENCE_LENGTH = 1;

  static final int COMPRESSED_HEADER_SIZE =
    CHUNKED_GELF_ID.length + COMPRESSED_MESSAGE_ID_LENGTH + COMPRESSED_SEQUENCE_LENGTH + COMPRESSED_SEQUENCE_LENGTH;

  static final int HEADER_SIZE =
    CHUNKED_GELF_ID.length + MESSAGE_ID_LENGTH + SEQUENCE_LENGTH + SEQUENCE_LENGTH;

  // Arbitrary size to avoid too much fragmentation
  static final int MAX_PACKET_SIZE = 2 * 1024;

  // Payload threshold (Maximum packet size minus the size of uncompressed header)
  static final int PAYLOAD_THRESHOLD = MAX_PACKET_SIZE - 38;
  static final int MAX_SEQ_NUMBER = Byte.MAX_VALUE;

  private static final BigDecimal TIME_DIVISOR = new BigDecimal( 1000 );
  private static final String DEFAULT_FACILITY = "GELF";

  private final MessageDigest _messageDigest;
  private final String _hostname;
  private final boolean _compressed;

  GelfEncoder( final String hostname, final boolean compressed )
    throws Exception
  {
    this( MessageDigest.getInstance( "MD5" ), hostname, compressed );
  }

  GelfEncoder( final MessageDigest messageDigest, final String hostname, final boolean compressed )
  {
    _messageDigest = messageDigest;
    _hostname = hostname;
    _compressed = compressed;
  }

  List<byte[]> encode( final GelfMessage message )
  {
    final String json = toJson( message );
    final byte[] encodedPayload = gzip( json );
    return null == encodedPayload ? null : createPackets( encodedPayload );
  }

  String toJson( final GelfMessage message )
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

    final String hostname = message.getHost();
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

    return JSONValue.toJSONString( map );
  }

  private String encodeTimestamp( final long time )
  {
    return new BigDecimal( time ).divide( TIME_DIVISOR ).toPlainString();
  }

  /**
   * Creates a message id that should be unique on every call. The message ID needs to be unique for every message. If
   * a message is chunked, then each chunk in a message needs the same message ID.
   *
   * @return unique message ID
   */
  byte[] generateMessageID()
  {
    // Uniqueness is guaranteed by combining the hostname and the current nanosecond, hashing the result, and
    // selecting the first x bytes of the result
    final String timestamp = String.valueOf( System.nanoTime() );
    final byte[] digestString = ( _hostname + timestamp ).getBytes();
    final int messageIdLength = _compressed ? COMPRESSED_MESSAGE_ID_LENGTH : MESSAGE_ID_LENGTH;
    return Arrays.copyOf( _messageDigest.digest( digestString ), messageIdLength );
  }

  /**
   * Create a list of packets for the specified payload. This may involve splitting the payload into multiple
   * chunks if it exceeds the max packet size.
   *
   * @param payload   The full payload
   * @return A list of packets which when added together, make up the full payload
   */
  List<byte[]> createPackets( final byte[] payload )
  {
    final List<byte[]> packets = new ArrayList<byte[]>();
    if ( payload.length <= MAX_PACKET_SIZE )
    {
      packets.add( payload );
    }
    else
    {
      final byte[] messageId = generateMessageID();

      final int fullChunksCount = payload.length / PAYLOAD_THRESHOLD;
      if ( fullChunksCount > MAX_SEQ_NUMBER )
      {
        return null;
      }
      final int remainingBytes = payload.length % PAYLOAD_THRESHOLD;
      final int chunkCount = fullChunksCount + ( remainingBytes != 0 ? 1 : 0 );
      final int headerSize = _compressed ? COMPRESSED_HEADER_SIZE : HEADER_SIZE;
      
      for ( int chunk = 0; chunk < fullChunksCount; chunk++ )
      {
        final ByteBuffer buffer = ByteBuffer.allocate( PAYLOAD_THRESHOLD + headerSize );
        buffer.put( CHUNKED_GELF_ID );
        buffer.put( messageId );
        if ( !_compressed )
        {
          buffer.put( (byte) 0 );
        }
        buffer.put( (byte) chunk );
        if ( !_compressed )
        {
          buffer.put( (byte) 0 );
        }
        buffer.put( (byte) chunkCount );
        buffer.put( payload, chunk * PAYLOAD_THRESHOLD, PAYLOAD_THRESHOLD );
        packets.add( buffer.array() );

      }
      if ( remainingBytes > 0 )
      {
        final ByteBuffer buffer = ByteBuffer.allocate( remainingBytes + headerSize );
        buffer.put( CHUNKED_GELF_ID );
        buffer.put( messageId );
        if ( !_compressed )
        {
          buffer.put( (byte) 0 );
        }
        buffer.put( (byte) (chunkCount - 1) );
        if ( !_compressed )
        {
          buffer.put( (byte) 0 );
        }
        buffer.put( (byte) chunkCount );
        buffer.put( payload, fullChunksCount * PAYLOAD_THRESHOLD, remainingBytes );
        packets.add( buffer.array() );
      }
    }
    return packets;
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
