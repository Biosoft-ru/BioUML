package ru.biosoft.table._test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;

import junit.framework.TestResult;
import junit.textui.TestRunner;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.ExtensionRegistrySupport;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * Regression test for the CI flake where {@code TestSuperAnnotateTable} failed with
 * "Incorrect column type 'ID2' expected StringSet but was String" depending on the test order
 * (gxp_dev run 45947733190).
 *
 * <p>Mechanism: {@code DataType} snapshots the {@code ru.biosoft.table.dataType} extension point in
 * its static initializer, and {@code ExtensionRegistrySupport} used to freeze an empty extension
 * list when the platform extension registry was not yet installed. A plain {@code TestCase} that
 * touches table code before any {@code AbstractBioUMLTest} subclass loads (e.g.
 * {@code TestTreeTableModel}) therefore made {@code DataType.fromClass(StringSet.class)} return
 * {@code Text} for the rest of the JVM: the extension registry appeared later, but neither class
 * ever looked again.
 *
 * <p>The fix: a registry initialized without its extension point re-loads (merging, keeping dynamic
 * entries) once the point appears, and {@code DataType} re-syncs the extension types on a lookup
 * miss until one sync has seen the point.
 */
public class DataTypeLateRegistryTest extends AbstractBioUMLTest
{
    /** ObjectExtensionRegistry with test hooks: dynamic entries and a postInit counter. */
    private static class ProbeRegistry extends ObjectExtensionRegistry<DataType>
    {
        int postInitCalls = 0;
        /** When set, postInit installs this platform registry - the point appears DURING the load. */
        IExtensionRegistry installOnPostInit;
        /** When true, loadElement calls back into this registry (re-entry). */
        boolean reenter;
        int loadCalls = 0;

        ProbeRegistry(String extensionPointId)
        {
            super( extensionPointId, DataType.class );
        }

        @Override
        protected void postInit()
        {
            postInitCalls++;
            if( installOnPostInit != null )
                Application.setExtensionRegistry( installOnPostInit );
        }

        @Override
        protected DataType loadElement(IConfigurationElement element, String elementName) throws Exception
        {
            loadCalls++;
            if( reenter )
                iterator(); // re-enter this registry while it is loading
            return super.loadElement( element, elementName );
        }

        void addDynamic(DataType type)
        {
            nameToExtension.put( type.name(), type );
            extensions.add( type );
        }

        boolean has(String name)
        {
            return nameToExtension.containsKey( name );
        }
    }

    /** Registry for a point that does not exist, with a postInit that can be told to fail. */
    private static class MissingPointRegistry extends ExtensionRegistrySupport<DataType>
    {
        int postInitCalls = 0;
        boolean failPostInit = false;

        MissingPointRegistry()
        {
            super( "ru.biosoft.no.such.point", "class" );
        }

        @Override
        protected DataType loadElement(IConfigurationElement element, String elementName)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void postInit()
        {
            postInitCalls++;
            if( failPostInit )
                throw new IllegalStateException( "postInit failed" );
        }

        void addDynamic(DataType type)
        {
            nameToExtension.put( type.name(), type );
            extensions.add( type );
        }

        int size()
        {
            init();
            return extensions.size();
        }
    }

    /**
     * A registry created (and first consulted) while no platform extension registry is installed
     * must pick the extensions up once the registry appears — and keep entries registered
     * dynamically in the meantime (BioHubRegistry adds hubs to these collections).
     */
    public void testRegistryLoadsLateAndKeepsDynamicEntries()
    {
        IExtensionRegistry real = Application.getExtensionRegistry();
        ProbeRegistry probe = new ProbeRegistry( "ru.biosoft.table.dataType" );
        try
        {
            Application.setExtensionRegistry( null );
            assertFalse( "without the platform registry the probe must serve an empty list",
                    probe.iterator().hasNext() );
            assertEquals( "postInit must run at the first initialization", 1, probe.postInitCalls );
            probe.addDynamic( DataType.Text );
            assertTrue( "the collections must stay mutable while the point is missing", probe.has( "Text" ) );
        }
        finally
        {
            Application.setExtensionRegistry( real );
        }
        assertTrue( "after the registry appears the same probe must see the dataType extensions",
                hasSetType( probe.iterator() ) );
        assertTrue( "the dynamically added entry must survive the late load", probe.has( "Text" ) );
        assertEquals( "postInit must not run again for the late load", 1, probe.postInitCalls );
    }

