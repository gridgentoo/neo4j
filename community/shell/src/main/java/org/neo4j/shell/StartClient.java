/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.shell.impl.AbstractServer;
import org.neo4j.shell.impl.SameJvmClient;
import org.neo4j.shell.impl.ShellBootstrap;
import org.neo4j.shell.impl.ShellServerExtension;
import org.neo4j.shell.impl.StandardConsole;
import org.neo4j.shell.kernel.GraphDatabaseShellServer;

/**
 * Can start clients, either remotely to another JVM running a server
 * or by starting a local {@link GraphDatabaseShellServer} in this JVM
 * and connecting to it.
 */
public class StartClient
{
    private AtomicBoolean hasBeenShutdown = new AtomicBoolean();
    
    /**
     * The path to the local (this JVM) {@link GraphDatabaseService} to
     * start and connect to.
     */
    public static final String ARG_PATH = "path";
    
    /**
     * Whether or not the shell client should be readonly.
     */
    public static final String ARG_READONLY = "readonly";
    
    /**
     * The host (ip or name) to connect remotely to.
     */
    public static final String ARG_HOST = "host";
    
    /**
     * The port to connect remotely on. A server must have been started
     * on that port.
     */
    public static final String ARG_PORT = "port";
    
    /**
     * The RMI name to use.
     */
    public static final String ARG_NAME = "name";
    
    /**
     * The PID (process ID) to connect to.
     */
    public static final String ARG_PID = "pid";
    
    /**
     * Commands (a line can contain more than one command, with && in between)
     * to execute when the shell client has been connected.
     */
    public static final String ARG_COMMAND = "c";
    
    /**
     * Configuration file to load and use if a local {@link GraphDatabaseService}
     * is started in this JVM.
     */
    public static final String ARG_CONFIG = "config";

    private StartClient()
    {
    }

    /**
     * Starts a shell client. Remote or local depending on the arguments.
     * @param arguments the arguments from the command line. Can contain
     * information about whether to start a local
     * {@link GraphDatabaseShellServer} or connect to an already running
     * {@link GraphDatabaseService}.
     */
    public static void main( String[] arguments )
    {
        new StartClient().start( arguments );
    }

    /**
     * Starts a shell client. Remote or local depending on the arguments.
     * @param agentArgs the arguments from the command line. Can contain
     * information about whether to start a local
     * {@link GraphDatabaseShellServer} or connect to an already running
     * {@link GraphDatabaseService}.
     */
    public static void agentmain( String agentArgs )
    {
        new ShellServerExtension().loadAgent( agentArgs );
    }

    private void start( String[] arguments )
    {
        Args args = new Args( arguments );
        if ( args.has( "?" ) || args.has( "h" ) || args.has( "help" ) || args.has( "usage" ) )
        {
            printUsage();
            return;
        }

        String path = args.get( ARG_PATH, null );
        String host = args.get( ARG_HOST, null );
        String port = args.get( ARG_PORT, null );
        String name = args.get( ARG_NAME, null );
        String pid = args.get( ARG_PID, null );

        if ( ( path != null && ( port != null || name != null || host != null || pid != null ) )
             || ( pid != null && host != null ) )
        {
            System.err.println( "You have supplied both " +
                ARG_PATH + " as well as " + ARG_HOST + "/" + ARG_PORT + "/" + ARG_NAME + ". " +
                "You should either supply only " + ARG_PATH +
                " or " + ARG_HOST + "/" + ARG_PORT + "/" + ARG_NAME + " so that either a local or " +
                "remote shell client can be started" );
            return;
        }
        // Local
        else if ( path != null )
        {
            try
            {
                checkNeo4jDependency();
            }
            catch ( ShellException e )
            {
                handleException( e, args );
            }
            startLocal( args );
        }
        // Remote
        else
        {
            // Start server on the supplied process
            if ( pid != null )
            {
                startServer( pid, args );
            }
            startRemote( args );
        }
    }

    private static final Method attachMethod, loadMethod;
    static
    {
        Method attach, load;
        try
        {
            Class<?> vmClass = Class.forName( "com.sun.tools.attach.VirtualMachine" );
            attach = vmClass.getMethod( "attach", String.class );
            load = vmClass.getMethod( "loadAgent", String.class, String.class );
        }
        catch ( Exception e )
        {
            attach = load = null;
        }
        attachMethod = attach;
        loadMethod = load;
    }

