package org.graylog2;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A set of utility methods for costructing the Gelf messages.
 */
public final class GelfMessageUtil
{
  private static final int MAX_SHORT_MESSAGE_LENGTH = 250;

  private GelfMessageUtil()
  {
  }

  public static GelfMessage newMessage( final GelfTargetConfig config,
                                        final SyslogLevel level,
                                        final String message,
                                        final long timestamp )
  {
    final GelfMessage gelfMessage = new GelfMessage();
    gelfMessage.setHostname( config.getOriginHost() );
    gelfMessage.setFacility( config.getFacility() );
    gelfMessage.setJavaTimestamp( timestamp );
    gelfMessage.setLevel( level );
    gelfMessage.setFullMessage( message );
    gelfMessage.setShortMessage( truncateShortMessage( message ) );
    return gelfMessage;
  }

  public static String extractStacktrace( final Throwable throwable )
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    throwable.printStackTrace( pw );
    return sw.toString();
  }

  private static String truncateShortMessage( final String message )
  {
    final String shortMessage;
    if( message.length() > MAX_SHORT_MESSAGE_LENGTH )
    {
      shortMessage = message.substring( 0, MAX_SHORT_MESSAGE_LENGTH - 1 );
    }
    else
    {
      shortMessage = message;
    }
    return shortMessage;
  }
}
