package gelf4j.forwarder;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

final class LogSourceConfig
{
  private final File _source;
  private final Pattern _extractionPattern;
  private final Map<String, Integer> _fieldMap;
  private final Map<String, Object> _defaultFields;

  LogSourceConfig( final File source,
                   final Pattern extractionPattern,
                   final Map<String, Integer> fieldMap,
                   final Map<String, Object> defaultFields )
  {
    _source = source;
    _extractionPattern = extractionPattern;
    _fieldMap = fieldMap;
    _defaultFields = defaultFields;
  }

  File getSource()
  {
    return _source;
  }

  Pattern getExtractionPattern()
  {
    return _extractionPattern;
  }

  Map<String, Integer> getFieldMap()
  {
    return _fieldMap;
  }

  Map<String, Object> getDefaultFields()
  {
    return _defaultFields;
  }
}