    private static void checkNeo4jDependency() throws ShellException
    {
        try
        {
            Class.forName( "org.neo4j.graphdb.GraphDatabaseService" );
        }
        catch ( Exception e )
        {
            throw new ShellException( "Neo4j not found on the classpath" );
        }
    }

    private void startLocal( Args args )
    {
        String dbPath = args.get( ARG_PATH, null );
        if ( dbPath == null )
        {
            System.err.println( "ERROR: To start a local Neo4j service and a " +
                "shell client on top of that you need to supply a path to a " +
                "Neo4j store or just a new path where a new store will " +
                "be created if it doesn't exist. -" + ARG_PATH +
                " /my/path/here" );
            return;
        }

        try
        {
            boolean readOnly = args.getBoolean( ARG_READONLY, false, true );
            tryStartLocalServerAndClient( dbPath, readOnly, args );
        }
        catch ( Exception e )
        {
            if ( storeWasLocked( e ) )
            {
                if ( wantToConnectReadOnlyInstead() )
                {
                    try
                    {
                        tryStartLocalServerAndClient( dbPath, true, args );
                    }
                    catch ( Exception innerException )
                    {
                        handleException( innerException, args );
                    }
                }
                else
                {
                    handleException( e, args );
                }
            }
            else
            {
                handleException( e, args );
            }
        }
        System.exit( 0 );
    }

    private static boolean wantToConnectReadOnlyInstead()
    {
        Console console = new StandardConsole();
        console.format( "\nThe store seem locked. Start a read-only client " +
            "instead (y/n) [y]? " );
        String input = console.readLine( "" );
        return input.length() == 0 || input.equals( "y" );
    }

    private static boolean storeWasLocked( Exception e )
    {
        // TODO Fix this when a specific exception is thrown
        return mineException( e, IllegalStateException.class,
            "Unable to lock store" );
    }

    private static boolean mineException( Throwable e,
        Class<IllegalStateException> eClass, String startOfMessage )
    {
        if ( eClass.isInstance( e ) &&
            e.getMessage().startsWith( startOfMessage ) )
        {
            return true;
        }

        Throwable cause = e.getCause();
        if ( cause != null )
        {
            return mineException( cause, eClass, startOfMessage );
        }
        return false;
    }

