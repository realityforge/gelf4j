package gelf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.List;

/**
 * Responsible for communicating with a GELF compliant server.
 */
public class GelfConnection
{
  private static final int MAX_SHORT_MESSAGE_LENGTH = 250;

  private final GelfTargetConfig _config;
  private final GelfEncoder _encoder;
  private DatagramSocket _socket;

  public GelfConnection( final GelfTargetConfig config )
    throws Exception
  {
    _config = config;
    _encoder = new GelfEncoder( MessageDigest.getInstance( "MD5" ), _config.getOriginHost(), _config.isCompressedChunking() );
  }

  public void close()
  {
    if( null != _socket )
    {
      _socket.close();
      _socket = null;
    }
  }

  public GelfMessage newMessage( final SyslogLevel level,
                                 final String message,
                                 final long timestamp )
  {
    final GelfMessage gelfMessage = new GelfMessage();
    gelfMessage.setHostname( _config.getOriginHost() );
    gelfMessage.setFacility( _config.getFacility() );
    gelfMessage.setJavaTimestamp( timestamp );
    gelfMessage.setLevel( level );
    gelfMessage.setFullMessage( message );
    gelfMessage.setShortMessage( truncateShortMessage( message ) );
    return gelfMessage;
  }

  /**
   * Sends a single GELF message to the server.
   *
   * @param message the GELF Message
   * @return false if sending failed
   */
  public boolean send( final GelfMessage message )
  {
    message.getAdditionalFields().putAll( _config.getAdditionalData() );
    final List<byte[]> packets = _encoder.encode( message );
    // Note: Returning false when encoding fails for whatever reason
    return null != packets && send( packets );
  }

  /**
   * Sends a bunch of GELF Chunks to the server
   *
   * @param packets The packets to send over the wire
   * @return false if sending failed
   */
  private boolean send( final List<byte[]> packets )
  {
    for( final byte[] packet : packets )
    {
      if( !sendPacket( new DatagramPacket( packet, packet.length, _config.getHostAddress(), _config.getPort() ) ) )
      {
        return false;
      }
    }
    return true;
  }

  private boolean sendPacket( final DatagramPacket datagramPacket )
  {
    try
    {
      getSocket().send( datagramPacket );
      return true;
    }
    catch( final IOException ioe )
    {
      return false;
    }
  }

  private DatagramSocket getSocket()
  {
    try
    {
      if( null == _socket )
      {
        _socket = new DatagramSocket();
      }
      return _socket;
    }
    catch( final SocketException se )
    {
      throw new RuntimeException( se );
    }
  }

  private static String truncateShortMessage( final String message )
  {
    final String shortMessage;
    if( message.length() > MAX_SHORT_MESSAGE_LENGTH )
    {
      shortMessage = message.substring( 0, MAX_SHORT_MESSAGE_LENGTH - 1 );
    }
    else
    {
      shortMessage = message;
    }
    return shortMessage;
  }
}
