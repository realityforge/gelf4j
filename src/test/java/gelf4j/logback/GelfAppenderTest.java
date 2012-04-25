package gelf4j.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import gelf4j.GelfMessage;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import gelf4j.TestGelfConnection;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import static org.junit.Assert.*;

public class GelfAppenderTest
{
  public static class TestGelfAppender extends GelfAppender
  {
    static TestGelfAppender c_appender;

    public TestGelfAppender()
    {
      c_appender = this;
    }
  }

  @Test
  public void configureSetsUpLoggerCorrectly()
    throws Exception
  {
    // Assume sl4j bound to logback by default
    final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    final JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext( context );

    // Clean out existing configuration
    context.reset();

    final String hostName = InetAddress.getLocalHost().getCanonicalHostName();
    final String facility = "LOG4J";

    final String configXml =
      "<configuration>\n" +
      "  <appender name=\"GELF\" class=\"" + TestGelfAppender.class.getName() + "\">\n" +
      "    <facility>" + facility + "</facility>\n" +
      "    <host>" + hostName + "</host>\n" +
      "    <originHost>" + hostName + "</originHost>\n" +
      "    <port>1971</port>\n" +
      "    <compressedChunking>false</compressedChunking>\n" +
      "    <additionalFields>{\"threadName\": \"threadName\", \"timestamp_in_millis\": \"timestampMs\", \"logger_name\": \"loggerName\", \"ip_address\": \"ipAddress\", \"exception\": \"exception\"}</additionalFields>\n" +
      "    <additionalData>{\"environment\": \"DEV\", \"application\": \"MyAPP\"}</additionalData>\n" +
      "  </appender>\n" +
      "\n" +
      "  <root level=\"debug\">\n" +
      "    <appender-ref ref=\"GELF\" />\n" +
      "  </root>\n" +
      "</configuration>\n" +
      "";
    configurator.doConfigure( new ByteArrayInputStream( configXml.getBytes() ) );

    //PropertyConfigurator.configure( properties );
    final Logger logger = LoggerFactory.getLogger( "ZipZipHooray");
    final GelfTargetConfig config = TestGelfAppender.c_appender.getConfig();

    assertEquals( hostName, config.getOriginHost() );
    assertEquals( hostName, config.getHost() );
    assertEquals( facility, config.getFacility() );
    assertEquals( 1971, config.getPort() );
    assertEquals( false, config.isCompressedChunking() );
    assertEquals( 2, config.getAdditionalData().size() );
    assertEquals( "DEV", config.getAdditionalData().get( "environment" ) );
    assertEquals( "MyAPP", config.getAdditionalData().get( "application" ) );

    assertEquals( 5, config.getAdditionalFields().size() );
    assertEquals( "threadName", config.getAdditionalFields().get( "threadName" ) );
    assertEquals( "timestampMs", config.getAdditionalFields().get( "timestamp_in_millis" ) );
    assertEquals( "loggerName", config.getAdditionalFields().get( "logger_name" ) );
    assertEquals( "ipAddress", config.getAdditionalFields().get( "ip_address" ) );
    assertEquals( "exception", config.getAdditionalFields().get( "exception" ) );

    // set up mock connection
    final TestGelfConnection connection = new TestGelfConnection( config );
    final Field field = GelfAppender.class.getDeclaredField( "_connection" );
    field.setAccessible( true );
    field.set( TestGelfAppender.c_appender, connection );

    final long then = System.currentTimeMillis();
    final String smallTextMessage = "HELO";
    logger.debug( smallTextMessage );

    GelfMessage message = connection.getLastMessage();
    assertEquals( smallTextMessage, message.getShortMessage() );
    assertEquals( smallTextMessage, message.getFullMessage() );
    assertEquals( SyslogLevel.DEBUG, message.getLevel() );
    assertEquals( facility, message.getFacility() );
    assertEquals( hostName, message.getHostname() );
    assertEquals( null, message.getFile() );
    assertNull( message.getLine() );
    // The message is in the last second
    assertTrue( message.getJavaTimestamp() - then < 1000 );

    assertEquals( logger.getName(), message.getAdditionalFields().get( "logger_name" ) );
    assertEquals( null, message.getAdditionalFields().get( "exception" ) );
    assertEquals( Thread.currentThread().getName(), message.getAdditionalFields().get( "threadName" ) );
    assertTrue( ( (Long) message.getAdditionalFields().get( "timestamp_in_millis" ) ) - then < 1000 );
    assertEquals( null, message.getAdditionalFields().get( "ip_address" ) );

    // now we test the MDC
    MDC.put( "ip_address", "42.42.42.42" );

    logger.info( smallTextMessage );
    message = connection.getLastMessage();
    assertEquals( SyslogLevel.INFO, message.getLevel() );
    assertEquals( "42.42.42.42", message.getAdditionalFields().get( "ip_address" ) );

    logger.error( smallTextMessage, new Exception(  ) );
    message = connection.getLastMessage();
    assertEquals( SyslogLevel.ERR, message.getLevel() );
    assertNotNull( message.getAdditionalFields().get( "exception" ) );

    // Force the closing of the appender
    context.reset();
  }
}
