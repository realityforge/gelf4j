package org.graylog2.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.graylog2.GelfConnection;
import org.graylog2.GelfMessage;
import org.graylog2.GelfMessageUtil;
import org.graylog2.GelfTargetConfig;
import org.graylog2.SyslogLevel;

public class GelfHandler
  extends Handler
{
  private final GelfTargetConfig _config = new GelfTargetConfig();

  private GelfConnection _connection;
  private boolean extractStacktrace;

  public GelfHandler()
  {
    final LogManager manager = LogManager.getLogManager();
    final String prefix = getClass().getName();

    final String host = manager.getProperty( prefix + ".graylogHost" );
    if( null != host )
    {
      _config.setHost( host );
    }
    final String port = manager.getProperty( prefix + ".graylogPort" );
    if( null != port )
    {
      _config.setPort( Integer.parseInt( port ) );
    }
    extractStacktrace = "true".equalsIgnoreCase( manager.getProperty( prefix + ".extractStacktrace" ) );
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
  public synchronized void flush()
  {
  }

  private String getLocalHostName()
  {
    try
    {
      return InetAddress.getLocalHost().getHostName();
    }
    catch( final UnknownHostException uhe )
    {
      reportError( "Unknown local hostname", uhe, ErrorManager.GENERIC_FAILURE );
    }

    return null;
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
        _connection = new GelfConnection( InetAddress.getByName( _config.getHost() ), _config.getPort() );
      }
      catch( final UnknownHostException e )
      {
        reportError( "Unknown Graylog2 hostname:" + _config.getHost(), e, ErrorManager.WRITE_FAILURE );
      }
      catch( final Exception e )
      {
        reportError( "Socket exception", e, ErrorManager.WRITE_FAILURE );
      }
    }
    if( null == _connection ||
        !_connection.send( makeMessage( record ) ) )
    {
      reportError( "Could not send GELF message", null, ErrorManager.WRITE_FAILURE );
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

  private GelfMessage makeMessage( final LogRecord record )
  {
    String renderedMessage = record.getMessage();

    final String shortMessage = GelfMessageUtil.truncateShortMessage( renderedMessage );

    if( extractStacktrace )
    {
      final Throwable thrown = record.getThrown();
      if( null != thrown )
      {
        renderedMessage += "\n\r" + GelfMessageUtil.extractStacktrace( thrown );
      }
    }

    final GelfMessage message = new GelfMessage();
    message.setHostname( _config.getOriginHost() );
    message.setShortMessage( shortMessage );
    message.setFullMessage( renderedMessage );
    message.setJavaTimestamp( record.getMillis() );
    message.setLevel( SyslogLevel.values()[ levelToSyslogLevel( record.getLevel() ) ] );
    message.getAdditionalFields().put( "SourceClassName", record.getSourceClassName() );

    message.getAdditionalFields().put( "SourceMethodName", record.getSourceMethodName() );

    message.setFacility( _config.getFacility() );

    for( final Map.Entry<String, String> entry : _config.getAdditionalData().entrySet() )
    {
      message.getAdditionalFields().put( entry.getKey(), entry.getValue() );
    }
    message.getAdditionalFields().putAll( _config.getAdditionalData() );

    return message;
  }

  private int levelToSyslogLevel( final Level level )
  {
    final int syslogLevel;
    if( level == Level.SEVERE )
    {
      syslogLevel = 3;
    }
    else if( level == Level.WARNING )
    {
      syslogLevel = 4;
    }
    else if( level == Level.INFO )
    {
      syslogLevel = 6;
    }
    else
    {
      syslogLevel = 7;
    }
    return syslogLevel;
  }

}
