package ru.biosoft.plugins.javascript._test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.plugins.javascript.ScriptClassShutter;

/** The class list of {@link ScriptClassShutter}, and that scripts run through {@link JScriptContext} get it. */
public class ScriptClassShutterTest extends AbstractBioUMLTest
{
    @Override
    protected void tearDown() throws Exception
    {
        ScriptClassShutter.setInstance( null );
        super.tearDown();
    }

    private static ScriptClassShutter shutter(ScriptClassShutter.Mode mode, String... fileLines)
    {
        return ScriptClassShutter.create( mode, Arrays.asList( fileLines ) );
    }

    public void testDefaultListAllowsTheScriptingApi()
    {
        ScriptClassShutter s = shutter( ScriptClassShutter.Mode.ENFORCE );
        for( String name : new String[] {"java.lang.String", "java.lang.Double", "java.util.ArrayList", "java.util.HashMap$Node",
                "java.awt.image.BufferedImage", "ru.biosoft.access.core.DataElementPath", "ru.biosoft.table.TableDataCollectionUtils",
                "ru.biosoft.table.RowDataElement", "biouml.model.util.DiagramImageGenerator", "biouml.plugins.cma.CMAUtils",
                "ru.biosoft.access.security.ProtectedDataCollection", "java.awt.Point", "ru.biosoft.util.LazyStringBuilder",
                "biouml.plugins.sbml.SbmlEModel", "[Ljava.lang.String;", "[D",
                // seen in the audit log of Genome Enhancer runs
                "ru.biosoft.bsa.MapAsVector", "ru.biosoft.bsa.project.ProjectAsLists", "biouml.plugins.ensembl.tracks.ExtendedGeneTrack",
                "biouml.plugins.cma.FullModelTrack", "biouml.plugins.cma.CMAUtils", "ru.biosoft.graphics.CompositeView",
                "ru.biosoft.graphics.access.ChartDataElement", "com.developmentontheedge.beans.PropertiesDPS",
                // and later: finding and running analyses and workflows
                "com.developmentontheedge.beans.DynamicProperty", "ru.biosoft.analysiscore.AnalysisMethodRegistry",
                "ru.biosoft.analysiscore.AnalysisMethodInfo", "com.genexplain.analyses.MEALR",
                "biouml.plugins.research.workflow.engine.WorkflowEngine",
                "biouml.plugins.research.workflow.items.WorkflowItemFactory"} )
            assertTrue( name, s.visibleToScripts( name ) );
    }

    public void testDefaultListHidesWhatReachesPastTheApi()
    {
        ScriptClassShutter s = shutter( ScriptClassShutter.Mode.ENFORCE );
        for( String name : new String[] {"java.io.File", "java.nio.file.Files", "java.net.URL", "java.sql.DriverManager",
                "java.lang.Runtime", "java.lang.ProcessBuilder", "java.lang.System", "java.lang.Thread", "java.lang.Class",
                "java.lang.ClassLoader", "java.lang.reflect.Method", "java.util.concurrent.Executors", "java.util.zip.ZipFile",
                "java.util.ServiceLoader", "ru.biosoft.access.core.CollectionFactory", "ru.biosoft.access.sql.SqlUtil",
                "ru.biosoft.access.security.SecurityManager", "ru.biosoft.access.script.ScriptTypeRegistry",
                "ru.biosoft.plugins.javascript.JScriptContext", "ru.biosoft.plugins.javascript.ScriptClassShutter",
                "ru.biosoft.util.ApplicationUtils", "ru.biosoft.util.FileItem", "[Ljava.io.File;",
                // also seen in the audit log, and kept hidden: files, properties files, server preferences, the registry
                "ru.biosoft.util.ExProperties", "com.developmentontheedge.beans.Preferences", "ru.biosoft.access.CollectionFactoryUtils",
                "ru.biosoft.bsa.importer.SomeImporter", "ru.biosoft.bsa.exporter.SomeExporter"} )
            assertFalse( name, s.visibleToScripts( name ) );
    }

