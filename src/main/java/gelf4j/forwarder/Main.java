package gelf4j.forwarder;

import gelf4j.GelfTargetConfig;
import java.util.List;
import org.realityforge.cli.CLArgsParser;
import org.realityforge.cli.CLOption;
import org.realityforge.cli.CLOptionDescriptor;
import org.realityforge.cli.CLUtil;

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

  private static boolean c_verbose;

  public static void main( final String[] args )
  {
    if( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    System.exit( SUCCESS_EXIT_CODE );
  }

  public static boolean processOptions( final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    if( null != parser.getErrorString() )
    {
      error( parser.getErrorString() );
      return false;
    }

    c_config.getDefaultFields().clear();

    // Get a list of parsed options
    @SuppressWarnings( "unchecked" ) final List<CLOption> options = parser.getArguments();
    for( final CLOption option : options )
    {
      switch( option.getId() )
      {
        case HOST_CONFIG_OPT:
          c_config.setHost( option.getArgument() );
          break;
        case FIELD_OPT:
        {
          c_config.getDefaultFields().put( option.getArgument(), option.getArgument( 1 ) );
          break;
        }
        case PORT_CONFIG_OPT:
        {
          final String port = option.getArgument();
          try
          {
            c_config.setPort( Integer.parseInt( port ) );
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
          c_config.setCompressedChunking( false );
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
      info( "Server Host: " + c_config.getHost() );
      info( "Server Port: " + c_config.getPort() );
      info( "Compressed Chunking Format?: " + c_config.isCompressedChunking() );
      info( "Default Fields: " + c_config.getDefaultFields() );
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
    msg.append( " [options]" );
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
