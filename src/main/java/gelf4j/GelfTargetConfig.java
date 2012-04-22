package gelf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONValue;

/**
 * Configuration about how to create GELF messages in a particular logging framework.
 */
public class GelfTargetConfig
{
  public static final String FIELD_THREAD_NAME = "threadName";
  public static final String FIELD_TIMESTAMP_MS = "timestampMs";
  public static final String FIELD_LOGGER_NAME = "loggerName";
  public static final String FIELD_EXCEPTION = "exception";

  private String _host = "localhost";
  private int _port = GelfConnection.DEFAULT_PORT;

  private String _originHost;
  private String _facility;
  private final Map<String, String> _additionalData = new HashMap<String, String>();
  private final Map<String, String> _additionalFields;

  public GelfTargetConfig()
  {
    try
    {
      _originHost = InetAddress.getLocalHost().getHostName();
    }
    catch( final UnknownHostException uhe )
    {
      //ignore
    }
    _additionalFields = new HashMap<String, String>();
    _additionalFields.put( FIELD_EXCEPTION, FIELD_EXCEPTION );
  }

  public String getOriginHost()
  {
    return _originHost;
  }

  public void setOriginHost( final String originHost )
  {
    _originHost = originHost;
  }

  public String getFacility()
  {
    return _facility;
  }

  public void setFacility( final String facility )
  {
    _facility = facility;
  }

  public String getHost()
  {
    return _host;
  }

  public void setHost( final String host )
  {
    _host = host;
  }

  public int getPort()
  {
    return _port;
  }

  public void setPort( final int port )
  {
    _port = port;
  }

  public GelfConnection createConnection()
    throws Exception
  {
    try
    {
      return new GelfConnection( InetAddress.getByName( getHost() ), getPort() );
    }
    catch( final UnknownHostException uhe )
    {
      throw new Exception( "Unknown GELF host " + getHost(), uhe );
    }
    catch( final Exception e )
    {
      throw new Exception( "Error connecting to GELF host " + getHost() + " on port " + getPort(), e );
    }
  }

  /**
   * Additional fields to add to the gelf message. The keys correspond to a symbol recognized by the underlying log
   * system and the value represents the key under which the value will be added to the GELF message.
   * 
   * <p>There are some common fields recognized by multiple frameworks (See FIELD_* constants) but in most cases
   * this will result in access to data such as Mapped Diagnostic Contexts (MDC) in Log4j and Logback</p>
   */
  public Map<String, String> getAdditionalFields()
  {
    return _additionalFields;
  }

  public void setAdditionalFields( final String additionalFields )
  {
    _additionalFields.clear();
    _additionalFields.putAll( parseJsonObject( additionalFields ) );
  }

  @SuppressWarnings( "unchecked" )
  private Map<String, String> parseJsonObject( final String additionalFields )
  {
    return (Map<String, String>) JSONValue.parse( additionalFields.replaceAll( "'", "\"" ) );
  }

  /**
   * @return the set of data fields that will be added to the GELF message.
   */
  public Map<String, String> getAdditionalData()
  {
    return _additionalData;
  }
}
