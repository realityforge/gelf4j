package gelf4j.logging;

import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfMessageUtil;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import java.io.IOException;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

public class GelfHandler
  extends Handler
{
  public static final String FIELD_THREAD_ID = "threadId";
  public static final String FIELD_SOURCE_CLASS_NAME = "SourceClassName";
  public static final String FIELD_SOURCE_METHOD_NAME = "SourceMethodName";

  private final GelfTargetConfig _config = new GelfTargetConfig();
  private GelfConnection _connection;

  public GelfHandler()
  {
    final LogManager manager = LogManager.getLogManager();
    final String prefix = getClass().getName();

    final String host = manager.getProperty( prefix + ".host" );
    if( null != host )
    {
      _config.setHost( host );
    }
    final String port = manager.getProperty( prefix + ".port" );
    if( null != port )
    {
      _config.setPort( Integer.parseInt( port ) );
    }
    final String additionalFields = manager.getProperty( prefix + ".additionalFields" );
    if( null != additionalFields )
    {
      _config.setAdditionalFields( additionalFields );
    }
    final String additionalData = manager.getProperty( prefix + ".defaultFields" );
    if( null != additionalData )
    {
      _config.setDefaultFields( additionalData );
    }
    final String compressedChunking = manager.getProperty( prefix + ".compressedChunking" );
    if( null != compressedChunking )
    {
      _config.setCompressedChunking( "true".equals( compressedChunking ) );
    }
    final String codecClass = manager.getProperty( prefix + ".codecClass" );
    if( null != codecClass )
    {
      _config.setCodecClass( codecClass );
    }

    final String level = manager.getProperty( prefix + ".level" );
    if( null != level )
    {
      setLevel( Level.parse( level.trim() ) );
    }
    else
    {
      setLevel( Level.INFO );
    }

    final String filter = manager.getProperty( prefix + ".filter" );
    try
    {
      if( null != filter )
      {
        final Class clazz = ClassLoader.getSystemClassLoader().loadClass( filter );
        setFilter( (Filter) clazz.newInstance() );
      }
    }
    catch( final Exception e )
    {
      //ignore
    }
  }

  @Override
  public void close()
  {
    if( null != _connection )
    {
      try
      {
        _connection.close();
      }
      catch ( final IOException ioe )
      {
        //Ignored
      }
      _connection = null;
    }
  }

  GelfTargetConfig getConfig()
  {
    return _config;
  }

  @Override
  public synchronized void flush()
  {
  }

  @Override
  public synchronized void publish( final LogRecord record )
  {
    if( !isLoggable( record ) )
    {
      return;
    }
    if( null == _connection )
    {
      try
      {
        _connection = _config.createConnection();
      }
      catch( final Exception e )
      {
        reportError( "Error initialising gelf connection: " + e.getMessage(), e, ErrorManager.WRITE_FAILURE );
      }
    }
    if( null == _connection ||
        !_connection.send( makeMessage( record ) ) )
    {
      reportError( "Could not send GELF message", null, ErrorManager.WRITE_FAILURE );
    }
  }

  private GelfMessage makeMessage( final LogRecord record )
  {
    final String renderedMessage = record.getMessage();
    final SyslogLevel level = levelToSyslogLevel( record.getLevel() );
    final GelfMessage message = _connection.newMessage( level, renderedMessage, record.getMillis() );

    for( final Map.Entry<String, String> entry : _config.getAdditionalFields().entrySet() )
    {
      final String fieldName = entry.getValue();
      final String key = entry.getKey();
      if( GelfTargetConfig.FIELD_LOGGER_NAME.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, record.getLoggerName() );
      }
      else if( FIELD_THREAD_ID.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, record.getThreadID() );
      }
      else if( GelfTargetConfig.FIELD_THREAD_NAME.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, Thread.currentThread().getName() );
      }
      else if( GelfTargetConfig.FIELD_TIMESTAMP_MS.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, message.getJavaTimestamp() );
      }
      else if( GelfTargetConfig.FIELD_EXCEPTION.equals( fieldName ) )
      {
        final Throwable throwable = record.getThrown();
        if( null != throwable )
        {
          GelfMessageUtil.setValue( message, key, GelfMessageUtil.extractStacktrace( throwable ) );
        }
      }
      else if( FIELD_SOURCE_CLASS_NAME.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, record.getSourceClassName() );
      }
      else if( FIELD_SOURCE_METHOD_NAME.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, record.getSourceMethodName() );
      }
    }

    return message;
  }

  private SyslogLevel levelToSyslogLevel( final Level level )
  {
    if( Level.SEVERE == level )
    {
      return SyslogLevel.ERR;
    }
    else if( Level.WARNING == level )
    {
      return SyslogLevel.WARNING;
    }
    else if( Level.INFO == level )
    {
      return SyslogLevel.INFO;
    }
    else
    {
      return SyslogLevel.DEBUG;
    }
  }
}
