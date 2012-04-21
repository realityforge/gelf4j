package org.graylog2.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.AppenderBase;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import org.graylog2.GelfConnection;
import org.graylog2.GelfMessage;
import org.graylog2.GelfMessageUtil;
import org.graylog2.SyslogLevel;

/**
 * Responsible for Formatting a log event and sending it to a Graylog2 Server. Note that you can't swap in a different
 * Layout since the GELF format is static.
 */
public class GelfAppender<E> extends AppenderBase<E>
{
  // The following are configurable via logback configuration
  private String _facility;
  private String _graylog2ServerHost = "localhost";
  private int _graylog2ServerPort = GelfConnection.DEFAULT_PORT;
  private boolean _useLoggerName = false;
  private boolean _useThreadName = false;
  private Map<String, String> _additionalFields = new HashMap<String, String>();

  // The following are hidden (not configurable)
  private String _hostname;
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
      _connection = new GelfConnection( InetAddress.getByName( _graylog2ServerHost ), _graylog2ServerPort );
      _hostname = InetAddress.getLocalHost().getHostName();
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
   * The name of your service. Appears in facility column in graylog2-web-interface
   */
  public String getFacility()
  {
    return _facility;
  }

  public void setFacility( final String facility )
  {
    _facility = facility;
  }

  /**
   * The hostname of the graylog2 server to send messages to
   */
  public String getGraylog2ServerHost()
  {
    return _graylog2ServerHost;
  }

  public void setGraylog2ServerHost( final String graylog2ServerHost )
  {
    _graylog2ServerHost = graylog2ServerHost;
  }

  /**
   * The port of the graylog2 server to send messages to
   */
  public int getGraylog2ServerPort()
  {
    return _graylog2ServerPort;
  }

  public void setGraylog2ServerPort( final int graylog2ServerPort )
  {
    _graylog2ServerPort = graylog2ServerPort;
  }

  /**
   * If true, an additional field call "_loggerName" will be added to each gelf message. Its contents will be the
   * fully qualified name of the logger. e.g: com.company.Thingo.
   */
  public boolean isUseLoggerName()
  {
    return _useLoggerName;
  }

  public void setUseLoggerName( final boolean useLoggerName )
  {
    _useLoggerName = useLoggerName;
  }

  /**
   * If true, an additional field call "_threadName" will be added to each gelf message. Its contents will be the
   * Name of the thread. Defaults to "false".
   */
  public boolean isUseThreadName()
  {
    return _useThreadName;
  }

  public void setUseThreadName( final boolean useThreadName )
  {
    _useThreadName = useThreadName;
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
    return _additionalFields;
  }

  public void setAdditionalFields( final Map<String, String> additionalFields )
  {
    _additionalFields = additionalFields;
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

    _additionalFields.put( splitted[ 0 ], splitted[ 1 ] );
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
    message.setFacility( _facility );
    message.setHostname( _hostname );
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

    if( _useLoggerName )
    {
      message.getAdditionalFields().put( "_loggerName", event.getLoggerName() );
    }

    if( _useThreadName )
    {
      message.getAdditionalFields().put( "_threadName", event.getThreadName() );
    }

    final Map<String, String> mdc = event.getMDCPropertyMap();
    if( null != mdc )
    {
      for( final String key : _additionalFields.keySet() )
      {
        String field = mdc.get( key );
        if( field != null )
        {
          message.getAdditionalFields().put( _additionalFields.get( key ), field );
        }
      }
    }

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
