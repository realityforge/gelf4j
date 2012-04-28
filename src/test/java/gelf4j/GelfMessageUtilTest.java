package gelf4j;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfMessageUtilTest
{
  @Test
  public void setValue()
    throws Exception
  {
    final GelfMessage message = new GelfMessage();
    GelfMessageUtil.setValue( message, GelfTargetConfig.FIELD_LEVEL, "WARNING" );
    assertEquals( SyslogLevel.WARNING, message.getLevel() );

    GelfMessageUtil.setValue( message, GelfTargetConfig.FIELD_FACILITY, "BINK" );
    assertEquals( "BINK", message.getFacility() );

    GelfMessageUtil.setValue( message, GelfTargetConfig.FIELD_LINE, "222" );
    assertEquals( (Long)222L, message.getLine() );

    GelfMessageUtil.setValue( message, GelfTargetConfig.FIELD_FILE, "BINKBINK" );
    assertEquals( "BINKBINK", message.getFile() );

    GelfMessageUtil.setValue( message, GelfTargetConfig.FIELD_MESSAGE, "ZANGZANGZANGZANGZANG" );
    assertEquals( "ZANGZANGZANGZANGZANG", message.getShortMessage() );
    assertEquals( "ZANGZANGZANGZANGZANG", message.getFullMessage() );

    GelfMessageUtil.setValue( message, GelfTargetConfig.FIELD_HOST, "foo.com" );
    assertEquals( "foo.com", message.getHost() );
  }

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
