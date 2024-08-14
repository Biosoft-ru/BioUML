package biouml.plugins.server.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;

import biouml.plugins.server.GraphSearchProtocol;
import biouml.standard.type.Base;
import biouml.workbench.graphsearch.GraphSearchOptions;
import biouml.workbench.graphsearch.QueryEngine;
import biouml.workbench.graphsearch.QueryEngineSupport;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.Request;
import ru.biosoft.util.TextUtil;

/**
 * Remote {@link QueryEngine} implementation
 */
public class ClientQueryEngine extends QueryEngineSupport
{
    protected static final Logger log = Logger.getLogger(ClientQueryEngine.class.getName());

    @Override
    public int canSearchLinked(TargetOptions dbOptions)
    {
        DataElementPathSet collections = dbOptions.getUsedCollectionPaths();
        if( collections.size() == 1 )
        {
            ClientModule module = collections.first().optDataElement(ClientModule.class);
            if( module != null )
            {
                int result = 0;
                Request request = null;
                try
                {
                    request = getRequest(module);
                    result = getSearchPriority(request, GraphSearchOptions.TYPE_NEIGHBOURS, dbOptions);
                }
                catch( Exception ex )
                {
                    log.log(Level.SEVERE, ex.getMessage(), ExceptionRegistry.translateException( ex ));
                }
                finally
                {
                    if(request != null)
                        request.close();
                }
                return result;
            }
        }
        return 0;
    }

    @Override
    public int canSearchPath(TargetOptions dbOptions)
    {
        DataElementPathSet collections = dbOptions.getUsedCollectionPaths();
        if( collections.size() == 1 )
        {
            ClientModule module = collections.first().optDataElement(ClientModule.class);
            if( module != null )
            {
                int result = 0;
                Request request = null;
                try
                {
                    request = getRequest(module);
                    result = getSearchPriority(request, GraphSearchOptions.TYPE_PATH, dbOptions);
                }
                catch(Exception ex)
                {
                    log.log(Level.SEVERE,  ExceptionRegistry.log(ex) );
                }
                finally
                {
                    if(request != null)
                        request.close();
                }
                return result;
            }
        }
        return 0;
    }

    @Override
    public int canSearchSet(TargetOptions dbOptions)
    {
        DataElementPathSet collections = dbOptions.getUsedCollectionPaths();
        if( collections.size() == 1 )
        {
            ClientModule module = collections.first().optDataElement(ClientModule.class);
            if( module != null )
            {
                int result = 0;
                Request request = null;
                try
                {
                    request = getRequest(module);
                    result = getSearchPriority(request, GraphSearchOptions.TYPE_SET, dbOptions);
                }
                catch( Exception ex )
                {
                    log.log(Level.SEVERE,  ExceptionRegistry.log( ex ) );
                }
                finally
                {
                    if(request != null)
                        request.close();
                }
                return result;
            }
        }
        return 0;
    }

    @Override
    public SearchElement[] searchLinked(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions,
            JobControl jobControl) throws Exception
    {
        ClientModule module = dbOptions.getUsedCollectionPaths().first().getDataElement(ClientModule.class);
        List<SearchElement> results = null;
        Request request = null;
        try
        {
            request = getRequest(module);
            GraphSearchOptions options = new GraphSearchOptions(null);
            options.setQueryOptions(queryOptions);
            options.setTargetOptions(dbOptions);
            options.setSearchType(GraphSearchOptions.TYPE_NEIGHBOURS);
            results = processSearch(request, startNodes, options, jobControl);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Search error", t);
        }
        finally
        {
            if(request != null)
                request.close();
        }
        if( results != null )
        {
            return results.toArray(new SearchElement[results.size()]);
        }
        return null;
    }

    @Override
    public SearchElement[] searchPath(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl)
            throws Exception
    {
        ClientModule module = dbOptions.getUsedCollectionPaths().first().getDataElement(ClientModule.class);
        List<SearchElement> results = null;
        Request request = null;
        try
        {
            request = getRequest(module);
            GraphSearchOptions options = new GraphSearchOptions(null);
            options.setQueryOptions(queryOptions);
            options.setTargetOptions(dbOptions);
            options.setSearchType(GraphSearchOptions.TYPE_PATH);
            results = processSearch(request, startNodes, options, jobControl);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Search error", t);
        }
        finally
        {
            if(request != null)
                request.close();
        }
        if( results != null )
        {
            return results.toArray(new SearchElement[results.size()]);
        }
        return null;
    }

