package gelf4j;

import org.json.simple.JSONValue;

public class SimpleJsonCodec implements JsonCodec {

  public String toJson(Object object) {
    return JSONValue.toJSONString(object);
  }

  public <T> T fromJson(String json, Class<T> type) {
    return (T) JSONValue.parse(json);
  }

}
