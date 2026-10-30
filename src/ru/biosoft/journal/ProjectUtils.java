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
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
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
    // admin who sees all databases). The cache is a map from user identity to their value +
    // the per-session databases view it was built from, so users do not evict each other:
    // each user keeps hitting their own entry as long as they keep working, regardless of
    // what other users do.
    //
    // Concurrency measures:
    //  1. The returned map is deep-immutable, so callers can never mutate the cached value.
    //  2. A generation counter makes global invalidation race-safe: a computation that starts
    //     under generation N is only installed if the generation is still N when it finishes,
    //     so an in-flight computation cannot repopulate the cache after a concurrent
    //     invalidateAvailableDatabaseVersions().
    //  3. Per-user entries are removed atomically (compute / remove) so a stale entry for
    //     user A is never observed by user B, and a user's entry is never partially published.
    //
    // Invalidation:
    //  - invalidateAvailableDatabaseVersions() clears ALL users' entries. It is called from
    //    SecurityManager.invalidatePermissions (a real permission change affects every user)
    //    and from the databases-collection listener (databases added/removed).
    //  - The map is bounded (MAX_CACHED_USERS): when too many distinct users have cached a
    //    value, the whole map is dropped so a long-running, many-user server cannot grow it
    //    unboundedly. Sessions are short-lived, so dropping everyone's entry occasionally is
    //    cheap insurance.
    private static final ConcurrentHashMap<String, VersionsEntry> versionsByUser = new ConcurrentHashMap<>();
    private static final AtomicLong availableVersionsGeneration = new AtomicLong();
    private static volatile DataCollectionListener availableVersionsListener;
    // Upper bound on the number of users holding a cached entry. Beyond this the whole map
    // is dropped (see getAvailableDatabaseVersions). Picked well above any realistic number
    // of concurrently-active users so steady-state workloads never hit it.
    private static final int MAX_CACHED_USERS = 128;

    /**
     * A cached value for one user: the computed versions map and the per-session databases
     * view it was built from. Both are only valid for the user that resolved them, so they
     * travel together and are dropped together on invalidation.
     */
    private static final class VersionsEntry
    {
        final Map<String, SortedSet<String>> versions;
        final DataCollection<DataCollection<?>> databases;

        VersionsEntry(Map<String, SortedSet<String>> versions, DataCollection<DataCollection<?>> databases)
        {
            this.versions = versions;
            this.databases = databases;
        }
    }

    private static void initAvailableVersionsListener()
    {
        if( availableVersionsListener == null )
        {
            synchronized( ProjectUtils.class )
            {
                if( availableVersionsListener == null )
                {
                    availableVersionsListener = new DataCollectionListenerSupport()
                    {
                        @Override
                        public void elementAdded(DataCollectionEvent e)
                        {
                            invalidateAvailableDatabaseVersions();
                        }

                        @Override
                        public void elementWillRemove(DataCollectionEvent e)
                        {
                            invalidateAvailableDatabaseVersions();
                        }
                    };
                    // The listener is attached to the current session's databases view (which
                    // exists because the databases collection has been resolved at least
                    // once by this point). It fires on add/remove, which invalidates every
                    // user's entry — a structural change to the databases set is visible to
                    // all users.
                    DataCollection<DataCollection<?>> databases = CollectionFactoryUtils.getDatabases();
                    databases.addDataCollectionListener( availableVersionsListener );
                }
            }
        }
    }

    /**
     * The identity of the current user for the purposes of the database version cache.
     * Returns the logged-in user's name, or {@code null} for the SYSTEM/bootstrap session.
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
     * databases are visible to every user) and from the databases-collection listener
     * (databases added/removed). Bumps the generation counter first so that any in-flight
     * computation is discarded rather than installed after the invalidation.
     */
    public static void invalidateAvailableDatabaseVersions()
    {
        availableVersionsGeneration.incrementAndGet();
        versionsByUser.clear();
    }

    public static Map<String, SortedSet<String>> getAvailableDatabaseVersions()
    {
        String user = currentUserIdentity();
        String key = userKey( user );
        VersionsEntry entry = versionsByUser.get( key );
        if( entry != null )
            return entry.versions;

        initAvailableVersionsListener();

        // Bound the map: if too many distinct users have cached a value, drop them all so a
        // long-running, many-user server cannot grow the map unboundedly. This is a global
        // drop, so it goes through the same race-safe generation bump as any invalidation.
        if( versionsByUser.size() >= MAX_CACHED_USERS )
        {
            availableVersionsGeneration.incrementAndGet();
            versionsByUser.clear();
        }

        long generation = availableVersionsGeneration.get();
        Map<String, SortedSet<String>> computed = buildAvailableDatabaseVersions();
        // Only publish if no global invalidation happened while we were computing; otherwise
        // a concurrent recomputation (started after the invalidation) will install the fresh
        // value, and we return our now-stale `computed` just for this caller.
        if( generation == availableVersionsGeneration.get() )
            versionsByUser.put( key, new VersionsEntry( computed, getDatabasesView() ) );
        return computed;
    }

    /**
     * The per-session "databases" view the current user sees. Resolved through
     * SecurityManager.getPermissions.
     */
    private static DataCollection<DataCollection<?>> getDatabasesView()
    {
        return CollectionFactoryUtils.getDatabases();
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