    @Override
    public SearchElement[] searchSet(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl)
            throws Exception
    {
        ClientModule module = dbOptions.getUsedCollectionPaths().first().getDataElement(ClientModule.class);
        List<SearchElement> results = null;
        Request request = null;
        try
        {
            request = getRequest(module);
            GraphSearchOptions options = new GraphSearchOptions(null);
            options.setQueryOptions(queryOptions);
            options.setTargetOptions(dbOptions);
            options.setSearchType(GraphSearchOptions.TYPE_SET);
            results = processSearch(request, startNodes, options, jobControl);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Search error", t);
        }
        finally
        {
            if(request != null)
                request.close();
        }
        if( results != null )
        {
            return results.toArray(new SearchElement[results.size()]);
        }
        return null;
    }

    /**
     * Process priority request
     */
    protected int getSearchPriority(Request request, String searchType, TargetOptions options) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(GraphSearchProtocol.KEY_SEARCHTYPE, searchType);
        map.put(GraphSearchProtocol.KEY_OPTIONS, JSONUtils.getModelAsJSON(ComponentFactory.getModel(options, Policy.DEFAULT, true)).toString());

        byte[] bytes = request(request, GraphSearchProtocol.GRAPH_SEARCH_PRIORITY, map, true);
        if( bytes != null && bytes.length > 0 )
        {
            return bytes[0];
        }
        return 0;
    }

    /**
     * Process search
     */
    protected List<SearchElement> processSearch(Request request, SearchElement[] startNodes, GraphSearchOptions options,
            JobControl jobControl) throws Exception
    {
        String elements = StreamEx.of(startNodes).filter( SearchElement::isUse ).map( SearchElement::getPath ).joining( ";" );

        if( jobControl != null )
        {
            jobControl.setPreparedness(0);
        }

        Map<String, String> map = new HashMap<>();
        map.put(GraphSearchProtocol.KEY_OPTIONS, JSONUtils.getModelAsJSON(ComponentFactory.getModel(options, Policy.DEFAULT, true)).toString());
        map.put(GraphSearchProtocol.KEY_ELEMENTS, elements);
        byte[] bytes = request(request, GraphSearchProtocol.GRAPH_SEARCH_START, map, true);
        if( bytes != null )
        {
            int processId = Integer.parseInt(new String(bytes, "UTF-16BE"));
            int status = -1;
            map = new HashMap<>();
            map.put(GraphSearchProtocol.KEY_ID, String.valueOf(processId));
            do
            {
                if( status > -1 )
                {
                    Thread.sleep(1000);
                }
                byte[] statusBytes = request(request, GraphSearchProtocol.GRAPH_SEARCH_STATUS, map, true);
                if( statusBytes == null )
                {
                    break;
                }
                String[] params = TextUtil.split( new String(statusBytes, "UTF-16BE"), ':');
                if( params.length != 2 )
                {
                    break;
                }
                status = Integer.parseInt(params[0]);
                if( jobControl != null )
                {
                    jobControl.setPreparedness(Integer.parseInt(params[1]));
                }
            }
            while( status == JobControl.RUNNING );

            if( status == JobControl.COMPLETED )
            {
                byte[] resultBytes = request(request, GraphSearchProtocol.GRAPH_SEARCH_RESULT, map, true);
                if( resultBytes != null )
                {
                    if( jobControl != null )
                    {
                        jobControl.setPreparedness(100);
                    }
                    return getSearchElements(resultBytes);
                }
            }
            else
            {
                log.log(Level.SEVERE, "Can't process search on the server");
            }
        }
        return null;
    }
    protected List<SearchElement> getSearchElements(byte[] bytes) throws Exception
    {
        List<SearchElement> results = new ArrayList<>();
        String result = new String(bytes, "UTF-16BE");

        StringTokenizer tokens = new StringTokenizer(result, "\n");
        while( tokens.hasMoreTokens() )
        {
            String line = tokens.nextToken();
            String params[] = TextUtil.split( line, ';' );
            if( params.length >= 5 )
            {
                DataElement de = CollectionFactory.getDataElement(params[0]);
                if( de != null )
                {
                    SearchElement se = new SearchElement((Base)de);
                    se.setLinkedDirection(Integer.parseInt(params[1]));
                    se.setLinkedFromPath(params[2]);
                    se.setLinkedLength(Float.parseFloat(params[3]));
                    se.setRelationType(params[4]);
                    results.add(se);
                }
            }
        }
        return results;
    }

    /**
     * Get server request object
     */
    protected static Request getRequest(ClientModule module) throws Exception
    {
        return new Request(module.getClientConnection(), log);
    }

    /**
     * Process request to server
     */
    protected byte[] request(Request request, int command, Map<String, String> arguments, boolean readAnswer)
            throws BiosoftNetworkException
    {
        if( request != null )
        {
            return request.request(GraphSearchProtocol.GRAPH_SEARH_SERVICE, command, arguments, readAnswer);
        }
        return null;
    }
}
