package biouml.plugins.server;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.xml.XmlDiagramType;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Base;
import biouml.workbench.graphsearch.GraphSearchOptions;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryEngineRegistry;
import biouml.workbench.graphsearch.SearchElement;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.server.Connection;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.Response;
import ru.biosoft.server.SynchronizedServiceSupport;
import ru.biosoft.util.Util;

/**
 * Network supporting for graph search
 */
public class GraphSearchService extends SynchronizedServiceSupport
{
    protected Response connection;
    protected Map arguments;

    @Override
    protected boolean processRequest(int command) throws Exception
    {
        connection = getSessionConnection();
        arguments = getSessionArguments();

        switch( command )
        {
            case GraphSearchProtocol.GRAPH_SEARCH_START:
                sendStartSearch();
                break;

            case GraphSearchProtocol.GRAPH_SEARCH_STATUS:
                sendSearchStatus();
                break;

            case GraphSearchProtocol.GRAPH_SEARCH_RESULT:
                sendSearchResult();
                break;

            case GraphSearchProtocol.GRAPH_SEARCH_PRIORITY:
                sendSearchPriority();
                break;

            case GraphSearchProtocol.GET_DIAGRAM_TYPES:
                sendDiagramTypes();
                break;

            default:
                return false;
        }
        return true;
    }

    //////////////////////////////////////////////
    // Protocol implementation functions
    //

