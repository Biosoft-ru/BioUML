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
                "biouml.plugins.sbml.SbmlEModel", "[Ljava.lang.String;", "[D"} )
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
                "ru.biosoft.util.ApplicationUtils", "ru.biosoft.util.FileItem", "[Ljava.io.File;"} )
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
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread( () -> {
            try
            {
                result.set( JScriptContext.evaluateString( script ) );
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
}
