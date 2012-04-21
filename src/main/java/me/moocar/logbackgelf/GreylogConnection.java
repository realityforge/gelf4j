package me.moocar.logbackgelf;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.List;

/**
 * Responsible for sending packet(s) to the graylog2 server
 */
public final class GreylogConnection
{
  private final GelfVersion _version;
  private final InetAddress _address;
  private final int _port;
  private DatagramSocket _socket;
  private final String _hostName;
  private final GelfEncoder _encoder;

  public GreylogConnection( final GelfVersion version,
                            final InetAddress address,
                            final int port )
    throws Exception
  {
    _version = version;
    _port = port;
    _address = address;
    _hostName = InetAddress.getLocalHost().getHostName();
    _encoder = new GelfEncoder( version, MessageDigest.getInstance( "MD5" ), _hostName );
  }

  /**
   * Sends a single GELF message to the graylog2 server
   *
   * @param message the GELF Message (JSON)
   * @return false if sending failed
   */
  public boolean send( final String message )
  {
    return send( _encoder.encode( message ) );
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
