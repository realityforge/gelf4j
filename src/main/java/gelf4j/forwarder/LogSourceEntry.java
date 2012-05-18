package gelf4j.forwarder;

final class LogSourceEntry
{
  private final LogSourceConfig _config;

  private boolean _persisted;
  private long _offset;
  private long _lastModifiedTime;

  LogSourceEntry( final LogSourceConfig config )
  {
    _config = config;
  }

  LogSourceConfig getConfig()
  {
    return _config;
  }

  boolean isPersisted()
  {
    return _persisted;
  }

  void markAsPersisted()
  {
    _persisted = true;
  }

  long getOffset()
  {
    return _offset;
  }

  void setOffset( final long offset )
  {
    if( _offset != offset )
    {
      _persisted = false;
      _offset = offset;
    }
  }

  long getLastModifiedTime()
  {
    return _lastModifiedTime;
  }

  void setLastModifiedTime( final long lastModifiedTime )
  {
    if( _lastModifiedTime != lastModifiedTime )
    {
      _persisted = false;
      _lastModifiedTime = lastModifiedTime;
    }
  }
}
