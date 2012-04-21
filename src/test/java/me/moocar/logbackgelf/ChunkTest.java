package me.moocar.logbackgelf;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChunkTest
{
  @Test
  public void test1ByteMoreThanThreshold()
    throws Exception
  {
    final int byteCount = GelfEncoder.PAYLOAD_THRESHOLD + 1;

    final List<byte[]> packets = encoder().encode( createGelfMessage( byteCount ) );
    assertNotNull( packets );
    assertEquals( 2, packets.size() );

    final byte[] packet1 = packets.get( 0 );
    assertEquals( GelfEncoder.HEADER_SIZE + GelfEncoder.PAYLOAD_THRESHOLD, packet1.length );
    assertEquals( GelfEncoder.HEADER_SIZE + 1, packets.get( 1 ).length );

    assertArrayEquals( GelfEncoder.CHUNKED_GELF_ID, Arrays.copyOfRange( packet1, 0, GelfEncoder.CHUNKED_GELF_ID.length ) );

    int count = 0;
    for( final byte[] packet : packets )
    {
      assertEquals( count, getSeqNumber( packets, count ) );
      assertEquals( 2, getNumChunks( packet ) );
      count++;
    }

  }

  @Test
  public void testThreeChunks()
    throws Exception
  {
    final int byteCount = 3 * GelfEncoder.PAYLOAD_THRESHOLD;

    final List<byte[]> packets = encoder().encode( createGelfMessage( byteCount ) );
    assertNotNull( packets );
    assertEquals( 3, packets.size() );

    for( final byte[] packet : packets )
    {
      assertEquals( GelfEncoder.PAYLOAD_THRESHOLD + GelfEncoder.HEADER_SIZE, packet.length );
    }
  }

  @Test
  public void testMessageIdsDifferent()
    throws Exception
  {
    final List<byte[]> packets1 = encoder().encode( createGelfMessage( 1 ) );
    final List<byte[]> packets2 = encoder().encode( createGelfMessage( 1 ) );

    byte[] messageId1 = Arrays.copyOfRange( packets1.get( 0 ), 2, 10 );
    byte[] messageId2 = Arrays.copyOfRange( packets2.get( 0 ), 2, 10 );

    assertFalse( Arrays.equals( messageId1, messageId2 ) );
  }

  @Test
  public void shouldCutoffAfterMaxChunks()
    throws Exception
  {
    final int byteCount = ( GelfEncoder.MAX_SEQ_NUMBER + 2 ) * GelfEncoder.PAYLOAD_THRESHOLD;

    final List<byte[]> packets = encoder().encode( createGelfMessage( byteCount ) );
    assertNull( packets );
  }

  private GelfMessage createGelfMessage( final int byteCount )
  {
    final GelfMessage message = new GelfMessage();
    message.setShortMessage( "X" );
    message.setFullMessage( createMessage( byteCount ) );
    return message;
  }

  private String createMessage( final int byteCount )
  {
    final StringBuilder sb = new StringBuilder();
    for( int i = 0; i < byteCount; i++ )
    {
      sb.append( "z" );
    }
    return sb.toString();
  }

  private int getNumChunks( byte[] packet )
  {
    return packet[ GelfEncoder.CHUNKED_GELF_ID.length +
                   GelfEncoder.MESSAGE_ID_LENGTH +
                   GelfEncoder.SEQUENCE_LENGTH +
                   GelfEncoder.SEQUENCE_LENGTH -
                   1 ];
  }

  private int getSeqNumber( List<byte[]> packets, int packetNum )
  {
    return packets.get( packetNum )[ GelfEncoder.CHUNKED_GELF_ID.length +
                                     GelfEncoder.MESSAGE_ID_LENGTH +
                                     GelfEncoder.SEQUENCE_LENGTH -
                                     1 ];
  }

  private GelfEncoder encoder()
    throws Exception
  {
    return new GelfEncoder();
  }
}
