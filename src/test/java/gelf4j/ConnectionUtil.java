package gelf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class ConnectionUtil
{
  private ConnectionUtil()
  {
  }

  public static DatagramSocket createServer( final String host, final int port )
    throws SocketException
  {
    return new DatagramSocket( new InetSocketAddress( host, port ) );
  }

  public static DatagramPacket receivePacket( final DatagramSocket socket )
    throws IOException
  {
    final DatagramPacket packet = newPacket();
    socket.receive( packet );
    return packet;
  }

  private static DatagramPacket newPacket()
  {
    final byte[] data = new byte[ 1024 * 8 ];
    return new DatagramPacket( data, data.length );
  }

}