    protected void sendStartSearch() throws Exception
    {
        Object searchOptionsObj = arguments.get(GraphSearchProtocol.KEY_OPTIONS);
        if( searchOptionsObj == null )
        {
            connection.error("didn't send search options");
            return;
        }

        JSONArray jsonOptions = new JSONArray((String)searchOptionsObj);
        GraphSearchOptions searchOptions = new GraphSearchOptions(CollectionFactoryUtils.getDatabases());
        JSONUtils.correctBeanOptions(searchOptions, jsonOptions);

        String species = searchOptions.getSpecies().getLatinName();

        Object elementsObj = arguments.get(GraphSearchProtocol.KEY_ELEMENTS);
        if( elementsObj == null )
        {
            connection.error("didn't send element list");
            return;
        }
        List<SearchElement> startElements = StreamEx.split(elementsObj.toString(), ';')
            .remove( String::isEmpty )
            .distinct()
                .map( CollectionFactory::getDataElement )
            .select( Base.class )
            .map( base -> {
                SearchElement se = new SearchElement( base );
                se.setUse( true );
                se.setRelationType( species );
                return se;
            })
            .toList();

        String searchType = searchOptions.getSearchType();

        TargetOptions targetOptions = searchOptions.getTargetOptions();
        if( !targetOptions.collections().anyMatch( collection -> !QueryEngineRegistry.getQueryEngines( collection, searchType ).isEmpty() ) )
        {
            connection.error("There is no available search engine for this options");
            return;
        }

        SearchThread searchProcess = new SearchThread(searchOptions, startElements.toArray(new SearchElement[startElements.size()]));
        searchProcess.start();

        long id = 0;
        synchronized( currentProcesses )
        {
            id = Util.getUniqueId();
            currentProcesses.put(id, searchProcess);
        }

        connection.send(String.valueOf(id).getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
    }
    protected void sendSearchStatus() throws Exception
    {
        Object processIdObj = arguments.get(GraphSearchProtocol.KEY_ID);
        if( processIdObj == null )
        {
            connection.error("didn't send process id options");
            return;
        }

        long processId;
        try
        {
            processId = Long.parseLong(processIdObj.toString());
        }
        catch( NumberFormatException e )
        {
            connection.error("ID parameter should be integer");
            return;
        }

        SearchThread searchThread = null;
        synchronized( currentProcesses )
        {
            searchThread = currentProcesses.get(processId);
        }
        if( searchThread == null )
        {
            connection.error("can't find process on the server");
            return;
        }

        byte status = (byte)searchThread.getJobControl().getStatus();
        byte percent = (byte)searchThread.getJobControl().getPreparedness();
        connection.send( ( String.valueOf(status) + ":" + String.valueOf(percent) ).getBytes("UTF-16BE"),
                Connection.FORMAT_SIMPLE);
    }

    protected void sendSearchResult() throws Exception
    {
        Object processIdObj = arguments.get(GraphSearchProtocol.KEY_ID);
        if( processIdObj == null )
        {
            connection.error("didn't send process id options");
            return;
        }

        long processId;
        try
        {
            processId = Long.parseLong(processIdObj.toString());
        }
        catch( NumberFormatException e )
        {
            connection.error("ID parameter should be integer");
            return;
        }

        SearchThread searchThread = null;
        synchronized( currentProcesses )
        {
            searchThread = currentProcesses.get(processId);
            currentProcesses.remove(processId);
        }
        if( searchThread == null )
        {
            connection.error("can't find process on the server");
            return;
        }

        SearchElement[] results = searchThread.getResults();
        if( results == null )
        {
            connection.error("Nothing found");
        }
        else
        {
            StringBuilder resultString = new StringBuilder();
            for( SearchElement se : results )
            {
                resultString.append(se.getElementPath());
                resultString.append(';');
                resultString.append(se.getLinkedDirection());
                resultString.append(';');
                resultString.append(se.getLinkedFromPath());
                resultString.append(';');
                resultString.append(se.getLinkedLength());
                resultString.append(';');
                resultString.append(se.getRelationType());
                resultString.append(';');
                resultString.append(se.getBaseTitle());
                resultString.append(';');
                resultString.append(se.getBase().getClass().getCanonicalName());
                resultString.append('\n');
            }
            connection.send(resultString.toString().getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    protected void sendSearchPriority() throws Exception
    {
        Object targetOptionsObj = arguments.get(GraphSearchProtocol.KEY_OPTIONS);
        if( targetOptionsObj == null )
        {
            connection.error("didn't send search options");
            return;
        }

        JSONArray jsonOptions = new JSONArray((String)targetOptionsObj);
        TargetOptions targetOptions = new GraphSearchOptions(CollectionFactoryUtils.getDatabases()).getTargetOptions();
        JSONUtils.correctBeanOptions(targetOptions, jsonOptions);

        Object searchType = arguments.get(GraphSearchProtocol.KEY_SEARCHTYPE);
        if( searchType == null )
        {
            connection.error("didn't send search options");
            return;
        }

        boolean foundEngine = false;
        int result = 0;
        for( CollectionRecord collection : targetOptions.collections() )
        {
            List<QueryEngine> engines = QueryEngineRegistry.getQueryEngines(collection, searchType.toString());
            TargetOptions options = new TargetOptions(collection);
            foundEngine = true;
            for(QueryEngine queryEngine: engines)
            {
                int res = 0;
                if( GraphSearchOptions.TYPE_NEIGHBOURS.equals(searchType) )
                {
                    res = queryEngine.canSearchLinked(options);
                }
                else if( GraphSearchOptions.TYPE_PATH.equals(searchType) )
                {
                    res = queryEngine.canSearchPath(options);
                }
                else if( GraphSearchOptions.TYPE_SET.equals(searchType) )
                {
                    res = queryEngine.canSearchSet(options);
                }
                result = Math.max(result, res);
            }
        }
        if( !foundEngine )
        {
            connection.error("There is no available search engine for this options");
            return;
        }
        connection.send(new byte[] {(byte)result}, Connection.FORMAT_SIMPLE);
    }

    protected void sendDiagramTypes() throws Exception
    {
        JSONArray diagramTypes = new JSONArray();

        Class[] dbDiagramClasses = new Class[]{PathwayDiagramType.class, PathwaySimulationDiagramType.class};
        for( Class dbDiagramClasse : dbDiagramClasses )
        {
            BeanInfo info = Introspector.getBeanInfo(dbDiagramClasse);
            JSONObject type = new JSONObject();
            type.put("name", info.getBeanDescriptor().getDisplayName());
            type.put("title", info.getBeanDescriptor().getDisplayName());
            type.put("description", info.getBeanDescriptor().getShortDescription());
            diagramTypes.put(type);
        }
        String[] xmlTypes = new String[]{"sbgn_simulation.xml"};
        DataCollection<XmlDiagramType> xmlTypesCollection = XmlDiagramType.getTypesCollection();
        for( String xmlType: xmlTypes )
        {
            XmlDiagramType xdt = xmlTypesCollection.get(xmlType);
            if(xdt != null)
            {
                JSONObject type = new JSONObject();
                type.put("name", xdt.getName());
                type.put("title", xdt.getTitle());
                type.put("description", "XML diagram type (" + xdt.getDescription() + ")");
                diagramTypes.put(type);
            }
        }
        connection.send(diagramTypes.toString().getBytes("UTF-16BE"), Connection.FORMAT_GZIP);
    }

    //processes support

    private static final Map<Long, SearchThread> currentProcesses = new HashMap<>();

    public static class SearchThread extends SessionThread
    {
        protected GraphSearchOptions searchOptions;
        protected SearchElement[] inputElements;
        protected SearchElement[] result = null;
        protected FunctionJobControl jobControl;

        public SearchThread(GraphSearchOptions searchOptions, SearchElement[] inputElements)
        {
            this.searchOptions = searchOptions;
            this.inputElements = inputElements;
            jobControl = new FunctionJobControl(null);
        }

        /**
         * Get process job control
         */
        public FunctionJobControl getJobControl()
        {
            return jobControl;
        }

        /**
         * Get process results
         */
        public SearchElement[] getResults()
        {
            return result;
        }

        @Override
        public void run()
        {
            try
            {
                jobControl.functionStarted();
                for( CollectionRecord collection : searchOptions.getTargetOptions().collections() )
                {
                    TargetOptions targetOptions = new TargetOptions(collection);
                    for( QueryEngine queryEngine : QueryEngineRegistry.getQueryEngines(collection, searchOptions.getSearchType()) )
                    {
                        SearchElement[] currentResult = null;
                        if( GraphSearchOptions.TYPE_NEIGHBOURS.equals(searchOptions.getSearchType()) )
                        {
                            currentResult = queryEngine
                                    .searchLinked(inputElements, searchOptions.getQueryOptions(), targetOptions, jobControl);
                        }
                        else if( GraphSearchOptions.TYPE_PATH.equals(searchOptions.getSearchType()) )
                        {
                            currentResult = queryEngine.searchPath(inputElements, searchOptions.getQueryOptions(), targetOptions, jobControl);
                        }
                        else if( GraphSearchOptions.TYPE_SET.equals(searchOptions.getSearchType()) )
                        {
                            currentResult = queryEngine.searchSet(inputElements, searchOptions.getQueryOptions(), targetOptions, jobControl);
                        }
                        if( currentResult != null )
                        {
                            if( result == null )
                                result = currentResult;
                            else
                                result = StreamEx.of( result ).append( currentResult ).toArray( SearchElement[]::new );
                        }
                    }
                }

                jobControl.functionFinished();
            }
            catch( Exception e )
            {
                jobControl.functionTerminatedByError(e);
                log.log(Level.SEVERE, "Search error", e);
            }
        }
    }
}
