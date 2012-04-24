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
  protected boolean performSend( final GelfMessage message )
  {
    this._lastMessage = message;
    return super.performSend( message );
  }

  public GelfMessage getLastMessage()
  {
    return _lastMessage;
  }
}