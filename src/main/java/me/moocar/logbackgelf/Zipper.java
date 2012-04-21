package me.moocar.logbackgelf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class Zipper
{
  private Zipper()
  {
  }

  /**
   * zips up a string into a GZIP format.
   *
   * @param str The string to zip
   * @return The zipped string
   */
  public static byte[] zip( final String str )
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
}
