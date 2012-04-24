package gelf4j;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfConnectionTest
{
  @Test
  public void ensureBasicSendWorksAsExpected()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();
    config.setPort( 1977 );
    config.setOriginHost( "Zoon" );
    config.setFacility( "OILZ" );

    final InetSocketAddress address = new InetSocketAddress( config.getHostAddress(), config.getPort() );
    final DatagramSocket socket = new DatagramSocket( address );
    try
    {

      final GelfConnection connection = new GelfConnection( config );

      //Make sure close can be called before a send
      connection.close();

      final SyslogLevel level = SyslogLevel.EMERG;
      final String textMessage = "Nightmare faces looking at me, and I can see now";
      final long timestamp = System.currentTimeMillis();
      final GelfMessage message1 = connection.newMessage( level, textMessage, timestamp );
      assertEquals( "Zoon", message1.getHostname() );
      assertEquals( "OILZ", message1.getFacility() );
      assertEquals( textMessage, message1.getShortMessage() );
      assertEquals( textMessage, message1.getFullMessage() );
      assertEquals( level, message1.getLevel() );
      assertEquals( (Long) timestamp, message1.getJavaTimestamp() );

      final boolean sent1 = connection.send( message1 );
      assertTrue( sent1 );

      final DatagramPacket packet = newPacket();

      socket.receive( packet );
      //Get the first two bytes of received message and compare it to magic numbers of gzip
      assertArrayEquals( new byte[]{ 0x1F, (byte) 0x8B }, Arrays.copyOfRange( packet.getData(), packet.getOffset(), packet.getOffset() + 2 ) );

      // Create a big enough message that it will require chunking
      final String textMessage2 = createString( 18823 );

      final GelfMessage message2 = connection.newMessage( level, textMessage2, timestamp );

      //The really large message should have been truncated for the short_message field
      assertEquals( textMessage2.substring( 0, GelfConnection.MAX_SHORT_MESSAGE_LENGTH ), message2.getShortMessage() );
      assertEquals( textMessage2, message2.getFullMessage() );

      final boolean sent2 = connection.send( message2 );
      assertTrue( sent2 );

      socket.receive( packet );
      //Get the first two bytes of received message and compare it to magic numbers of gzip
      assertArrayEquals( new byte[]{ 0x1e, 0x0f }, Arrays.copyOfRange( packet.getData(), packet.getOffset(), packet.getOffset() + 2 ) );

      connection.close();

      //Make sure close can be called multiple times in a row
      connection.close();
    }
    finally
    {
      socket.close();
    }
  }

  private String createString( final int byteCount )
  {
    final Random random = new Random( 723 );
    final StringBuilder sb = new StringBuilder();
    for( int i = 0; i < byteCount; i++ )
    {
      sb.append( (char) random.nextInt() );
    }
    return sb.toString();
  }

  private DatagramPacket newPacket()
  {
    final byte[] data = new byte[ 1024 * 8 ];
    return new DatagramPacket( data, data.length );
  }

  static class TestGelfConnection
    extends GelfConnection
  {
    private GelfMessage _lastMessage;

    public TestGelfConnection( final GelfTargetConfig config )
      throws Exception
    {
      super( config );
    }

    @Override
    protected boolean performSend( final GelfMessage message )
    {
      this._lastMessage = message;
      return super.performSend( message );
    }

    public GelfMessage getLastMessage()
    {
      return _lastMessage;
    }
  }
}