    /**
     * A permanently missing extension point: postInit runs exactly once, the collections are
     * mutable, and accesses do not repeat the initialization.
     */
    public void testMissingPointInitializesOnceAndStaysMutable()
    {
        MissingPointRegistry probe = new MissingPointRegistry();
        assertFalse( probe.isExtensionPointAvailable() );
        assertEquals( 0, probe.size() );
        assertEquals( 1, probe.postInitCalls );
        probe.addDynamic( DataType.Text );
        assertEquals( 1, probe.size() );
        assertEquals( "postInit must not be repeated on later accesses", 1, probe.postInitCalls );
    }

    /**
     * A failing postInit propagates once; later accesses serve the loaded extensions instead of
     * retrying (and repeating postInit's side effects), as before the late-registry change.
     */
    public void testFailingPostInitIsNotRetried()
    {
        MissingPointRegistry probe = new MissingPointRegistry();
        probe.failPostInit = true;
        try
        {
            probe.size();
            fail( "the failing postInit must propagate" );
        }
        catch( IllegalStateException expected )
        {
        }
        probe.failPostInit = false;
        assertEquals( 0, probe.size() );
        assertEquals( "postInit must not be retried", 1, probe.postInitCalls );
    }

    /**
     * The extension point appearing DURING the first load (e.g. installed by a postInit) must not
     * mark the registry as loaded: nothing was loaded then, and the next access must merge.
     */
    public void testPointAppearingDuringLoadIsStillMergedLate()
    {
        IExtensionRegistry real = Application.getExtensionRegistry();
        ProbeRegistry probe = new ProbeRegistry( "ru.biosoft.table.dataType" );
        try
        {
            Application.setExtensionRegistry( null );
            probe.installOnPostInit = real;
            assertFalse( "nothing is loaded while the point is missing",
                    probe.iterator().hasNext() );
        }
        finally
        {
            Application.setExtensionRegistry( real );
        }
        assertTrue( "the next access must merge the extensions",
                hasSetType( probe.iterator() ) );
    }

