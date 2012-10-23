package gelf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class GelfEncoderTest
{
  @Test
  public void fullWorkflow()
    throws Exception
  {
    final GelfMessage message = new GelfMessage();
    message.setShortMessage( "MyShortMessage" );
    final List<byte[]> packets = new GelfEncoder( "localhost", true, new SimpleJsonCodec() ).encode( message );
    assertEquals( 1, packets.size() );
  }

  @Test
  public void messageToJson_MissingShortMessage()
    throws Exception
  {
    final GelfMessage message = new GelfMessage();
    assertNull( new GelfEncoder( "localhost", true, new SimpleJsonCodec() ).toJson( message ) );
  }

  @Test
  public void messageToJson_WithDefaults()
    throws Exception
  {
    final GelfMessage message = new GelfMessage();
    final String myShortMessage = "MyShortMessage";
    message.setShortMessage( myShortMessage );
    final String json = new GelfEncoder( "localhost", true, new SimpleJsonCodec() ).toJson( message );
    assertNotNull( json );
    final Map<String, Object> object = parseJsonObject( json );

    assertEquals( "1.0", object.get( "version" ) );
    assertEquals( myShortMessage, object.get( "short_message" ) );
    assertEquals( "GELF", object.get( "facility" ) );
    assertEquals( null, object.get( "full_message" ) );
    assertNotNull( object.get( "timestamp" ) );
    assertEquals( null, object.get( "level" ) );
    assertEquals( null, object.get( "file" ) );
    assertEquals( null, object.get( "line" ) );
    assertEquals( "localhost", object.get( "host" ) );
    assertEquals( 5, object.size() );
  }

  @Test
  public void messageToJson_WithNoDefaults()
    throws Exception
  {
    final GelfMessage message = new GelfMessage();
    final String myShortMessage = "MyShortMessage";
    final String myFullMessage = "myFullMessage";
    final String myFile = "myFile";
    final long line = 42L;
    final String funkyHost = "FunkyHost";
    final String myFacility = "KnowckEmDownStorm";
    message.setFacility( myFacility );
    message.setShortMessage( myShortMessage );
    message.setFullMessage( myFullMessage );
    message.setLevel( SyslogLevel.ERR );
    message.setFile( myFile );
    message.setLine( line );
    message.setHost( funkyHost );
    message.setJavaTimestamp( 3000 );
    message.getAdditionalFields().put( "zang", "zing" );
    final String json = new GelfEncoder( "localhost", true, new SimpleJsonCodec() ).toJson( message );
    assertNotNull( json );
    final Map<String, Object> object = parseJsonObject( json );

    assertEquals( "1.0", object.get( "version" ) );
    assertEquals( myShortMessage, object.get( "short_message" ) );
    assertEquals( myFullMessage, object.get( "full_message" ) );
    assertEquals( myFacility, object.get( "facility" ) );
    assertEquals( "3", object.get( "timestamp" ) );
    assertEquals( 3L, object.get( "level" ) );
    assertEquals( myFile, object.get( "file" ) );
    assertEquals( line, object.get( "line" ) );
    assertEquals( funkyHost, object.get( "host" ) );
    assertEquals( "zing", object.get( "_zang" ) );
    assertEquals( 10, object.size() );
  }

  @SuppressWarnings( "unchecked" )
  private Map<String, Object> parseJsonObject( final String json )
  {
    return (Map<String, Object>) new SimpleJsonCodec().fromJson( json, Map.class );
  }

  @Test
  public void generateMessageID()
    throws Exception
  {
    assertEquals( 8, encoder( true ).generateMessageID().length );
    assertEquals( 32, encoder( false ).generateMessageID().length );
  }

  @Test
  public void ensureTooLargePayloadIsDropped()
    throws Exception
  {
    final int payloadSize = GelfEncoder.PAYLOAD_THRESHOLD * ( GelfEncoder.MAX_SEQ_NUMBER + 1 );
    final byte[] payload = createData( payloadSize );
    final List<byte[]> packets = encoder( true ).createPackets( payload );
    assertNull( packets );
  }

  @Test
  public void ensurePayloadUnderThresholdCreatesASinglePacket()
    throws Exception
  {
    final int payloadSize = GelfEncoder.MAX_PACKET_SIZE;
    final byte[] payload = createData( payloadSize );
    final List<byte[]> packets = encoder( true ).createPackets( payload );
    assertEquals( 1, packets.size() );
    assertArrayEquals( packets.get( 0 ), payload );
  }

  @Test
  public void ensurePayloadOverThresholdCreatesMultipleChunks()
    throws Exception
  {
    ensurePayloadOverThresholdCreatesMultipleChunks( true );
    ensurePayloadOverThresholdCreatesMultipleChunks( false );
  }

  private void ensurePayloadOverThresholdCreatesMultipleChunks( final boolean compressed )
    throws Exception
  {
    final int payloadSize = GelfEncoder.MAX_PACKET_SIZE + 1;
    final byte[] payload = createData( payloadSize );
    final List<byte[]> packets = encoder( compressed ).createPackets( payload );
    assertEquals( 2, packets.size() );

    final LinkedList<byte[]> messageIDs = new LinkedList<byte[]>();
    final LinkedList<byte[]> payloadData = new LinkedList<byte[]>();

    expectChunk( compressed, packets.get( 0 ), 0, 2, messageIDs, payloadData );
    expectChunk( compressed, packets.get( 1 ), 1, 2, messageIDs, payloadData );

    // Make sure that the message ids are the same across chunks
    assertArrayEquals( messageIDs.get( 0 ), messageIDs.get( 1 ) );

    // Make sure the content that comes through is the entire packet
    final byte[] payload1 = payloadData.get( 0 );
    final byte[] payload2 = payloadData.get( 1 );

    assertEquals( payload1.length + payload2.length, payload.length );
    assertArrayEquals( payload1, Arrays.copyOfRange( payload, 0, payload1.length ) );
    assertArrayEquals( payload2, Arrays.copyOfRange( payload, payload1.length, payload1.length + payload2.length ) );
  }

  private void expectChunk( final boolean compressed,
                            final byte[] packet,
                            final int sequence,
                            final int chunkCount,
                            final LinkedList<byte[]> messageIDs,
                            final LinkedList<byte[]> payload )
    throws IOException
  {
    int start = 0;
    final byte[] chunkID = { 0x1e, 0x0f };
    start = start + chunkID.length;

    final int messageIDLength = compressed ? 8 : 32;
    messageIDs.add( Arrays.copyOfRange( packet, start, start + messageIDLength ) );
    start += messageIDLength;

    if( !compressed )
    {
      assertEquals( 0, packet[ start++ ] );
    }
    assertEquals( sequence, packet[ start++ ] );
    if( !compressed )
    {
      assertEquals( 0, packet[ start++ ] );
    }
    assertEquals( chunkCount, packet[ start++ ] );

    payload.add( Arrays.copyOfRange( packet, start, packet.length ) );
  }

  private String createString( final int byteCount )
  {
    final StringBuilder sb = new StringBuilder();
    for( int i = 0; i < byteCount; i++ )
    {
      sb.append( (char) ( 'a' + ( i % 26 ) ) );
    }
    return sb.toString();
  }

  private byte[] createData( final int byteCount )
  {
    return createString( byteCount ).getBytes();
  }

  private GelfEncoder encoder( final boolean compressed )
    throws Exception
  {
    return new GelfEncoder( "localhost", compressed, new SimpleJsonCodec() );
  }
}
