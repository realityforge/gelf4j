package gelf4j;

public class TestGelfConnection
  extends GelfConnection
{
  private GelfMessage _lastMessage;

  public TestGelfConnection( final GelfTargetConfig config )
    throws Exception
  {
    super( config );
  }

  @Override
  public boolean send( final GelfMessage message )
  {
    this._lastMessage = message;
    return super.send( message );
  }

  public GelfMessage getLastMessage()
  {
    return _lastMessage;
  }
}