    /**
     * Subclasses that sort the list in postInit repeat it in extensionsChanged; late-loaded
     * entries must not stay appended unsorted (DynamicActionFactory, PerspectiveRegistry).
     */
    public void testLateLoadRunsExtensionsChangedHook()
    {
        IExtensionRegistry real = Application.getExtensionRegistry();
        ObjectExtensionRegistry<DataType> probe = new ObjectExtensionRegistry<DataType>( "ru.biosoft.table.dataType", DataType.class )
        {
            @Override
            protected void postInit()
            {
                sort();
            }

            @Override
            protected void extensionsChanged()
            {
                sort();
            }

            void sort()
            {
                extensions.sort( java.util.Comparator.comparing( DataType::name ) );
            }
        };
        try
        {
            Application.setExtensionRegistry( null );
            assertFalse( probe.iterator().hasNext() );
        }
        finally
        {
            Application.setExtensionRegistry( real );
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for( DataType type : probe )
            names.add( type.name() );
        java.util.List<String> sorted = new java.util.ArrayList<>( names );
        java.util.Collections.sort( sorted );
        assertEquals( "late-loaded entries must go through the extensionsChanged hook (sorted)",
                sorted, names );
        assertTrue( "extensions must be loaded", names.size() > 1 );
    }

    /**
     * A loadElement that calls back into its own registry must hit the re-entry guard on the
     * late-load path too, not recurse (the first-init path always had the guard).
     */
    public void testLateLoadHasReentryGuard()
    {
        IExtensionRegistry real = Application.getExtensionRegistry();
        ProbeRegistry probe = new ProbeRegistry( "ru.biosoft.table.dataType" );
        try
        {
            Application.setExtensionRegistry( null );
            assertFalse( probe.iterator().hasNext() );
            probe.reenter = true;
        }
        finally
        {
            Application.setExtensionRegistry( real );
        }
        probe.iterator(); // late load with a re-entering loadElement
        assertTrue( "the re-entry must be stopped by the guard, not recurse: " + probe.loadCalls
                + " load calls", probe.loadCalls < 20 );
    }

    /**
     * A DataType lookup that misses because the snapshot ran before the extension registry was
     * installed must re-sync and find the extension type, not degrade to Text for the whole JVM.
     * Simulated by removing the Set type from the maps and clearing the synced flag, as after an
     * early snapshot.
     */
    @SuppressWarnings ( "unchecked" )
    public void testDataTypeResyncsAfterEarlySnapshot() throws Exception
    {
        Field registryField = DataType.class.getDeclaredField( "registry" );
        Field classMapField = DataType.class.getDeclaredField( "classMap" );
        Field syncedField = DataType.class.getDeclaredField( "extensionsSynced" );
        registryField.setAccessible( true );
        classMapField.setAccessible( true );
        syncedField.setAccessible( true );
        Map<String, DataType> registry = (Map<String, DataType>) registryField.get( null );
        Map<Class<?>, DataType> classMap = (Map<Class<?>, DataType>) classMapField.get( null );

        try
        {
            // Simulate the early snapshot: the Set type is not in the maps and no sync has seen
            // the extension point yet (this is also the natural state when a plain TestCase
            // touched DataType before the harness installed the registry).
            classMap.remove( StringSet.class );
            syncedField.setBoolean( null, false );
            DataType resolved = DataType.fromClass( StringSet.class );
            assertEquals( "a fromClass miss must re-sync the extension types", StringSet.class, resolved.getType() );

            String setName = resolved.name();
            registry.remove( setName );
            classMap.remove( StringSet.class );
            syncedField.setBoolean( null, false );
            assertEquals( "a fromString miss must re-sync too", StringSet.class,
                    DataType.fromString( setName ).getType() );
        }
        finally
        {
            syncedField.setBoolean( null, false );
            DataType.fromClass( StringSet.class ); // restore the maps for other tests
        }
    }

    /**
     * End-to-end reproduction of the CI failure in a fresh JVM: a plain TestCase touching table
     * code runs before any AbstractBioUMLTest subclass installs the extension registry, then
     * TestSuperAnnotateTable must still see the Set data type.
     */
    public void testEarlyTouchInFreshJvm() throws Exception
    {
        String javaBin = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        ProcessBuilder pb = new ProcessBuilder( javaBin,
                "--add-opens", "java.base/java.util=ALL-UNNAMED",
                "--add-opens", "java.desktop/sun.java2d=ALL-UNNAMED",
                "--add-opens", "java.sql/java.sql=ALL-UNNAMED",
                "-cp", System.getProperty( "java.class.path" ),
                DataTypeEarlyTouchMain.class.getName() );
        // Output goes to a file: reading the pipe would block past the timeout if the child hung.
        File outFile = File.createTempFile( "earlyTouch", ".log" );
        outFile.deleteOnExit();
        pb.redirectErrorStream( true );
        pb.redirectOutput( outFile );
        Process process = pb.start();
        boolean finished = process.waitFor( 300, TimeUnit.SECONDS );
        if( !finished )
            process.destroyForcibly();
        String output = new String( java.nio.file.Files.readAllBytes( outFile.toPath() ) );
        assertTrue( "the child JVM did not finish in time; output so far:\n" + output, finished );
        assertEquals( "the early table touch must not break later type lookups; child output:\n" + output,
                0, process.exitValue() );
    }

    /** Entry point of the fresh-JVM reproduction, run by {@link #testEarlyTouchInFreshJvm}. */
    public static class DataTypeEarlyTouchMain
    {
        public static void main(String[] args)
        {
            // TestTreeTableModel is a plain TestCase and touches table code, as in the CI failure;
            // it runs before any AbstractBioUMLTest subclass installs the extension registry.
            TestRunner runner = new TestRunner();
            TestResult early = runner.doRun( new junit.framework.TestSuite( ru.biosoft.treetable._test.TestTreeTableModel.class ) );
            TestResult annotate = runner.doRun( new junit.framework.TestSuite( ru.biosoft.analysis._test.TestSuperAnnotateTable.class ) );
            System.exit( early.wasSuccessful() && annotate.wasSuccessful() ? 0 : 1 );
        }
    }

    private static boolean hasSetType(Iterator<DataType> it)
    {
        while( it.hasNext() )
        {
            if( it.next().getType() == StringSet.class )
                return true;
        }
        return false;
    }
}
