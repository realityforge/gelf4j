package gelf4j;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A set of utility methods for constructing the Gelf messages.
 */
public final class GelfMessageUtil
{
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
}
