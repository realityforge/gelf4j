package gelf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration about how to create GELF messages in a particular logging framework.
 */
public class GelfTargetConfig
{
  public static final String FIELD_THREAD_NAME = "threadName";
  public static final String FIELD_TIMESTAMP_PREFIX = "timestamp:";
  public static final String FIELD_TIMESTAMP_MS = "timestampMs";
  public static final String FIELD_LOGGER_NAME = "loggerName";
  public static final String FIELD_EXCEPTION = "exception";
  public static final String FIELD_LEVEL = "level";
  public static final String FIELD_FACILITY = "facility";
  public static final String FIELD_LINE = "line";
  public static final String FIELD_FILE = "file";
  public static final String FIELD_HOST = "host";
  public static final String FIELD_MESSAGE = "message";
  public static final String DEFAULT_JSON_CODEC = "gelf4j.SimpleJsonCodec";

  public static final int DEFAULT_PORT = 12201;

  private String _host;
  private InetAddress _hostAddress;
  private int _port = DEFAULT_PORT;
  private boolean _compressedChunking = true;
  private String _codecClass = DEFAULT_JSON_CODEC;
  private JsonCodec _codec;

  private String _defaultFieldsJson;
  private final Map<String, Object> _defaultFields;
  private String _additionalFieldsJson;
  private final Map<String, String> _additionalFields;

  public GelfTargetConfig()
  {
    try
    {
      _host = GelfMessageUtil.getLocalHost();
    }
    catch( final IOException ioe )
    {
      //ignore
    }
    _additionalFields = new HashMap<String, String>();
    _additionalFields.put( FIELD_EXCEPTION, FIELD_EXCEPTION );
    _additionalFields.put( FIELD_THREAD_NAME, FIELD_THREAD_NAME );
    _additionalFields.put( FIELD_LOGGER_NAME, FIELD_LOGGER_NAME );
    _additionalFields.put( FIELD_TIMESTAMP_MS, FIELD_TIMESTAMP_MS );

    _defaultFields = new HashMap<String, Object>();
    _defaultFields.put( FIELD_HOST, _host );
  }

  public boolean isCompressedChunking()
  {
    return _compressedChunking;
  }

  public void setCompressedChunking( final boolean compressedChunking )
  {
    _compressedChunking = compressedChunking;
  }

  public String getHost()
  {
    return _host;
  }

  public InetAddress getHostAddress()
  {
    if( null == _hostAddress && null != _host )
    {
      try
      {
        _hostAddress = InetAddress.getByName( _host );
      }
      catch( final UnknownHostException uhe )
      {
        //Ignored
      }
    }
    return _hostAddress;
  }

  public void setHost( final String host )
  {
    _host = host;
    _hostAddress = null;
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
      return new GelfConnection( this );
    }
    catch( final Exception e )
    {
      throw new Exception( "Error connecting to GELF host " + getHost() + " on port " + getPort(), e );
    }
  }

  public String getCodecClass() {
    return _codecClass;
  }

  public void setCodecClass( final String codecClass ) {
    _codecClass = codecClass;
  }

    /**
   * Additional fields to add to the gelf message. The keys are the key in the GELF message and the value corresponds
   * to a symbol recognized by the underlying log system.
   * 
   * <p>There are some common fields recognized by multiple frameworks (See FIELD_* constants) but in most cases
   * this will result in access to application specific data such as Mapped Diagnostic Contexts (MDC) in Log4j and
   * Logback.</p>
   */
  public Map<String, String> getAdditionalFields()
  {
    if( null != _additionalFieldsJson )
    {
        _additionalFields.clear();
        for( final Map.Entry<String, Object> entry : parseJsonObject( _additionalFieldsJson ).entrySet() )
        {
            _additionalFields.put( entry.getKey(), String.valueOf( entry.getValue() ) );
        }
    }
    return _additionalFields;
  }

  public void setAdditionalFields( final String additionalFields )
  {
    _additionalFieldsJson = additionalFields;
  }

  /**
   * @return the default set of fields that will be added to the GELF message.
   */
  public Map<String, Object> getDefaultFields()
  {
    if( null != _defaultFieldsJson )
    {
      _defaultFields.clear();
      _defaultFields.putAll( parseJsonObject( _defaultFieldsJson ) );
    }
    return _defaultFields;
  }

  public void setDefaultFields( final String additionalData )
  {
    _defaultFieldsJson = additionalData;
  }

  @SuppressWarnings( "unchecked" )
  private Map<String, Object> parseJsonObject( final String additionalFields )
  {
    return ( Map<String, Object> ) getCodec().fromJson( additionalFields, Map.class );
  }

  public JsonCodec getCodec()
  {
    if( null == _codec )
    {
      try
      {
        _codec = ( JsonCodec ) Class.forName( _codecClass ).newInstance();
      }
      catch( final Exception e )
      {
        _codec = new SimpleJsonCodec();
      }
    }
    return _codec;
  }
}
