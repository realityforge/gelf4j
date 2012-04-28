package gelf4j.sender;

import gelf4j.GelfConnection;
import gelf4j.GelfMessage;
import gelf4j.GelfMessageUtil;
import gelf4j.GelfTargetConfig;
import java.util.List;
import org.realityforge.cli.CLArgsParser;
import org.realityforge.cli.CLOption;
import org.realityforge.cli.CLOptionDescriptor;
import org.realityforge.cli.CLUtil;

/**
 * A simple commandline application for sending a log to a gelf server.
 */
public class Main
{
  private static final int HELP_OPT = 1;
  private static final int HOST_CONFIG_OPT = 'h';
  private static final int PORT_CONFIG_OPT = 'p';
  private static final int VERBOSE_OPT = 'v';
  private static final int UNCOMPRESSED_CHUNKING_OPT = 'u';
  private static final int FIELD_OPT = 'D';

  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]{
    new CLOptionDescriptor( "help",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            HELP_OPT,
                            "print this message and exit" ),
    new CLOptionDescriptor( "host",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            HOST_CONFIG_OPT,
                            "the host to send the message to. Defaults to the local host." ),
    new CLOptionDescriptor( "port",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            PORT_CONFIG_OPT,
                            "the port on the server. Defaults to " + GelfTargetConfig.DEFAULT_PORT ),
    new CLOptionDescriptor( "field",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            FIELD_OPT,
                            "fields added to the message." ),
    new CLOptionDescriptor( "verbose",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            VERBOSE_OPT,
                            "print verbose message while sending the message." ),
    new CLOptionDescriptor( "uncompressed-chunking",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            UNCOMPRESSED_CHUNKING_OPT,
                            "use the uncompressed chunking format used by graylog prior to 0.9.6." ),
  };

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 1;
  private static final int ERROR_SENDING_EXIT_CODE = 2;

  private static boolean c_verbose;
  private static String c_message;

  public static void main( final String[] args )
  {
    final GelfTargetConfig config = new GelfTargetConfig();
    if( !processOptions( config, args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    GelfConnection connection = null;

    try
    {
      if( c_verbose )
      {
        info( "Attempting to transmit message" );
      }
      connection = config.createConnection();
      final GelfMessage message = connection.newMessage();
      if( null != c_message )
      {
        GelfMessageUtil.setValue( message, GelfTargetConfig.FIELD_MESSAGE, c_message );
      }
      if( null == message.getShortMessage() )
      {
        error( "No message specified" );
        System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      }
      if( !connection.send( message ) )
      {
        error( "Failed to send message: " + message );
        System.exit( ERROR_SENDING_EXIT_CODE );
      }
      connection.close();
      if( c_verbose )
      {
        info( "Log transmitted" );
      }
      System.exit( SUCCESS_EXIT_CODE );
    }
    catch( final Exception e )
    {
      error( "Transmitting message: " + e );
      if( c_verbose )
      {
        e.printStackTrace( System.out );
      }
      System.exit( ERROR_SENDING_EXIT_CODE );
    }
    finally
    {
      if( null != connection )
      {
        connection.close();
      }
    }
  }

  public static boolean processOptions( final GelfTargetConfig config, final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    if( null != parser.getErrorString() )
    {
      error( parser.getErrorString() );
      return false;
    }

    config.getDefaultFields().clear();
    config.getAdditionalFields().clear();

    // Get a list of parsed options
    @SuppressWarnings( "unchecked" ) final List<CLOption> options = parser.getArguments();
    for( final CLOption option : options )
    {
      switch( option.getId() )
      {
        case CLOption.TEXT_ARGUMENT:
          if( null == c_message )
          {
            c_message = option.getArgument();
          }
          else
          {
            error( "Duplicate message specified: " + option.getArgument() );
            return false;
          }
          break;
        case HOST_CONFIG_OPT:
          config.setHost( option.getArgument() );
          break;
        case FIELD_OPT:
        {
          config.getDefaultFields().put( option.getArgument(), option.getArgument( 1 ) );
          break;
        }
        case PORT_CONFIG_OPT:
        {
          final String port = option.getArgument();
          try
          {
            config.setPort( Integer.parseInt( port ) );
          }
          catch( final NumberFormatException nfe )
          {
            error( "parsing port: " + port );
            return false;
          }
          break;
        }
        case UNCOMPRESSED_CHUNKING_OPT:
        {
          config.setCompressedChunking( false );
          break;
        }
        case VERBOSE_OPT:
        {
          c_verbose = true;
          break;
        }
        case HELP_OPT:
        {
          printUsage();
          return false;
        }

      }
    }
    if( c_verbose )
    {
      info( "Server Host: " + config.getHost() );
      info( "Server Port: " + config.getPort() );
      info( "Compressed Chunking Format?: " + config.isCompressedChunking() );
      info( "Additional Data: " + config.getDefaultFields() );
    }

    return true;
  }

  /**
   * Print out a usage statement
   */
  private static void printUsage()
  {
    final String lineSeparator = System.getProperty( "line.separator" );

    final StringBuilder msg = new StringBuilder();

    msg.append( "java " );
    msg.append( Main.class.getName() );
    msg.append( " [options] message" );
    msg.append( lineSeparator );
    msg.append( "Options: " );
    msg.append( lineSeparator );

    msg.append( CLUtil.describeOptions( OPTIONS ).toString() );

    info( msg.toString() );
  }

  private static void info( final String message )
  {
    System.out.println( message );
  }

  private static void error( final String message )
  {
    System.out.println( "Error: " + message );
  }
}
