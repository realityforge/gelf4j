package gelf4j.forwarder;

import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfMessageUtil;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONValue;

public class Forwarder
{
  private final File _configDirectory;
  private final Map<File, LogSourceEntry> _entries = new Hashtable<File, LogSourceEntry>();

  public Forwarder( final File configDirectory )
  {
    _configDirectory = configDirectory;
  }

  public void scanConfigDirectory()
  {

  }

  @SuppressWarnings( "unchecked" )
  LogSourceConfig readConfig( final File file )
    throws IOException
  {
    final FileReader reader = new FileReader( file );
    try
    {
      final Map<String, Object> object = (Map<String, Object>) JSONValue.parse( reader );
      final File source = new File( getValue( object, "source", String.class ) );
      final Pattern extractionPattern = Pattern.compile( getValue( object, "extraction_regex", String.class ) );
      final Map<String, Object> data = getValue( object, "data", Map.class );

      final Map<String, Object> fieldMap = getValue( object, "fields", Map.class );
      for( final Map.Entry<String, Object> entry : fieldMap.entrySet() )
      {
        final Object value = entry.getValue();
        if( !( value instanceof Long ) )
        {
          throw new IOException( "Expected field map to contain maps of fields to numbers but " +
                                 "field named '" + entry.getKey() + "' mapped to value " + value );
        }
      }
      return new LogSourceConfig( source, extractionPattern, (Map<String, Integer>) fieldMap, data );
    }
    finally
    {
      reader.close();
    }
  }

  @SuppressWarnings( "unchecked" )
  private static <T> T getValue( final Map<String, Object> object, final String key, final Class<T> type )
    throws IOException
  {
    final Object value = object.get( key );
    if( null == value )
    {
      throw new IOException( "Unable to locate field named '" + key + "'" );
    }
    else if( !type.isInstance( value ) )
    {
      throw new IOException( "Field named '" + key + "' is not of the expected type '" + type.getName() + "'" );
    }
    else
    {
      return (T) value;
    }
  }

  @SuppressWarnings( "SynchronizationOnLocalVariableOrMethodParameter" )
  static ParseStatus processFile( final LogSourceEntry entry, final GelfConnection connection, final int batchSize )
  {
    synchronized( entry )
    {
      final File file = entry.getConfig().getSource();
      final long fileLength = file.length();
      final long fileLastModifiedTime = file.lastModified();

      // The source requires processing if the file exists and has been modified since last check or we have not
      // read all the records yet.
      if( file.exists() && ( fileLastModifiedTime != entry.getLastModifiedTime() || fileLength != entry.getOffset() ) )
      {
        return ParseStatus.UPTODATE;
      }
      else
      {
        // Log has been rotated. Let's just start from the start...
        if( fileLength < entry.getOffset() )
        {
          entry.setOffset( 0 );
        }
        RandomAccessFile raf = null;
        try
        {
          raf = new RandomAccessFile( entry.getConfig().getSource(), "r" );
          raf.seek( entry.getOffset() );

          for( int i = 0; i < batchSize; i++ )
          {
            final String line = raf.readLine();
            if( null == line )
            {
              break;
            }
            else
            {
              entry.setOffset( raf.getFilePointer() );
              if( !processLine( entry, connection, line ) )
              {
                return ParseStatus.ERROR;
              }
            }
          }
        }
        catch( final IOException ioe )
        {
          //Not sure what has happened but just try to read from it in next tick.
          return ParseStatus.ERROR;
        }
        finally
        {
          try
          {
            if( null != raf )
            {
              raf.close();
              entry.setLastModifiedTime( fileLastModifiedTime );
            }
          }
          catch( final IOException ioe )
          {
            //Ignored.
          }
        }
        return ParseStatus.DATA_REMAINING;
      }
    }
  }

  private static boolean processLine( final LogSourceEntry entry, final GelfConnection connection, final String line )
  {
    final Matcher matcher = entry.getConfig().getExtractionPattern().matcher( line );
    if( matcher.matches() )
    {
      final ArrayList<String> list = new ArrayList<String>();
      final int groupCount = matcher.groupCount();
      for( int i = 0; i <= groupCount; i++ )
      {
        list.add( matcher.group( i ) );
      }

      final GelfMessage message = connection.newMessage();
      for( Map.Entry<String, Object> fieldEntry : entry.getConfig().getDefaultFields().entrySet() )
      {
        GelfMessageUtil.setValue( message, fieldEntry.getKey(), fieldEntry.getValue() );
      }
      for( final Map.Entry<String, Integer> fieldEntry : entry.getConfig().getFieldMap().entrySet() )
      {
        final int index = fieldEntry.getValue();
        if( index < list.size() )
        {
          GelfMessageUtil.setValue( message, fieldEntry.getKey(), list.get( index ) );
        }
      }
      return connection.send( message );
    }
    else
    {
      return false;
    }
  }
}
