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
}
