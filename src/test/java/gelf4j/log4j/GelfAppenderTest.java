package gelf4j.log4j;

import gelf4j.ConnectionUtil;
import gelf4j.GelfMessage;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import gelf4j.TestGelfConnection;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfAppenderTest
{
  @Test
  public void configureSetsUpLoggerCorrectly()
    throws Exception
  {
    final Properties properties = new Properties();

    properties.setProperty( "log4j.appender.gelf", GelfAppender.class.getName() );
    final String hostName = InetAddress.getLocalHost().getCanonicalHostName();
    properties.setProperty( "log4j.appender.gelf.host", hostName );
    properties.setProperty( "log4j.appender.gelf.port", "1971" );
    final String facility = "LOG4J";
    properties.setProperty( "log4j.appender.gelf.compressedChunking", "false" );
    properties.setProperty( "log4j.appender.gelf.defaultFields",
                            "{\"environment\": \"DEV\", " +
                            "\"application\": \"MyAPP\", " +
                            "\"host\": \"" + hostName + "\", " +
                            "\"facility\": \"" + facility + "\"}" );
    properties.setProperty( "log4j.appender.gelf.additionalFields",
                            "{\"threadName\": \"threadName\", \"timestamp_in_millis\": \"timestampMs\", \"logger_name\": \"loggerName\", \"ip_address\": \"ipAddress\", \"exception\": \"exception\", \"loggerNdc\": \"loggerNdc\", \"coolUserName\": \"userName\"}" );

    properties.setProperty( "log4j.rootLogger", "DEBUG, gelf" );

    PropertyConfigurator.configure( properties );
    final Logger logger = Logger.getLogger( "ZipZipHooray");
    final Appender appender = Logger.getRootLogger().getAppender( "gelf" );
    assertTrue( appender instanceof GelfAppender );
    final GelfTargetConfig config = ( (GelfAppender) appender ).getConfig();

    assertEquals( hostName, config.getHost() );
    final int port = 1971;
    assertEquals( port, config.getPort() );
    assertEquals( false, config.isCompressedChunking() );
    assertEquals( 4, config.getDefaultFields().size() );
    assertEquals( facility, config.getDefaultFields().get( "facility" ) );
    assertEquals( hostName, config.getDefaultFields().get( "host" ) );
    assertEquals( "DEV", config.getDefaultFields().get( "environment" ) );
    assertEquals( "MyAPP", config.getDefaultFields().get( "application" ) );

    assertEquals( 7, config.getAdditionalFields().size() );
    assertEquals( "threadName", config.getAdditionalFields().get( "threadName" ) );
    assertEquals( "timestampMs", config.getAdditionalFields().get( "timestamp_in_millis" ) );
    assertEquals( "loggerName", config.getAdditionalFields().get( "logger_name" ) );
    assertEquals( "ipAddress", config.getAdditionalFields().get( "ip_address" ) );
    assertEquals( "exception", config.getAdditionalFields().get( "exception" ) );
    assertEquals( "loggerNdc", config.getAdditionalFields().get( "loggerNdc" ) );
    assertEquals( "userName", config.getAdditionalFields().get( "coolUserName" ) );

    //Setup fake server
    final DatagramSocket socket = ConnectionUtil.createServer( hostName, port );

    // set up mock connection
    final TestGelfConnection connection = new TestGelfConnection( config );
    final Field field = appender.getClass().getDeclaredField( "_connection" );
    field.setAccessible( true );
    field.set( appender, connection );

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
    assertEquals( "GelfAppenderTest.java", message.getFile() );
    assertNotNull( message.getLine() );
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

    // now test the NDC
    NDC.push( "ProcessX" );
    logger.warn( smallTextMessage );
    message = connection.getLastMessage();
    assertEquals( SyslogLevel.WARNING, message.getLevel() );
    assertEquals( "ProcessX", message.getAdditionalFields().get( "loggerNdc" ) );

    logger.error( smallTextMessage, new Exception(  ) );
    message = connection.getLastMessage();
    assertEquals( SyslogLevel.ERR, message.getLevel() );
    assertNotNull( message.getAdditionalFields().get( "exception" ) );

    // Force a reconfigure that will ensure that the close method is invoked to shutdown appender
    PropertyConfigurator.configure( properties );

    socket.close();
  }
}
