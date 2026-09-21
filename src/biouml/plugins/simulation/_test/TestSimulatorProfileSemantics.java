package biouml.plugins.simulation._test;

import junit.framework.TestCase;

import biouml.plugins.simulation.SimulatorProfile;

/**
 * Establishes the intended semantics of SimulatorProfile.getX() after the
 * setX() buffer-reuse optimization: the returned array is NOT a snapshot.
 * While the state-vector length is unchanged, successive setX() calls
 * overwrite the same backing array, so callers must not retain a getX()
 * reference across steps (they must copy it out if they need the old value).
 */
public class TestSimulatorProfileSemantics extends TestCase
{
    public TestSimulatorProfileSemantics(String name)
    {
        super(name);
    }

    public void testSetXReusesBufferForSameLength()
    {
        SimulatorProfile profile = new SimulatorProfile();
        double[] x1 = { 1.0, 2.0, 3.0 };
        double[] x2 = { 4.0, 5.0, 6.0 };

        profile.setX( x1 );
        double[] ref = profile.getX();
        profile.setX( x2 );

        // Same backing array: the earlier reference now sees the new values
        assertSame( ref, profile.getX() );
        assertSameContents( x2, ref );
    }

    public void testSetXAllocatesNewBufferWhenLengthChanges()
    {
        SimulatorProfile profile = new SimulatorProfile();
        profile.setX( new double[] { 1.0, 2.0 } );
        double[] ref = profile.getX();
        profile.setX( new double[] { 1.0, 2.0, 3.0 } );

        assertNotSame( ref, profile.getX() );
        // The old reference is untouched by the larger setX
        assertEquals( 2, ref.length );
        assertEquals( 1.0, ref[0], 0.0 );
        assertEquals( 2.0, ref[1], 0.0 );
        assertSameContents( new double[] { 1.0, 2.0, 3.0 }, profile.getX() );
    }

    public void testInitSetsBackingArrayDirectly()
    {
        // init() assigns the array by reference (no copy) — preserved
        SimulatorProfile profile = new SimulatorProfile();
        double[] x = { 7.0, 8.0 };
        profile.init( x, 0.0 );
        assertSame( x, profile.getX() );
    }

    public void testInitThenSetXReusesBuffer()
    {
        SimulatorProfile profile = new SimulatorProfile();
        profile.init( new double[] { 1.0, 2.0 }, 0.0 );
        double[] ref = profile.getX();
        profile.setX( new double[] { 9.0, 10.0 } );

        assertSame( ref, profile.getX() );
        assertEquals( 9.0, ref[0], 0.0 );
        assertEquals( 10.0, ref[1], 0.0 );
    }

    public void testSetXCopiesInputSoCallerCannotMutateProfile()
    {
        // setX() copies the input into the backing buffer, so later mutation
        // of the caller's array must not affect the profile.
        SimulatorProfile profile = new SimulatorProfile();
        double[] x1 = { 1.0, 2.0, 3.0 };
        profile.setX( x1 );
        x1[0] = 99.0;
        assertEquals( 1.0, profile.getX()[0], 0.0 );
        assertEquals( 2.0, profile.getX()[1], 0.0 );
        assertEquals( 3.0, profile.getX()[2], 0.0 );
    }

    private static void assertSameContents(double[] expected, double[] actual)
    {
        assertEquals( expected.length, actual.length );
        for( int i = 0; i < expected.length; i++ )
            assertEquals( "element " + i, expected[i], actual[i], 0.0 );
    }
}
