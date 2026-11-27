package ru.biosoft.table._test;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.datatype.DataType;
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
 * <p>The fix: the extension registry retries until the extension point is actually available, and
 * {@code DataType} re-syncs the extension types on a lookup miss.
 */
public class DataTypeLateRegistryTest extends AbstractBioUMLTest
{
    /**
     * A registry created (and first consulted) while no platform extension registry is installed
     * must pick the extensions up once the registry appears, not stay empty forever.
     */
    public void testRegistryRetriesUntilExtensionPointAppears()
    {
        IExtensionRegistry real = Application.getExtensionRegistry();
        ObjectExtensionRegistry<DataType> probe = new ObjectExtensionRegistry<>( "ru.biosoft.table.dataType", DataType.class );
        try
        {
            Application.setExtensionRegistry( null );
            assertFalse( "without the platform registry the probe must serve an empty list",
                    probe.iterator().hasNext() );
        }
        finally
        {
            Application.setExtensionRegistry( real );
        }
        assertTrue( "after the registry appears the same probe must see the dataType extensions",
                hasSetType( probe.iterator() ) );
    }

    /**
     * A DataType lookup that misses because the snapshot ran before the extension registry was
     * installed must re-sync and find the extension type, not degrade to Text for the whole JVM.
     * Simulated by removing the Set type from the maps, as if the snapshot had run without it.
     */
    @SuppressWarnings ( "unchecked" )
    public void testDataTypeResyncsAfterEarlySnapshot() throws Exception
    {
        Field registryField = DataType.class.getDeclaredField( "registry" );
        Field classMapField = DataType.class.getDeclaredField( "classMap" );
        registryField.setAccessible( true );
        classMapField.setAccessible( true );
        Map<String, DataType> registry = (Map<String, DataType>) registryField.get( null );
        Map<Class<?>, DataType> classMap = (Map<Class<?>, DataType>) classMapField.get( null );

        // In the harness JVM the Set type is present; remove it to simulate the early snapshot.
        DataType set = classMap.get( StringSet.class );
        assertNotNull( "precondition: the harness registry provides the Set data type", set );
        registry.remove( set.name() );
        classMap.remove( StringSet.class );

        DataType resolved = DataType.fromClass( StringSet.class );
        assertEquals( "a lookup miss must re-sync the extension types", StringSet.class, resolved.getType() );
        assertNotNull( "fromString must re-sync too", DataType.fromString( set.name() ) );
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
