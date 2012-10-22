package gelf4j;

/**
 * This is a fairly simple interface that defines methods to provide JSON
 * parsing capabilities.  Abstracting here allows clients to provide their
 * own implementations and not have to pull in unwanted dependencies,
 * ultimately making GELF4J more palatable to a wider audience.
 *
 * This class may be deemed to simple as a general JSON class, but is
 * sufficient for what we use it for.
 */
public interface JsonCodec {

  public String toJson( Object object );

  public <T> T fromJson( String json, Class<T> type );

}
