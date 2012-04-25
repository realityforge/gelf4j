package gelf4j.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.AppenderBase;
import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import java.util.Map;

/**
 * Responsible for Formatting a log event and sending it to the Server. Note that you can't swap in a different
 * Layout since the GELF format is static.
 */
public class GelfAppender<E> extends AppenderBase<E>
{
  private final GelfTargetConfig _config = new GelfTargetConfig();
  private GelfConnection _connection;

  public boolean isCompressedChunking()
  {
    return _config.isCompressedChunking();
  }

  public void setCompressedChunking( final boolean compressedChunking )
  {
    _config.setCompressedChunking( compressedChunking );
  }

  public String getOriginHost()
  {
    return _config.getOriginHost();
  }

  public void setOriginHost( final String originHost )
  {
    _config.setOriginHost( originHost );
  }

  public int getPort()
  {
    return _config.getPort();
  }

  public void setPort( final int port )
  {
    _config.setPort( port );
  }

  public String getHost()
  {
    return _config.getHost();
  }

  public void setHost( final String host )
  {
    _config.setHost( host );
  }

  public String getFacility()
  {
    return _config.getFacility();
  }

  public void setFacility( final String facility )
  {
    _config.setFacility( facility );
  }

  public void setAdditionalFields( final String additionalFields )
  {
    _config.setAdditionalFields( additionalFields );
  }

  public void setAdditionalData( final String additionalData )
  {
    _config.setAdditionalData( additionalData );
  }

  @Override
  public void start()
  {
    super.start();
    try
    {
      _connection = _config.createConnection();
    }
    catch( final Exception e )
    {
      throw new RuntimeException( "Error initialising gelf connection", e );
    }
  }

  @Override
  public void stop()
  {
    if( null != _connection )
    {
      _connection.close();
      _connection = null;
    }
    super.stop();
  }

  /**
   * The main append method. Takes the event that is being logged, formats if for GELF and then sends it over the wire
   * to the log server
   *
   * @param logEvent The event that we are logging
   */
  @Override
  protected void append( final E logEvent )
  {
    try
    {
      _connection.send( toGelf( logEvent ) );
    }
    catch( RuntimeException e )
    {
      addError( "Error occurred: ", e );
      throw e;
    }
  }

  /**
   * Converts a log event into GELF JSON.
   *
   * @param logEvent The log event we're converting
   * @return The log event converted into GELF JSON
   */
  private GelfMessage toGelf( final E logEvent )
  {
    final ILoggingEvent event = (ILoggingEvent) logEvent;

    final String formattedMessage = event.getFormattedMessage();
    final SyslogLevel level = SyslogLevel.values()[ LevelToSyslogSeverity.convert( event ) ];
    final GelfMessage message = _connection.newMessage( level, formattedMessage, event.getTimeStamp() );

    final Map<String, String> mdc = event.getMDCPropertyMap();
    for( final Map.Entry<String, String> entry : _config.getAdditionalFields().entrySet() )
    {
      final String fieldName = entry.getValue();
      final String key = entry.getKey();
      if( GelfTargetConfig.FIELD_LOGGER_NAME.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, event.getLoggerName() );
      }
      else if( GelfTargetConfig.FIELD_THREAD_NAME.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, event.getThreadName() );
      }
      else if( GelfTargetConfig.FIELD_TIMESTAMP_MS.equals( fieldName ) )
      {
        message.getAdditionalFields().put( key, message.getJavaTimestamp() );
      }
      else if( GelfTargetConfig.FIELD_EXCEPTION.equals( fieldName ) )
      {
        final IThrowableProxy proxy = event.getThrowableProxy();
        if( null != proxy )
        {
          message.getAdditionalFields().put( key, toStackTraceString( proxy.getStackTraceElementProxyArray() ) );
        }
      }
      else if( null != mdc )
      {
        final String value = mdc.get( key );
        if( null != value )
        {
          message.getAdditionalFields().put( key, value );
        }
      }
    }
    message.getAdditionalFields().putAll( _config.getAdditionalData() );

    return message;
  }

  private String toStackTraceString( final StackTraceElementProxy[] elements )
  {
    final StringBuilder str = new StringBuilder();
    for( final StackTraceElementProxy element : elements )
    {
      str.append( element.getSTEAsString() );
    }
    return str.toString();
  }
}
