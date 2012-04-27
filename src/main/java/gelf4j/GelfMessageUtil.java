package gelf4j;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A set of utility methods for constructing the Gelf messages.
 */
public final class GelfMessageUtil
{
  static final int MAX_SHORT_MESSAGE_LENGTH = 250;

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

  public static SyslogLevel parseLevel( final String level )
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
      final String textMessage = String.valueOf( message );
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
      return  message;
    }
  }
}
