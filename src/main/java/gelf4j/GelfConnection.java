package gelf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Map;

/**
 * Responsible for communicating with a GELF compliant server.
 */
public class GelfConnection
{
  private final GelfTargetConfig _config;
  private final GelfEncoder _encoder;
  private DatagramChannel _channel;

  protected GelfConnection( final GelfTargetConfig config )
    throws Exception
  {
    _config = config;
    _encoder = new GelfEncoder( GelfMessageUtil.getLocalHost(), _config.isCompressedChunking(), _config.getCodec() );
  }

  public void close()
    throws IOException
  {
    if ( null != _channel )
    {
      try
      {
        _channel.close();
      }
      finally
      {
        _channel = null;
      }
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
    for ( final Map.Entry<String, Object> entry : _config.getDefaultFields().entrySet() )
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
    for ( final byte[] packet : packets )
    {
      if ( !sendPacket( packet ) )
      {
        return false;
      }
    }
    return true;
  }

  private boolean sendPacket( final byte[] packet )
  {
    try
    {
      final ByteBuffer buffer = ByteBuffer.allocate( packet.length );
      buffer.put( packet );
      buffer.flip();
      getChannel().write( buffer );
      return true;
    }
    catch ( final IOException ioe )
    {
      return false;
    }
  }

  private DatagramChannel getChannel()
    throws IOException
  {
    if ( null == _channel )
    {
      _channel = DatagramChannel.open();
      _channel.socket().bind( new InetSocketAddress( 0 ) );
      _channel.connect( new InetSocketAddress( _config.getHostAddress(), _config.getPort() ) );
      _channel.configureBlocking( false );
    }
    return _channel;
  }
}
