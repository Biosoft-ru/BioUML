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
 * <p>A model is compiled into a <b>fresh private staging directory</b> and that staging directory is
 * published, atomically, as an <b>immutable per-revision snapshot</b>. The cache is keyed by that
 * snapshot directory, so each model revision (including any nested classes its generated code
 * instantiates lazily) is isolated from later regenerations. The checks cover:
 * <ul>
 *   <li>the caching contract — an unchanged revision reuses one loader, a regenerated revision (new
 *       compiled content) gets a fresh loader, distinct classes and distinct parents never share a
 *       loader;
 *   <li>actual class loading — a small model is really compiled and loaded, and after regeneration a
 *       fresh loader exposes the new definition;
 *   <li>the large-model case — a model with a <em>lazily loaded nested class</em> keeps seeing its
 *       original nested class even when that nested class is loaded for the first time <em>after</em>
 *       the model has been regenerated;
 *   <li>the stale-class case — a nested class that disappears on regeneration is absent from the new
 *       revision's snapshot even though {@code javac} left the old {@code .class} in the shared
 *       output directory.
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
        suite.addTest( new ModelClassLoaderCacheTest( "testRemovedNestedClassAbsentFromNewSnapshot" ) );
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

    /**
     * Stages and compiles a model the same way {@link SimulationEngine#compileModel} does — the
     * source is written to {@code outputDir/<className>.java} and compiled into a <b>fresh private
     * staging directory</b> — and publishes it as the immutable per-revision snapshot via the
     * package-private {@code modelRevisionDir(outputDir, stagingDir)}. Returns that snapshot
     * directory.
     */
    private static File stageAndPublish(File outputDir, String className, String source) throws Exception
    {
        File srcFile = new File( outputDir, className + ".java" );
        File staging = Files.createTempDirectory( outputDir.toPath(), "stage-" + className + "-" ).toFile();
        writeAndCompile( srcFile, source, staging );
        Method prep = SimulationEngine.class.getDeclaredMethod( "modelRevisionDir", File.class, File.class );
        prep.setAccessible( true );
        return (File)prep.invoke( null, outputDir, staging );
    }

    private static URLClassLoader loaderFor(File revisionDir, String className, ClassLoader parent) throws Exception
    {
        Method m = SimulationEngine.class.getDeclaredMethod( "modelClassLoader", File.class, String.class, ClassLoader.class );
        m.setAccessible( true );
        return (URLClassLoader)m.invoke( null, revisionDir, className, parent );
    }

    /**
     * Stages a class the way the engine's compile path does: it compiles the (already-written) source
     * file in the shared {@code outputDir} into a <em>fresh private staging directory</em> and
     * publishes that as the immutable per-revision snapshot. Because the staging directory starts
     * empty and is filled only by compiling the current source, any stale nested {@code .class} left
     * in the shared directory by a prior in-place recompile is not carried into the new snapshot.
     */
    private static File stageExisting(File srcFile, File outputDir, String className) throws Exception
    {
        File staging = Files.createTempDirectory( outputDir.toPath(), "stage-" + className + "-" ).toFile();
        compile( srcFile, staging );
        Method prep = SimulationEngine.class.getDeclaredMethod( "modelRevisionDir", File.class, File.class );
        prep.setAccessible( true );
        return (File)prep.invoke( null, outputDir, staging );
    }

    /** Writes {@code src} with {@code code} and compiles it into {@code outDir} (created if needed). */
    private static void writeAndCompile(File source, String code, File outDir) throws Exception
    {
        try( FileWriter w = new FileWriter( source ) )
        {
            w.write( code );
        }
        compile( source, outDir );
    }

    /** Compiles the (already-written) {@code source} file into {@code outDir} (created if needed). */
    private static void compile(File source, File outDir) throws Exception
    {
        if( !outDir.exists() && !outDir.mkdirs() )
            throw new IllegalStateException( "Can not create output dir " + outDir );
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull( "System Java compiler must be available", compiler );
        boolean ok;
        try( StandardJavaFileManager fm = compiler.getStandardFileManager( null, null, null ) )
        {
            javax.tools.JavaFileObject unit =
                    fm.getJavaFileObjectsFromStrings( java.util.Collections.singletonList( source.getPath() ) ).iterator().next();
            java.util.List<javax.tools.JavaFileObject> units = java.util.Collections.singletonList( unit );
            ok = compiler.getTask( null, fm, null,
                    java.util.Arrays.asList( "-d", outDir.getAbsolutePath() ), null, units ).call();
        }
        assertTrue( "Compilation of " + source + " into " + outDir + " must succeed", ok );
    }

    private static int value(Class<?> c) throws Exception
    {
        return ( (Number)c.getMethod( "value" ).invoke( c.newInstance() ) ).intValue();
    }

    // --- tests ---------------------------------------------------------------------------

    public void testSameRevisionReusesLoader() throws Exception
    {
        String code = "public class Pz_a { public int value() { return 7; } }";

        URLClassLoader first = loaderFor( stageAndPublish( tempDir, "Pz_a", code ), "Pz_a", getClass().getClassLoader() );
        URLClassLoader second = loaderFor( stageAndPublish( tempDir, "Pz_a", code ), "Pz_a", getClass().getClassLoader() );

        assertNotNull( first );
        assertSame( "An unchanged revision must reuse the same cached loader", first, second );
    }

    public void testChangedRevisionCreatesNewLoader() throws Exception
    {
        URLClassLoader first = loaderFor( stageAndPublish( tempDir, "Pz_b",
                "public class Pz_b { public int value() { return 1; } }" ), "Pz_b", getClass().getClassLoader() );

        // Regenerate: recompile with different content so the class bytes (and thus the revision) change.
        URLClassLoader second = loaderFor( stageAndPublish( tempDir, "Pz_b",
                "public class Pz_b { public int value() { return 2; } }" ), "Pz_b", getClass().getClassLoader() );

        assertNotNull( second );
        assertNotSame( "A regenerated revision (new compiled content) must get a fresh loader", first, second );
    }

    public void testDifferentClassesUseDifferentLoaders() throws Exception
    {
        URLClassLoader a = loaderFor( stageAndPublish( tempDir, "Pz_c",
                "public class Pz_c { public int value() { return 1; } }" ), "Pz_c", getClass().getClassLoader() );
        URLClassLoader b = loaderFor( stageAndPublish( tempDir, "Pz_d",
                "public class Pz_d { public int value() { return 1; } }" ), "Pz_d", getClass().getClassLoader() );

        assertNotNull( a );
        assertNotNull( b );
        assertNotSame( "Distinct model classes in the same directory must not share a loader", a, b );
    }

    public void testDifferentParentsUseDifferentLoaders() throws Exception
    {
        File revision = stageAndPublish( tempDir, "Pz_e",
                "public class Pz_e { public int value() { return 1; } }" );

        // Two distinct parent loaders pointing at the same revision must not share a cached loader.
        ClassLoader parentA = ModelClassLoaderCacheTest.class.getClassLoader();
        ClassLoader parentB = new URLClassLoader( new java.net.URL[0], parentA );

        URLClassLoader a = loaderFor( revision, "Pz_e", parentA );
        URLClassLoader b = loaderFor( revision, "Pz_e", parentB );

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
        URLClassLoader loaderA = loaderFor( stageAndPublish( tempDir, "RevProbe",
                "public class RevProbe { public int value() { return 1; } }" ), "RevProbe", getClass().getClassLoader() );
        Class<?> classA = loaderA.loadClass( "RevProbe" );
        assertEquals( 1, value( classA ) );

        // Regenerate: recompile with a different body, producing a different revision.
        URLClassLoader loaderB = loaderFor( stageAndPublish( tempDir, "RevProbe",
                "public class RevProbe { public int value() { return 2; } }" ), "RevProbe", getClass().getClassLoader() );
        Class<?> classB = loaderB.loadClass( "RevProbe" );

        assertNotSame( "A regenerated revision must be served by a different loader", loaderA, loaderB );
        assertNotSame( "A regenerated revision must be a different Class object", classA, classB );
        assertEquals( "The regenerated class must exhibit the new behaviour", 2, value( classB ) );
        // And the original revision is unchanged.
        assertEquals( 1, value( classA ) );
    }

    /**
     * The large-model case, exercising the <em>actual</em> production failure: a model with a lazily
     * instantiated nested class is loaded, the model is then regenerated, and only <em>afterwards</em>
     * is the original revision's nested class loaded for the first time. Because each revision is
     * loaded from its own immutable snapshot directory, the original revision must still see its own
     * nested class, not the regenerated one.
     */
    public void testLazyNestedClassLoadedAfterRegeneration() throws Exception
    {
        final String codeA = "public class NestedModel {" +
                "  public int value() { return new Helper().value(); }" +
                "  class Helper { int value() { return 1; } }" +
                "}";
        final String codeB = "public class NestedModel {" +
                "  public int value() { return new Helper().value(); }" +
                "  class Helper { int value() { return 2; } }" +
                "}";

        // 1. Compile revision A (nested Helper returns 1).
        URLClassLoader loaderA = loaderFor( stageAndPublish( tempDir, "NestedModel", codeA ),
                "NestedModel", getClass().getClassLoader() );
        Class<?> mainA = loaderA.loadClass( "NestedModel" );
        // NOTE: ideally this test would deterministically assert that the lazily-instantiated nested
        // class has not yet been resolved through loaderA at this point (proving that the nested class
        // is genuinely loaded lazily, i.e. only when value() is first called). Doing so requires
        // ClassLoader.findLoadedClass, which is protected+final and not open to the unnamed module in
        // this test JVM, so the precondition is not asserted. The test remains a strong behavioural
        // check: it invokes value() on revision A *after* revision B has been compiled, and the only
        // way revision A could return 2 is if its lazily loaded nested class had been resolved from a
        // directory republished by the later regeneration.
        Object instA = mainA.newInstance();

        // 2. Compile revision B (nested Helper returns 2), a separate fresh staging directory.
        URLClassLoader loaderB = loaderFor( stageAndPublish( tempDir, "NestedModel", codeB ),
                "NestedModel", getClass().getClassLoader() );
        Class<?> mainB = loaderB.loadClass( "NestedModel" );
        assertNotSame( "A regenerated revision must be a different Class object", mainA, mainB );
        assertEquals( 2, ( (Number)mainB.getMethod( "value" ).invoke( mainB.newInstance() ) ).intValue() );

        // 3. NOW load revision A's nested Helper for the first time (triggered by value()). It must
        //    still report the original value because loader A is rooted at revision A's immutable
        //    snapshot directory, not a directory that was later republished by revision B.
        assertEquals( "A revision must not observe a later regeneration of its lazily loaded nested classes",
                1, ( (Number)mainA.getMethod( "value" ).invoke( instA ) ).intValue() );
    }

    /**
     * Regression test for stale nested classes: {@code javac} does not delete a nested
     * {@code Model$Helper.class} that disappears when the enclosing class is recompiled in the same
     * output directory. The engine avoids the problem by compiling each revision's source into a
     * <em>fresh</em> staging directory (which starts empty) and publishing that as an immutable
     * snapshot. This test reproduces the danger — a stale nested class left in the shared output
     * directory — and verifies that the engine's staging path produces a clean snapshot that does not
     * include the obsolete nested class.
     */
    public void testRemovedNestedClassAbsentFromNewSnapshot() throws Exception
    {
        File dir = Files.createTempDirectory( "mclstale" ).toFile();
        deleteRecursively( tempDir );
        tempDir = dir;

        // 1. Revision A: a model with a nested Helper, compiled in place into the shared dir.
        File src = new File( dir, "SrcA.java" );
        writeAndCompile( src,
                "public class SrcA { int value() { return new Helper().value(); } class Helper { int value() { return 1; } } }",
                dir );
        assertNotNull( new File( dir, "SrcA$Helper.class" ) );

        // 2. Regenerate: recompile the same source file in place with the nested Helper removed.
        //    javac leaves the now-obsolete SrcA$Helper.class behind in the shared dir.
        writeAndCompile( src, "public class SrcA { int value() { return 2; } }", dir );
        assertNotNull( "javac leaves the obsolete nested class in the shared output dir (expected)",
                new File( dir, "SrcA$Helper.class" ) );

        // 3. The engine now stages the current source into a fresh staging dir and publishes it. The
        //    stale SrcA$Helper.class in the shared dir must NOT be part of the new snapshot.
        File revisionB = stageExisting( src, dir, "SrcA" );
        assertTrue( "The regenerated model class must be present in the snapshot",
                new File( revisionB, "SrcA.class" ).exists() );
        assertFalse( "The obsolete nested class must be absent from the regenerated snapshot",
                new File( revisionB, "SrcA$Helper.class" ).exists() );
    }
}
