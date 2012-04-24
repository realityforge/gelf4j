package gelf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfEncoderTest
{
  @Test
  public void generateMessageID()
    throws Exception
  {
    assertEquals( 8, encoder( true ).generateMessageID().length );
    assertEquals( 32, encoder( false ).generateMessageID().length );
  }

  @Test
  public void ensureTooLargePayloadIsDropped()
    throws Exception
  {
    final int payloadSize = GelfEncoder.PAYLOAD_THRESHOLD * (GelfEncoder.MAX_SEQ_NUMBER + 1);
    final byte[] payload = createData( payloadSize );
    final List<byte[]> packets = encoder( true ).createPackets( payload );
    assertNull( packets );
  }

  @Test
  public void ensurePayloadUnderThresholdCreatesASinglePacket()
    throws Exception
  {
    final int payloadSize = GelfEncoder.MAX_PACKET_SIZE;
    final byte[] payload = createData( payloadSize );
    final List<byte[]> packets = encoder( true ).createPackets( payload );
    assertEquals( 1, packets.size() );
    assertArrayEquals( packets.get( 0 ), payload );
  }

  @Test
  public void ensurePayloadOverThresholdCreatesMultipleChunks()
    throws Exception
  {
    ensurePayloadOverThresholdCreatesMultipleChunks( true );
    ensurePayloadOverThresholdCreatesMultipleChunks( false );
  }

  private void ensurePayloadOverThresholdCreatesMultipleChunks( final boolean compressed )
    throws Exception
  {
    final int payloadSize = GelfEncoder.MAX_PACKET_SIZE + 1;
    final byte[] payload = createData( payloadSize );
    final List<byte[]> packets = encoder( compressed ).createPackets( payload );
    assertEquals( 2, packets.size() );

    final LinkedList<byte[]> messageIDs = new LinkedList<byte[]>();
    final LinkedList<byte[]> payloadData = new LinkedList<byte[]>();

    expectChunk( compressed, packets.get( 0 ), 0, 2, messageIDs, payloadData );
    expectChunk( compressed, packets.get( 1 ), 1, 2, messageIDs, payloadData );

    // Make sure that the message ids are the same across chunks
    assertArrayEquals( messageIDs.get( 0 ), messageIDs.get( 1 ) );

    // Make sure the content that comes through is the entire packet
    final byte[] payload1 = payloadData.get( 0 );
    final byte[] payload2 = payloadData.get( 1 );

    assertEquals( payload1.length + payload2.length, payload.length );
    assertArrayEquals( payload1, Arrays.copyOfRange( payload, 0, payload1.length ) );
    assertArrayEquals( payload2, Arrays.copyOfRange( payload, payload1.length, payload1.length + payload2.length ) );
  }

  private void expectChunk( final boolean compressed,
                            final byte[] packet,
                            final int sequence,
                            final int chunkCount,
                            final LinkedList<byte[]> messageIDs,
                            final LinkedList<byte[]> payload )
    throws IOException
  {
    int start = 0;
    final byte[] chunkID = { 0x1e, 0x0f };
    start = start + chunkID.length;

    final int messageIDLength = compressed ? 8 : 32;
    messageIDs.add( Arrays.copyOfRange( packet, start, start + messageIDLength ) );
    start += messageIDLength;

    if( !compressed )
    {
      assertEquals( 0, packet[ start++ ] );
    }
    assertEquals( sequence, packet[ start++ ] );
    if( !compressed )
    {
      assertEquals( 0, packet[ start++ ] );
    }
    assertEquals( chunkCount, packet[ start++ ] );

    payload.add( Arrays.copyOfRange( packet, start, packet.length ) );
  }

  private String createString( final int byteCount )
  {
    final StringBuilder sb = new StringBuilder();
    for( int i = 0; i < byteCount; i++ )
    {
      sb.append( (char) ( 'a' + ( i % 26 ) ) );
    }
    return sb.toString();
  }

  private byte[] createData( final int byteCount )
  {
    return createString( byteCount ).getBytes();
  }

  private GelfEncoder encoder( final boolean compressed )
    throws Exception
  {
    return new GelfEncoder( "localhost", compressed );
  }
}