    public void testFileEntriesExtendTheList()
    {
        ScriptClassShutter s = shutter( ScriptClassShutter.Mode.ENFORCE, "# extra classes", "allow ru.biosoft.util.TextUtil2  # ok",
                "allow com.example.", "deny com.example.Secret", "" );
        assertTrue( s.visibleToScripts( "ru.biosoft.util.TextUtil2" ) );
        assertTrue( s.visibleToScripts( "com.example.Thing" ) );
        assertFalse( s.visibleToScripts( "com.example.Secret" ) );
        assertFalse( s.visibleToScripts( "ru.biosoft.util.TempFiles" ) );
        try
        {
            shutter( ScriptClassShutter.Mode.ENFORCE, "permit java.io.File" );
            fail( "a malformed line was accepted" );
        }
        catch( IllegalArgumentException e )
        {
            assertTrue( e.getMessage(), e.getMessage().contains( "permit java.io.File" ) );
        }
    }

    public void testModes()
    {
        assertEquals( ScriptClassShutter.Mode.AUDIT, ScriptClassShutter.parseMode( null ) );
        assertEquals( ScriptClassShutter.Mode.AUDIT, ScriptClassShutter.parseMode( "" ) );
        assertEquals( ScriptClassShutter.Mode.ENFORCE, ScriptClassShutter.parseMode( " Enforce " ) );
        assertEquals( ScriptClassShutter.Mode.OFF, ScriptClassShutter.parseMode( "off" ) );
        // a typo must not switch the protection off
        assertEquals( ScriptClassShutter.Mode.ENFORCE, ScriptClassShutter.parseMode( "enforced" ) );

        // from preferences.xml
        assertEquals( ScriptClassShutter.Mode.AUDIT, ScriptClassShutter.modeFrom( null ) );
        com.developmentontheedge.beans.Preferences preferences = new com.developmentontheedge.beans.Preferences();
        assertEquals( ScriptClassShutter.Mode.AUDIT, ScriptClassShutter.modeFrom( preferences ) );
        preferences.add( new com.developmentontheedge.beans.DynamicProperty( ScriptClassShutter.MODE_PREFERENCE, String.class, "enforce" ) );
        assertEquals( ScriptClassShutter.Mode.ENFORCE, ScriptClassShutter.modeFrom( preferences ) );

        ScriptClassShutter audit = shutter( ScriptClassShutter.Mode.AUDIT );
        assertTrue( "audit allows, and logs", audit.visibleToScripts( "java.io.File" ) );
        assertTrue( audit.visibleToScripts( "java.io.File" ) );
    }

    /** What a script evaluated through JScriptContext on a fresh thread (a new Rhino context) returns. */
    private static String evaluate(String script) throws InterruptedException
    {
        return onFreshThread( () -> JScriptContext.evaluateString( script ) );
    }

    private static String onFreshThread(java.util.concurrent.Callable<String> action) throws InterruptedException
    {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread( () -> {
            try
            {
                result.set( action.call() );
            }
            catch( Throwable t )
            {
                failure.set( t );
            }
        } );
        thread.start();
        thread.join( 60_000 );
        assertFalse( "script did not finish", thread.isAlive() );
        if( failure.get() != null )
            throw new AssertionError( failure.get() );
        return result.get();
    }

    private static final String PROBE = "var r; try { r = 'visible: ' + new java.io.File('x').getName(); }"
            + " catch( e ) { r = 'hidden: ' + e.message; } r";

    public void testScriptsGetTheShutter() throws Exception
    {
        ScriptClassShutter.setInstance( shutter( ScriptClassShutter.Mode.ENFORCE ) );
        String hidden = evaluate( PROBE );
        assertTrue( hidden, hidden.startsWith( "hidden: " ) && hidden.contains( "java.io.File" ) );
        assertEquals( "3", evaluate( "var l = new java.util.ArrayList(); l.add('a'); l.add('b'); l.add('c'); '' + l.size()" ) );
        assertEquals( "x/y", evaluate( "'' + Packages.ru.biosoft.access.core.DataElementPath.create('x/y')" ) );

        ScriptClassShutter.setInstance( shutter( ScriptClassShutter.Mode.AUDIT ) );
        assertEquals( "visible: x", evaluate( PROBE ) );

        ScriptClassShutter.setInstance( shutter( ScriptClassShutter.Mode.OFF ) );
        assertEquals( "visible: x", evaluate( PROBE ) );
    }

