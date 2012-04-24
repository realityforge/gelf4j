package gelf4j;

import java.net.InetAddress;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfTargetConfigTest
{
  @Test
  public void validateDefaults()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();
    assertEquals( InetAddress.getLocalHost().getCanonicalHostName(), config.getOriginHost() );
    assertEquals( InetAddress.getLocalHost().getCanonicalHostName(), config.getHost() );
    assertEquals( InetAddress.getLocalHost().getCanonicalHostName(), config.getHostAddress().getCanonicalHostName() );
    assertEquals( null, config.getFacility() );
    assertEquals( 12201, config.getPort() );
    assertEquals( true, config.isCompressedChunking() );
    assertEquals( 0, config.getAdditionalData().size() );

    assertEquals( 4, config.getAdditionalFields().size() );
    assertEquals( "exception", config.getAdditionalFields().get( "exception" ) );
    assertEquals( "threadName", config.getAdditionalFields().get( "threadName" ) );
    assertEquals( "loggerName", config.getAdditionalFields().get( "loggerName" ) );
    assertEquals( "timestampMs", config.getAdditionalFields().get( "timestampMs" ) );
  }

  @Test
  public void setAdditionalFields()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();

    assertEquals( 4, config.getAdditionalFields().size() );
    config.setAdditionalFields("{\"foo\":\"bar\"}");
    assertEquals( 1, config.getAdditionalFields().size() );
    assertEquals( "bar", config.getAdditionalFields().get( "foo" ) );
  }

  @Test
  public void setAdditionalData()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();

    assertEquals( 0, config.getAdditionalData().size() );
    config.setAdditionalData("{\"foo\":1}");
    assertEquals( 1, config.getAdditionalData().size() );
    config.setAdditionalData("{\"foo2\":\"x\",\"baz\":7}");
    assertEquals( 2, config.getAdditionalData().size() );
    assertEquals( 7L, config.getAdditionalData().get( "baz" ) );
    assertEquals( "x", config.getAdditionalData().get( "foo2" ) );
  }
}
