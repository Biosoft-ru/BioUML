package ru.biosoft.journal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckForNull;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.DatabaseVersionComparator;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class ProjectUtils
{
    public static final String NEWEST_VERSION = "(newest)";
    public static final String DATABASE_VERSION_PROPERTY_PREFIX = "defaultVersion.";

    /**
     * Returns database name without version
     * @param dc - Database main collection (like "databases/Ensembl")
     */
    public static String getDatabaseName(DataCollection dc)
    {
        if(dc == null) return null;
        Properties properties = dc.getInfo().getProperties();
        String database = properties.getProperty("database");
        if(database == null) return null;
        String species = properties.getProperty(DataCollectionUtils.SPECIES_PROPERTY);
        if(species != null) database = database+" ("+species+")";
        return database;
    }
    
    public static boolean isDatabasePreferred(DataElementPath database)
    {
        return isDatabasePreferred(JournalRegistry.getProjectPath(), database);
    }
    
    public static boolean isDatabasePreferred(DataElementPath project, DataElementPath database)
    {
        DataCollection<?> db = database.optDataCollection();
        if(db == null) return false;
        String databaseName = getDatabaseName(db);
        if(databaseName == null) return true;
        String version = getVersion( db );
        SortedSet<String> versions = getAvailableDatabaseVersions().get(databaseName);
        if(versions == null)
            return true;
        String property = null;
        try
        {
            if( project != null )
                property = project.getDataCollection().getInfo().getProperty( DATABASE_VERSION_PROPERTY_PREFIX + databaseName );
        }
        catch( RepositoryException e )
        {
        }
        if(property != null && versions.contains(property)) return version.equals(property);
        return version.equals(versions.last());
    }
    
    public static Map<String, DataElementPath> getPreferredDatabasePaths()
    {
        return getPreferredDatabasePaths(JournalRegistry.getProjectPath());
    }

    public static Map<String, DataElementPath> getPreferredDatabasePaths(DataElementPath project)
    {
        return getPreferredDatabasePaths(project == null ? null : project.getDataCollection().getInfo().getProperties());
    }

    public static Map<String, DataElementPath> getPreferredDatabasePaths(Properties properties)
    {
        Map<String, SortedSet<String>> versions = getAvailableDatabaseVersions();
        Map<String, DataElementPath> result = new HashMap<>();
        for( DataCollection<?> dc : CollectionFactoryUtils.getDatabases() )
        {
            String databaseName = getDatabaseName(dc);
            if(databaseName == null) continue;
            SortedSet<String> dbVersions = versions.get(databaseName);
            boolean preferred;
            if(dbVersions == null)
            {
                preferred = true;
            } else
            {
                String version = getVersion(dc);
                if(version.isEmpty()) continue;
                String property = properties == null ? null : properties.getProperty( DATABASE_VERSION_PROPERTY_PREFIX + databaseName );
                if(property != null && dbVersions.contains(property))
                    preferred = version.equals(property);
                else
                    preferred = version.equals(dbVersions.last());
            }
            if(preferred)
            {
                result.put(databaseName, DataElementPath.create(dc));
            }
        }
        return result;
    }
    
    public static String getVersion(DataCollection<?> dc)
    {
        if(dc == null)
            return "";
        return TextUtil2.nullToEmpty( dc.getInfo().getProperty( "version" ) );
    }

    public static DataElementPath getPreferredDatabasePath(String wantedDatabase)
    {
        return getPreferredDatabasePath(wantedDatabase, JournalRegistry.getProjectPath());
    }

    public static DataElementPath getPreferredDatabasePath(String wantedDatabase, DataElementPath project)
    {
        return getPreferredDatabasePath(wantedDatabase, project == null ? null : project.getDataCollection().getInfo().getProperties());
    }

    public static DataElementPath getPreferredDatabasePath(String wantedDatabase, Properties properties)
    {
        String wantedVersion = properties == null ? null : properties.getProperty( DATABASE_VERSION_PROPERTY_PREFIX + wantedDatabase );
        Optional<DataCollection<?>> result;
        if( wantedVersion == null )
        {
            result = CollectionFactoryUtils.getDatabases().stream().filter( dc -> wantedDatabase.equals( getDatabaseName( dc ) ) )
                    .max( (x,y) -> new DatabaseVersionComparator().compare(getVersion(x), getVersion(y)));
        }
        else
        {
            result = CollectionFactoryUtils.getDatabases().stream()
                    .filter( dc -> wantedDatabase.equals( getDatabaseName( dc ) ) && getVersion( dc ).equals( wantedVersion ) ).findFirst();
        }
        return result.map( ru.biosoft.access.core.DataCollection::getCompletePath ).orElse( null );
    }

    /**
     * @return map databaseName -> set of available versions
     */
    // Per-user cache of getAvailableDatabaseVersions() results. The method scans all
    // databases and re-resolves every database element on each call (each resolution goes
    // through SecurityManager.getPermissions). The list of installed databases rarely changes
    // during a server run, while the method is called from hot paths
    // (BioHubRegistry.isHubPathAvailable, ProjectUtils.isDatabasePreferred) for every BioHub
    // on every workflow initialization.
    //
    // The value is PER-USER: the databases view is per-session, so a value computed for one
    // user (e.g. a restricted user who sees a subset) must not be served to another (e.g. an
    // admin who sees all databases). The cache is a map from user identity to their value,
    // so users do not evict each other: each user keeps hitting their own entry as long as
    // they keep working, regardless of what other users do.
    //
    // Concurrency: invalidation, publication, and the capacity bound are all performed under
    // versionsCacheLock. A computation that started under generation N is only installed if
    // the generation is still N when publication happens, so an in-flight computation cannot
    // repopulate the cache after a concurrent invalidateAvailableDatabaseVersions(). The
    // capacity check is re-run at publication time (under the same lock) so the map never
    // exceeds MAX_CACHED_USERS, even when several users miss and compute concurrently. The
    // returned map is deep-immutable, so callers can never mutate the cached value.
    //
    // Invalidation:
    //  - invalidateAvailableDatabaseVersions() clears ALL users' entries. It is called from
    //    SecurityManager.invalidatePermissions (a permission change affects every user's
    //    database visibility). This is the invalidation path in use: databases are
    //    provisioned into the repository at startup (from the data directories) and are
    //    not installed or removed at runtime, so a permission reload is the only event
    //    that changes which databases are visible.
    //  - The map is bounded (MAX_CACHED_USERS): when too many distinct users have cached a
    //    value, the whole map is dropped so a long-running, many-user server cannot grow it
    //    unboundedly. Sessions are short-lived, so dropping everyone's entry occasionally is
    //    cheap insurance.
    private static final ConcurrentHashMap<String, Map<String, SortedSet<String>>> versionsByUser = new ConcurrentHashMap<>();
    private static final Object versionsCacheLock = new Object();
    private static final AtomicLong availableVersionsGeneration = new AtomicLong();
    // Upper bound on the number of users holding a cached entry. Beyond this the whole map
    // is dropped (see getAvailableDatabaseVersions). Picked well above any realistic number
    // of concurrently-active users so steady-state workloads never hit it.
    private static final int MAX_CACHED_USERS = 128;

    // Test hook: if non-null, invoked just BEFORE the publication critical section of
    // getAvailableDatabaseVersions() — after the expensive computation, before the lock is
    // acquired and the generation checked. A regression test uses this to pause a thread so
    // another thread can run an invalidation (which takes the lock) while the paused thread
    // holds an old generation, then verify the stale value is not installed. Null in
    // production.
    static volatile Runnable publicationHook;

    /**
     * The identity of the current user for the purposes of the database version cache.
     * Returns the logged-in user's name, or {@code null} for the SYSTEM/bootstrap session.
     *
     * <p>The value is keyed by user name because database visibility in this codebase is
     * resolved by user name: {@code SecurityManager.getPermissions} looks up the user's
     * {@code UserPermissions} (by session → user) and calls
     * {@code SecurityProvider.getPermissions(path, userInfo)}, and the provider's
     * decision is based on {@code userInfo.getUser()}, not on the session id. Two sessions
     * for the same user name therefore see the same databases, so user name is a safe cache
     * key. (If a future SecurityProvider were to make visibility session-specific — e.g.
     * per-session impersonation or repository binding — the key would need to incorporate
     * the session id as well.)
     */
    private static String currentUserIdentity()
    {
        return SecurityManager.getSessionUser();
    }

    private static String userKey(String user)
    {
        return user == null ? "" : user;
    }

    /**
     * Invalidate the cache of available database versions for ALL users. Called from
     * {@code SecurityManager.invalidatePermissions} (a permission change affects which
     * databases are visible to every user). The generation bump and the clear are atomic
     * with any concurrent publication (which holds the same lock), so an in-flight
     * computation cannot install a stale entry after the invalidation.
     *
     * <p>Semantics: this guarantees that a stale value is never <em>cached</em>. A request
     * already in flight may return the value it computed, even if permissions are
     * invalidated while it is running. Such a value is not published to the cache. Calls
     * made after invalidation completes will recompute using the current security state.
     *
     * <p>Note: the cache does not enforce snapshot isolation for permissions. The
     * returned value may reflect the old state, the new state, or a mixture, depending on
     * when the underlying {@code getDatabases()} resolution observes the permission change.
     */
    public static void invalidateAvailableDatabaseVersions()
    {
        synchronized( versionsCacheLock )
        {
            availableVersionsGeneration.incrementAndGet();
            versionsByUser.clear();
        }
    }

    public static Map<String, SortedSet<String>> getAvailableDatabaseVersions()
    {
        String user = currentUserIdentity();
        String key = userKey( user );
        Map<String, SortedSet<String>> versions = versionsByUser.get( key );
        if( versions != null )
            return versions;

        // Compute the value OUTSIDE the lock (the expensive database scan) so unrelated
        // users' requests are not serialized. Capture the generation now; publication below
        // discards the result if an invalidation happens while we compute.
        long generation = availableVersionsGeneration.get();
        Map<String, SortedSet<String>> computed = buildAvailableDatabaseVersions();

        // Publish under versionsCacheLock (the same lock invalidation holds), so the
        // generation check, the capacity bound, and the put are atomic with respect to any
        // concurrent invalidation:
        //  - if the generation changed (an invalidation ran while we computed), the result is
        //    stale and is discarded — the caller still gets the correct `computed` value, just
        //    not installed;
        //  - the capacity is enforced HERE, at publication, so the map never exceeds
        //    MAX_CACHED_USERS even when several users miss and compute concurrently (a
        //    pre-check alone would not hold: two threads could both observe size 127 and
        //    both publish, reaching 129);
        //  - a capacity clear bumps the generation, so our own (old-generation) result is
        //    discarded rather than installed into the freshly-cleared map.
        // Test hook: if non-null, invoked just BEFORE the publication critical section —
        // after the expensive computation, before the lock is acquired and the generation
        // checked. A regression test uses this to pause a thread so another thread can run
        // an invalidation (which takes the lock) while the paused thread holds an old
        // generation. The paused thread then enters the block and must discard its stale
        // value. Null in production.
        if( publicationHook != null )
            publicationHook.run();
        synchronized( versionsCacheLock )
        {
            if( generation == availableVersionsGeneration.get() )
            {
                if( !versionsByUser.containsKey( key ) && versionsByUser.size() >= MAX_CACHED_USERS )
                {
                    availableVersionsGeneration.incrementAndGet();
                    versionsByUser.clear();
                }
                else
                {
                    versionsByUser.put( key, computed );
                }
            }
        }
        return computed;
    }

    private static Map<String, SortedSet<String>> buildAvailableDatabaseVersions()
    {
        DataCollection<DataCollection<?>> databases = CollectionFactoryUtils.getDatabases();
        TreeMap<String, SortedSet<String>> result = new TreeMap<>();
        StreamEx.of( databases.stream() )
                .mapToEntry( ProjectUtils::getDatabaseName, ProjectUtils::getVersion )
                .nonNullKeys().removeValues( String::isEmpty ).groupingTo( TreeMap::new, () -> {
                    return new TreeSet<>( new DatabaseVersionComparator() );
                } ).forEach( result::put );
        // Deep-freeze so the cached value cannot be mutated by any caller.
        for( Map.Entry<String, SortedSet<String>> entry : result.entrySet() )
            entry.setValue( Collections.unmodifiableSortedSet( entry.getValue() ) );
        return Collections.unmodifiableSortedMap( result );
    }

    public static Map<String, String> getPreferredDatabaseVersions(DataElementPath project)
    {
        Map<String, SortedSet<String>> versions = getAvailableDatabaseVersions();
        Map<String, String> result = new HashMap<>();
        for( DataCollection<?> dc : CollectionFactoryUtils.getDatabases() )
        {
            String databaseName = getDatabaseName(dc);
            if(databaseName == null) continue;
            SortedSet<String> dbVersions = versions.get(databaseName);
            if(dbVersions == null) continue;
            String version = getVersion(dc);
            if(version.isEmpty()) continue;
            String property = null;
            try
            {
                property = project.getDataCollection().getInfo()
                        .getProperty(DATABASE_VERSION_PROPERTY_PREFIX + databaseName);
            }
            catch(Exception e)
            {
            }
            boolean preferred;
            if(property != null && dbVersions.contains(property))
                preferred = version.equals(property);
            else
                preferred = version.equals(dbVersions.last());
            if(preferred)
            {
                result.put(databaseName, version);
            }
        }
        return result;
    }
    
    public static Map<String, String> getPreferredDatabaseVersions()
    {
        return getPreferredDatabaseVersions(JournalRegistry.getProjectPath());
    }

    public static @CheckForNull DataElementPath getDestinationProjectPath(DataElementPath destPath)
    {
        if( destPath == null )
            return null;
        DataElementPath targetPath = destPath.getTargetPath();
        DataElementPath userProjectPath = CollectionFactoryUtils.getUserProjectsPath().getTargetPath();
        if(!targetPath.isDescendantOf( userProjectPath ))
            return null;
        String[] pathComponents = destPath.getPathComponents();
        if(pathComponents.length >=3)
            return ru.biosoft.access.core.DataElementPath
                    .create( String.join( DataElementPath.PATH_SEPARATOR, pathComponents[0], pathComponents[1], pathComponents[2] ) );
        return null;
    }

    public static DataElementPath getProjectPath(DataElementPath destPath)
    {
        DataElementPath defaultPath = JournalRegistry.getProjectPath();
        if( destPath == null )
            return defaultPath;
        DataElementPath targetPath = destPath.getTargetPath();
        DataElementPath userProjectPath = CollectionFactoryUtils.getUserProjectsPath().getTargetPath();
        if( !targetPath.isDescendantOf( userProjectPath ) )
            return defaultPath;
        String[] pathComponents = destPath.getPathComponents();
        if( pathComponents.length >= 3 )
            return ru.biosoft.access.core.DataElementPath
                    .create( String.join( DataElementPath.PATH_SEPARATOR, pathComponents[0], pathComponents[1], pathComponents[2] ) );
        return defaultPath;
    }
}
