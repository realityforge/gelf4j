package gelf4j.logging;

import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfMessageUtil;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
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
    int fieldNumber = 0;
    while( true )
    {
      final String property = manager.getProperty( prefix + ".additionalField." + fieldNumber );
      if( null == property )
      {
        break;
      }
      final int index = property.indexOf( '=' );
      if( -1 != index )
      {
        _config.getAdditionalFields().put( property.substring( 0, index ), property.substring( index + 1 ) );
      }

      fieldNumber++;
    }
    final String facility = manager.getProperty( prefix + ".facility" );
    if( null != facility )
    {
      _config.setFacility( facility );
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
      _connection.close();
      _connection = null;
    }
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
    final GelfMessage message = GelfMessageUtil.newMessage( _config, level, renderedMessage, record.getMillis() );

    for( final Map.Entry<String, String> entry : _config.getAdditionalFields().entrySet() )
    {
      final String fieldName = entry.getKey();
      final String key = entry.getKey();
      if( GelfTargetConfig.FIELD_LOGGER_NAME.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, record.getLoggerName() );
      }
      else if( GelfTargetConfig.FIELD_THREAD_NAME.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, record.getThreadID() );
      }
      else if( GelfTargetConfig.FIELD_TIMESTAMP_MS.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, message.getJavaTimestamp() );
      }
      else if( GelfTargetConfig.FIELD_EXCEPTION.equals( fieldName ) )
      {
        final Throwable throwable = record.getThrown();
        if( null != throwable )
        {
          message.getAdditionalFields().put( key, GelfMessageUtil.extractStacktrace( throwable ) );
        }
      }
      else if( FIELD_SOURCE_CLASS_NAME.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, record.getSourceClassName() );
      }
      else if( FIELD_SOURCE_METHOD_NAME.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, record.getSourceMethodName() );
      }
    }
    message.getAdditionalFields().putAll( _config.getAdditionalData() );

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
