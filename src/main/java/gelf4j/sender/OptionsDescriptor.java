package gelf4j.sender;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import gelf4j.GelfTargetConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java bean to hold command line options
 */
@Parameters(commandDescription = "GELF4j sender")
public class OptionsDescriptor {

    @Parameter(names = {"-h", "--host"}, description = "Recipient of the message (default: localhost)")
    private String host = "localhost";

    @Parameter(
            names = {"-p", "--port"},
            description = "Port on the server (default: " + GelfTargetConfig.DEFAULT_PORT + ")",
            validateWith = PortParameterValidator.class
    )
    private int port = GelfTargetConfig.DEFAULT_PORT;

    @DynamicParameter(names = {"-f", "--fields"}, description = "Fields added to the message")
    private Map<String, String> fields = new HashMap<String, String>();

    @Parameter(names = {"-v", "--verbose"}, description = "Turn on verbose output while sending the message")
    private boolean verbose = false;

    @Parameter(
            names = "--uncompressed-chunking",
            description = "Use the uncompressed chunking format used by Graylog2 prior to version 0.9.6"
    )
    private boolean uncompressedChunking = false;

    @Parameter(names = "--version", description = "Show version and exit")
    private boolean showVersion = false;

    @Parameter(names = "--help", description = "Show usage information and exit")
    private boolean showHelp = false;

    @Parameter(description = "The message to send", required = true)
    private List<String> messages;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }

    public boolean isUncompressedChunking() {
        return uncompressedChunking;
    }

    public void setUncompressedChunking(boolean uncompressedChunking) {
        this.uncompressedChunking = uncompressedChunking;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}