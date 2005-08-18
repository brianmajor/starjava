package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.ObjectFactory;

/**
 * Invokes the Stilts tasks using a {@link LineEnvironment}.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class LineInvoker {

    private final String toolName_;
    private final ObjectFactory taskFactory_;
    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.ttools" );

    /**
     * Constructor.
     *
     * @param   toolName  name of the overall application
     * @param   taskFactory  factory which can create the tasks known to
     *          the application
     */
    public LineInvoker( String toolName, ObjectFactory taskFactory ) {
        toolName_ = toolName;
        taskFactory_ = taskFactory;
    }

    /**
     * Invokes one of the known tasks given a string of command-line words.
     * The <code>args</code> string will typically come straight out of
     * a static <code>main()</code> method.
     * 
     * @param   args   argument list
     */
    public void invoke( String[] args ) {
        List argList = new ArrayList( Arrays.asList( args ) );
        LineEnvironment env = new LineEnvironment();

        /* Treat flags. */
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) ) {
                if ( arg.equals( "-help" ) ||
                     arg.equals( "-h" ) ) {
                    it.remove();
                    System.out.println( "\n" + getUsage() );
                    return;
                }
                else if ( arg.equals( "-version" ) ) {
                    it.remove();
                    System.out.println( "\n" + "STILTS version " 
                                             + Stilts.getVersion() + "\n" );
                    return;
                }
                else if ( arg.equals( "-disk" ) ) {
                    it.remove();
                    StoragePolicy.setDefaultPolicy( StoragePolicy.PREFER_DISK );
                    env.getTableFactory()
                       .setStoragePolicy( StoragePolicy.PREFER_DISK );
                }
                else if ( arg.equals( "-votstrict" ) ) {
                    it.remove();
                    env.setStrictVotable( true );
                }
                else if ( arg.equals( "-novotstrict" ) ) {
                    it.remove();
                    env.setStrictVotable( false );
                }
                else if ( arg.equals( "-debug" ) ) {
                    it.remove();
                    env.setDebug( true );
                }
                else {
                    it.remove();
                    System.err.println( "\n" + getUsage() );
                    System.exit( 1 );
                }
            }
            else {
                break;
            }
        }

        if ( argList.size() == 0 ) {
            System.err.println( "\n" + getUsage() );
            System.exit( 1 );
        }

        String taskName = (String) argList.remove( 0 );
        if ( taskFactory_.isRegistered( taskName ) ) {
            Task task = null;
            try {
                task = (Task) taskFactory_.createObject( taskName );
                String[] taskArgs = (String[])
                                    argList.toArray( new String[ 0 ] );
                String helpText = helpMessage( env, task, taskName, taskArgs );
                if ( helpText != null ) {
                    System.out.println( "\n" + helpText );
                }
                else {
                    env.setArgs( taskArgs );
                    task.invoke( env );
                    String unused = env.getUnused();
                    if ( unused != null ) {
                        System.err.println( "\nWARNING: Unused arguments " 
                                          + unused + "\n" );
                    }
                }
            }
            catch ( TaskException e ) {
                if ( env.isDebug() ) {
                    e.printStackTrace( System.err );
                }
                else {
                    String msg = e.getMessage();
                    if ( msg == null ) {
                        msg = e.toString();
                    }
                    System.err.println( "\n" + msg );
                }
                if ( e instanceof UsageException && task != null ) {
                    System.err.println( getTaskUsage( task, taskName ) );
                }
                System.exit( 1 );
            }
            catch ( LoadException e ) {
                System.err.println( "Task " + taskName + " not available" );
                if ( e.getMessage() != null ) {
                    System.err.println( e.getMessage() );
                }
                if ( env.isDebug() ) {
                    e.printStackTrace( System.err );
                }
            }
        }
        else {
            System.err.println( "\nNo such task: " + taskName );
            System.err.println( "\n" + getUsage() );
            System.exit( 1 );
        }
    }

    /**
     * If a command line represents a request for help, appropriate help
     * text is returned.  Otherwise, null is returned.
     *
     * @param   task   task
     * @param   taskName  task nickname
     * @param   taskArgs  argument list for task (not including task name)
     * @return  help text, or null
     */
    private String helpMessage( TableEnvironment env, Task task,
                                String taskName, String[] taskArgs ) {
        for ( int i = 0; i < taskArgs.length; i++ ) {
            String arg = taskArgs[ i ];
            String helpFor = null;
            if ( arg.equals( "-help" ) ||
                 arg.equals( "-h" ) ||
                 arg.equalsIgnoreCase( "help" ) ) {
                return getTaskUsage( task, taskName );
            }
            else if ( arg.toLowerCase().startsWith( "-help=" ) ) {
                helpFor = arg.substring( 6 ).trim().toLowerCase();
                if ( helpFor.startsWith( "-" ) ) {
                    helpFor = helpFor.substring( 1 );
                }
            }
            else if ( arg.startsWith( "-" ) && arg.indexOf( '=' ) < 0 ) {
                helpFor = arg.substring( 1 ).trim().toLowerCase();
            }
            if ( helpFor != null ) {
                Parameter[] params = task.getParameters();
                for ( int j = 0; j < params.length; j++ ) {
                    Parameter param = params[ j ];
                    if ( helpFor.equals( param.getName() ) ) {
                        return getParamUsage( env, task, taskName, param );
                    }
                }
                return "No such argument: -" + helpFor + "\n\n" 
                     + getTaskUsage( task, taskName );
            }
        }
        return null;
    }

    /**
     * Returns a usage string for this invoker.
     *
     * @return  usage string
     * @see   #invoke
     */
    private String getUsage() {
        StringBuffer sbuf = new StringBuffer()
            .append( "Usage: " )
            .append( toolName_ )
            .append( " [-help]" )
            .append( " [-version]" )
            .append( " [-disk]" )
            .append( " [-debug]" )
            .append( " <task-name> <task-args>" )
            .append( '\n' );
        sbuf.append( "\n   Known tasks:\n" );
        String[] tasks = taskFactory_.getNickNames();
        for ( int i = 0; i < tasks.length; i++ ) {
            sbuf.append( "      " )
                .append( tasks[ i ] )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    /**
     * Returns a usage string for a task known to this application.
     *
     * @param   task   task object
     * @param   taskName  task nickname (the one by which it is known to
     *          the user)
     * @return   usage string
     */
    private static String getTaskUsage( Task task, String taskName ) {
        String prefix = "Usage: " + taskName;
        StringBuffer usage = new StringBuffer();
        usage.append( getPrefixedTaskUsage( task, prefix ) );
        String pad = prefix.replaceAll( ".", " " );
     // usage.append( pad )
     //      .append( " [-help=<arg-name>]" )
     //      .append( '\n' );
        return usage.toString();
    }

    /**
     * Returns a usage string for a task, prefixed by a given string.
     *
     * @param   task   task object
     * @param   prefix   string to prepend to the first line
     * @return   usage string
     */
    public static String getPrefixedTaskUsage( Task task, String prefix ) {

        /* Assemble two lists of usage elements: one for parameters 
         * which must be specified by name, and another for parameters
         * which can be specified only by position. */
        Parameter[] params = task.getParameters();
        List namedWords = new ArrayList();
        List numberedWords = new ArrayList();
        int iPos = 0;
        for ( int i = 0; i < params.length; i++ ) {
            StringBuffer word = new StringBuffer();
            word.append( ' ' );
            Parameter param = params[ i ];
            int pos = param.getPosition();
            boolean byPos = false;
            if ( param.getPosition() > 0 ) {
                if ( pos == ++iPos ) {
                    byPos = true;
                }
                else {
                    logger_.warning( "Parameter positions out of sync for " +
                                     param );
                }
            }
            if ( byPos ) {
                word.append( '[' );
            }
            word.append( '-' )
                .append( param.getName() )
                .append( '=' );
            if ( byPos ) {
                word.append( ']' );
            }
            word.append( param.getUsage() );
            (byPos ? numberedWords : namedWords).add( word.toString() );
        }

        /* Start the usage string, noting the amount of padding required
         * at the head to accommodate the prefix on lines after the first. */
        StringBuffer usage = new StringBuffer();
        StringBuffer line = new StringBuffer();
        line.append( prefix );
        String pad = line.toString().replaceAll( ".", " " );

        /* Add the named usage elements. */
        for ( Iterator it = namedWords.iterator(); it.hasNext(); ) {
            String word = (String) it.next();
            if ( line.length() + word.length() > 78 ) {
                usage.append( line )
                     .append( '\n' );
                line = new StringBuffer( pad );
            }
            line.append( word );
        }
        usage.append( line )
             .append( '\n' );
        line = new StringBuffer( pad );

        /* Add the numbered usage elements. */
        for ( Iterator it = numberedWords.iterator(); it.hasNext(); ) {
            String word = (String) it.next();
            if ( line.length() + word.length() > 78 ) {
                usage.append( line )
                     .append( '\n' );
                line = new StringBuffer( pad );
            }
            line.append( word );
        }
        usage.append( line )
             .append( '\n' );
        line = new StringBuffer( pad );

        /* Return the final usage string. */
        return usage.toString();
    }

    /**
     * Returns a usage string for a parameter of one of the tasks known
     * to this application.  May include extended usage information.
     * Consider the result to be a formatted string, that is, one which
     * contains newlines to keep line lengths down to a reasonable level.
     *
     * @param  env  execution environment
     * @param  task   task 
     * @param  taskName  task nickname
     * @param  param   parameter for which usage information is required
     * @return   usage message
     */
    private String getParamUsage( TableEnvironment env, Task task,
                                  String taskName, Parameter param ) {
        boolean byPos = param.getPosition() > 0;
        boolean isOptional = param.getDefault() != null 
                          || param.isNullPermitted();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "Help for parameter " )
            .append( param.getName().toUpperCase() )
            .append( " in task " )
            .append( taskName.toUpperCase() )
            .append( '\n' )
            .append( sbuf.toString().replaceAll( ".", "-" ) )
            .append( "\n   Name:\n" )
            .append( "      " )
            .append( param.getName() )
            .append( "\n\n   Usage:\n" )
            .append( "      " )
            .append( isOptional ? "[" : "" )
            .append( byPos ? "[" : "" )
            .append( '-' )
            .append( param.getName() )
            .append( '=' )
            .append( byPos ? "]" : "" )
            .append( param.getUsage() )
            .append( isOptional ? "]" : "" )
            .append( "\n\n   Description:\n" )
            .append( "      " )
            .append( param.getPrompt() )
            .append( "\n\n   Default:\n" )
            .append( "      " )
            .append( param.getDefault() )
            .append( "\n" );
        if ( param instanceof ExtraParameter ) {
            sbuf.append( "\n" )
                .append( ((ExtraParameter) param).getExtraUsage( env ) );
        }   
        return sbuf.toString();
    }
}
