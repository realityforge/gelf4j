package me.moocar.logbackgelf;

/**
 * Converts a log event into a a payload or chunks and sends them to the graylog2-server
 */
public class AppenderExecutor<E> {

    private final GreylogConnection _connection;
    private final PayloadChunker payloadChunker;
    private final GelfConverter gelfConverter;
    private final int chunkThreshold;

    public AppenderExecutor(GreylogConnection connection,
                            PayloadChunker payloadChunker,
                            GelfConverter gelfConverter,
                            int chunkThreshold) {
        this._connection = connection;
        this.payloadChunker = payloadChunker;
        this.gelfConverter = gelfConverter;
        this.chunkThreshold = chunkThreshold;
    }

    /**
     * The main append method. Takes the event that is being logged, formats if for GELF and then sends it over the wire
     * to the log server
     *
     * @param logEvent The event that we are logging
     */
    public void append(E logEvent) {

        byte[] payload = Zipper.zip(gelfConverter.toGelf(logEvent));

        // If we can fit all the information into one packet, then just send it
        if (payload.length < chunkThreshold) {

            _connection.send( payload );

        // If the message is too long, then slice it up and send multiple packets
        }
        else {

            _connection.send( payloadChunker.chunkIt( payload ) );
        }
    }

  public void close()
  {
    _connection.close();
  }


}
