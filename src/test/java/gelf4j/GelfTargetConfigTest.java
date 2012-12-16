package gelf4j;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfTargetConfigTest
{
  @Test
  public void validateDefaults()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();
    assertEquals( InetAddress.getLocalHost().getCanonicalHostName(), config.getHost() );
    assertEquals( InetAddress.getLocalHost().getCanonicalHostName(), config.getHostAddress().getCanonicalHostName() );
    assertEquals( 12201, config.getPort() );
    assertEquals( true, config.isCompressedChunking() );
    assertEquals( 1, config.getDefaultFields().size() );
    assertEquals( InetAddress.getLocalHost().getCanonicalHostName(), config.getDefaultFields().get( "host" ) );

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
    config.setAdditionalFields( "{\"foo\":\"bar\"}" );
    assertEquals( 1, config.getAdditionalFields().size() );
    assertEquals( "bar", config.getAdditionalFields().get( "foo" ) );
  }

  @Test
  public void setDefaultFields()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();

    assertEquals( 1, config.getDefaultFields().size() );
    config.setDefaultFields( "{\"foo\":1}" );
    assertEquals( 1, config.getDefaultFields().size() );
    config.setDefaultFields( "{\"foo2\":\"x\",\"baz\":7}" );
    assertEquals( 2, config.getDefaultFields().size() );
    assertEquals( 7L, config.getDefaultFields().get( "baz" ) );
    assertEquals( "x", config.getDefaultFields().get( "foo2" ) );
  }

  @Test
  public void setJsonCodecClass()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();
    config.setCodecClass( "gelf4j.MockJsonCodec" );
    config.setDefaultFields( "{ \"foo\":10, \"bar\": { \"baz\": \"bingo\", \"otherBaz\": 10 } }" );

    assertEquals( MockJsonCodec.class, config.getCodec().getClass() );
    assertEquals( 1, config.getDefaultFields().size() );
    assertEquals( 10, config.getDefaultFields().get( "foo" ) );
  }

  @Test
  public void setUnknownJsonCodecClass()
    throws Exception
  {
    final GelfTargetConfig config = new GelfTargetConfig();
    config.setCodecClass( "blah.blah.UnknownJsonCodec" );

    assertEquals( SimpleJsonCodec.class, config.getCodec().getClass() );
  }

  static final class MockJsonCodec
    implements JsonCodec
  {
    public String toJson( Object object )
    {
      // always return mock data
      return "{ \"foo\":10, \"bar\": { \"baz\": \"bingo\", \"otherBaz\": 10 } }";
    }

    @SuppressWarnings( "unchecked" )
    public <T> T fromJson( String json, Class<T> type )
    {
      final Map<String, Object> mockResult = new HashMap<String, Object>();
      mockResult.put( "foo", 10 );
      return (T) mockResult;
    }
  }
}
