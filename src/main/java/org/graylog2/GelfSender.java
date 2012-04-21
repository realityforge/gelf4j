package org.graylog2;

import java.net.InetAddress;
import java.util.List;
import me.moocar.logbackgelf.GreylogConnection;

public class GelfSender
{
  public static final int DEFAULT_PORT = 12201;

  private final GreylogConnection _connection;

  public GelfSender( final String host, final int port )
    throws Exception
  {
    _connection = new GreylogConnection( InetAddress.getByName( host ), port );
  }

  public boolean sendMessage( GelfMessage message )
  {
    return sendDatagrams( message.toDatagrams() );
  }

  public boolean sendDatagrams( List<byte[]> bytesList )
  {
    try
    {
      _connection.send( bytesList );
      return true;
    }
    catch( final Throwable t )
    {
      return false;
    }
  }

  public void close()
  {
    _connection.close();
  }
}
