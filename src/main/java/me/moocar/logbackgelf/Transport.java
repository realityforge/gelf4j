package me.moocar.logbackgelf;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

/**
 * Responsible for sending packet(s) to the graylog2 server
 */
public final class Transport
{
  private final InetAddress _address;
  private final int _port;

  public Transport( final InetAddress address, final int port )
  {
    _port = port;
    _address = address;
  }

  /**
   * Sends a single packet GELF message to the graylog2 server
   *
   * @param data gzipped GELF Message (JSON)
   */
  public void send( final byte[] data )
  {
    sendPacket( new DatagramPacket( data, data.length, _address, _port ) );
  }

  /**
   * Sends a bunch of GELF Chunks to the graylog2 server
   *
   * @param packets The packets to send over the wire
   */
  public void send( final List<byte[]> packets )
  {
    for( final byte[] packet : packets )
    {
      send( packet );
    }
  }

  private void sendPacket( final DatagramPacket datagramPacket )
  {
    final DatagramSocket datagramSocket = getDatagramSocket();
    try
    {
      datagramSocket.send( datagramPacket );
    }
    catch( final IOException ioe )
    {
      throw new RuntimeException( ioe );
    }
    finally
    {
      datagramSocket.close();
    }
  }

  private DatagramSocket getDatagramSocket()
  {
    try
    {
      return new DatagramSocket();
    }
    catch( final SocketException se )
    {
      throw new RuntimeException( se );
    }
  }
}
