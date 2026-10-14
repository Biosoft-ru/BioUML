package biouml.workbench.graphsearch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Union of QueryOptions and TargetOptions.
 * Is used in graph search properties pane.
 */
public class GraphSearchOptions
{
    //search type constants
    public static final String TYPE_NEIGHBOURS = "neighbours";
    public static final String TYPE_PATH = "path";
    public static final String TYPE_SET = "set";

    /**
     * Possible search types
     */
    static final String[] searchTypes = new String[] {TYPE_NEIGHBOURS, TYPE_PATH, TYPE_SET};

    private QueryOptions queryOptions;
    private TargetOptions targetOptions;
    private String searchType;
    private Species species = Species.getDefaultSpecies( null );

    /**
     * Cache for query engine names by collection path.
     * Query engines are registered via extension point and don't change
     * after initial registration, so we cache the expensive
     * lookForQueryEngines() result for the lifetime of the JVM.
     *
     * Note: If the query engine registry is reinitialized at runtime,
     * this cache would need to be cleared. Currently no such mechanism
     * exists in the codebase.
     *
     * Key: collection path string, Value: array of query engine names
     */
    private static final Map<String, String[]> queryEngineNamesCache = new ConcurrentHashMap<>();

    public GraphSearchOptions(DataCollection<DataCollection<?>> databaseCollection)
    {
        this.queryOptions = new QueryOptions();

        if( databaseCollection != null )
        {
            CollectionRecord[] records = databaseCollection.stream( ru.biosoft.access.core.DataCollection.class )
                    .filter( dc -> Boolean.parseBoolean( dc.getInfo().getProperty( DataCollectionUtils.GRAPH_SEARCH ) ) )
                    .map( ru.biosoft.access.core.DataCollection::getCompletePath )
                    .map( path -> {
                        // Use computeIfAbsent for atomic cache population
                        String[] engineNames = queryEngineNamesCache.computeIfAbsent(
                                path.toString(),
                                pathStr -> QueryEngineRegistry
                                        .lookForQueryEngines( new CollectionRecord( path, true ) )
                                        .keySet().stream().toArray( String[]::new ) );
                        // Clone to protect cached array from downstream mutation
                        return new CollectionRecord( path, false, engineNames.clone() );
                    })
                    .toArray( CollectionRecord[]::new );
            this.targetOptions = new TargetOptions(records);
        }
        this.searchType = TYPE_NEIGHBOURS;
    }

    public QueryOptions getQueryOptions()
    {
        return queryOptions;
    }

    public void setQueryOptions(QueryOptions queryOptions)
    {
        this.queryOptions = queryOptions;
    }

    public TargetOptions getTargetOptions()
    {
        return targetOptions;
    }

    public void setTargetOptions(TargetOptions targetOptions)
    {
        this.targetOptions = targetOptions;
    }

    public String getSearchType()
    {
        return searchType;
    }

    public void setSearchType(String searchType)
    {
        this.searchType = searchType;
    }

    @PropertyName("Species")
    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        this.species = species;
    }
}
