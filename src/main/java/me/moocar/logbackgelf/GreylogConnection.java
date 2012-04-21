package me.moocar.logbackgelf;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

/**
 * Responsible for communicating with a graylog2 server
 */
public class GreylogConnection
{
  public static final int DEFAULT_PORT = 12201;
  private final GelfEncoder _encoder;
  private final InetAddress _address;
  private final int _port;
  private DatagramSocket _socket;

  public GreylogConnection( final InetAddress address,
                            final int port )
    throws Exception
  {
    _port = port;
    _address = address;
    _encoder = new GelfEncoder();
  }

  /**
   * Sends a single GELF message to the graylog2 server
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
   * Sends a bunch of GELF Chunks to the graylog2 server
   *
   * @param packets The packets to send over the wire
   * @return false if sending failed
   */
  public boolean send( final List<byte[]> packets )
  {
    for( final byte[] packet : packets )
    {
      if( !sendPacket( new DatagramPacket( packet, packet.length, _address, _port ) ) )
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

  public void close()
  {
    if( null != _socket )
    {
      _socket.close();
      _socket = null;
    }
  }
}
