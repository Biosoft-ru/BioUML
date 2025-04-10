package ru.biosoft.journal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.CheckForNull;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
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
     * @return map databaseMap -> set of available versions
     */
    public static Map<String, SortedSet<String>> getAvailableDatabaseVersions()
    {
        return StreamEx.of( CollectionFactoryUtils.getDatabases().stream() )
                .mapToEntry( ProjectUtils::getDatabaseName, ProjectUtils::getVersion )
                .nonNullKeys().removeValues( String::isEmpty ).groupingTo( TreeMap::new, () -> {
                    return new TreeSet<>( new DatabaseVersionComparator() );
                } );
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
