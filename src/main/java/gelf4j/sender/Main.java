package gelf4j.sender;

import com.beust.jcommander.JCommander;
import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfMessageUtil;
import gelf4j.GelfTargetConfig;

import java.io.IOException;

/**
 * A simple commandline application for sending a log to a gelf server.
 */
public class Main {
    private static final int SUCCESS_EXIT_CODE = 0;
    private static final int ERROR_PARSING_ARGS_EXIT_CODE = 1;
    private static final int ERROR_SENDING_EXIT_CODE = 2;

    private static final GelfTargetConfig c_config = new GelfTargetConfig();
    private static boolean c_verbose;
    private static String c_message;

    public static void main(final String[] args) {
        if (!processOptions(args)) {
            System.exit(ERROR_PARSING_ARGS_EXIT_CODE);
            return;
        }

        GelfConnection connection = null;

        try {
            if (c_verbose) {
                info("Attempting to transmit message");
                debug(c_message);
            }
            connection = c_config.createConnection();
            final GelfMessage message = connection.newMessage();
            if (null != c_message) {
                GelfMessageUtil.setValue(message, GelfTargetConfig.FIELD_MESSAGE, c_message);
            }
            if (null == message.getShortMessage()) {
                error("No message specified");
                System.exit(ERROR_PARSING_ARGS_EXIT_CODE);
            }
            if (!connection.send(message)) {
                error("Failed to send message: " + message);
                System.exit(ERROR_SENDING_EXIT_CODE);
            }
            connection.close();
            if (c_verbose) {
                info("Log transmitted");
            }
            System.exit(SUCCESS_EXIT_CODE);
        } catch (final Exception e) {
            error("Transmitting message: " + e);
            if (c_verbose) {
                e.printStackTrace(System.out);
            }
            System.exit(ERROR_SENDING_EXIT_CODE);
        } finally {
            if (null != connection) {
                try {
                    connection.close();
                } catch (final IOException ioe) {
                    //Ignored
                }
            }
        }
    }

    private static boolean processOptions(final String[] args) {
        final OptionsDescriptor options = new OptionsDescriptor();
        final JCommander jCommander = new JCommander(options, args);
        jCommander.setProgramName("gelf4j");

        if (options.isShowHelp()) {
            jCommander.usage();
            System.exit(SUCCESS_EXIT_CODE);
        }

        if (options.getMessages().size() > 0) {
            c_message = options.getMessages().get(0);
        }

        c_config.setHost(options.getHost());
        c_config.setPort(options.getPort());

        c_config.getDefaultFields().putAll(options.getFields());

        c_config.setCompressedChunking(options.isUncompressedChunking());
        c_verbose = options.isVerbose();

        if (c_verbose) {
            info("Server Host: " + c_config.getHost());
            info("Server Port: " + c_config.getPort());
            info("Compressed Chunking Format?: " + c_config.isCompressedChunking());
            info("Default Fields: " + c_config.getDefaultFields());
        }

        return true;
    }

    private static void info(final String message) {
        System.out.println(message);
    }

    private static void error(final String message) {
        System.err.println("Error: " + message);
    }

    private static void debug(final String message) {
        System.out.println("Debug: " + message);
    }
}