    private void tryStartLocalServerAndClient( String dbPath,
        boolean readOnly, Args args ) throws Exception
    {
        String configFile = args.get( ARG_CONFIG, null );
        final GraphDatabaseShellServer server = new GraphDatabaseShellServer( dbPath, readOnly, configFile );
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                shutdownIfNecessary( server );
            }
        } );

        if ( !isCommandLine( args ) )
            System.out.println( "NOTE: Local Neo4j graph database service at '" + dbPath + "'" );
        ShellClient client = new SameJvmClient( server );
        setSessionVariablesFromArgs( client, args );
        grabPromptOrJustExecuteCommand( client, args );
        shutdownIfNecessary( server );
    }

    private void shutdownIfNecessary( ShellServer server )
    {
        try
        {
            if ( !hasBeenShutdown.compareAndSet( false, true ) )
            {
                server.shutdown();
            }
        }
        catch ( RemoteException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void startServer( String pid, Args args )
    {
        String port = args.get( "port", Integer.toString( AbstractServer.DEFAULT_PORT ) );
        String name = args.get( "name", AbstractServer.DEFAULT_NAME );
        try
        {
            String jarfile = new File(
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI() ).getAbsolutePath();
            Object vm = attachMethod.invoke( null, pid );
            loadMethod.invoke( vm, jarfile, new ShellBootstrap( port, name ).serialize() );
        }
        catch ( Exception e )
        {
            handleException( e, args );
        }
    }

    private static void startRemote( Args args )
    {
        try
        {
            String host = args.get( ARG_HOST, "localhost" );
            int port = args.getNumber( ARG_PORT, AbstractServer.DEFAULT_PORT ).intValue();
            String name = args.get( ARG_NAME, AbstractServer.DEFAULT_NAME );
            ShellClient client = ShellLobby.newClient( host, port, name );
            if ( !isCommandLine( args ) )
                System.out.println( "NOTE: Remote Neo4j graph database service '" + name + "' at port " + port );
            setSessionVariablesFromArgs( client, args );
            grabPromptOrJustExecuteCommand( client, args );
        }
        catch ( Exception e )
        {
            handleException( e, args );
        }
    }

    private static boolean isCommandLine( Args args )
    {
        return args.get( ARG_COMMAND, null ) != null;
    }

    private static void grabPromptOrJustExecuteCommand( ShellClient client, Args args ) throws Exception
    {
        String command = args.get( ARG_COMMAND, null );
        if ( command != null )
        {
            client.getServer().interpretLine( command, client.session(), client.getOutput() );
            client.shutdown();
        }
        else
        {
            client.grabPrompt();
        }
    }

    static void setSessionVariablesFromArgs(
        ShellClient client, Args args ) throws RemoteException
    {
        String profile = args.get( "profile", null );
        if ( profile != null )
        {
            applyProfileFile( new File( profile ), client );
        }

        for ( Map.Entry<String, String> entry : args.asMap().entrySet() )
        {
            String key = entry.getKey();
            if ( key.startsWith( "D" ) )
            {
                key = key.substring( 1 );
                client.session().set( key, entry.getValue() );
            }
        }
    }

    private static void applyProfileFile( File file, ShellClient client )
    {
        InputStream in = null;
        try
        {
            Properties properties = new Properties();
            properties.load( new FileInputStream( file ) );
            for ( Object key : properties.keySet() )
            {
                String stringKey = ( String ) key;
                String value = properties.getProperty( stringKey );
                client.session().set( stringKey, value );
            }
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( "Couldn't find profile '" +
                file.getAbsolutePath() + "'" );
        }
        finally
        {
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( IOException e )
                {
                    // OK
                }
            }
        }
    }

    private static void handleException( Exception e, Args args )
    {
        String message = e.getCause() instanceof ConnectException ?
                "Connection refused" : e.getMessage();
        System.err.println( "ERROR (-v for expanded information):\n\t" + message );
        if ( args.has( "v" ) )
        {
            e.printStackTrace( System.err );
        }
        System.err.println();
        printUsage();
        System.exit( 1 );
    }
    
    private static int longestString( String... strings )
    {
        int length = 0;
        for ( String string : strings )
        {
            if ( string.length() > length )
            {
                length = string.length();
            }
        }
        return length;
    }

    private static void printUsage()
    {
        int port = AbstractServer.DEFAULT_PORT;
        String name = AbstractServer.DEFAULT_NAME;
        int longestArgLength = longestString( ARG_COMMAND, ARG_CONFIG, ARG_HOST, ARG_NAME,
                ARG_PATH, ARG_PID, ARG_PORT, ARG_READONLY );
        System.out.println(
            padArg( ARG_HOST, longestArgLength ) + "Domain name or IP of host to connect to (default: localhost)\n" +
            padArg( ARG_PORT, longestArgLength ) + "Port of host to connect to (default: " + AbstractServer.DEFAULT_PORT + ")\n" +
            padArg( ARG_NAME, longestArgLength ) + "RMI name, i.e. rmi://<host>:<port>/<name> (default: " + AbstractServer.DEFAULT_NAME + ")\n" +
            padArg( ARG_PID, longestArgLength ) + "Process ID to connect to\n" +
            padArg( ARG_COMMAND, longestArgLength ) + "Command line to execute. After executing it the shell exits\n" +
            padArg( ARG_READONLY, longestArgLength ) + "Connect in readonly mode\n" +
            padArg( ARG_PATH, longestArgLength ) + "Points to a neo4j db path so that a local server can be started there\n" +
            padArg( ARG_CONFIG, longestArgLength ) + "Points to a config file when starting a local server\n\n" +
                
            "Example arguments for remote:\n" +
                "\t-" + ARG_PORT + " " + port + "\n" +
                "\t-" + ARG_HOST + " " + "192.168.1.234" + " -" + ARG_PORT + " " + port + " -" + ARG_NAME + " " + name + "\n" +
                "\t-" + ARG_HOST + " " + "localhost" + " -" + ARG_READONLY + "\n" +
                "\t...or no arguments for default values\n" +
            "Example arguments for local:\n" +
                "\t-" + ARG_PATH + " /path/to/db" + "\n" +
                "\t-" + ARG_PATH + " /path/to/db -" + ARG_CONFIG + " /path/to/neo4j.config" + "\n" +
                "\t-" + ARG_PATH + " /path/to/db -" + ARG_READONLY
        );
    }

    private static String padArg( String arg, int length )
    {
        return " -" + pad( arg, length ) + "  ";
    }

    private static String pad( String string, int length )
    {
        // Rather inefficient
        while ( string.length() < length )
        {
            string = string + " ";
        }
        return string;
    }
}
