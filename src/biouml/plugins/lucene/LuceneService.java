package biouml.plugins.lucene;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import one.util.streamex.StreamEx;

import org.apache.lucene.queryparser.classic.ParseException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.server.Connection;
import ru.biosoft.server.ServiceSupport;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.FunctionJobControl;
import com.developmentontheedge.server.JobControlServerListener;

/**
 * Provides functionality of the LuceneService.
 * 
 * @see Connection
 */
public class LuceneService extends ServiceSupport
{
    protected JobControlServerListener listener = null;

    // //////////////////////////////////////
    // Functions for data access
    //

    protected LuceneQuerySystem getLuceneFacade(ServiceRequest request) throws IOException
    {
        LuceneQuerySystem luceneFacade;
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            if( dc.getInfo().getQuerySystem() instanceof LuceneQuerySystem )
            {
                luceneFacade = (LuceneQuerySystem)dc.getInfo().getQuerySystem();
                if( luceneFacade.testHaveLuceneDir() )
                {
                    if( luceneFacade.testHaveIndex() )
                        return luceneFacade;
                    
                }
            }
            request.error("Search is not available for "+dc.getCompletePath());
        }
        return null;
    }
    
    @Override
    protected boolean processRequest(ServiceRequest request, int command) throws Exception
    {
        switch( command )
        {
            case LuceneProtocol.DB_TEST_HAVE_LUCENE_DATABASE:
                sendTestModule(request);
                break;
            case LuceneProtocol.DB_LIST_WITH_BUILD_INDEX:
                sendBuildDCNames(request);
                break;
            case LuceneProtocol.DB_GET_INDEXES:
                sendIndexes(request);
                break;
            case LuceneProtocol.DB_SEND_INDEXES:
                writeIndexes(request);
                break;
            case LuceneProtocol.DB_TEST_HAVE_INDEX:
                sendTestHaveIndex(request);
                break;
            case LuceneProtocol.DB_CREATE_INDEX:
                createIndex(request);
                break;
            case LuceneProtocol.DB_DELETE_INDEX:
                deleteIndex(request);
                break;
            case LuceneProtocol.DB_ADD_TO_INDEX:
                addToIndex(request);
                break;
            case LuceneProtocol.DB_DELETE_FROM_INDEX:
                deleteFromIndex(request);
                break;
            case LuceneProtocol.DB_LUCENE_SEARCH:
                sendSearchResults(request);
                break;
            case LuceneProtocol.DB_LUCENE_SEARCH_RECURSIVE:
                sendRecursiveSearchResults(request);
                break;
            case LuceneProtocol.DB_LIST_WITH_PROPERTY:
                sendDCNamesWithProperty(request);
                break;

            default:
                return false;
        }
        return true;
    }

    // ////////////////////////////////////////////
    // Protocol implementation functions
    //

    /**
     * Sent test result - have this data collection lucene module or not
     * @param request
     * 
     * @throws IOException
     * @throws IOException
     */
    protected void sendTestModule(ServiceRequest request) throws IOException
    {
        if( getLuceneFacade(request) != null )
        {
            request.send("");
        }
    }

    protected void sendBuildDCNames(ServiceRequest request) throws IOException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            Iterator<String> i = luceneFacade.getDCWithBuildIndex().iterator();

            StringBuffer s = new StringBuffer();
            while( i.hasNext() )
            {
                s.append(i.next());
                s.append('\n');
            }

            request.send(s.toString());
        }
    }

    protected void sendIndexes(ServiceRequest request) throws IOException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            Object relativeName = request.get(LuceneProtocol.KEY_NAME);
            if( relativeName == null )
            {
                request.error("didn't send data collection relative name.");
                return;
            }
            String indexes = luceneFacade.getIndexes(relativeName.toString());
            request.send( indexes.trim());
        }
    }

    protected void writeIndexes(ServiceRequest request) throws Exception
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            Object relativeName = request.get(LuceneProtocol.KEY_NAME);
            if( relativeName == null )
            {
                request.error("didn't send data collection relative name.");
                return;
            }
            Object indexes = request.get(LuceneProtocol.KEY_INDEXES);
            if( indexes == null )
                luceneFacade.setIndexes(relativeName.toString(), null);
            else
                luceneFacade.setIndexes(relativeName.toString(), indexes.toString());
            request.send("");
        }
    }

    protected void sendTestHaveIndex(ServiceRequest request) throws IOException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            if( luceneFacade.testHaveIndex() )
            {
                request.send("");
                return;
            }
            request.error("there is no index.");
        }
    }

    protected void createIndex(ServiceRequest request) throws IOException, ParseException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            FunctionJobControl jobControl = new FunctionJobControl(null);
            listener = new JobControlServerListener(jobControl, request.getDataCollection().getCompletePath() + "/");
            jobControl.addListener(listener);
            luceneFacade.createIndex(null, jobControl);
            jobControl.removeListener(listener);
            // System.out.println ( "Server: create index" );
            request.send("");
        }
    }

    protected void deleteIndex(ServiceRequest request) throws IOException, ParseException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            luceneFacade.deleteIndex(null);
            request.send("");
        }
    }

    protected void addToIndex(ServiceRequest request) throws IOException, ParseException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            Object relativeName = request.get(LuceneProtocol.KEY_NAME);
            if( relativeName == null )
            {
                request.error("didn't send data collection relative name.");
                return;
            }
            FunctionJobControl jobControl = new FunctionJobControl(null);
            listener = new JobControlServerListener(jobControl, request.getDataCollection().getCompletePath() + "/");
            jobControl.addListener(listener);
            luceneFacade.addToIndex(relativeName.toString(), true, null, jobControl);
            request.send("");
        }
    }

    protected void deleteFromIndex(ServiceRequest request) throws IOException, ParseException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            String relativeName = request.get(LuceneProtocol.KEY_NAME);
            if( relativeName == null )
            {
                request.error("didn't send data collection relative name.");
                return;
            }
            luceneFacade.deleteFromIndex(relativeName, null);
            request.send("");
        }
    }

    /**
     * Sends search result for the data collection.
     * 
     * Using from and to parameters it is possible to specify range of passed
     * DynamicPropertySets used to represent hit and assotiated document.
     * 
     * @throws Exception
     */
    protected void sendSearchResults(ServiceRequest request) throws Exception
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            // relativeName
            String relativeName = request.get(LuceneProtocol.KEY_NAME);
            if( relativeName == null )
            {
                request.error("didn't send data collection relative name.");
                return;
            }

            // queryString
            String queryString = request.get(LuceneProtocol.KEY_QUERY);
            if( queryString == null )
            {
                request.error("didn't send query string.");
                return;
            }

            // fields
            String fieldsObj = request.get(LuceneProtocol.KEY_FIELDS);
            String[] fields = null;
            if( fieldsObj != null )
            {
                ByteArrayInputStream bais = new ByteArrayInputStream(fieldsObj.getBytes());
                ObjectInputStream ois = new ObjectInputStream(bais);
                List<Object> f = new ArrayList<>();
                try
                {
                    f.add(ois.readObject());
                }
                catch( Throwable t )
                {
                }
                if( f.size() > 0 )
                {
                    fields = StreamEx.of(f).map( Object::toString ).toArray( String[]::new );
                }
            }

            // formatter
            Formatter formatter = null;
            String formatterPrefix = request.get(LuceneProtocol.KEY_FORMATTER_PREFIX);
            if( formatterPrefix != null )
            {
                String formatterPostfix = request.get(LuceneProtocol.KEY_FORMATTER_POSTFIX);
                if( formatterPostfix != null )
                    formatter = new Formatter(formatterPrefix, formatterPostfix);
            }
            boolean alternativeView = Boolean.valueOf(request.get(LuceneProtocol.KEY_VIEW));
            int from = request.getInt(LuceneProtocol.KEY_FROM, 0);
            int to = request.getInt(LuceneProtocol.KEY_TO, LuceneQuerySystem.MAX_DEFAULT_SEARCH_RESULTS_COUNT);

            // search
            DynamicPropertySet[] dps = null;
            try
            {
                dps = luceneFacade.search(relativeName, queryString, fields, formatter, alternativeView, from, to);
            }
            catch( ParseException pe )
            {
                request.send( ( "" + pe.getMessage() ));
                return;
            }
            if( dps != null && dps.length > 0 )
            {
                request.getSessionConnection().sendDPSArray(dps);
                return;
            }
            request.send("null");
        }
    }

    /**
     * Sends recursive search result for the data collection.
     * 
     * @throws Exception
     */
    protected void sendRecursiveSearchResults(ServiceRequest request) throws Exception
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade(request);
        if( luceneFacade != null )
        {
            // relativeName
            Object relativeName = request.get(LuceneProtocol.KEY_NAME);
            if( relativeName != null )
            {
                if( relativeName.equals("null") )
                {
                    relativeName = null;
                }
                else
                {
                    relativeName = relativeName.toString();
                }
            }

            // queryString
            Object queryString = request.get(LuceneProtocol.KEY_QUERY);
            if( queryString == null )
            {
                request.error("didn't send query string.");
                return;
            }

            // formatter
            Formatter formatter = null;
            Object formatterPrefix = request.get(LuceneProtocol.KEY_FORMATTER_PREFIX);
            if( formatterPrefix != null )
            {
                Object formatterPostfix = request.get(LuceneProtocol.KEY_FORMATTER_POSTFIX);
                if( formatterPostfix != null )
                    formatter = new Formatter(formatterPrefix.toString(), formatterPostfix.toString());
            }

            // from
            Object fromObj = request.get(LuceneProtocol.KEY_FROM);
            int from = 0;
            if( fromObj != null )
                try
                {
                    from = Integer.parseInt(fromObj.toString());
                }
                catch( NumberFormatException e )
                {
                }

            // to
            Object toObj = request.get(LuceneProtocol.KEY_TO);
            int to = LuceneQuerySystem.MAX_DEFAULT_SEARCH_RESULTS_COUNT;
            if( toObj != null )
                try
                {
                    to = Integer.parseInt(toObj.toString());
                }
                catch( NumberFormatException e )
                {
                }

            // search
            DynamicPropertySet[] dps = null;
            try
            {
                dps = luceneFacade.searchRecursive((String)relativeName, queryString.toString(), formatter, from, to);
            }
            catch( ParseException pe )
            {
                request.send("" + pe.getMessage());
                return;
            }
            if( dps != null && dps.length > 0 )
            {
                request.getSessionConnection().sendDPSArray(dps);
                return;
            }
            request.send("null");
        }
    }

    /**
     * Sends list of all data collection NAMES thich contain property field.
     * 
     * @throws Exception
     */
    protected void sendDCNamesWithProperty(ServiceRequest request) throws Exception
    {
        DataCollection<?> dc = request.getDataCollection();
        if( dc != null )
        {
            Object propertyObj = request.get(LuceneProtocol.KEY_PROPERTY);
            String property = null;
            if( propertyObj != null )
                property = propertyObj.toString();

            StringBuffer s = new StringBuffer();
            for(String name: LuceneUtils.getCollectionsNames(dc, property))
            {
                s.append(name);
                s.append('\n');
            }

            request.send(s.toString());
        }
    }

}
