package biouml.workbench.graphsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.util.ExtensionRegistrySupport;

/**
 * Factory class for Query Engines
 */
public class QueryEngineRegistry extends ExtensionRegistrySupport<QueryEngineRegistry.QueryEngineInfo>
{
    public static final String NAME_ATTR = "name";
    public static final String CLASS_ATTR = "class";
    public static final String COLLECTION_ELEMENT = "collection";
    public static final String COLLECTION_PATH_ATTR = "path";

    public static class QueryEngineInfo
    {
        protected String name;
        protected QueryEngine queryEngine;
        protected List<String> collections;

        public QueryEngineInfo(String name, QueryEngine queryEngine)
        {
            this.name = name;
            this.queryEngine = queryEngine;
            this.collections = new ArrayList<>();
        }

        public void addCollectionPath(String path)
        {
            collections.add(path);
        }

        public String getName()
        {
            return name;
        }

        public QueryEngine getQueryEngine()
        {
            return queryEngine;
        }

        public List<String> getCollections()
        {
            return collections;
        }
    }
    
    private static final QueryEngineRegistry instance = new QueryEngineRegistry();
    
    private QueryEngineRegistry()
    {
        super("biouml.workbench.queryEngine", NAME_ATTR);
    }

    @Override
    protected QueryEngineInfo loadElement(IConfigurationElement element, String name) throws Exception
    {
        QueryEngine queryEngine = getClassAttribute(element, CLASS_ATTR, QueryEngine.class).newInstance();
        QueryEngineInfo queryEngineInfo = new QueryEngineInfo(name, queryEngine);
    
        IConfigurationElement[] collections = element.getChildren(COLLECTION_ELEMENT);
        if( collections != null )
        {
            for( IConfigurationElement collectionElement : collections )
            {
                queryEngineInfo.addCollectionPath(getStringAttribute(collectionElement, COLLECTION_PATH_ATTR));
            }
        }
        return queryEngineInfo;
    }

    /**
     * Get the best query engine for selected options
     * TODO: add support for different search types
     */
    public static QueryEngine lookForQueryEngine(TargetOptions target, String queryType)
    {
        String bestName = null;
        int priority = 0;
        for( QueryEngineInfo info: instance )
        {
            try
            {
                QueryEngine queryEngine = info.getQueryEngine();
                int p = 0;
                if( GraphSearchOptions.TYPE_NEIGHBOURS.equals(queryType) )
                {
                    p = queryEngine.canSearchLinked(target);
                }
                else if( GraphSearchOptions.TYPE_PATH.equals(queryType) )
                {
                    p = queryEngine.canSearchPath(target);
                }
                else if( GraphSearchOptions.TYPE_SET.equals(queryType) )
                {
                    p = queryEngine.canSearchSet(target);
                }
                if( p > priority )
                {
                    priority = p;
                    bestName = info.getName();
                }
            }
            catch( Exception e )
            {
            }
        }

        if( bestName != null )
        {
            return instance.getExtension(bestName).getQueryEngine();
        }
        return null;
    }

    /**
     * Get all query engines specified for CollectionRecord
     */
    public static @Nonnull List<QueryEngine> getQueryEngines(CollectionRecord cr, String queryType)
    {
        String[] names = cr.getQueryEngineNamesStrings();
        if( names == null || names.length == 0 )
            return Collections.emptyList();
        Map<String, QueryEngine> enginesMap = lookForQueryEngines(cr, queryType);
        if(enginesMap.size() == 0)
            return Collections.emptyList();
        return StreamEx.of(names).map( enginesMap::get ).nonNull().toList();
    }
    
    /**
     * Get query engines with highest priority, available for collection
     */
    public static Map<String, QueryEngine> lookForQueryEngines(CollectionRecord collection)
    {
        return lookForQueryEngines(collection, "");
    }
    
    /**
     * Get query engines with highest priority, available for collection and search type specified
     */
    public static Map<String, QueryEngine> lookForQueryEngines(CollectionRecord collection, String queryType)
    {
        class EngineWithPriority implements Comparable<EngineWithPriority>
        {
            String name;
            QueryEngine engine;
            int priority;
            
            public EngineWithPriority(String queryType, TargetOptions target, QueryEngine engine)
            {
                this.name = engine.getName( target );
                this.engine = engine;
                this.priority = calcEnginePriority( queryType, target, engine );
            }

            int calcEnginePriority(String queryType, TargetOptions target, QueryEngine engine)
            {
                switch( queryType )
                {
                    case GraphSearchOptions.TYPE_NEIGHBOURS:
                        return engine.canSearchLinked( target );
                    case GraphSearchOptions.TYPE_PATH:
                        return engine.canSearchPath( target );
                    case GraphSearchOptions.TYPE_SET:
                        return engine.canSearchSet( target );
                    default:
                        return IntStream
                                .of( engine.canSearchLinked( target ), engine.canSearchPath( target ), engine.canSearchSet( target ) )
                                .max().getAsInt();
                }
            }

            @Override
            public int compareTo(EngineWithPriority o)
            {
                return priority > o.priority ? -1 : priority < o.priority ? 1 : name.compareTo(o.name);
            }
        }
        
        TargetOptions target = new TargetOptions(collection);
        return instance.stream()
                .map( QueryEngineInfo::getQueryEngine )
                .map( queryEngine -> new EngineWithPriority( queryType, target, queryEngine ) )
                .filter( ewp -> ewp.priority > 0 )
                .sorted()
                .mapToEntry( e -> e.name, e -> e.engine )
                .toCustomMap( LinkedHashMap::new );
    }
}
