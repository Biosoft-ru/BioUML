package biouml.plugins.simulation._test;

import biouml.plugins.simulation.SimulationEngine;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Verifies the shared {@link URLClassLoader} cache used by {@link SimulationEngine#compileModel} to
 * fix the per-run class loader leak. The fix returns a cached loader for an unchanged model revision
 * and a fresh loader once the model source is regenerated, while keeping the number of live loaders
 * bounded.
 *
 * <p>This test exercises the caching contract directly (without Velocity code generation, which is not
 * available headless) by creating real class/source files in a temp directory. The cache method is
 * package-private in {@code biouml.plugins.simulation}, so it is reached via reflection.
 */
public class ModelClassLoaderCacheTest extends TestCase
{
    public ModelClassLoaderCacheTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( ModelClassLoaderCacheTest.class.getName() );
        suite.addTest( new ModelClassLoaderCacheTest( "testSameRevisionReusesLoader" ) );
        suite.addTest( new ModelClassLoaderCacheTest( "testChangedRevisionCreatesNewLoader" ) );
        suite.addTest( new ModelClassLoaderCacheTest( "testDifferentClassesUseDifferentLoaders" ) );
        return suite;
    }

    private static Method loaderMethod() throws Exception
    {
        Method m = SimulationEngine.class.getDeclaredMethod( "modelClassLoader", File.class, File.class, ClassLoader.class );
        m.setAccessible( true );
        return m;
    }

    private static URLClassLoader loader(File classFile, File sourceFile) throws Exception
    {
        return (URLClassLoader)loaderMethod().invoke( null, classFile, sourceFile,
                ModelClassLoaderCacheTest.class.getClassLoader() );
    }

    private static File touch(File f, long lastModified) throws Exception
    {
        if( !f.exists() && !f.createNewFile() )
            throw new Exception( "Cannot create " + f );
        if( !f.setLastModified( lastModified ) )
            throw new Exception( "Cannot set mtime on " + f );
        return f;
    }

    public void testSameRevisionReusesLoader() throws Exception
    {
        File dir = Files.createTempDirectory( "mclcache" ).toFile();
        File cls = touch( new File( dir, "Pz_a.class" ), 1000L );
        File src = touch( new File( dir, "Pz_a.java" ), 1000L );

        URLClassLoader first = loader( cls, src );
        URLClassLoader second = loader( cls, src );

        assertNotNull( first );
        assertSame( "An unchanged model revision must reuse the same cached loader", first, second );
    }

    public void testChangedRevisionCreatesNewLoader() throws Exception
    {
        File dir = Files.createTempDirectory( "mclcache" ).toFile();
        File cls = touch( new File( dir, "Pz_b.class" ), 2000L );
        File src = touch( new File( dir, "Pz_b.java" ), 2000L );

        URLClassLoader first = loader( cls, src );

        // Simulate the model being regenerated: the source file is rewritten (new lastModified).
        try( FileWriter w = new FileWriter( src ) )
        {
            w.write( "// regenerated\n" );
        }
        touch( src, 3000L );

        URLClassLoader second = loader( cls, src );
        assertNotNull( second );
        assertNotSame( "A regenerated model (new source revision) must get a fresh loader", first, second );
    }

    public void testDifferentClassesUseDifferentLoaders() throws Exception
    {
        File dir = Files.createTempDirectory( "mclcache" ).toFile();
        File clsA = touch( new File( dir, "Pz_c.class" ), 4000L );
        File srcA = touch( new File( dir, "Pz_c.java" ), 4000L );
        File clsB = touch( new File( dir, "Pz_d.class" ), 4000L );
        File srcB = touch( new File( dir, "Pz_d.java" ), 4000L );

        URLClassLoader a = loader( clsA, srcA );
        URLClassLoader b = loader( clsB, srcB );

        assertNotNull( a );
        assertNotNull( b );
        assertNotSame( "Distinct model classes in the same directory must not share a loader", a, b );
    }
}
