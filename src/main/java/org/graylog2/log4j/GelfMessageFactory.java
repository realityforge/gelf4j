package org.graylog2.log4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import me.moocar.logbackgelf.SyslogLevel;
import org.apache.log4j.Level;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.graylog2.GelfMessage;

final class GelfMessageFactory {
    private static final int MAX_SHORT_MESSAGE_LENGTH = 250;
    private static final String ORIGIN_HOST_KEY = "originHost";
    private static final String LOGGER_NAME = "logger";
    private static final String LOGGER_NDC = "loggerNdc";
    private static final String THREAD_NAME = "thread";
    private static final String JAVA_TIMESTAMP = "timestampMs";
    
    public static GelfMessage makeMessage(LoggingEvent event, GelfMessageProvider provider) {
        long timeStamp = Log4jVersionChecker.getTimeStamp(event);
        Level level = event.getLevel();

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

        String renderedMessage = event.getRenderedMessage();
        String shortMessage;

        if (renderedMessage == null) {
            renderedMessage = "";
        }

        if (renderedMessage.length() > MAX_SHORT_MESSAGE_LENGTH) {
            shortMessage = renderedMessage.substring(0, MAX_SHORT_MESSAGE_LENGTH - 1);
        }
        else {
            shortMessage = renderedMessage;
        }

        if (provider.isExtractStacktrace()) {
            ThrowableInformation throwableInformation = event.getThrowableInformation();
            if (throwableInformation != null) {
                renderedMessage += "\n\r" + extractStacktrace(throwableInformation);
            }
        }
        
        final GelfMessage gelfMessage = new GelfMessage();
        gelfMessage.setShortMessage( shortMessage );
        gelfMessage.setFullMessage( renderedMessage );
        gelfMessage.setJavaTimestamp( timeStamp );
        gelfMessage.setLevel( SyslogLevel.values()[ level.getSyslogEquivalent() ] );
        gelfMessage.setLine( lineNumber );
        gelfMessage.setFile( file );

        if (provider.getOriginHost() != null) {
            gelfMessage.setHostname( provider.getOriginHost() );
        }

        if (provider.getFacility() != null) {
            gelfMessage.setFacility(provider.getFacility());
        }

        Map<String, String> fields = provider.getFields();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getKey().equals(ORIGIN_HOST_KEY) && gelfMessage.getHostname() == null) {
                gelfMessage.setHostname( fields.get( ORIGIN_HOST_KEY ) );
            } else {
                gelfMessage.getAdditionalFields().put( entry.getKey(), entry.getValue() );
            }
        }

        if (provider.isAddExtendedInformation()) {

            gelfMessage.getAdditionalFields().put( THREAD_NAME, event.getThreadName() );
          gelfMessage.getAdditionalFields().put( LOGGER_NAME, event.getLoggerName() );

          gelfMessage.getAdditionalFields().put( JAVA_TIMESTAMP, Long.toString(gelfMessage.getJavaTimestamp()) );

          // Get MDC and add a GELF field for each key/value pair
            @SuppressWarnings( "unchecked" ) Map<String, Object> mdc = (Map<String, Object>) MDC.getContext();

            if(mdc != null) {
                for(Map.Entry<String, Object> entry : mdc.entrySet()) {

                  gelfMessage.getAdditionalFields().put( entry.getKey(), entry.getValue().toString() );

                }
            }

            // Get NDC and add a GELF field
            String ndc = event.getNDC();

            if(ndc != null) {

              gelfMessage.getAdditionalFields().put( LOGGER_NDC, ndc );

            }
        }

        return gelfMessage;
    }
    
    private static String extractStacktrace(ThrowableInformation throwableInformation) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwableInformation.getThrowable().printStackTrace(pw);
        return sw.toString();
    }
}
