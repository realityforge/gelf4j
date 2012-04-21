package org.graylog2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import me.moocar.logbackgelf.SyslogLevel;
import org.json.simple.JSONValue;

public class GelfMessage
  extends me.moocar.logbackgelf.GelfMessage
{

  private static final String ID_NAME = "id";
  private static final String GELF_VERSION = "1.0";
  private static final byte[] GELF_CHUNKED_ID = new byte[]{ 0x1e, 0x0f };
  private static final int MAXIMUM_CHUNK_SIZE = 1420;
  private static final BigDecimal TIME_DIVISOR = new BigDecimal( 1000 );

  private byte[] hostBytes = lastFourAsciiBytes( "none" );

  public GelfMessage()
  {
  }

  public GelfMessage( String shortMessage, String fullMessage, Date timestamp, String level )
  {
    setShortMessage( shortMessage );
    setFullMessage( fullMessage );
    setJavaTimestamp( timestamp.getTime() );
    setLevel( SyslogLevel.valueOf( level ) );
  }

  public GelfMessage( String shortMessage, String fullMessage, Long timestamp, String level, int line, String file )
  {
    setShortMessage( shortMessage );
    setFullMessage( fullMessage );
    setJavaTimestamp( timestamp );
    setLevel( SyslogLevel.valueOf( level ) );
    setLine( line );
    setFile( file );
  }

  /**
   * zips up a string into a GZIP format.
   *
   * @param str The string to zip
   * @return The zipped string
   */
  private static byte[] zip( final String str )
  {
    GZIPOutputStream zipStream = null;
    try
    {
      final ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
      zipStream = new GZIPOutputStream( targetStream );
      zipStream.write( str.getBytes() );
      zipStream.close();
      final byte[] zipped = targetStream.toByteArray();
      targetStream.close();
      return zipped;
    }
    catch( final IOException ioe )
    {
      throw new RuntimeException( ioe );
    }
    finally
    {
      try
      {
        if( null != zipStream )
        {
          zipStream.close();
        }
      }
      catch( final IOException ioe )
      {
        throw new RuntimeException( ioe );
      }
    }
  }

  public String toJson()
  {
    Map<String, Object> map = new HashMap<String, Object>();

    map.put( "version", GELF_VERSION );
    map.put( "host", getHostname() );
    map.put( "short_message", getShortMessage() );
    map.put( "full_message", getFullMessage() );
    map.put( "timestamp", getTimestamp() );

    map.put( "level", getLevel() );
    map.put( "facility", getFacility() );
    if( null != getFile() )
    {
      map.put( "file", getFile() );
    }
    if( null != getLine() )
    {
      map.put( "line", getLine() );
    }

    for( Map.Entry<String, Object> additionalField : getAdditionalFields().entrySet() )
    {
      if( !ID_NAME.equals( additionalField.getKey() ) )
      {
        map.put( "_" + additionalField.getKey(), additionalField.getValue() );
      }
    }

    return JSONValue.toJSONString( map );
  }

  public List<byte[]> toDatagrams()
  {
    byte[] messageBytes = zip( toJson() );
    List<byte[]> datagrams = new ArrayList<byte[]>();
    if( messageBytes.length > MAXIMUM_CHUNK_SIZE )
    {
      sliceDatagrams( messageBytes, datagrams );
    }
    else
    {
      datagrams.add( messageBytes );
    }
    return datagrams;
  }

  private void sliceDatagrams( byte[] messageBytes, List<byte[]> datagrams )
  {
    int messageLength = messageBytes.length;
    byte[] messageId = ByteBuffer.allocate( 8 )
      .putInt( (int) System.currentTimeMillis() )       // 4 least-significant-bytes of the time in millis
      .put( hostBytes )                                // 4 least-significant-bytes of the host
      .array();

    int num = ( (Double) Math.ceil( (double) messageLength / MAXIMUM_CHUNK_SIZE ) ).intValue();
    for( int idx = 0; idx < num; idx++ )
    {
      byte[] header = concatByteArray( GELF_CHUNKED_ID, concatByteArray( messageId, new byte[]{ (byte) idx, (byte) num } ) );
      int from = idx * MAXIMUM_CHUNK_SIZE;
      int to = from + MAXIMUM_CHUNK_SIZE;
      if( to >= messageLength )
      {
        to = messageLength;
      }
      byte[] datagram = concatByteArray( header, Arrays.copyOfRange( messageBytes, from, to ) );
      datagrams.add( datagram );
    }
  }

  private byte[] lastFourAsciiBytes( String host )
  {
    final String shortHost = host.length() >= 4 ? host.substring( host.length() - 4 ) : host;
    try
    {
      return shortHost.getBytes( "ASCII" );
    }
    catch( UnsupportedEncodingException e )
    {
      throw new RuntimeException( "JVM without ascii support?", e );
    }
  }

  public String getTimestamp()
  {
    return new BigDecimal( getJavaTimestamp() ).divide( TIME_DIVISOR ).toPlainString();
  }

  private byte[] concatByteArray( byte[] first, byte[] second )
  {
    byte[] result = Arrays.copyOf( first, first.length + second.length );
    System.arraycopy( second, 0, result, first.length, second.length );
    return result;
  }
}
