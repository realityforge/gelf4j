package org.graylog2;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import me.moocar.logbackgelf.Transport;

public class GelfSender
{
  public static final int DEFAULT_PORT = 12201;

  private final Transport transport;

  public GelfSender( String host, int port )
    throws UnknownHostException, SocketException
  {
    transport = new Transport( InetAddress.getByName( host ), port );
  }

  public boolean sendMessage( GelfMessage message )
  {
    return message.isValid() && sendDatagrams( message.toDatagrams() );
  }

  public boolean sendDatagrams( List<byte[]> bytesList )
  {
    try
    {
      transport.send( bytesList );
      return true;
    }
    catch( final Throwable t )
    {
      return false;
    }
  }

  public void close()
  {
    transport.close();
  }
}
