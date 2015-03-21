package gelf4j.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import gelf4j.ConnectionUtil;
import gelf4j.GelfMessage;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import gelf4j.TestGelfConnection;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
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

    final int port = 1971;
    final String configXml =
      "<configuration>\n" +
      "  <appender name=\"GELF\" class=\"" + TestGelfAppender.class.getName() + "\">\n" +
      "    <host>" + hostName + "</host>\n" +
      "    <port>" + port + "</port>\n" +
      "    <compressedChunking>false</compressedChunking>\n" +
      "    <additionalFields>{\"threadName\": \"threadName\", \"timestamp_in_millis\": \"timestampMs\", \"logger_name\": \"loggerName\", \"ip_address\": \"ipAddress\", \"exception\": \"exception\", \"coolUserName\": \"userName\"}</additionalFields>\n" +
      "    <defaultFields>{\"environment\": \"DEV\", \"application\": \"MyAPP\", \"facility\": \"" + facility + "\", \"host\":\"" + hostName + "\"}</defaultFields>\n" +
      "  </appender>\n" +
      "\n" +
      "  <root level=\"debug\">\n" +
      "    <appender-ref ref=\"GELF\" />\n" +
      "  </root>\n" +
      "</configuration>\n" +
      "";
    configurator.doConfigure( new ByteArrayInputStream( configXml.getBytes() ) );

    //Setup fake server
    final DatagramSocket socket = ConnectionUtil.createServer( hostName, port );

    final Logger logger = LoggerFactory.getLogger( "ZipZipHooray");
    final GelfTargetConfig config = TestGelfAppender.c_appender.getConfig();

    assertEquals( hostName, config.getHost() );
    assertEquals( port, config.getPort() );
    assertEquals( false, config.isCompressedChunking() );
    assertEquals( 4, config.getDefaultFields().size() );
    assertEquals( hostName, config.getDefaultFields().get( "host" ) );
    assertEquals( facility, config.getDefaultFields().get( "facility" ) );
    assertEquals( "DEV", config.getDefaultFields().get( "environment" ) );
    assertEquals( "MyAPP", config.getDefaultFields().get( "application" ) );

    assertEquals( 6, config.getAdditionalFields().size() );
    assertEquals( "threadName", config.getAdditionalFields().get( "threadName" ) );
    assertEquals( "timestampMs", config.getAdditionalFields().get( "timestamp_in_millis" ) );
    assertEquals( "loggerName", config.getAdditionalFields().get( "logger_name" ) );
    assertEquals( "ipAddress", config.getAdditionalFields().get( "ip_address" ) );
    assertEquals( "exception", config.getAdditionalFields().get( "exception" ) );
    assertEquals( "userName", config.getAdditionalFields().get( "coolUserName" ) );

    // set up mock connection
    final TestGelfConnection connection = new TestGelfConnection( config );
    final Field field = GelfAppender.class.getDeclaredField( "_connection" );
    field.setAccessible( true );
    field.set( TestGelfAppender.c_appender, connection );

    final long then = System.currentTimeMillis();
    final String smallTextMessage = "HELO";
    logger.debug( smallTextMessage );
    assertTrue( ConnectionUtil.receivePacketAsString( socket ).contains( smallTextMessage ) );

    GelfMessage message = connection.getLastMessage();
    assertEquals( smallTextMessage, message.getShortMessage() );
    assertEquals( smallTextMessage, message.getFullMessage() );
    assertEquals( SyslogLevel.DEBUG, message.getLevel() );
    assertEquals( facility, message.getFacility() );
    assertEquals( hostName, message.getHost() );
    assertEquals( null, message.getFile() );
    assertNull( message.getLine() );
    // The message is in the last second
    assertTrue( message.getJavaTimestamp() - then < 1000 );

    assertEquals( logger.getName(), message.getAdditionalFields().get( "logger_name" ) );
    assertEquals( null, message.getAdditionalFields().get( "exception" ) );
    assertEquals( Thread.currentThread().getName(), message.getAdditionalFields().get( "threadName" ) );
    assertTrue( ( (Long) message.getAdditionalFields().get( "timestamp_in_millis" ) ) - then < 1000 );
    assertEquals( null, message.getAdditionalFields().get( "ip_address" ) );

    assertEquals( "DEV", message.getAdditionalFields().get( "environment" ) );
    assertEquals( "MyAPP", message.getAdditionalFields().get( "application" ) );

    // now we test the MDC
    MDC.put( "ip_address", "42.42.42.42" );

    logger.info( smallTextMessage );
    assertTrue( ConnectionUtil.receivePacketAsString( socket ).contains( smallTextMessage ) );
    message = connection.getLastMessage();
    assertEquals( SyslogLevel.INFO, message.getLevel() );
    assertEquals( "42.42.42.42", message.getAdditionalFields().get( "ip_address" ) );

    //now we test the MDC with custom message key
    MDC.put( "userName", "johnsmith" );

    logger.info( smallTextMessage );
    assertTrue( ConnectionUtil.receivePacketAsString( socket ).contains( smallTextMessage ) );
    message = connection.getLastMessage();
    assertEquals( SyslogLevel.INFO, message.getLevel() );
    assertEquals( "johnsmith", message.getAdditionalFields().get( "coolUserName" ) );

    logger.error( smallTextMessage, new Exception() );
    assertTrue( ConnectionUtil.receivePacketAsString( socket ).contains( smallTextMessage ) );
    message = connection.getLastMessage();
    {
      assertEquals( SyslogLevel.ERR, message.getLevel() );
      final String exception = (String) message.getAdditionalFields().get( "exception" );
      assertNotNull( exception );
      assertTrue( exception.contains( "java.lang.Exception\n" ) );
      assertTrue( exception.contains( "\tat gelf4j.logback.GelfAppenderTest.configureSetsUpLoggerCorrectly(" ) );
    }

    logger.error( smallTextMessage, new Exception( "MyError", new IllegalStateException() ) );
    assertTrue( ConnectionUtil.receivePacketAsString( socket ).contains( smallTextMessage ) );
    message = connection.getLastMessage();
    {
      assertEquals( SyslogLevel.ERR, message.getLevel() );
      final String exception = (String) message.getAdditionalFields().get( "exception" );
      assertNotNull( exception );
      assertTrue( exception.contains( "java.lang.Exception: MyError\n" ) );
      assertTrue( exception.contains( "Caused by: java.lang.IllegalStateException\n" ) );
    }

    // Force the closing of the appender
    context.reset();
  }
}
