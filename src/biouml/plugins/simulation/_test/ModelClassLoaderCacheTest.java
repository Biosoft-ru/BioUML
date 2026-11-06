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
 *   <li>the caching contract — an unchanged revision reuses one loader, a regenerated revision gets a
 *       fresh loader, distinct classes and distinct parents never share a loader;
 *   <li>actual class loading — a small model is really compiled and loaded, and after regeneration a
 *       fresh loader exposes the new definition;
 *   <li>the large-model case — a model with a <em>lazily loaded nested class</em> keeps seeing its
 *       original nested class after the revision is regenerated (the mutable-directory hazard the
 *       per-revision directory is designed to prevent).
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
        suite.addTest( new ModelClassLoaderCacheTest( "testLazyNestedClassIsolatedAcrossRegeneration" ) );
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

    private static URLClassLoader revisionLoader(File outputDir, String className, File sourceFile, ClassLoader parent) throws Exception
    {
        Method prep = SimulationEngine.class.getDeclaredMethod( "modelRevisionDir", File.class, String.class, File.class );
        prep.setAccessible( true );
        File revisionDir = (File)prep.invoke( null, outputDir, className, sourceFile );

        Method m = SimulationEngine.class.getDeclaredMethod( "modelClassLoader", File.class, String.class, ClassLoader.class );
        m.setAccessible( true );
        return (URLClassLoader)m.invoke( null, revisionDir, className, parent );
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

    /** Returns the int returned by the no-arg {@code value()} method of a freshly created instance. */
    private static int value(Class<?> c) throws Exception
    {
        return ( (Number)c.getMethod( "value" ).invoke( c.newInstance() ) ).intValue();
    }

    // --- tests ---------------------------------------------------------------------------

    public void testSameRevisionReusesLoader() throws Exception
    {
        File src = touch( new File( tempDir, "Pz_a.java" ), 1000L );
        File cls = touch( new File( tempDir, "Pz_a.class" ), 1000L );

        URLClassLoader first = revisionLoader( tempDir, "Pz_a", src, getClass().getClassLoader() );
        URLClassLoader second = revisionLoader( tempDir, "Pz_a", src, getClass().getClassLoader() );

        assertNotNull( first );
        assertSame( "An unchanged revision must reuse the same cached loader", first, second );
    }

    public void testChangedRevisionCreatesNewLoader() throws Exception
    {
        File src = touch( new File( tempDir, "Pz_b.java" ), 2000L );
        File cls = touch( new File( tempDir, "Pz_b.class" ), 2000L );

        URLClassLoader first = revisionLoader( tempDir, "Pz_b", src, getClass().getClassLoader() );

        // Regenerate the model: rewrite the source (new size) and bump its last-modified time.
        try( FileWriter w = new FileWriter( src ) )
        {
            w.write( "// regenerated source body\n" );
        }
        touch( src, 3000L );

        URLClassLoader second = revisionLoader( tempDir, "Pz_b", src, getClass().getClassLoader() );
        assertNotNull( second );
        assertNotSame( "A regenerated revision must get a fresh loader", first, second );
    }

    public void testDifferentClassesUseDifferentLoaders() throws Exception
    {
        File srcA = touch( new File( tempDir, "Pz_c.java" ), 4000L );
        File clsA = touch( new File( tempDir, "Pz_c.class" ), 4000L );
        File srcB = touch( new File( tempDir, "Pz_d.java" ), 4000L );
        File clsB = touch( new File( tempDir, "Pz_d.class" ), 4000L );

        URLClassLoader a = revisionLoader( tempDir, "Pz_c", srcA, getClass().getClassLoader() );
        URLClassLoader b = revisionLoader( tempDir, "Pz_d", srcB, getClass().getClassLoader() );

        assertNotNull( a );
        assertNotNull( b );
        assertNotSame( "Distinct model classes in the same directory must not share a loader", a, b );
    }

    public void testDifferentParentsUseDifferentLoaders() throws Exception
    {
        File src = touch( new File( tempDir, "Pz_e.java" ), 5000L );
        File cls = touch( new File( tempDir, "Pz_e.class" ), 5000L );

        // Two distinct parent loaders pointing at the same files must not share a cached loader.
        ClassLoader parentA = ModelClassLoaderCacheTest.class.getClassLoader();
        ClassLoader parentB = new URLClassLoader( new java.net.URL[0], parentA );

        URLClassLoader a = revisionLoader( tempDir, "Pz_e", src, parentA );
        URLClassLoader b = revisionLoader( tempDir, "Pz_e", src, parentB );

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
        File cls = new File( tempDir, "RevProbe.class" );

        writeAndCompile( src, "public class RevProbe { public int value() { return 1; } }" );
        touch( src, 10_000L );
        touch( cls, 10_000L );

        URLClassLoader loaderA = revisionLoader( tempDir, "RevProbe", src, getClass().getClassLoader() );
        Class<?> classA = loaderA.loadClass( "RevProbe" );
        assertEquals( 1, value( classA ) );

        // Regenerate: new body, new revision (source size + mtime change).
        writeAndCompile( src, "public class RevProbe { public int value() { return 2; } }" );
        touch( src, 20_000L );
        touch( cls, 20_000L );

        URLClassLoader loaderB = revisionLoader( tempDir, "RevProbe", src, getClass().getClassLoader() );
        Class<?> classB = loaderB.loadClass( "RevProbe" );

        assertNotSame( "A regenerated revision must be served by a different loader", loaderA, loaderB );
        assertNotSame( "A regenerated revision must be a different Class object", classA, classB );
        assertEquals( "The regenerated class must exhibit the new behaviour", 2, value( classB ) );
        // And the original revision is unchanged.
        assertEquals( 1, value( classA ) );
    }

    /**
     * The large-model case. Generated large models emit nested helper classes that are instantiated
     * lazily (e.g. {@code new CalculateReactionRates().run(...)}) and therefore loaded from the
     * classpath <em>after</em> the main model class. Because each revision is loaded from its own
     * immutable directory, a model loaded from revision A must keep seeing revision A's nested class
     * even after revision B overwrites the shared output directory.
     */
    public void testLazyNestedClassIsolatedAcrossRegeneration() throws Exception
    {
        File dir = Files.createTempDirectory( "mclnested" ).toFile();
        deleteRecursively( tempDir );
        tempDir = dir;

        // Outer class with a nested (non-static) helper instantiated inline at call time, mirroring
        // the large-model pattern (e.g. `new CalculateReactionRates().run(...)`), which is what makes
        // the nested class load lazily from the classpath.
        File src = new File( dir, "NestedModel.java" );
        writeAndCompile( src,
                "public class NestedModel {" +
                "  public int value() { return new Helper().value(); }" +
                "  class Helper { int value() { return 1; } }" +
                "}" );
        touch( src, 10_000L );
        // NestedModel.class and NestedModel$Helper.class now exist in dir.
        touch( new File( dir, "NestedModel.class" ), 10_000L );

        URLClassLoader loaderA = revisionLoader( dir, "NestedModel", src, getClass().getClassLoader() );
        Class<?> mainA = loaderA.loadClass( "NestedModel" );
        Object instA = mainA.newInstance();
        // value() lazily loads and instantiates the nested Helper through loaderA.
        assertEquals( 1, ( (Number)mainA.getMethod( "value" ).invoke( instA ) ).intValue() );
        // The nested class that loaderA resolved.
        Class<?> helperA = loaderA.loadClass( "NestedModel$Helper" );
        assertNotNull( helperA );

        // Regenerate: the nested helper now returns 2. Overwrite both class files in the shared dir.
        writeAndCompile( src,
                "public class NestedModel {" +
                "  public int value() { return new Helper().value(); }" +
                "  class Helper { int value() { return 2; } }" +
                "}" );
        touch( src, 20_000L );
        touch( new File( dir, "NestedModel.class" ), 20_000L );

        // A fresh revision must see the new behaviour...
        URLClassLoader loaderB = revisionLoader( dir, "NestedModel", src, getClass().getClassLoader() );
        Class<?> mainB = loaderB.loadClass( "NestedModel" );
        assertNotSame( "A regenerated revision must be a different Class object", mainA, mainB );
        assertEquals( 2, ( (Number)mainB.getMethod( "value" ).invoke( mainB.newInstance() ) ).intValue() );

        // ...while the original revision's lazily loaded nested class must STILL report the old value.
        // This is the property the per-revision immutable directory guarantees and a shared mutable
        // directory would not.
        assertEquals( "A loaded revision must not observe a later regeneration of its nested classes",
                1, ( (Number)mainA.getMethod( "value" ).invoke( instA ) ).intValue() );
    }
}
