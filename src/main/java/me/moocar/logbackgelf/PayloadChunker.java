package me.moocar.logbackgelf;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for converting a Gelf Payload into chunks if need be.
 * <p/>
 * A Payload is the raw Gzipped Gelf JSON message.
 * A Chunk is comprised of a Gelf Chunk header as well as a portion of the payload
 */
public class PayloadChunker
{
  private static final int PROTOCOL_VERSION_0_9_ID_LENGTH = 32;
  private static final int PROTOCOL_VERSION_1_0_ID_LENGTH = 8;

  private static final int PROTOCOL_VERSION_0_9_SEQUENCE_LENGTH = 2;
  private static final int PROTOCOL_VERSION_1_0_SEQUENCE_LENGTH = 1;

  private static final byte[] CHUNKED_GELF_ID = new byte[]{ 0x1e, 0x0f };

  private static final int PROTOCOL_VERSION_0_9_HEADER_SIZE =
    CHUNKED_GELF_ID.length + getMessageIdLength( GelfVersion.V0_9 ) + PROTOCOL_VERSION_0_9_SEQUENCE_LENGTH * 2;
  @SuppressWarnings( "PointlessArithmeticExpression" )
  private static final int PROTOCOL_VERSION_1_0_HEADER_SIZE =
    CHUNKED_GELF_ID.length + getMessageIdLength( GelfVersion.V1_0 ) + PROTOCOL_VERSION_1_0_SEQUENCE_LENGTH * 2;

  private static final int PROTOCOL_VERSION_0_9_PAYLOAD_THRESHOLD = 1024 - PROTOCOL_VERSION_0_9_HEADER_SIZE;
  private static final int PROTOCOL_VERSION_1_0_PAYLOAD_THRESHOLD = 1024 - PROTOCOL_VERSION_1_0_HEADER_SIZE;

  // Take the smaller sequence size of version 1.0 of protocol
  public static final int MAX_SEQ_NUMBER = Byte.MAX_VALUE;

  private final MessageDigest _messageDigest;
  private final String _hostname;
  private final GelfVersion _version;

  public PayloadChunker( final GelfVersion version,
                         final MessageDigest messageDigest,
                         final String hostname )
  {
    _version = version;
    _messageDigest = messageDigest;
    _hostname = hostname;
  }

  /**
   * Converts a payload into a number of full GELF chunks.
   *
   * @param payload The original gzipped payload
   * @return A list of chunks that should be sent to the graylog2 server
   */
  public List<byte[]> chunkIt( final byte[] payload )
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
    return Arrays.copyOf( _messageDigest.digest( digestString ), getMessageIdLength( _version ) );
  }

  private static int getMessageIdLength( final GelfVersion version )
  {
    return version == GelfVersion.V1_0 ? PROTOCOL_VERSION_1_0_ID_LENGTH : PROTOCOL_VERSION_0_9_ID_LENGTH;
  }

  private static int getPayloadThreshold( final GelfVersion version )
  {
    return version == GelfVersion.V1_0 ?
      PROTOCOL_VERSION_1_0_PAYLOAD_THRESHOLD :
      PROTOCOL_VERSION_0_9_PAYLOAD_THRESHOLD;
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

    final int payloadThreshold = getPayloadThreshold( _version );
    final int numFullSubs = payload.length / payloadThreshold;
    final int lastSubLength = payload.length % payloadThreshold;

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
    final int payloadThreshold = getPayloadThreshold( _version );
    return Arrays.copyOfRange( payload, subPaylod * payloadThreshold, ( subPaylod + 1 ) * payloadThreshold );
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
    return concatArrays( concatArrays( concatArrays( CHUNKED_GELF_ID, messageId ), getSeqNumbers( seqNum, numChunks ) ), subPayload );
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
   * Returns an array of sequence numbers for each chunk. Needed because apparently we need to pad this for 0.9.5??
   *
   * @param seqNum    The Sequence Number
   * @param numChunks The number of chunks that will be sent
   * @return An array of sequence numbers for each chunk.
   */
  private byte[] getSeqNumbers( final byte seqNum, final byte numChunks )
  {
    return _version == GelfVersion.V0_9 ? new byte[]{ 0x00, seqNum, 0x00, numChunks } : new byte[]{ seqNum, numChunks };
  }
}
