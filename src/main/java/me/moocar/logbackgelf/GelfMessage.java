package me.moocar.logbackgelf;

import java.util.HashMap;
import java.util.Map;

public class GelfMessage
{
  private String _hostname;
  private String _shortMessage;
  private String _fullMessage;
  private Long _javaTimestamp;
  private SyslogLevel _level;
  private String _facility;
  private Long _line;
  private String _file;
  private final Map<String, Object> _additionalFields = new HashMap<String, Object>(  );

  public String getHostname()
  {
    return _hostname;
  }

  public void setHostname( final String hostname )
  {
    _hostname = hostname;
  }

  public String getShortMessage()
  {
    return _shortMessage;
  }

  public void setShortMessage( final String shortMessage )
  {
    _shortMessage = shortMessage;
  }

  public String getFullMessage()
  {
    return _fullMessage;
  }

  public void setFullMessage( final String fullMessage )
  {
    _fullMessage = fullMessage;
  }

  public Long getJavaTimestamp()
  {
    return _javaTimestamp;
  }

  public void setJavaTimestamp( final long javaTimestamp )
  {
    _javaTimestamp = javaTimestamp;
  }

  public SyslogLevel getLevel()
  {
    return _level;
  }

  public void setLevel( final SyslogLevel level )
  {
    _level = level;
  }

  public String getFacility()
  {
    return _facility;
  }

  public void setFacility( final String facility )
  {
    _facility = facility;
  }

  public Long getLine()
  {
    return _line;
  }

  public void setLine( final long line )
  {
    _line = line;
  }

  public String getFile()
  {
    return _file;
  }

  public void setFile( final String file )
  {
    _file = file;
  }

  public Map<String, Object> getAdditionalFields()
  {
    return _additionalFields;
  }
}
