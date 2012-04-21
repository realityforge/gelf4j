package org.graylog2.log4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import me.moocar.logbackgelf.GelfEncoder;
import me.moocar.logbackgelf.GelfMessage;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.json.simple.JSONValue;

/**
 * Configure it this way:
 * <p/>
 * log4j.appender.console.additionalFields={'environment': 'DEV', 'application': 'MyAPP'}
 * log4j.appender.console.extractStacktrace=true
 * log4j.appender.console.addExtendedInformation=true
 * log4j.appender.console.originHost=www.example.com
 * log4j.appender.console=org.graylog2.log.GelfConsoleAppender
 * log4j.appender.console.layout=org.apache.log4j.PatternLayout
 */
public class GelfConsoleAppender extends ConsoleAppender implements GelfMessageProvider
{

  private static String originHost;
  private boolean extractStacktrace;
  private boolean addExtendedInformation;
  private Map<String, String> fields;
  private GelfEncoder _encoder;

  public GelfConsoleAppender()
  {
    super();
    setupEncoder();
  }

  public GelfConsoleAppender( Layout layout )
  {
    super( layout );
    setupEncoder();
  }

  public GelfConsoleAppender( Layout layout, String target )
  {
    super( layout, target );
    setupEncoder();
  }

  private void setupEncoder()

  {
    try
    {
      _encoder = new GelfEncoder();
    }
    catch( Exception e )
    {
      throw new RuntimeException( e.getMessage(), e );
    }
  }

  // GelfMessageProvider interface.

  public void setAdditionalFields( String additionalFields )
  {
    fields = (Map<String, String>) JSONValue.parse( additionalFields.replaceAll( "'", "\"" ) );
  }

  public boolean isExtractStacktrace()
  {
    return extractStacktrace;
  }

  public void setExtractStacktrace( boolean extractStacktrace )
  {
    this.extractStacktrace = extractStacktrace;
  }

  public boolean isAddExtendedInformation()
  {
    return addExtendedInformation;
  }

  public void setAddExtendedInformation( boolean addExtendedInformation )
  {
    this.addExtendedInformation = addExtendedInformation;
  }

  public String getOriginHost()
  {
    return originHost;
  }

  public void setOriginHost( String originHost )
  {
    this.originHost = originHost;
  }

  public String getFacility()
  {
    return null;
  }

  public Map<String, String> getFields()
  {
    if( fields == null )
    {
      fields = new HashMap<String, String>();
    }
    return Collections.unmodifiableMap( fields );
  }

  // the important parts.

  @Override
  protected void subAppend( LoggingEvent event )
  {
    GelfMessage gelf = GelfMessageFactory.makeMessage( event, this );
    this.qw.write( _encoder.toJson( gelf ) );
    this.qw.write( Layout.LINE_SEP );

    if( this.immediateFlush )
    {
      this.qw.flush();
    }
  }
}
