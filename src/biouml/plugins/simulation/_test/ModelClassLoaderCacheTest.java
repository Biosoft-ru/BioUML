package biouml.plugins.simulation._test;

import biouml.plugins.simulation.SimulationEngine;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Verifies the shared {@link URLClassLoader} cache used by {@link SimulationEngine#compileModel} to
 * fix the per-run class loader leak.
 *
 * <p>Two kinds of checks are made:
 * <ul>
 *   <li>the <b>caching contract</b> — an unchanged model revision reuses one loader, a regenerated
 *       revision (new source fingerprint) gets a fresh loader, distinct classes never share a loader;
 *   <li><b>actual class loading</b> — a small class is really compiled and loaded through the cache,
 *       and after regeneration the fresh loader exposes the <em>new</em> definition (different class
 *       identity, new behaviour) rather than a stale one. This is the reason the cache exists.
 * </ul>
 *
 * <p>The cache method is package-private in {@code biouml.plugins.simulation}, so it is reached via
 * reflection.
 */
public class ModelClassLoaderCacheTest extends TestCase
{
    private File tempDir;

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
        suite.addTest( new ModelClassLoaderCacheTest( "testSameParentDifferentParentsUseDifferentLoaders" ) );
        suite.addTest( new ModelClassLoaderCacheTest( "testRegeneratedModelLoadsNewDefinition" ) );
        return suite;
    }

    protected void setUp() throws Exception
    {
        tempDir = Files.createTempDirectory( "mclcache" ).toFile();
    }

    protected void tearDown()
    {
        // Best-effort cleanup of the temp tree.
        deleteRecursively( tempDir );
    }

    private static void deleteRecursively(File f)
    {
        if( f == null || !f.exists() )
            return;
        File[] children = f.listFiles();
        if( children != null )
            for( File c : children )
                deleteRecursively( c );
        f.delete();
    }

    // --- helpers to reach the package-private cache method -------------------------------

    private static Method loaderMethod() throws Exception
    {
        Method m = SimulationEngine.class.getDeclaredMethod( "modelClassLoader", File.class, File.class, ClassLoader.class );
        m.setAccessible( true );
        return m;
    }

    private static URLClassLoader loader(File classFile, File sourceFile, ClassLoader parent) throws Exception
    {
        return (URLClassLoader)loaderMethod().invoke( null, classFile, sourceFile, parent );
    }

    private static File touch(File f, long lastModified) throws Exception
    {
        if( !f.exists() && !f.createNewFile() )
            throw new Exception( "Cannot create " + f );
        if( !f.setLastModified( lastModified ) )
            throw new Exception( "Cannot set mtime on " + f );
        return f;
    }

    /** Writes {@code src} and compiles it into the same directory using the JDK compiler. */
    private static void writeAndCompile(File source, String code) throws Exception
    {
        try( FileWriter w = new FileWriter( source ) )
        {
            w.write( code );
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull( "System Java compiler must be available", compiler );
        boolean ok;
        try( StandardJavaFileManager fm = compiler.getStandardFileManager( null, null, null ) )
        {
            javax.tools.JavaFileObject unit =
                    fm.getJavaFileObjectsFromStrings( java.util.Collections.singletonList( source.getPath() ) ).iterator().next();
            java.util.List<javax.tools.JavaFileObject> units = java.util.Collections.singletonList( unit );
            ok = compiler.getTask( null, fm, null,
                    java.util.Arrays.asList( "-d", source.getParent() ), null, units ).call();
        }
        assertTrue( "Compilation of " + source + " must succeed", ok );
    }

    // --- tests ---------------------------------------------------------------------------

    public void testSameRevisionReusesLoader() throws Exception
    {
        File cls = touch( new File( tempDir, "Pz_a.class" ), 1000L );
        File src = touch( new File( tempDir, "Pz_a.java" ), 1000L );

        URLClassLoader first = loader( cls, src, getClass().getClassLoader() );
        URLClassLoader second = loader( cls, src, getClass().getClassLoader() );

        assertNotNull( first );
        assertSame( "An unchanged model revision must reuse the same cached loader", first, second );
    }

    public void testChangedRevisionCreatesNewLoader() throws Exception
    {
        File cls = touch( new File( tempDir, "Pz_b.class" ), 2000L );
        File src = touch( new File( tempDir, "Pz_b.java" ), 2000L );

        URLClassLoader first = loader( cls, src, getClass().getClassLoader() );

        // Regenerate the model: rewrite the source and give it a new fingerprint.
        try( FileWriter w = new FileWriter( src ) )
        {
            w.write( "// regenerated\n" );
        }
        touch( src, 3000L );

        URLClassLoader second = loader( cls, src, getClass().getClassLoader() );
        assertNotNull( second );
        assertNotSame( "A regenerated model (new source fingerprint) must get a fresh loader", first, second );
    }

    public void testDifferentClassesUseDifferentLoaders() throws Exception
    {
        File clsA = touch( new File( tempDir, "Pz_c.class" ), 4000L );
        File srcA = touch( new File( tempDir, "Pz_c.java" ), 4000L );
        File clsB = touch( new File( tempDir, "Pz_d.class" ), 4000L );
        File srcB = touch( new File( tempDir, "Pz_d.java" ), 4000L );

        URLClassLoader a = loader( clsA, srcA, getClass().getClassLoader() );
        URLClassLoader b = loader( clsB, srcB, getClass().getClassLoader() );

        assertNotNull( a );
        assertNotNull( b );
        assertNotSame( "Distinct model classes in the same directory must not share a loader", a, b );
    }

    public void testSameParentDifferentParentsUseDifferentLoaders() throws Exception
    {
        File cls = touch( new File( tempDir, "Pz_e.class" ), 5000L );
        File src = touch( new File( tempDir, "Pz_e.java" ), 5000L );

        // Two distinct parent loaders pointing at the same files must not share a cached loader.
        ClassLoader parentA = ModelClassLoaderCacheTest.class.getClassLoader();
        ClassLoader parentB = new URLClassLoader( new java.net.URL[0], parentA );

        URLClassLoader a = loader( cls, src, parentA );
        URLClassLoader b = loader( cls, src, parentB );

        assertNotNull( a );
        assertNotNull( b );
        assertNotSame( "The same files loaded through different parents must not share a loader", a, b );
    }

    /**
     * The core guarantee: really compile a model, load it, regenerate it with a different body,
     * recompile, and confirm the cache hands out a fresh loader that exposes the <em>new</em> class
     * (new identity and new behaviour) instead of a stale one.
     */
    public void testRegeneratedModelLoadsNewDefinition() throws Exception
    {
        File dir = Files.createTempDirectory( "mclreal" ).toFile();
        deleteRecursively( tempDir );
        tempDir = dir;

        File src = new File( dir, "RevProbe.java" );
        File cls = new File( dir, "RevProbe.class" );

        writeAndCompile( src, "public class RevProbe { public int value() { return 1; } }" );
        // Align the source mtime with a fresh value so the first and (potential) stale lookups differ
        // from the regeneration; the real signal is the fingerprint change on rewrite below.
        touch( src, 10_000L );
        touch( cls, 10_000L );

        URLClassLoader loaderA = loader( cls, src, getClass().getClassLoader() );
        Class<?> classA = loaderA.loadClass( "RevProbe" );
        Object instanceA = classA.newInstance();
        int valueA = ( (Number)classA.getMethod( "value" ).invoke( instanceA ) ).intValue();
        assertEquals( 1, valueA );

        // Regenerate: new body, new source fingerprint.
        writeAndCompile( src, "public class RevProbe { public int value() { return 2; } }" );
        touch( src, 20_000L );
        touch( cls, 20_000L );

        URLClassLoader loaderB = loader( cls, src, getClass().getClassLoader() );
        Class<?> classB = loaderB.loadClass( "RevProbe" );
        Object instanceB = classB.newInstance();

        assertNotSame( "A regenerated revision must be served by a different loader", loaderA, loaderB );
        assertNotSame( "A regenerated revision must be a different Class object", classA, classB );
        int valueB = ( (Number)classB.getMethod( "value" ).invoke( instanceB ) ).intValue();
        assertEquals( "The regenerated class must exhibit the new behaviour", 2, valueB );

        // And the original loader must still see the original definition.
        int valueAAfter = ( (Number)classA.getMethod( "value" ).invoke( instanceA ) ).intValue();
        assertEquals( 1, valueAAfter );
    }
}
