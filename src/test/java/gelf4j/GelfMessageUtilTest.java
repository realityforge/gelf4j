package gelf4j;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfMessageUtilTest
{
  @Test
  public void parseLevel()
    throws Exception
  {
    assertEquals( SyslogLevel.DEBUG, GelfMessageUtil.parseLevel( "7" ) );
    assertEquals( SyslogLevel.DEBUG, GelfMessageUtil.parseLevel( "DEBUG" ) );
    assertEquals( SyslogLevel.EMERG, GelfMessageUtil.parseLevel( "0" ) );
    assertEquals( null, GelfMessageUtil.parseLevel( "XXX" ) );
  }

  @Test
  public void truncateShortMessage()
    throws Exception
  {
    final String shortMessage = createString( 10 );
    assertEquals( 10, GelfMessageUtil.truncateShortMessage( shortMessage ).length() );
    final String longMessage = createString( 10000 );
    final String truncatedLongMessage = GelfMessageUtil.truncateShortMessage( longMessage );
    assertEquals( 250, truncatedLongMessage.length() );
    assertEquals( true, longMessage.startsWith( truncatedLongMessage ) );
  }

  private String createString( final int byteCount )
  {
    final Random random = new Random( 723 );
    final StringBuilder sb = new StringBuilder();
    for( int i = 0; i < byteCount; i++ )
    {
      sb.append( (char) random.nextInt() );
    }
    return sb.toString();
  }
}
