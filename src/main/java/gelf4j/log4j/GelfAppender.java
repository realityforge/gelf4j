package gelf4j.log4j;

import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfMessageUtil;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * @author Anton Yakimov
 * @author Jochen Schalanda
 */
public class GelfAppender
  extends AppenderSkeleton
{
  private static final String FIELD_LOGGER_NDC = "loggerNdc";

  private static boolean hasGetTimeStamp = true;
  private static Method methodGetTimeStamp = null;

  private final GelfTargetConfig _config = new GelfTargetConfig();
  private GelfConnection _connection;

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
  public void activateOptions()
  {
    try
    {
      _connection = _config.createConnection();
    }
    catch( final Exception e )
    {
      errorHandler.error( "Error initialising gelf connection", e, ErrorCode.WRITE_FAILURE );
    }
  }

  public void close()
  {
    _connection.close();
  }

  @Override
  protected void append( LoggingEvent event )
  {
    if( _connection == null || !_connection.send( makeMessage( event ) ) )
    {
      errorHandler.error( "Could not send GELF message" );
    }
  }

  public boolean requiresLayout()
  {
    return false;
  }

  private GelfMessage makeMessage( LoggingEvent event )
  {
    long timestamp = getTimeStamp( event );

    LocationInfo locationInformation = event.getLocationInformation();
    String file = locationInformation.getFileName();
    Integer lineNumber = null;
    try
    {
      final String lineInfo = locationInformation.getLineNumber();
      if( null != lineInfo )
      {
        lineNumber = Integer.parseInt( lineInfo );
      }
    }
    catch( final NumberFormatException nfe )
    {
      //ignore
    }

    final String renderedMessage = event.getRenderedMessage();
    final SyslogLevel level = SyslogLevel.values()[ event.getLevel().getSyslogEquivalent() ];
    final GelfMessage message =
      _connection.newMessage( level, renderedMessage == null ? "" : renderedMessage, timestamp );
    if( null != lineNumber )
    {
      message.setLine( lineNumber );
    }
    if( null != file )
    {
      message.setFile( file );
    }

    @SuppressWarnings( "unchecked" ) final Map<String, String> mdc = MDC.getContext();
    for( final Map.Entry<String, String> entry : _config.getAdditionalFields().entrySet() )
    {
      final String fieldName = entry.getKey();
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
        final ThrowableInformation throwable = event.getThrowableInformation();
        if( null != throwable )
        {
          message.getAdditionalFields().put( key, GelfMessageUtil.extractStacktrace( throwable.getThrowable() ) );
        }
      }
      else if( FIELD_LOGGER_NDC.equals( fieldName ) )
      {
        final String ndc = event.getNDC();
        if( null != ndc )
        {
          message.getAdditionalFields().put( key, ndc );
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

  static long getTimeStamp( LoggingEvent event )
  {

    long timeStamp = System.currentTimeMillis();

    if( hasGetTimeStamp && methodGetTimeStamp == null )
    {

      hasGetTimeStamp = false;

      Method[] declaredMethods = event.getClass().getDeclaredMethods();
      for( Method m : declaredMethods )
      {
        if( m.getName().equals( "getTimeStamp" ) )
        {
          methodGetTimeStamp = m;
          hasGetTimeStamp = true;

          break;
        }
      }
    }

    if( hasGetTimeStamp )
    {

      try
      {
        timeStamp = (Long) methodGetTimeStamp.invoke( event );
      }
      catch( IllegalAccessException e )
      {
        // Just return the current timestamp
      }
      catch( InvocationTargetException e )
      {
        // Just return the current timestamp
      }
    }

    return timeStamp;
  }
}
