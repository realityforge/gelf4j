package org.graylog2.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.AppenderBase;
import java.net.InetAddress;
import java.util.Map;
import org.graylog2.GelfConnection;
import org.graylog2.GelfMessage;
import org.graylog2.GelfMessageUtil;
import org.graylog2.GelfTargetConfig;
import org.graylog2.SyslogLevel;

/**
 * Responsible for Formatting a log event and sending it to a Graylog2 Server. Note that you can't swap in a different
 * Layout since the GELF format is static.
 */
public class GelfAppender<E> extends AppenderBase<E>
{
  private final GelfTargetConfig _config = new GelfTargetConfig();
  private GelfConnection _connection;

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

  @Override
  public void start()
  {
    super.start();
    try
    {
      _connection = new GelfConnection( InetAddress.getByName( _config.getHost() ), _config.getPort() );
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

  public String getFacility()
  {
    return _config.getFacility();
  }

  public void setFacility( final String facility )
  {
    _config.setFacility( facility );
  }

  public String getGraylog2ServerHost()
  {
    return _config.getHost();
  }

  public void setGraylog2ServerHost( final String graylog2ServerHost )
  {
    _config.setHost( graylog2ServerHost );
  }

  public int getGraylog2ServerPort()
  {
    return _config.getPort();
  }

  public void setGraylog2ServerPort( final int graylog2ServerPort )
  {
    _config.setPort( graylog2ServerPort );
  }

  /**
   * additional fields to add to the gelf message. Here's how these work: <br/> Let's take an example. I want to log
   * the client's ip address of every request that comes into my web server. To do this, I add the ipaddress to the
   * slf4j MDC on each request as follows: <code> ... MDC.put("ipAddress", "44.556.345.657"); ... </code> Now, to
   * include the ip address in the gelf message, i just add the following to my logback.groovy: <code>
   * appender("GELF", GelfAppender) { ... additionalFields = [identity:"_identity"] ... } </code> in the
   * additionalFields map, the key is the name of the MDC to look up. the value is the name that should be given to
   * the key in the additional field in the gelf message.
   */
  public Map<String, String> getAdditionalFields()
  {
    return _config.getAdditionalFields();
  }

  /**
   * Add an additional field. This is mainly here for compatibility with logback.xml
   *
   * @param keyValue This must be in format key:value where key is the MDC key, and value is the GELF field
   *                 name. e.g "ipAddress:_ip_address"
   */
  public void addAdditionalField( final String keyValue )
  {
    final String[] splitted = keyValue.split( ":" );

    if( splitted.length != 2 )
    {

      throw new IllegalArgumentException( "additionalField must be of the format key:value, where key is the MDC "
                                          + "key, and value is the GELF field name. But found '" + keyValue + "' instead." );
    }

    _config.getAdditionalFields().put( splitted[ 0 ], splitted[ 1 ] );
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

    final GelfMessage message = new GelfMessage();
    message.setHostname( _config.getOriginHost() );
    message.setFacility( _config.getFacility() );
    message.setJavaTimestamp( event.getTimeStamp() );
    message.setLevel( SyslogLevel.values()[ LevelToSyslogSeverity.convert( event ) ] );

    final String formattedMessage = event.getFormattedMessage();

    // Format up the stack trace
    final IThrowableProxy proxy = event.getThrowableProxy();
    if( null != proxy )
    {
      final String messageHead = formattedMessage + "\n " + proxy.getClassName() + ": " + proxy.getMessage();
      final String fullMessage = messageHead + "\n" + toStackTraceString( proxy.getStackTraceElementProxyArray() );
      message.setFullMessage( fullMessage );
      message.setShortMessage( GelfMessageUtil.truncateShortMessage( messageHead ) );
    }
    else
    {
      message.setFullMessage( formattedMessage );
      message.setShortMessage( GelfMessageUtil.truncateShortMessage( formattedMessage ) );
    }

    final Map<String, String> mdc = event.getMDCPropertyMap();
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

  private String toStackTraceString( StackTraceElementProxy[] elements )
  {
    StringBuilder str = new StringBuilder();
    for( StackTraceElementProxy element : elements )
    {
      str.append( element.getSTEAsString() );
    }
    return str.toString();
  }
}
