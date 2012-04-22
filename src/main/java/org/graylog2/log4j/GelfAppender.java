package org.graylog2.log4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.graylog2.GelfConnection;
import org.graylog2.GelfMessage;
import org.graylog2.GelfMessageUtil;
import org.graylog2.GelfTargetConfig;
import org.graylog2.SyslogLevel;

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

  public void setAdditionalFields( final String additionalFields )
  {
    _config.setAdditionalFields( additionalFields );
  }

  public int getGraylogPort()
  {
    return _config.getPort();
  }

  public void setGraylogPort( int graylogPort )
  {
    _config.setPort( graylogPort );
  }

  public String getGraylogHost()
  {
    return _config.getHost();
  }

  public void setGraylogHost( String graylogHost )
  {
    _config.setHost( graylogHost );
  }

  public String getFacility()
  {
    return _config.getFacility();
  }

  public void setFacility( String facility )
  {
    _config.setFacility( facility );
  }

  public Map<String, String> getFields()
  {
    return _config.getAdditionalFields();
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

  @Override
  protected void append( LoggingEvent event )
  {
    GelfMessage gelfMessage = makeMessage( event );

    if( _connection == null || !_connection.send( gelfMessage ) )
    {
      errorHandler.error( "Could not send GELF message" );
    }
  }

  public void close()
  {
    _connection.close();
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
      GelfMessageUtil.newMessage( _config, level, renderedMessage == null ? "" : renderedMessage, timestamp );
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
