package gelf4j;

import org.json.simple.JSONValue;

/**
 * An implementation of JsonCodec that uses google-json's
 * simple json library
 */
public class SimpleJsonCodec implements JsonCodec
{

  public String toJson( final Object object )
  {
    return JSONValue.toJSONString( object );
  }

  @SuppressWarnings( "unchecked" )
  public <T> T fromJson( final String json, final Class<T> type )
  {
    return ( T ) JSONValue.parse( json );
  }

}
