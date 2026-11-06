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
 * <p>The cache is keyed by an <b>immutable per-revision compilation directory</b>, so each model
 * revision (including any nested classes its generated code instantiates lazily) is isolated from
 * later regenerations. The checks cover:
 * <ul>
 *   <li>the caching contract — an unchanged revision reuses one loader, a regenerated revision (new
 *       compiled content) gets a fresh loader, distinct classes and distinct parents never share a
 *       loader;
 *   <li>actual class loading — a small model is really compiled and loaded, and after regeneration a
 *       fresh loader exposes the new definition;
 *   <li>the large-model case — a model with a <em>lazily loaded nested class</em> keeps seeing its
 *       original nested class even when that nested class is loaded for the first time <em>after</em>
 *       the shared output directory has been overwritten by a regeneration.
 * </ul>
 *
 * <p>The cache helpers are package-private in {@code biouml.plugins.simulation}, so they are reached
 * via reflection.
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
        suite.addTest( new ModelClassLoaderCacheTest( "testDifferentParentsUseDifferentLoaders" ) );
        suite.addTest( new ModelClassLoaderCacheTest( "testRegeneratedModelLoadsNewDefinition" ) );
        suite.addTest( new ModelClassLoaderCacheTest( "testLazyNestedClassLoadedAfterRegeneration" ) );
        return suite;
    }

    protected void setUp() throws Exception
    {
        // Start each test from an empty cache so tests are isolated from one another.
        clearCache();
        tempDir = Files.createTempDirectory( "mclcache" ).toFile();
    }

    protected void tearDown() throws Exception
    {
        // Clean up the temp tree and the shared static cache; a failure here is a real test
        // misconfiguration and should surface, not be swallowed.
        deleteRecursively( tempDir );
        clearCache();
    }

    private static void clearCache() throws Exception
    {
        Method m = SimulationEngine.class.getDeclaredMethod( "clearModelClassLoaderCache" );
        m.setAccessible( true );
        m.invoke( null );
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

    // --- helpers to reach the package-private cache methods -------------------------------

    private static URLClassLoader revisionLoader(File outputDir, String className, ClassLoader parent) throws Exception
    {
        Method prep = SimulationEngine.class.getDeclaredMethod( "modelRevisionDir", File.class, String.class );
        prep.setAccessible( true );
        File revisionDir = (File)prep.invoke( null, outputDir, className );

        Method m = SimulationEngine.class.getDeclaredMethod( "modelClassLoader", File.class, String.class, ClassLoader.class );
        m.setAccessible( true );
        return (URLClassLoader)m.invoke( null, revisionDir, className, parent );
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

    private static int value(Class<?> c) throws Exception
    {
        return ( (Number)c.getMethod( "value" ).invoke( c.newInstance() ) ).intValue();
    }

    // --- tests ---------------------------------------------------------------------------

    public void testSameRevisionReusesLoader() throws Exception
    {
        File src = new File( tempDir, "Pz_a.java" );
        writeAndCompile( src, "public class Pz_a { public int value() { return 7; } }" );

        URLClassLoader first = revisionLoader( tempDir, "Pz_a", getClass().getClassLoader() );
        URLClassLoader second = revisionLoader( tempDir, "Pz_a", getClass().getClassLoader() );

        assertNotNull( first );
        assertSame( "An unchanged revision must reuse the same cached loader", first, second );
    }

    public void testChangedRevisionCreatesNewLoader() throws Exception
    {
        File src = new File( tempDir, "Pz_b.java" );
        writeAndCompile( src, "public class Pz_b { public int value() { return 1; } }" );

        URLClassLoader first = revisionLoader( tempDir, "Pz_b", getClass().getClassLoader() );

        // Regenerate: recompile with different content so the class bytes (and thus the revision) change.
        writeAndCompile( src, "public class Pz_b { public int value() { return 2; } }" );

        URLClassLoader second = revisionLoader( tempDir, "Pz_b", getClass().getClassLoader() );
        assertNotNull( second );
        assertNotSame( "A regenerated revision (new compiled content) must get a fresh loader", first, second );
    }

    public void testDifferentClassesUseDifferentLoaders() throws Exception
    {
        File srcA = new File( tempDir, "Pz_c.java" );
        writeAndCompile( srcA, "public class Pz_c { public int value() { return 1; } }" );
        File srcB = new File( tempDir, "Pz_d.java" );
        writeAndCompile( srcB, "public class Pz_d { public int value() { return 1; } }" );

        URLClassLoader a = revisionLoader( tempDir, "Pz_c", getClass().getClassLoader() );
        URLClassLoader b = revisionLoader( tempDir, "Pz_d", getClass().getClassLoader() );

        assertNotNull( a );
        assertNotNull( b );
        assertNotSame( "Distinct model classes in the same directory must not share a loader", a, b );
    }

    public void testDifferentParentsUseDifferentLoaders() throws Exception
    {
        File src = new File( tempDir, "Pz_e.java" );
        writeAndCompile( src, "public class Pz_e { public int value() { return 1; } }" );

        // Two distinct parent loaders pointing at the same revision must not share a cached loader.
        ClassLoader parentA = ModelClassLoaderCacheTest.class.getClassLoader();
        ClassLoader parentB = new URLClassLoader( new java.net.URL[0], parentA );

        URLClassLoader a = revisionLoader( tempDir, "Pz_e", parentA );
        URLClassLoader b = revisionLoader( tempDir, "Pz_e", parentB );

        assertNotNull( a );
        assertNotNull( b );
        assertNotSame( "The same revision loaded through different parents must not share a loader", a, b );
    }

    /**
     * The core guarantee: really compile a model, load it, regenerate it with a different body,
     * recompile, and confirm the cache hands out a fresh loader that exposes the <em>new</em> class
     * (new identity and new behaviour) instead of a stale one.
     */
    public void testRegeneratedModelLoadsNewDefinition() throws Exception
    {
        File src = new File( tempDir, "RevProbe.java" );

        writeAndCompile( src, "public class RevProbe { public int value() { return 1; } }" );

        URLClassLoader loaderA = revisionLoader( tempDir, "RevProbe", getClass().getClassLoader() );
        Class<?> classA = loaderA.loadClass( "RevProbe" );
        assertEquals( 1, value( classA ) );

        // Regenerate: recompile with a different body, producing a different revision.
        writeAndCompile( src, "public class RevProbe { public int value() { return 2; } }" );

        URLClassLoader loaderB = revisionLoader( tempDir, "RevProbe", getClass().getClassLoader() );
        Class<?> classB = loaderB.loadClass( "RevProbe" );

        assertNotSame( "A regenerated revision must be served by a different loader", loaderA, loaderB );
        assertNotSame( "A regenerated revision must be a different Class object", classA, classB );
        assertEquals( "The regenerated class must exhibit the new behaviour", 2, value( classB ) );
        // And the original revision is unchanged.
        assertEquals( 1, value( classA ) );
    }

    /**
     * The large-model case, exercising the <em>actual</em> production failure: a model with a lazily
     * instantiated nested class is loaded, the shared output directory is then overwritten by a
     * regeneration, and only <em>afterwards</em> is the original revision's nested class loaded for
     * the first time. Because each revision is loaded from its own immutable directory, the original
     * revision must still see its own nested class, not the regenerated one.
     */
    public void testLazyNestedClassLoadedAfterRegeneration() throws Exception
    {
        File dir = Files.createTempDirectory( "mclnested" ).toFile();
        deleteRecursively( tempDir );
        tempDir = dir;

        File src = new File( dir, "NestedModel.java" );

        // 1. Compile revision A (nested Helper returns 1).
        writeAndCompile( src,
                "public class NestedModel {" +
                "  public int value() { return new Helper().value(); }" +
                "  class Helper { int value() { return 1; } }" +
                "}" );

        // 2. Load NestedModel through loader A, but do NOT invoke value() yet (nested Helper not loaded).
        URLClassLoader loaderA = revisionLoader( dir, "NestedModel", getClass().getClassLoader() );
        Class<?> mainA = loaderA.loadClass( "NestedModel" );
        Object instA = mainA.newInstance();

        // 3. Compile revision B (nested Helper returns 2), overwriting the shared output directory.
        writeAndCompile( src,
                "public class NestedModel {" +
                "  public int value() { return new Helper().value(); }" +
                "  class Helper { int value() { return 2; } }" +
                "}" );

        // 4. Load revision B through loader B and verify it returns 2.
        URLClassLoader loaderB = revisionLoader( dir, "NestedModel", getClass().getClassLoader() );
        Class<?> mainB = loaderB.loadClass( "NestedModel" );
        assertNotSame( "A regenerated revision must be a different Class object", mainA, mainB );
        assertEquals( 2, ( (Number)mainB.getMethod( "value" ).invoke( mainB.newInstance() ) ).intValue() );

        // 5. NOW load revision A's nested Helper for the first time (triggered by value()). It must
        //    still report the original value because loader A is rooted at revision A's immutable
        //    directory, not the overwritten shared directory.
        assertEquals( "A revision must not observe a later regeneration of its lazily loaded nested classes",
                1, ( (Number)mainA.getMethod( "value" ).invoke( instA ) ).intValue() );
    }
}
