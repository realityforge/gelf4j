package gelf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

/**
 * Responsible for communicating with a GELF compliant server.
 */
public class GelfConnection
{
  private final GelfTargetConfig _config;
  private final GelfEncoder _encoder;
  private DatagramSocket _socket;

  protected GelfConnection( final GelfTargetConfig config )
    throws Exception
  {
    _config = config;
    _encoder = new GelfEncoder( InetAddress.getLocalHost().getCanonicalHostName(), _config.isCompressedChunking() );
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
    final GelfMessage gelfMessage = newMessage();
    gelfMessage.setJavaTimestamp( timestamp );
    gelfMessage.setLevel( level );
    gelfMessage.setFullMessage( message );
    gelfMessage.setShortMessage( GelfMessageUtil.truncateShortMessage( message ) );
    return gelfMessage;
  }

  public GelfMessage newMessage()
  {
    final GelfMessage gelfMessage = new GelfMessage();
    for( final Map.Entry<String, Object> entry : _config.getDefaultFields().entrySet() )
    {
      GelfMessageUtil.setValue( gelfMessage, entry.getKey(), entry.getValue() );
    }
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
}
