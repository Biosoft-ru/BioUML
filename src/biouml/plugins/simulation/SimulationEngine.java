package biouml.plugins.simulation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import one.util.streamex.StreamEx;

import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.WeakPropertyChangeForwarder;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.access.core.PluginEntry;
import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.java.RunTimeCompiler;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;

import com.developmentontheedge.beans.Option;
import ru.biosoft.jobcontrol.FunctionJobControl;


/**
 * General class for simulation engine. It defines common contract
 * (properties and methods) for any simulation engine.
 */
abstract public class SimulationEngine extends Option implements PropertyChangeListener, JSONBean
{
    public static int DETERMINISTIC_TYPE = 0;
    public static int STOCHASTIC_TYPE = 1;

    public static String VAR_PATH_DELIMITER = "/";

    protected SimulationEngineLogger log = new SimulationEngineLogger();

    /**
     * Process-wide cache of {@link URLClassLoader}s used to load compiled mathematical models.
     *
     * <p>Historically every {@link #compileModel} call constructed a fresh
     * {@code URLClassLoader} and discarded it. The returned model instance, however, is retained
     * by long-lived engines/tasks (e.g. {@link #model}), and a live instance keeps its {@code Class}
     * (and therefore its defining class loader) reachable. With one loader per simulation run this
     * accumulated hundreds of thousands of class loaders and their loaded classes until the heap was
     * exhausted ({@code java.lang.OutOfMemoryError}).
     *
     * <p>Loaders are now shared. A loader is reused as long as the model revision it was created for
     * is unchanged; the revision is described by the (canonical) directory, class name, the
     * parent class loader, and a fingerprint of the model source. When the model is regenerated a
     * new loader is created so the freshly compiled class is read rather than a stale cached
     * definition.
     *
     * <p><b>Bounding guarantee.</b> The *cache* itself is bounded to {@link #MODEL_CLASSLOADER_CACHE_LIMIT}
     * entries. The number of *live* class loaders is <b>not</b> bounded by the cache: a class loader
     * stays alive while any loaded model instance is still reachable (a live instance pins its
     * defining class and, through it, its loader). This patch fixes the original defect — one loader
     * created per simulation run — by reusing a single loader across all runs of an unchanged model.
     * It does not, and cannot, reclaim loaders of model instances that are still referenced.
     *
     * <p><b>Plugin / class-loader lifecycle.</b> The cache is process-wide and its keys (and the cached
     * loaders) hold strong references to the parent class loader. Therefore a parent loader is not
     * collectible while a cache entry that references it remains. In BioUML the parent is the OSGi
     * bundle class loader of the simulation plugin, so the LRU cap bounds — but does not eliminate —
     * the retention of a plugin's class loader after the plugin is no longer in use. There is currently
     * no OSGi {@code BundleActivator} disposal hook for this plugin to hook explicit invalidation into;
     * the cap is the mechanism that keeps this bounded. If a suitable lifecycle hook is added later,
     * clearing the entries for the disposed parent here is the intended extension point.
     *
     * <p><b>Revision isolation.</b> Each model revision is loaded from its own <b>immutable
     * per-revision directory</b>, and the shared loader's classpath is exactly that directory. This
     * matters because generated <em>large</em> models emit nested helper classes (e.g.
     * {@code CalculateReactionRates}) that are instantiated — and therefore loaded — lazily from the
     * classpath at simulation time. Rooting each loader at an immutable directory guarantees that a
     * loaded revision can only ever see its own classes, never a later regeneration's.
     *
     * <p><b>Compilation is staged, then published.</b> A model is compiled into a fresh private
     * staging directory ({@link #compileModel}) rather than the shared output directory, and only
     * afterwards is the staging directory's content hashed and published, atomically, as the
     * immutable {@code rev-<hash>} snapshot ({@link #modelRevisionDir}). Compiling into an empty
     * staging directory means the snapshot is exactly the output of one compilation: a nested class
     * that no longer exists in the source cannot survive as a stale {@code .class} file, and nothing
     * is read back from the (possibly concurrently mutating) shared output directory.
     *
     * <p><b>Publication is atomic or it fails.</b> A snapshot is moved into place with
     * {@link java.nio.file.StandardCopyOption#ATOMIC_MOVE}; if the filesystem does not support an
     * atomic move the publication throws rather than degrading to a non-atomic copy. A non-atomic copy
     * would let a concurrent reader observe a partially populated snapshot, contradicting the guarantee
     * that readers only ever see a complete snapshot. Because every snapshot is therefore either absent
     * or fully published, and a snapshot is never modified after publication, the existence-of-every-
     * expected-file check used to recognize an already-published snapshot is sufficient: a directory
     * named {@code rev-<hash>} that contains the expected class files is, by construction, a complete
     * and immutable snapshot (a partial or corrupted directory can only arise from an atomic move, which
     * never leaves one behind).
     */
    private static final Map<ModelClassLoaderKey, URLClassLoader> MODEL_CLASSLOADER_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<ModelClassLoaderKey, URLClassLoader>(16, 0.75f, true));

    private static final int MODEL_CLASSLOADER_CACHE_LIMIT = 128;

    /**
     * Per-output-directory serialization of (cleanup + publication). The key is the <em>canonical</em>
     * path of the output directory, so two different path strings that refer to the same directory
     * (via symlinks or {@code ..}) share one lock. The lock is held from the startup cleanup of the
     * directory's {@code rev-*} snapshots through the atomic publication of a snapshot, so a cleanup
     * can never race with — and delete — a snapshot that a concurrent thread is in the middle of
     * publishing.
     */
    private static final ConcurrentMap<String, Object> REVISION_DIR_LOCKS = new ConcurrentHashMap<String, Object>();

    /**
     * Output directories for which stale {@code rev-*} snapshots have already been purged at startup.
     * Kept as a (small) strong set because it is used only to avoid re-scanning a directory on every
     * compile; the set grows by at most one entry per distinct output directory used by the process.
     * The value is the canonical path, so the set is also deduplicated by physical directory.
     */
    private static final ConcurrentMap<String, Boolean> CLEANED_REVISION_DIRS = new ConcurrentHashMap<String, Boolean>();

    /** Returns (creating if needed) the per-directory lock for {@code dir}'s canonical path. */
    private static Object revisionDirLock(File dir)
    {
        String key;
        try
        {
            key = dir.getCanonicalPath();
        }
        catch( IOException e )
        {
            // Fall back to the raw path if canonicalization is not possible (e.g. the directory was
            // just deleted); it is still unique enough to serialize publication for this directory.
            key = dir.getPath();
        }
        return REVISION_DIR_LOCKS.computeIfAbsent( key, k -> new Object() );
    }

    /**
     * Identity of a model revision whose compiled classes should be loaded through a single shared
     * {@link URLClassLoader}. Two revisions share a loader only when all of these match, including
     * the parent class loader (compared by reference) — a loader created under one parent must never
     * be handed to a caller expecting a different parent.
     *
     * <p>{@code revisionDir} is an <b>immutable, per-revision compilation directory</b>: its path
     * encodes the model revision, and it is never rewritten. The loader's classpath is exactly this
     * directory, so a loaded model (including any nested classes it instantiates lazily) can only
     * ever see the classes of the revision it was created from — never a later regeneration. This is
     * what makes sharing a directory-backed loader across revisions safe even for large models, whose
     * generated code emits nested helper classes loaded lazily from the classpath.
     */
    static final class ModelClassLoaderKey
    {
        private final String revisionDir;
        private final String className;
        private final ClassLoader parent;
        private final int hash;

        private ModelClassLoaderKey( String revisionDir, String className, ClassLoader parent )
        {
            this.revisionDir = revisionDir;
            this.className = className;
            this.parent = parent;
            int result = revisionDir.hashCode();
            result = 31 * result + className.hashCode();
            result = 31 * result + System.identityHashCode( parent );
            this.hash = result;
        }

        @Override
        public boolean equals( Object o )
        {
            if( this == o )
                return true;
            if( !( o instanceof ModelClassLoaderKey ) )
                return false;
            ModelClassLoaderKey that = (ModelClassLoaderKey)o;
            // Parent must be compared by reference: ClassLoader identity is what matters for
            // delegation, and two distinct loader instances are never interchangeable.
            return parent == that.parent
                    && revisionDir.equals( that.revisionDir )
                    && className.equals( that.className );
        }

        @Override
        public int hashCode()
        {
            return hash;
        }
    }

    /**
     * Returns a shared {@link URLClassLoader} rooted at {@code revisionDir}, the immutable per-revision
     * directory that holds the compiled classes for this model revision.
     *
     * @param revisionDir the per-revision directory the loader must use as its (only) classpath entry
     * @param className the compiled model class to load through it (part of the cache key)
     * @param parent the parent class loader; part of the cache key
     * @return a cached (or newly created) class loader for {@code revisionDir}
     */
    static URLClassLoader modelClassLoader(File revisionDir, String className, ClassLoader parent) throws MalformedURLException
    {
        String dirPath = revisionDir.getPath();
        ModelClassLoaderKey key = new ModelClassLoaderKey( dirPath, className, parent );

        URLClassLoader cached = MODEL_CLASSLOADER_CACHE.get( key );
        if( cached != null )
            return cached;

        synchronized( MODEL_CLASSLOADER_CACHE )
        {
            cached = MODEL_CLASSLOADER_CACHE.get( key );
            if( cached != null )
                return cached;

            URLClassLoader created = new URLClassLoader( new URL[] {revisionDir.toURI().toURL()}, parent );
            MODEL_CLASSLOADER_CACHE.put( key, created );

            // Bound the *cache* (not the number of live loaders — see the class-level note): evict the
            // least-recently-used entry once the cap is exceeded. An evicted loader becomes
            // collectible only once no loaded model instance still references it.
            while( MODEL_CLASSLOADER_CACHE.size() > MODEL_CLASSLOADER_CACHE_LIMIT )
            {
                Map.Entry<ModelClassLoaderKey, URLClassLoader> oldest =
                        MODEL_CLASSLOADER_CACHE.entrySet().iterator().next();
                MODEL_CLASSLOADER_CACHE.remove( oldest.getKey() );
            }
            return created;
        }
    }

    /**
     * Publishes the model's compiled classes, taken from a freshly created staging directory, as an
     * immutable per-revision snapshot and returns that snapshot's directory.
     *
     * <p>{@code stagingDir} is the output of a <b>single</b> compilation: {@link #compileModel}
     * compiles into a private, otherwise-empty staging directory, so it contains exactly the class
     * files that current source produces (the model class plus any nested {@code Model$*.class}
     * files) and nothing else. There is therefore no read-back from the shared output directory and
     * no window in which a concurrently regenerated model could contribute a different file. A nested
     * class that has disappeared from the source is simply absent, because {@code javac} is not asked
     * to delete stale class files — the staging directory starts empty.
     *
     * <p>The snapshot directory is named {@code rev-<sha256>} where the hash is over the <b>content</b>
     * of the compiled classes (name plus bytes, in sorted order). It is content-based, so an unchanged
     * model (byte-identical recompile) maps to the same snapshot and is reused, while any content change
     * maps to a new one; it does not depend on filesystem timestamps.
     *
     * <p>If the snapshot already exists (the common case — re-running an unchanged model) its content is
     * verified to match the staging content and the staging directory is discarded. Otherwise the
     * staging directory is moved into place atomically, so a published snapshot is never mutated and a
     * concurrent reader can only ever observe a complete snapshot.
     *
     * @param outputDir the shared directory the snapshots are kept in (the model was staged under it)
     * @param stagingDir the private staging directory holding exactly this compilation's class files
     * @return the immutable per-revision snapshot directory
     * @throws IOException if the snapshot cannot be verified or published
     */
    static File modelRevisionDir(File outputDir, File stagingDir) throws IOException
    {
        // Serialize (cleanup + publication) per output directory so the startup cleanup of this
        // directory's rev-* snapshots can never race with — and delete — a snapshot that a concurrent
        // thread is in the middle of publishing. The lock is held for the whole critical section.
        Object lock = revisionDirLock( outputDir );
        synchronized( lock )
        {
            // Once per directory per JVM, purge stale rev-* snapshots (see cleanStaleRevisionSnapshots).
            // Done under the lock so no publication for this directory can start until cleanup finishes.
            cleanStaleRevisionSnapshots( outputDir, lock );

            // The complete set of classes that make up this revision, from the staging directory only.
            String[] classFiles = stagingDir.list( ( d, name ) -> name.endsWith( ".class" ) );
            if( classFiles == null || classFiles.length == 0 )
                throw new IOException( "No compiled class produced for model in staging directory " + stagingDir );
            Arrays.sort( classFiles );

            String fingerprint = contentHash( stagingDir, classFiles );
            File revisionDir = new File( outputDir, "rev-" + fingerprint );

            // Fast path: this exact revision is already published and complete.
            if( allPresent( revisionDir, classFiles ) )
            {
                // The snapshot already holds these exact bytes (the name is a content hash), so it is
                // safe to discard this staging copy.
                deleteRecursively( stagingDir );
                return revisionDir;
            }

            // Otherwise publish this staging directory as the new snapshot with an atomic move, so a
            // published snapshot is never mutated and a concurrent reader can only ever observe a
            // complete snapshot. See the class-level note on why a filename-only existence check is
            // sufficient here.
            try
            {
                Files.move( stagingDir.toPath(), revisionDir.toPath(), StandardCopyOption.ATOMIC_MOVE );
            }
            catch( FileAlreadyExistsException e )
            {
                // A concurrent thread published the same revision first (possible if cleanup ran for
                // this directory but a publication had already happened); use it and discard ours.
                deleteRecursively( stagingDir );
            }

            // Do not depend on the publish mechanism alone: verify the published snapshot is complete.
            if( !allPresent( revisionDir, classFiles ) )
            {
                deleteRecursively( stagingDir );
                throw new IOException( "Revision snapshot is incomplete after publication: " + revisionDir );
            }
            return revisionDir;
        }
    }

    /**
     * Loads an already-compiled model by copying its classes out of the root of the shared output
     * directory (under the per-output-directory lock, so the read is consistent with any concurrent
     * root population) and publishing them as an immutable per-revision snapshot. Used when a model is
     * not recompiled ({@code compile == false}).
     *
     * @param outputDir the shared output directory whose root holds the previously compiled classes
     * @param className the model class name (without extension)
     * @return the published immutable per-revision snapshot directory
     */
    static File loadFromExistingRoot(File outputDir, String className) throws IOException
    {
        Object lock = revisionDirLock( outputDir );
        synchronized( lock )
        {
            File stagingDir = Files.createTempDirectory( outputDir.toPath(), "stage-" + className + "-" ).toFile();
            try
            {
                stageExistingClasses( outputDir, className, stagingDir );
                return modelRevisionDir( outputDir, stagingDir );
            }
            catch( IOException e )
            {
                deleteRecursively( stagingDir );
                throw e;
            }
        }
    }

    /**
     * Publishes the compiled classes for a single model (from a staging directory holding exactly that
     * model's classes) under the per-output-directory lock, and copies the published snapshot into the
     * root of the shared output directory.
     *
     * <p>The lock serializes the root-directory population for this output directory against any
     * concurrent compilation, so two concurrent compilations of the same model cannot interleave their
     * root-level copies into a mixed set of revisions. The root is a legacy, mutable location (read by
     * later {@code compile == false} loads), so it must be updated as a complete, consistent set for a
     * model.
     *
     * @param outputDir the shared output directory (snapshots + legacy root classes live here)
     * @param stagingDir a staging directory holding exactly the compiled classes for one model
     * @param className the model class name (without extension), used to select the root copies
     * @return the published immutable per-revision snapshot directory
     */
    static File publishRevision(File outputDir, File stagingDir, String className) throws IOException
    {
        return publishRevision( outputDir, stagingDir, new String[] {className} );
    }

    /**
     * Publishes the compiled classes in {@code stagingDir} (which may hold several models from one
     * batch compilation) as a single immutable per-revision snapshot, and copies the requested models'
     * classes into the root of the shared output directory — all under the per-output-directory lock.
     *
     * <p>The lock serializes the root-directory population for this output directory against any
     * concurrent compilation, so two concurrent compilations cannot interleave their root-level copies
     * into a mixed set of revisions. The root is a legacy, mutable location (read by later
     * {@code compile == false} loads), so it must be updated as a complete, consistent set.
     *
     * @param outputDir the shared output directory (snapshots + legacy root classes live here)
     * @param stagingDir a staging directory holding the compiled classes to publish (one or more models)
     * @param classNames the model class names (without extension) whose classes are copied to the root;
     *        {@code null} copies every class in the snapshot
     * @return the published immutable per-revision snapshot directory
     */
    static File publishRevision(File outputDir, File stagingDir, String[] classNames) throws IOException
    {
        Object lock = revisionDirLock( outputDir );
        synchronized( lock )
        {
            File revisionDir = modelRevisionDir( outputDir, stagingDir );
            // Populate the legacy root from the published snapshot (which always exists). Copying the
            // requested models' complete class sets while holding the lock keeps a concurrent
            // compilation from interleaving a different revision's files into the root.
            if( classNames == null )
                copyAllClassesToRoot( revisionDir, outputDir );
            else
                for( String className : classNames )
                    copyClassesToRoot( revisionDir, outputDir, className );
            return revisionDir;
        }
    }

    /**
     * Once per output directory per JVM, removes {@code rev-*} snapshots left over from a previous run.
     *
     * <p>The JVM is restarted on every server start and the process-wide loader cache is empty at that
     * point, so before any model is compiled and any loader can refer to a snapshot, any snapshot in the
     * shared output directory is stale and can be removed. This keeps the snapshots from accumulating on
     * disk across restarts. The set of snapshots in use during a run is small (bounded by the number of
     * distinct revisions) and is recreated on demand.
     *
     * <p><b>This must be called while holding {@link #revisionDirLock} for the directory.</b> The marker
     * is set only after the deletion completes, so a concurrent caller blocked on the lock sees that
     * cleanup is already done and never deletes a directory that a peer is concurrently publishing.
     * The canonical path is used as the key so that equivalent path strings (symlinks, {@code ..})
     * deduplicate to a single cleanup.
     */
    private static void cleanStaleRevisionSnapshots(File outputDir, Object lock)
    {
        String key;
        try
        {
            key = outputDir.getCanonicalPath();
        }
        catch( IOException e )
        {
            key = outputDir.getPath();
        }
        if( CLEANED_REVISION_DIRS.putIfAbsent( key, Boolean.TRUE ) != null )
            return;
        File[] entries = outputDir.listFiles( ( d, name ) -> name.startsWith( "rev-" ) );
        if( entries == null )
            return;
        for( File entry : entries )
            deleteRecursively( entry );
    }

    /** True if every expected class file is present in {@code dir}. */
    private static boolean allPresent(File dir, String[] classFiles)
    {
        for( String name : classFiles )
            if( !new File( dir, name ).isFile() )
                return false;
        return true;
    }

    /**
     * SHA-256 over the name and content of each class file, in sorted order, so identical class
     * content yields an identical revision id regardless of filesystem metadata. The file content is
     * streamed into the digest so large class files do not require a whole-file heap buffer.
     */
    private static String contentHash(File dir, String[] classFiles) throws IOException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
            for( String name : classFiles )
            {
                // Hash the file name, a delimiter, and the file content, so the concatenation of
                // distinct (name, content) pairs is unambiguous.
                digest.update( name.getBytes( StandardCharsets.UTF_8 ) );
                digest.update( (byte)0 );
                hashFileInto( digest, new File( dir, name ) );
                digest.update( (byte)0 );
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder( hash.length * 2 );
            for( byte b : hash )
                sb.append( Character.forDigit( ( b >> 4 ) & 0xF, 16 ) ).append( Character.forDigit( b & 0xF, 16 ) );
            return sb.toString();
        }
        catch( NoSuchAlgorithmException e )
        {
            throw new IOException( "SHA-256 is not available", e );
        }
    }

    /** Streams the bytes of {@code file} into {@code digest} in 8 KiB chunks. */
    private static void hashFileInto(MessageDigest digest, File file) throws IOException
    {
        try( InputStream in = new BufferedInputStream( new FileInputStream( file ) ) )
        {
            byte[] buf = new byte[ 8192 ];
            int n;
            while( ( n = in.read( buf ) ) != -1 )
                digest.update( buf, 0, n );
        }
    }

    /** Best-effort recursive delete, used to discard a failed temporary snapshot. */
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

    /**
     * Clears the model class loader cache. Package-private: intended for tests (and, if a suitable
     * OSGi disposal hook is added, for explicit invalidation on plugin unload). Not part of the
     * public API.
     */
    static void clearModelClassLoaderCache()
    {
        MODEL_CLASSLOADER_CACHE.clear();
    }

    /**Directory where generated files will be stored*/
    private String srcDir = ".";
    
    /**
     * Flag indicating that diagram was modified (elements added\removed, new diagram set) and we should reinit engine
     * For now it is used only to avoid double work during getModel - getModel2 execution
     * TODO: actually listen to the diagram and update this flag
     */
    protected boolean diagramModified = false;
    
    protected WeakPropertyChangeForwarder diagramListener;
    
    /**Preprocessors added by user that should be fired before default preprocessors*/
    private final List<Preprocessor> additionalPreprocessors = new ArrayList<>();
    
    /**Preprocessors added by user that should be fired after default preprocessors*/
    private final List<Preprocessor> additionalPreprocessors2 = new ArrayList<>();
    
    /** Span characterizing result output */
    private double initialTime = 0;
    private double timeIncrement = 1;
    private double completionTime = 100;
        
    protected FunctionJobControl jobControl;

    /**Simulator type: "JAVA", "JAVA_STOCHASTIC", etc. Defines list of available simulators for current engine*/
    protected String simulatorType = "NONE";
    protected String solverName;
    protected Simulator simulator;
    protected Diagram diagram;

    protected Model model;

    /**Diagram before any preprocessing*/
    protected Diagram originalDiagram;
    protected EModel executableModel;

    protected boolean terminated = false;

    /**If true than simulation engine should show results on plot while simulating*/
    public boolean needToShowPlot = true;

    /**Type of the model: STATIC, ODE, etc.*/
    protected int modelType;

    protected SimulationResult result;
    
    public String getSimulatorType()
    {
        return simulatorType;
    }
    public FunctionJobControl getJobControl()
    {
        return jobControl;
    }
    public void setJobControl(FunctionJobControl jobControl)
    {
        this.jobControl = jobControl;
    }
    public int getModelType()
    {
        return modelType;
    }

    public void setNeedToShowPlot(boolean value)
    {
        needToShowPlot = value;
    }

    @PropertyName("Show plot")
    public boolean getNeedToShowPlot()
    {
        return needToShowPlot;
    }

    public boolean hasVariablesToPlot()
    {
        Object plotObj = executableModel.getParent().getAttributes().getValue("Plots");

        if( ! ( plotObj instanceof PlotsInfo ) )
            return false;

        PlotsInfo plots = (PlotsInfo)plotObj;

        for( PlotInfo plot : plots.getActivePlots() )
        {
            if( plot.getYVariables().length > 0 )
                return true;
        }
        return false;
    }

    public List<String> getIncorrectPlotVariables() throws Exception
    {
        List<String> incorrect = new ArrayList<>();
        Object plotObj = executableModel.getParent().getAttributes().getValue( "Plots" );
        if( plotObj == null )
            return incorrect;
        PlotsInfo plots = (PlotsInfo)plotObj;
        for( PlotInfo plot : plots.getActivePlots() )
        {
            PlotVariable xVar = plot.getXVariable();
            String path = xVar.getPath();
            EModel emodel = path.isEmpty() ? executableModel
                    : Util.getSubDiagram( originalDiagram, path ).getDiagram().getRole( EModel.class );

            if( !emodel.containsVariable( xVar.getName() ) )
                incorrect.add( xVar.getTitle() );

            for( Curve curve : plot.getYVariables() )
            {
                path = curve.getPath();
                if( path.isEmpty() )
                    emodel = executableModel;
                else
                {
                    SubDiagram subDiagram = Util.getSubDiagram( originalDiagram, path );
                    if( subDiagram == null )                    
                        throw new Exception( "Can not find subdiagram" + path + " in diagram " + originalDiagram );                    
                    emodel = subDiagram.getDiagram().getRole( EModel.class );
                }

                if( !emodel.containsVariable( curve.getName() ) )
                    incorrect.add( curve.getTitle() );
            }
        }
        return incorrect;
    }

    @Override
    public SimulationEngine clone()
    {
        SimulationEngine simulationEngine = null;
        try
        {
            simulationEngine = getClass().newInstance();

            Object solver = getSolver().getClass().newInstance();

            if( solver instanceof Simulator )
            {
                ( (Simulator)solver ).setOptions( ( (Simulator)getSolver() ).getOptions() );
                simulationEngine.setSolver( solver );
            }

            simulationEngine.setCompletionTime( completionTime );
            simulationEngine.setTimeIncrement( timeIncrement );
            simulationEngine.setDiagram( getDiagram() );
        }
        catch( Exception e )
        {
            log.error( "Can not clone simulation engine.", e );
        }
        return simulationEngine;
    }

    //Abstract methods
    public abstract String getEngineDescription();

    /**
     * Get variable name used in mathematical model<br>
     * Engine must guarantee that generated model will have field with name equal to returned by this method String
     */
    public abstract String getVariableCodeName(String varName);

    /**
     * Get variable name used in plain mathematical model of composite diagrams<br>
     */
    public abstract String getVariableCodeName(String diagramName, String varName);

    /**
     * Mapping between variable name and its index in results array.
     */
    public abstract Map<String, Integer> getVarIndexMapping();
    
    /**
     * Mapping between variable path and its index in results array.
     */
    public abstract Map<String, Integer> getVarPathIndexMapping();

    /**
     * Creates mathematical model for further simulation
     */
    public abstract Model createModel() throws Exception;

    /**
     * The method is required for the optimization process, where model is the same for all threads.
     */
    public void setModel(Model model)
    {
        this.model = model;
    }

    /**
     *
     * @param varName
     * @param value
     * @throws IllegalArgumentException
     */
    public void setInitialValue(String varName, double value) throws IllegalArgumentException
    {
        if( !executableModel.containsVariable( varName ) )
            throw new IllegalArgumentException( "Variable " + varName + " not found" );

        executableModel.getVariable( varName ).setInitialValue( value );
    }

    public String[] getVariableNames()
    {
        try
        {
            DataCollection<Variable> collection = executableModel.getVariables();
            return collection.names().toArray( String[]::new );
        }
        catch( Exception ex )
        {
            return new String[] {};
        }
    }

    //override this method if necessary
    public void setOutputDir(String outputDir)
    {
        //do nothing by default
    }

    //override this method if necessary
    public String getOutputDir()
    {
        return null;
    }

    /**
     * Some strange method... not recommended<br>
     *  (it can return String instead of simulator or it can return inner solver without EventLoopSimulator around it which can be inappropriate).
     */
    abstract public Object getSolver();
    abstract public void setSolver(Object solver);

    public File[] generateModel(boolean forceRewrite) throws Exception
    {
        throw new Exception( "Current engine does not support generating model into file" );
    }

    /**
     * Creates mathematical model from file
     * @param files
     * @param compile
     * @param outputDir
     * @return
     * @throws Exception
     */
    public Object[] compileModel(File[] files, boolean compile, String outputDir) throws Exception
    {
        log.info( "Model " + diagram.getName() + ": Java code compilation..." );
        Object[] result = new Object[files.length];
        for( int i = 0; i < files.length; i++ )
        {
            String className = files[i].getName();
            className = className.substring( 0, className.length() - 5 );

            if( compile )
            {
                files[i] = new File( outputDir, className + ".java" );
                StringBuilder classPath = new StringBuilder(outputDir);
                for( PluginEntry f : getClassPathEntries() )
                    classPath.append(File.pathSeparator).append(f.extract().getAbsolutePath());

                // Compile into a fresh private staging directory (never the shared outputDir) so the
                // revision is exactly this compilation's output: no leftover nested classes from an
                // earlier revision, and no read-back from a possibly concurrently mutated outputDir.
                File outDir = new File( outputDir );
                File stagingDir = Files.createTempDirectory( outDir.toPath(), "stage-" + className + "-" ).toFile();
                try
                {
                    RunTimeCompiler comp = new RunTimeCompiler( classPath.toString(), outputDir, stagingDir.getAbsolutePath(),
                            new String[] {outputDir + File.separator + files[i].getName()} );
                    if( !comp.execute() )
                        throw new BiosoftParseException( new Exception(comp.getMessages()), files[i].getName() );

                    // Publish the compiled classes as an immutable per-revision snapshot and populate
                    // the legacy root, both under the per-directory lock. Load through a shared class
                    // loader rooted at that snapshot (see modelClassLoader() / modelRevisionDir()). A
                    // per-run loader leaked because the returned model instance pinned its defining
                    // loader in the heap.
                    File revisionDir = publishRevision( outDir, stagingDir, className );
                    URLClassLoader cl = modelClassLoader( revisionDir, className, getClass().getClassLoader() );
                    try
                    {
                        result[i] = cl.loadClass( className ).newInstance();
                    }
                    catch( Throwable t )
                    {
                        throw new Exception( "Can not load model, name=" + className + ", error=" + t );
                    }
                }
                catch( Exception e )
                {
                    // The staging directory is consumed (moved) by a successful publish; discard it if
                    // anything above failed before that.
                    deleteRecursively( stagingDir );
                    throw e;
                }
                continue;
            }

            // compile == false: no compilation happened, so the class must already be present in the
            // root of the shared output directory. Load it (root read + snapshot publish under the
            // per-directory lock, see loadFromExistingRoot).
            File outDir = new File( outputDir );
            File revisionDir = loadFromExistingRoot( outDir, className );
            URLClassLoader cl = modelClassLoader( revisionDir, className, getClass().getClassLoader() );
            try
            {
                result[i] = cl.loadClass( className ).newInstance();
            }
            catch( Throwable t )
            {
                throw new Exception( "Can not load model, name=" + className + ", error=" + t );
            }
        }
        return result;
    }

    public Object[] compileModel(File[] files, String classPath) throws Exception
    {
        log.info( "Model " + diagram.getName() + ": Java code compilation..." );
        Object[] result = new Object[files.length];

        String outDirName = files[0].getParentFile().getAbsolutePath();
        String[] fileNames = new String[files.length];
        for( int i = 0; i < files.length; i++ )
            fileNames[i] = files[i].getAbsolutePath();

        // Compile all supplied sources into a fresh private staging directory so the output is a clean
        // set of class files (no leftover nested classes from earlier compilations sharing the source
        // directory). The whole staging output is published as a single shared snapshot, because the
        // supplied classes may reference one another (a per-class subset would drop such a dependency
        // and break with NoClassDefFoundError at resolution time).
        File outDir = new File( outDirName );
        String[] classNames = new String[ files.length ];
        for( int i = 0; i < files.length; i++ )
            classNames[i] = files[i].getName().substring( 0, files[i].getName().lastIndexOf( "." ) );

        File stagingDir = Files.createTempDirectory( outDir.toPath(), "stage-batch-" ).toFile();
        try
        {
            RunTimeCompiler comp = new RunTimeCompiler( classPath, outDirName, stagingDir.getAbsolutePath(), fileNames );
            if( !comp.execute() )
                throw new BiosoftParseException( new Exception( comp.getMessages() ), files[0].getName() );

            // Publish the complete compilation output as one immutable, shared snapshot and populate
            // the legacy root with the requested models' classes, both under the per-directory lock.
            File revisionDir = publishRevision( outDir, stagingDir, classNames );

            for( int i = 0; i < files.length; i++ )
            {
                String className = classNames[i];
                URLClassLoader cl = modelClassLoader( revisionDir, className, getClass().getClassLoader() );
                try
                {
                    result[i] = cl.loadClass( className ).newInstance();
                }
                catch( Throwable t )
                {
                    throw new Exception( "Can not load model, name=" + className + ", error=" + t );
                }
            }
        }
        finally
        {
            // The staging directory is consumed (moved) by a successful publish, or still present on
            // failure; clean it up either way so a successful batch compilation does not leak it.
            deleteRecursively( stagingDir );
        }
        return result;
    }

    /**
     * Copies the class files belonging to {@code className} (the class plus any nested
     * {@code className$*.class} files) from the shared {@code outputDir} into a fresh private staging
     * directory. Used when a model is not recompiled ({@code compile == false}): the class is taken
     * from its existing location in the shared directory, staged for a consistent immutable snapshot.
     */
    private static void stageExistingClasses(File outputDir, String className, File stagingDir) throws IOException
    {
        String[] all = outputDir.list( ( d, name ) -> name.endsWith( ".class" ) );
        if( all != null )
        {
            for( String name : all )
            {
                if( name.equals( className + ".class" ) || ( name.startsWith( className + "$" ) && name.endsWith( ".class" ) ) )
                    Files.copy( new File( outputDir, name ).toPath(), new File( stagingDir, name ).toPath(),
                            StandardCopyOption.REPLACE_EXISTING );
            }
        }
    }

    /**
     * Copies the class files belonging to {@code className} (the class plus any nested
     * {@code className$*.class} files) from the staging directory into the root of the shared output
     * directory, preserving the legacy contract that a compiled model's classes are present in the
     * output directory root (so a later {@code compile == false} load can find them).
     */
    private static void copyClassesToRoot(File stagingDir, File outputRoot, String className) throws IOException
    {
        String[] all = stagingDir.list( ( d, name ) -> name.endsWith( ".class" ) );
        if( all != null )
        {
            for( String name : all )
            {
                if( name.equals( className + ".class" ) || ( name.startsWith( className + "$" ) && name.endsWith( ".class" ) ) )
                    Files.copy( new File( stagingDir, name ).toPath(), new File( outputRoot, name ).toPath(),
                            StandardCopyOption.REPLACE_EXISTING );
            }
        }
    }

    /**
     * Copies every {@code .class} file from the staging directory into the root of the shared output
     * directory, preserving the legacy contract for batch compilation.
     */
    private static void copyAllClassesToRoot(File stagingDir, File outputRoot) throws IOException
    {
        String[] all = stagingDir.list( ( d, name ) -> name.endsWith( ".class" ) );
        if( all != null )
        {
            for( String name : all )
                Files.copy( new File( stagingDir, name ).toPath(), new File( outputRoot, name ).toPath(),
                        StandardCopyOption.REPLACE_EXISTING );
        }
    }

    protected List<PluginEntry> getClassPathEntries()
    {
        try
        {
            return Arrays.asList( ApplicationUtils.resolvePluginPath( "biouml.plugins.simulation:src.jar" ) );
        }
        catch( Exception e )
        {
            return Collections.emptyList();
        }
    }

    public String getSrcDir()
    {
        return srcDir;
    }


    public void setSrcDir(String dir)
    {
        this.srcDir = dir;
    }

    /**
     * Utility method to write needed source files from the specified source
     * to the specified directory.
     */
    protected void writeFile(String source, boolean rewrite) throws Exception
    {
        File out = new File( getOutputDir(), source );
        if( out.exists() && !rewrite )
            return;
        out.getParentFile().mkdirs();

        // load from srcDir
        File file = new File( source );
        if( !file.exists() )
            file = new File( getSrcDir(), source );

        InputStream is;
        if( file.exists() )
            is = new FileInputStream( file );
        else
        {
            // try to load through plugin class loader
            ClassLoader cl = getClass().getClassLoader();
            URL url = cl.getResource( source );

            if( url == null ) // try to get resources as file
                throw new FileNotFoundException( "Can not find file: " + source );

            is = url.openStream();
        }

        com.developmentontheedge.application.ApplicationUtils.copyStream( new FileOutputStream( out, false ), is );
    }

    public String[] getAvailableSolvers()
    {
        return SimulatorRegistry.registry( getSimulatorType() ).keys().toArray( String[]::new );
    }

    /**
     * Simulate mathematical model <b>model</b>
     * @param model
     * @param resultListeners
     * @return
     * @throws Exception
     */
    public abstract String simulate(Model model, ResultListener[] resultListeners) throws Exception;

    /**
     * Method to simulate model generated from currently set diagram
     * @param resultListeners
     * @return
     * @throws Exception
     */
    public String simulate(ResultListener[] resultListeners) throws Exception //again only in tests
    {
        return simulate( createModel(), resultListeners );
    }

    public String simulate(Model model) throws Exception //used only in tests (except TreatmentSystem)
    {
        return simulate( model, getListeners() );
    }

    public String simulate() throws Exception //used only in tests
    {
        return simulate( createModel() );
    }

    protected List<ResultListener> listeners = new ArrayList<>();
    public void addListeners(ResultListener ... resultListeners)
    {
        listeners.addAll( Arrays.asList( resultListeners ) );
    }

    public ResultListener[] getListeners()
    {
        return listeners.toArray( new ResultListener[listeners.size()] );
    }

    public void removeAllListeners()
    {
        listeners.clear();
    }

    /**
     * Method to simulate model and setting results to SimulationResult <b>result</b>
     * @param model
     * @param result
     * @return
     * @throws Exception
     */
    public String simulate(Model model, SimulationResult result) throws Exception
    {
        initSimulationResult( result );
        return simulate( model, new ResultListener[] {new ResultWriter( result )} );
    }

    /**
     * Method to simulate model and setting results to SimulationResult <b>result</b>
     * @param model
     * @param result
     * @return
     * @throws Exception
     */
    public String simulate(SimulationResult result) throws Exception
    {
        return simulate( createModel(), result );
    }


    /**
     * Common method to stop simulation by stopping simulator work
     */
    public void stopSimulation()
    {
        setTerminated(true);
        if( simulator != null )
            simulator.stop();
    }

    /**
     * Returns options for simulator
     * @return
     */
    @PropertyName("Simulator options")
    @PropertyDescription("Simulator options.")
    public Options getSimulatorOptions()
    {
        return simulator.getOptions();
    }

    public void setSimulatorOptions(Options options)
    {
        Options oldValue = this.simulator.getOptions();
        options.setParent( this );
        simulator.setOptions( options );
        firePropertyChange( "simulatorOptions", oldValue, options );
        firePropertyChange( "*", null, null );
    }


    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /**
     * Method to get real simulator for further using independently from this engine.<br>
     * @return
     */
    public Simulator getSimulator()
    {
        return simulator;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    /**
     * returns unprocessed diagram
     * @return
     */
    public Diagram getOriginalDiagram()
    {
        return originalDiagram;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( doNotAffectSimulation( evt))
           return;
        diagramModified = true;
        restoreOriginalDiagram();
    }

    protected boolean doNotAffectSimulation(PropertyChangeEvent evt)
    {
        if( evt != null )
        {
            String propertyName = evt.getPropertyName();
            if( "path".equals( propertyName ) || "location".equals( propertyName ) )
                return true;
        }
        return false;
    }

    public void releaseDiagram()
    {
        if( this.diagram != null )
            this.diagram.removePropertyChangeListener( diagramListener );
    }

    public void setDiagram(Diagram diagram)
    {
        Diagram oldValue = this.diagram;
        this.originalDiagram = diagram;
        this.diagram = diagram;
        if( diagram != null && diagram.getRole() != null && diagram.getRole() instanceof EModel )
        {
            executableModel = diagram.getRole( EModel.class );
        }
        firePropertyChange( "diagram", oldValue, diagram );

        diagramModified = true;

        if( oldValue != null )
            oldValue.removePropertyChangeListener( diagramListener );

        diagramListener = diagram != null ? new WeakPropertyChangeForwarder( this, diagram ) : null;
    }

    public EModel getExecutableModel()
    {
        return executableModel;
    }

    @PropertyName("Simulator name")
    @PropertyDescription("Simulator name.")
    public String getSolverName()
    {
        return SimulatorRegistry.getSolverName( getSolver().getClass() );
    }

    public void setSolverName(String solverName)
    {
        if( Objects.equals( this.solverName, solverName ) )
            return;

        try
        {
            Simulator simulator = SimulatorRegistry.getSimulator( solverName );
            simulator.getOptions().setParent( this );
            setSolver( simulator );
            this.solverName = solverName;
        }
        catch( Exception e )
        {
            log.error( "Can set solver name by name, error: " + e, e );
            return;
        }
    }
    
    /**
     * Sets initial, completion and time step from span
     */
    public void setSpan(Span span)
    {
        setInitialTime(span.getTimeStart());
        setCompletionTime(span.getTimeFinal());
        if( span instanceof UniformSpan )
        {
            setTimeIncrement( ( (UniformSpan)span ).getTimeIncrement());
        }
        else
        {
            setTimeIncrement(span.getTime(1) - span.getTimeStart());
        }
    }

    @PropertyName("Initial time")
    @PropertyDescription("Initial time.")
    public double getInitialTime()
    {
        return this.initialTime;
    }

    public void setInitialTime(double initialTime)
    {
        double oldValue = getInitialTime();
        this.initialTime = initialTime;
        firePropertyChange( "initialTime", oldValue, initialTime );
    }
   
    @PropertyName("Completion time")
    @PropertyDescription("Completion time.")
    public double getCompletionTime()
    {
        return this.completionTime;
    }

    public void setCompletionTime(double completionTime)
    {
        double oldValue = getCompletionTime();
        this.completionTime = completionTime;
        firePropertyChange( "completionTime", oldValue, completionTime );
    }
    
    @PropertyName("Time increment")
    @PropertyDescription("Time increment.")
    public double getTimeIncrement()
    {
        return this.timeIncrement;
    }

    public void setTimeIncrement(double timeIncrement)
    {
        double oldValue = getTimeIncrement();
        this.timeIncrement = timeIncrement;
        firePropertyChange( "timeIncrement", oldValue, timeIncrement );
    }

    public void initSimulationResult(SimulationResult simuationResult)
    {
        simuationResult.setInitialTime( getInitialTime() );
        simuationResult.setCompletionTime( getCompletionTime() );
        //        simuationResult.setSimulatorName( getSolverName() );
        if( diagram != null )
            simuationResult.setDiagramPath( diagram.getCompletePath() );

        // fill initial values
        for( Variable var : executableModel.getVariables() )
        {
            if( Boolean.TRUE.equals( var.getAttributes().getValue( Preprocessor.AUTOGENERATED_VAR ) ) )
                continue;
            simuationResult.addInitialValue( var );
        }
        simuationResult.setVariableMap( getVarPathIndexMapping()  );
        simuationResult.setVariablePathMap( getVarPathIndexMapping() );
        this.result = simuationResult;
    }

    /** Returns information about x variable for this particular plot */
    public SimulationEngine.Var getXVariable(PlotInfo plot)
    {
        PlotVariable xVariable = plot.getXVariable();
        String name = xVariable.getName();
        String title = xVariable.getTitle();
        String path = xVariable.getPath();
        SubDiagram subDiagram = Util.getSubDiagram(originalDiagram, path);
        EModel emodel = (subDiagram != null)? subDiagram.getDiagram().getRole(EModel.class): originalDiagram.getRole(EModel.class);
        Variable var = emodel.getVariable( name );
        double initialValue = var.getInitialValue();
        Integer index = getVarPathIndexMapping().get(path.isEmpty() ? name : path + VAR_PATH_DELIMITER + name);
        return new Var( name, title, initialValue, index, null );
    }
    
    /** Returns information about all plots that should be demonstrated */
    public PlotInfo[] getPlots()
    {
        Object plotObj = executableModel.getParent().getAttributes().getValue("Plots");
        if( plotObj instanceof PlotsInfo )
            return ( (PlotsInfo)plotObj ).getActivePlots();
        return new PlotInfo[0];
    }
    
    /** Returns all variables which should be demonstrated on this particular plot */
    public Map<SimulationEngine.Var, List<Series>> getVariablesToPlot(PlotInfo plot)
    {
        Map<Var, List<Series>> variablesToPlot = new TreeMap<>();

        for( Curve curve : plot.getYVariables() )
        {
            String name = curve.getName();
            String title = curve.getTitle();
            String path = curve.getPath();
            SubDiagram subDiagram = Util.getSubDiagram(originalDiagram, path);
            EModel emodel = (subDiagram != null)? subDiagram.getDiagram().getRole(EModel.class): originalDiagram.getRole(EModel.class);
            double initialValue = emodel.getVariable(name).getInitialValue();
            Integer index = getVarPathIndexMapping().get(path.isEmpty() ? name : path + VAR_PATH_DELIMITER + name);
            List<Series> list = new ArrayList<>();   
            list.add(new XYSeries(title, false, true));
            if( !variablesToPlot.keySet().stream().anyMatch( k -> k.title.equals( title ) ) )
                variablesToPlot.put( new Var( name, title, initialValue, index, curve.getPen() ), list );
        }
        return variablesToPlot;
    }

    public void setStandalone(boolean standalone)
    {
        //do nothing by default
    }
    
    public boolean isTerminated()
    {
        return terminated;
    }
    public void setTerminated(boolean terminated)
    {
        this.terminated = terminated;
    }


    public void restoreOriginalDiagram()
    {
        this.diagram = originalDiagram;
        this.executableModel = (EModel)originalDiagram.getRole();
    }

    protected List<Preprocessor> getDiagramPreprocessors()
    {
        return new ArrayList<>();
    }

    public void preprocess(Diagram originalDiagram) throws Exception
    {
        executableModel = diagram.getRole( EModel.class );

        boolean notify = executableModel.isNotificationEnabled();
        boolean notifyDiagram = diagram.isNotificationEnabled();
        executableModel.setNotificationEnabled( false );
        diagram.setNotificationEnabled( false );

        for( Preprocessor p : StreamEx.of(additionalPreprocessors).append(getDiagramPreprocessors()).append(additionalPreprocessors2)
                .filter(p -> p.accept(diagram)) )
            diagram = p.preprocess(diagram);

        executableModel.setNotificationEnabled( notify );
        diagram.setNotificationEnabled( notifyDiagram );
        executableModel = diagram.getRole( EModel.class );
    }

    /**
     * Add preprocessor which will be used before all other preprocessors
     */
    public void addPreprocessor(Preprocessor preprocessor)
    {
        additionalPreprocessors.add( preprocessor );
    }

    /**
     * Add preprocessor which will be used after all other preprocessors
     */
    public void addPreprocessor2(Preprocessor preprocessor)
    {
        additionalPreprocessors2.add( preprocessor );
    }

    public void resetPreprocessors()
    {
        additionalPreprocessors.clear();
        additionalPreprocessors2.clear();
    }

    public static String escapeWrongSymbols(String str)
    {
        String wrongLetters = ": .,;|()[]{}+-*/%^!~&|$'\"";
        char[] data = str.toCharArray();
        for( int i = 0; i < data.length; i++ )
        {
            if( wrongLetters.indexOf( data[i] ) >= 0 )
                data[i] = '_';
        }

        str = new String( data );
        if( Character.isDigit( str.charAt( 0 ) ) )
            str = "_" + str;

        return str;
    }

    /**
     * Normalize name to be valid simulation script name.
     */
    public String normalize(String name)
    {
        // cut $ prefix
        while( name.charAt( 0 ) == '$' )
        {
            name = name.substring( 1 );
        }

        // cut '.xml' suffix
        int offset = name.lastIndexOf( "." );
        if( offset > 0 && name.length() > offset + 3 && name.charAt( offset + 1 ) == 'x' && name.charAt( offset + 2 ) == 'm'
                && name.charAt( offset + 3 ) == 'l' )
        {
            name = name.substring( 0, offset );
            // replaces most common error characters
        }

        name = escapeWrongSymbols( name );

        return name;
    }

    public void clearContext()
    {
        //nothing by default
    }
    
    public SimulationEngineLogger getLogger()
    {
        return log;
    }
    
    public void setLogLevel(Level level)
    {
        log.getLogger().setLevel( level );
    }
    
    public static class Var implements Comparable<Var>
    {
        public String name;
        public double initialValue;
        public Integer index;
        public Pen pen;
        public Var mainVar = null;
        public String title;

        public Var(Var mainVar)
        {
            this(mainVar.name, mainVar.initialValue, mainVar.index, mainVar.pen);
            this.mainVar = mainVar;
        }

        public Var(String name, double initialValue, Integer index, Pen pen)
        {
            this(name, name, initialValue, index, pen);
        }
        
        public Var(String name, String title, double initialValue, Integer index, Pen pen)
        {
            this.name = name;
            this.title = title;
            this.initialValue = initialValue;
            this.index = index;
            this.pen = pen;
        }

        @Override
        public int compareTo(Var o)
        {
            int compareNames = name.compareTo(o.name);
            return compareNames == 0 ? title.compareTo(o.title) : 1;
        }
    }

    /**Utility method. Finds NANs and Infinity in model parameters and logs message*/
    public void checkVariables(double[] values, Map<Integer, String> names)
    {
        List<String> nan = new ArrayList<>();
        List<String> inf = new ArrayList<>();

        for( int i = 0; i < values.length; i++ )
        {
            double val = values[i];
            if( Double.isNaN(val) )
                nan.add(names.get(i));
            else if( Double.isInfinite(val) )
                inf.add(names.get(i));
        }
        if( nan.isEmpty() && inf.isEmpty() )
            return;

        log.info("At time = " + getSimulator().getProfile().getTime() + ":");
        if( !nan.isEmpty() )
            log.info("Next variables turned NaN: " + StreamEx.of(nan).joining(",\t"));

        if( !inf.isEmpty() )
            log.info("Next variables turned INFINITE: " + StreamEx.of(inf).joining(",\t"));
    }
    
    public SimulationResult generateSimulationResult()
    {
        SimulationResult result = new SimulationResult(null, "tmp");
        initSimulationResult(result);
        return result;
    }
    
    public int getSimulationType()
    {
        return DETERMINISTIC_TYPE;
    }

    public ResultListener generateResultPlot(FunctionJobControl jobControl, PlotInfo plotInfo)
    {
        return new ResultPlotPane( this, jobControl, plotInfo );
    }
    
    public Object getPlotsBean(Diagram diagram)
    {
        return null;
    }
}