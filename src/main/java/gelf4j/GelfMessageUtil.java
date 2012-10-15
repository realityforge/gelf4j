package gelf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A set of utility methods for constructing the Gelf messages.
 */
public final class GelfMessageUtil
{
  static final int MAX_SHORT_MESSAGE_LENGTH = 250;
  private static final Map<String, DateFormat> c_formats = new WeakHashMap<String, DateFormat>();

  private GelfMessageUtil()
  {
  }

  public static String extractStacktrace( final Throwable throwable )
  {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter( sw );
    throwable.printStackTrace( pw );
    return sw.toString();
  }

  static SyslogLevel parseLevel( final String level )
  {
    try
    {
      return SyslogLevel.values()[ Integer.parseInt( level ) ];
    }
    catch( final Exception e )
    {
      try
      {
        return SyslogLevel.valueOf( level );
      }
      catch( final Exception e2 )
      {
        return null;
      }
    }
  }

  @SuppressWarnings( "SynchronizationOnLocalVariableOrMethodParameter" )
  private static long parseTimestamp( final String format, final String time )
  {
    try
    {
      final DateFormat dateFormat = getDateFormat( format );
      synchronized( dateFormat )
      {
        return dateFormat.parse( time ).getTime();
      }
    }
    catch( final Exception e )
    {
      return 0;
    }
  }

  private static DateFormat getDateFormat( final String format )
  {
    synchronized( c_formats )
    {
      DateFormat dateFormat = c_formats.get( format );
      if( null == dateFormat )
      {
        dateFormat = new SimpleDateFormat( format );
        c_formats.put( format, dateFormat );
      }
      return dateFormat;
    }
  }

  public static void setValue( final GelfMessage message, final String key, final Object value )
  {
    if( key.equals( GelfTargetConfig.FIELD_LEVEL ) )
    {
      final SyslogLevel level = parseLevel( String.valueOf( value ) );
      message.setLevel( null != level ? level : SyslogLevel.INFO );
    }
    else if( key.equals( GelfTargetConfig.FIELD_FACILITY ) )
    {
      message.setFacility( String.valueOf( value ) );
    }
    else if( key.startsWith( GelfTargetConfig.FIELD_TIMESTAMP_PREFIX ) )
    {
      final String format = key.substring( GelfTargetConfig.FIELD_TIMESTAMP_PREFIX.length() );
      final long timestamp = parseTimestamp( format, String.valueOf( value ) );
      if( 0 != timestamp )
      {
        message.setJavaTimestamp( timestamp );
      }
    }
    else if( key.equals( GelfTargetConfig.FIELD_LINE ) )
    {
      try
      {
        message.setLine( Integer.parseInt( String.valueOf( value ) ) );
      }
      catch( final NumberFormatException nfe )
      {
        //Ignore
      }
    }
    else if( key.equals( GelfTargetConfig.FIELD_FILE ) )
    {
      message.setFile( String.valueOf( value ) );
    }
    else if( key.equals( GelfTargetConfig.FIELD_HOST ) )
    {
      message.setHost( String.valueOf( value ) );
    }
    else if( key.equals( GelfTargetConfig.FIELD_MESSAGE ) )
    {
      final String textMessage = String.valueOf( value );
      message.setShortMessage( truncateShortMessage( textMessage ) );
      message.setFullMessage( textMessage );
    }
    else
    {
      message.getAdditionalFields().put( key, value );
    }
  }

  public static String truncateShortMessage( final String message )
  {
    if( message.length() > MAX_SHORT_MESSAGE_LENGTH )
    {
      return message.substring( 0, MAX_SHORT_MESSAGE_LENGTH );
    }
    else
    {
      return message;
    }
  }

  static String getLocalHost()
    throws IOException
  {
    try
    {
      return InetAddress.getLocalHost().getCanonicalHostName();
    }
    catch( final UnknownHostException uhe )
    {
      final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while( interfaces.hasMoreElements() )
      {
        final NetworkInterface networkInterface = interfaces.nextElement();
        if( null != networkInterface )
        {
          final Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
          if( addresses.hasMoreElements() )
          {
            return addresses.nextElement().getHostAddress();
          }
        }
      }
      throw uhe;
    }
  }
}