    /** A script's variables as a workflow script node binds them, then the script; on a fresh thread. */
    private static String withVariables(String script) throws InterruptedException
    {
        return onFreshThread( () -> {
            org.mozilla.javascript.Context context = JScriptContext.getContext();
            org.mozilla.javascript.ScriptableObject scope = JScriptContext.getScope();
            java.util.Map<String, Object> variables = new java.util.LinkedHashMap<>();
            variables.put( "importPath", new ru.biosoft.util.FileItem( "x" ) );
            variables.put( "name", "abc" );
            variables.put( "path", ru.biosoft.access.core.DataElementPath.create( "a/b" ) );
            ru.biosoft.plugins.javascript.JavaScriptUtils.defineVariables( scope, variables );
            return JScriptContext.evaluateString( context, scope, script, null );
        } );
    }

    /**
     * A workflow script node gets every workflow variable, the system ones included (importPath is a file): in enforce
     * mode a hidden one is left out, so a script that does not use it still runs. In audit mode it is bound as before.
     */
    public void testHiddenVariablesAreLeftOut() throws Exception
    {
        String script = "typeof importPath + ',' + name + ',' + path";
        ScriptClassShutter.setInstance( shutter( ScriptClassShutter.Mode.ENFORCE ) );
        assertEquals( "undefined,abc,a/b", withVariables( script ) );
        ScriptClassShutter.setInstance( shutter( ScriptClassShutter.Mode.AUDIT ) );
        assertEquals( "object,abc,a/b", withVariables( script ) );
    }

    /** The log lines of the shutter while the action runs. */
    private static java.util.List<String> logged(java.util.concurrent.Callable<?> action) throws Exception
    {
        java.util.List<String> messages = java.util.Collections.synchronizedList( new java.util.ArrayList<>() );
        java.util.logging.Handler handler = new java.util.logging.Handler()
        {
            @Override
            public void publish(java.util.logging.LogRecord record)
            {
                messages.add( record.getMessage() );
            }

            @Override
            public void flush()
            {
            }

            @Override
            public void close()
            {
            }
        };
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger( ScriptClassShutter.class.getName() );
        logger.addHandler( handler );
        try
        {
            action.call();
        }
        finally
        {
            logger.removeHandler( handler );
        }
        return messages;
    }

    /**
     * A logged class names the script and line that asked, and the thread, so the audit log says which workflow node or
     * script needs a class: interpreted (as workflow script nodes run) and compiled.
     */
    public void testLogNamesTheScript() throws Exception
    {
        for( int optimization : new int[] {-1, 0} )
        {
            ScriptClassShutter.setInstance( shutter( ScriptClassShutter.Mode.AUDIT ) );
            java.util.List<String> messages = logged( () -> onFreshThread( () -> {
                org.mozilla.javascript.Context context = JScriptContext.getContext();
                context.setOptimizationLevel( optimization );
                return JScriptContext.evaluateString( context, JScriptContext.getScope(),
                        "var a = 1;\nvar f = new java.io.File('x');\n'' + f.getName()", "Find regulators node", null );
            } ) );
            String message = String.join( "\n", messages );
            assertTrue( message, message.contains( "java.io.File" ) );
            assertTrue( optimization + ": " + message, message.contains( "script Find regulators node:2, thread " ) );
        }
    }

    /** Outside a script (e.g. while a workflow node's variables are bound) the log says so, and gives the thread. */
    public void testLogOutsideAScript() throws Exception
    {
        ScriptClassShutter audit = shutter( ScriptClassShutter.Mode.AUDIT );
        java.util.List<String> messages = logged( () -> audit.visibleToScripts( "java.io.File" ) );
        assertEquals( 1, messages.size() );
        assertTrue( messages.get( 0 ), messages.get( 0 ).contains( "no script running, thread " + Thread.currentThread().getName() ) );
    }
}
