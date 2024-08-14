package ru.biosoft.plugins.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Main;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.BiosoftIconManager;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionListenerRegistry;
import ru.biosoft.access.QuerySystemRegistry;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.util.HtmlUtil;
import ru.biosoft.workbench.Framework;

/**
 * JavaScript shell.
 *
 * Can execute scripts interactively or in batch mode at the command line.
 *
 * @pending usage message
 * @pending errorReporter
 * @pending options: -e, -w
 * @pending exit from context
 */
public class JScriptShell implements IApplication
{
    // command line argument constants
    public static final String VERSION = "-js.version";
    public static final String OPTIMISATION_LEVEL = "-js.opt";
    public static final String REPORT_WARNINGS = "-js.warn";
    public static final String EVALUATE = "-js.e";
    public static final String PROCESS_FILE = "-js.f";

    protected static final Logger log = Logger.getLogger(JScriptShell.class.getName());
    protected boolean processStdin = true;
    protected List<String> fileList = new ArrayList<>();

    /** Top level function that starts the shell. */
    @Override
    public Object start(IApplicationContext arg0)
    {
        AccessCoreInit.init();
        QuerySystemRegistry.initQuerySystems();
        DataCollectionListenerRegistry.initDataCollectionListeners();

        configureJUL();
        loadPreferences();

        String[] commandLineArgs = new String[0];
        Object arg = arg0.getArguments().get( "application.args" );
        if( arg instanceof String[] )
            commandLineArgs = (String[])arg;

        processRepositories(commandLineArgs);
        processOptions(commandLineArgs);
        if( processStdin )
            fileList.add(null);

        StreamEx.of(fileList).forEach( this::processSource );

        return IApplication.EXIT_OK;
    }

    private void loadPreferences()
    {
        String fileName = Platform.getInstallLocation().getURL().getPath() + "preferences.xml";
        Preferences preferences = new Preferences();
        try
        {
            ClassLoader cl = this.getClass().getClassLoader();
            preferences.load(fileName, cl);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Load preferences error", t);
        }

        Application.setPreferences(preferences);        
    }

    protected static void configureJUL()
    {
        //configure JUL logging
        Handler[] handlers = log.getHandlers();
        if( handlers.length == 0 )
        {
            try (java.io.InputStream fis = Platform.getBundle( "ru.biosoft.plugins.javascript" ).getEntry( "shell.lcf" ).openStream())
            {
                LogManager.getLogManager().readConfiguration( fis );
            }
            catch( IOException e )
            {
                System.out.println( "Cannot configure java.util.logging.Logger: " + e.getMessage() );
                e.printStackTrace( System.out );
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // properties
    //

    private InputStream inputStream;
    public InputStream getInputStream()
    {
        return inputStream == null ? System.in : inputStream;
    }

    public void setInputStream(InputStream in)
    {
        inputStream = in;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utility functions
    //

    /**
     * Print a usage message.
     *
     * @pending provide own implementation
     */
    public void usage(String s)
    {
        Main.getGlobal().getOut().println(s);
    }

    public void print(String msg)
    {
        System.out.print(msg);
    }

    public void println(String msg)
    {
        System.out.println(msg);
    }

    ////////////////////////////////////////////////////////////////////////////

    protected void processRepositories(String args[])
    {
        for(int i=0; i<args.length; i++ )
        {
            String arg = args[i];

            if( !arg.startsWith("-js") )
            {
                // initialise BioUML repository
                try
                {
                    log.config("Load BioUML databases: " + arg);
                    CollectionFactoryUtils.init();
                    Framework.initRepository(arg);
                }
                catch( Throwable t )
                {
                    String msg = "Can not initialise BioUML repository, error: " + t.getMessage();
                    log.log(Level.SEVERE, msg, t);
                }
            }
            else
            {
                i++;
            }
        }
        Plugins.getPlugins();
    }

    /** Parse command line arguments. */
    protected void processOptions(String args[])
    {
        Context cx = JScriptContext.getContext();

        for( int i = 0; i < args.length; i++ )
        {
            String arg = args[i];

            if( arg.startsWith("-js") )
            {
                if( arg.equals(VERSION) )
                {
                    if( ++i == args.length )
                        usage(arg);
                    double d = Context.toNumber(args[i]);
                    cx.setLanguageVersion((int)d);
                    log.log(Level.FINE, "Set version: " + d);
                    continue;
                }

                if( arg.equals(OPTIMISATION_LEVEL) )
                {
                    if( ++i == args.length )
                        usage(arg);
                    double d = Context.toNumber(args[i]);
                    cx.setOptimizationLevel((int)d);
                    log.log(Level.FINE, "Set optimisation level: " + d);
                    continue;
                }

                if( arg.equals(EVALUATE) )
                {
                    processStdin = false;
                    if( ++i == args.length )
                        usage(arg);

                    log.info("evaluate: " + args[i]);
                    Object result = JScriptContext.evaluateString(args[i]);
                    log.info(result.toString());
                    continue;
                }

                if( arg.equals(REPORT_WARNINGS) )
                {
                    log.log(Level.FINE, "Report warnings: enabled");
                    continue;
                }

                if( arg.equals(PROCESS_FILE) )
                {
                    processStdin = false;
                    if( ++i == args.length )
                        usage(arg);
                    String file = args[i].equals("-") ? null : args[i];
                    fileList.add(file);
                    log.log(Level.FINE, "Process file: " + file);
                    continue;
                }

                usage(arg);
            }
        }
    }

    /**
     * Evaluate JavaScript source.
     *
     * @param cx the current context
     * @param filename the name of the file to compile, or null for interactive mode.
     */
    protected void processSource(String filename)
    {
        Context cx = JScriptContext.getContext();
        if( filename != null && !filename.equals("-") )
        {
            JScriptContext.processFile(filename);
        }
        else
        {
            // Use the interpreter for interactive input
            cx.setOptimizationLevel( -1);

            try( BufferedReader in = new BufferedReader( new InputStreamReader( getInputStream() ) ) )
            {
                int lineno = 1;

                JScriptShellEnvironment environment = new JScriptShellEnvironment( System.out );

                boolean hitEOF = false;
                while( !hitEOF )
                {
                    StringBuilder source = new StringBuilder();
                    int startline = lineno;

                    if( filename == null )
                        print( "js> " );

                    // Collect lines of source to compile.
                    while( true )
                    {
                        String newline;
                        try
                        {
                            newline = in.readLine();
                        }
                        catch( IOException ioe )
                        {
                            log.log( Level.SEVERE, ioe.toString(), ioe );
                            break;
                        }

                        if( newline == null )
                        {
                            hitEOF = true;
                            break;
                        }

                        source.append( newline ).append( '\n' );
                        lineno++;

                        if( cx.stringIsCompilableUnit( source.toString() ) )
                            break;
                    }

                    Object result = JScriptContext.evaluateString( source.toString(), environment );
                    if( !result.toString().isEmpty() && !result.toString().equals( "undefined" ) )
                        println( HtmlUtil.convertToText( result.toString() ) );

                    if( result.equals( Global.QUIT ) )
                        hitEOF = true;
                }
            }
            catch( IOException e )
            {
                log.log( Level.SEVERE, "Error during file '" + filename + "' processing.", e );
            }
        }
    }

    @Override
    public void stop()
    {
    }
}
