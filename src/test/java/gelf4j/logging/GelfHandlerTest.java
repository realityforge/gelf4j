package gelf4j.logging;

import gelf4j.GelfMessage;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import gelf4j.TestGelfConnection;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfHandlerTest
{
  @Test
  public void configureSetsUpLoggerCorrectly()
    throws Exception
  {
    final String hostName = InetAddress.getLocalHost().getCanonicalHostName();
    final String facility = "LOG4J";

    final String prefix = GelfHandler.class.getName();
    final String configData =
      "handlers = " + prefix + "\n" +
      ".level = FINE\n" +
      "\n" +
      prefix + ".host=" + hostName + "\n" +
      prefix + ".port=1998\n" +
      prefix + ".compressedChunking=false\n" +
      prefix + ".level=FINE\n" +
      prefix + ".additionalFields={\"thread_id\": \"threadId\", \"threadName\": \"threadName\", \"timestamp_in_millis\": \"timestampMs\", \"logger_name\": \"loggerName\", \"SourceClassName\": \"SourceClassName\", \"SourceMethodName\": \"SourceMethodName\", \"exception\": \"exception\"}\n" +
      prefix + ".defaultFields={\"environment\": \"DEV\", \"application\": \"MyAPP\", \"host\":\"" + hostName +
      "\", \"facility\":\"" + facility + "\"}\n" +
      "\n";

    LogManager.getLogManager().reset();
    LogManager.getLogManager().readConfiguration( new ByteArrayInputStream( configData.getBytes() ) );

    //PropertyConfigurator.configure( properties );
    final Logger logger = Logger.getLogger( "ZipZipHooray" );
    final Handler[] handlers = logger.getParent().getHandlers();
    assertEquals( 1, handlers.length );
    final Handler handler = handlers[ 0 ];
    assertTrue( handler instanceof GelfHandler );
    final GelfHandler gelfHandler = (GelfHandler) handler;
    final GelfTargetConfig config = gelfHandler.getConfig();

    assertEquals( hostName, config.getHost() );
    assertEquals( 1998, config.getPort() );
    assertEquals( false, config.isCompressedChunking() );
    assertEquals( 4, config.getDefaultFields().size() );
    assertEquals( hostName, config.getDefaultFields().get( "host" ) );
    assertEquals( facility, config.getDefaultFields().get( "facility" ) );
    assertEquals( "DEV", config.getDefaultFields().get( "environment" ) );
    assertEquals( "MyAPP", config.getDefaultFields().get( "application" ) );

    assertEquals( 7, config.getAdditionalFields().size() );
    assertEquals( "threadId", config.getAdditionalFields().get( "thread_id" ) );
    assertEquals( "threadName", config.getAdditionalFields().get( "threadName" ) );
    assertEquals( "timestampMs", config.getAdditionalFields().get( "timestamp_in_millis" ) );
    assertEquals( "loggerName", config.getAdditionalFields().get( "logger_name" ) );
    assertEquals( "SourceClassName", config.getAdditionalFields().get( "SourceClassName" ) );
    assertEquals( "SourceMethodName", config.getAdditionalFields().get( "SourceMethodName" ) );
    assertEquals( "exception", config.getAdditionalFields().get( "exception" ) );

    // set up mock connection
    final TestGelfConnection connection = new TestGelfConnection( config );
    final Field field = gelfHandler.getClass().getDeclaredField( "_connection" );
    field.setAccessible( true );
    field.set( gelfHandler, connection );

    final long then = System.currentTimeMillis();
    final String smallTextMessage = "HELO";
    logger.fine( smallTextMessage );

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
    assertNotNull( message.getAdditionalFields().get( "thread_id" ) );
    assertTrue( ( (Long) message.getAdditionalFields().get( "timestamp_in_millis" ) ) - then < 1000 );
    assertEquals( null, message.getAdditionalFields().get( "ip_address" ) );
    assertEquals( Thread.currentThread().getName(), message.getAdditionalFields().get( "threadName" ) );
    assertEquals( GelfHandlerTest.class.getName(), message.getAdditionalFields().get( "SourceClassName" ) );
    assertEquals( "configureSetsUpLoggerCorrectly", message.getAdditionalFields().get( "SourceMethodName" ) );

    assertEquals( "DEV", message.getAdditionalFields().get( "environment" ) );
    assertEquals( "MyAPP", message.getAdditionalFields().get( "application" ) );

    logger.log( Level.SEVERE, smallTextMessage, new Exception() );
    message = connection.getLastMessage();
    assertEquals( SyslogLevel.ERR, message.getLevel() );
    assertNotNull( message.getAdditionalFields().get( "exception" ) );

    // Force the closing of the appender
    LogManager.getLogManager().reset();
  }
}
