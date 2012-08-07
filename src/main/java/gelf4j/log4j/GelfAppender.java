package gelf4j.log4j;

import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfMessageUtil;
import gelf4j.GelfTargetConfig;
import gelf4j.SyslogLevel;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * @author Anton Yakimov
 * @author Jochen Schalanda
 */
@SuppressWarnings( "UnusedDeclaration" )
public class GelfAppender
  extends AppenderSkeleton
{
  private static final String FIELD_LOGGER_NDC = "loggerNdc";

  private static boolean c_searchForMethodOccurred;
  private static Method c_getTimestampMethod;

  private final GelfTargetConfig _config = new GelfTargetConfig();
  private GelfConnection _connection;

  public final GelfTargetConfig getConfig()
  {
    return _config;
  }

  public void setCompressedChunking( final boolean compressedChunking )
  {
    _config.setCompressedChunking( compressedChunking );
  }

  public void setPort( final int port )
  {
    _config.setPort( port );
  }

  public void setHost( final String host )
  {
    _config.setHost( host );
  }

  public void setAdditionalFields( final String additionalFields )
  {
    _config.setAdditionalFields( additionalFields );
  }

  public void setDefaultFields( final String data )
  {
    _config.setDefaultFields( data );
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
    if( null != _connection )
    {
      try
      {
        _connection.close();
      }
      catch ( final IOException ioe )
      {
        //Ignored.
      }
      _connection = null;
    }
  }

  @Override
  protected void append( final LoggingEvent event )
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
    final long timestamp = getTimestamp( event );
    final LocationInfo locationInformation = event.getLocationInformation();
    final String file = locationInformation.getFileName();
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

    for( final Map.Entry<String, String> entry : _config.getAdditionalFields().entrySet() )
    {
      final String fieldName = entry.getValue();
      final String key = entry.getKey();
      if( GelfTargetConfig.FIELD_LOGGER_NAME.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, event.getLoggerName() );
      }
      else if( GelfTargetConfig.FIELD_THREAD_NAME.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, event.getThreadName() );
      }
      else if( GelfTargetConfig.FIELD_TIMESTAMP_MS.equals( fieldName ) )
      {
        GelfMessageUtil.setValue( message, key, message.getJavaTimestamp() );
      }
      else if( GelfTargetConfig.FIELD_EXCEPTION.equals( fieldName ) )
      {
        final ThrowableInformation throwable = event.getThrowableInformation();
        if( null != throwable )
        {
          GelfMessageUtil.setValue( message, key, GelfMessageUtil.extractStacktrace( throwable.getThrowable() ) );
        }
      }
      else if( FIELD_LOGGER_NDC.equals( fieldName ) )
      {
        final String ndc = event.getNDC();
        if( null != ndc )
        {
          GelfMessageUtil.setValue( message, key, ndc );
        }
      }
      else
      {
        final Object value = event.getMDC( key );
        if( null != value )
        {
          GelfMessageUtil.setValue( message, key, value );
        }
      }
    }
    message.getAdditionalFields().putAll( _config.getDefaultFields() );

    return message;
  }

  private long getTimestamp( final LoggingEvent event )
  {
    if( !c_searchForMethodOccurred )
    {
      for( final Method method : event.getClass().getDeclaredMethods() )
      {
        if( method.getName().equals( "getTimeStamp" ) )
        {
          c_getTimestampMethod = method;
          c_searchForMethodOccurred = true;
        }
      }
    }

    if( null != c_getTimestampMethod )
    {

      try
      {
        return (Long) c_getTimestampMethod.invoke( event );
      }
      catch( final Throwable t )
      {
        //Ignored
      }
    }

    return System.currentTimeMillis();
  }
}
