package biouml.plugins.expression.web;

import java.util.Set;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.plugins.expression.ExpressionFilter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TextUtil;

public class ExpressionFilterProvider extends WebJSONProviderSupport
{
    /**
     * Sends to the client the list of filters for given diagram
     */
    private static void sendDiagramFilterList(Diagram diagram, JSONResponse response) throws Exception
    {
        JsonArray result = new JsonArray();

        for( DiagramFilter filter : diagram.getFilterList() )
        {
            if( ! ( filter instanceof ExpressionFilter ) )
                continue;
            JsonObject jsonFilter = new JsonObject();
            jsonFilter.add("name", filter.getName());
            if( filter == diagram.getFilter() )
                jsonFilter.add("active", true);
            result.add(jsonFilter);
        }
        response.sendJSON(result);
    }

    /**
     * Adds new ExpressionFilter to diagram and select it
     * @param diagram - diagram to process
     * @param filterName - name of newly created filter (there should not be existing filter with such name)
     */
    private static void addDiagramFilter(Diagram diagram, String filterName) throws WebException
    {
        for( DiagramFilter filter : diagram.getFilterList() )
        {
            if( filter.getName().equals(filterName) )
                throw new WebException("EX_ACCESS_FILTER_EXISTS", filterName);
        }
        ExpressionFilter filter = new ExpressionFilter(filterName);
        filter.setEnabled(true);
        disableExpressionFilters( diagram );
        // TODO: support undoManager
        diagram.setFilterList((DiagramFilter[])ArrayUtils.add(diagram.getFilterList(), filter));
        diagram.setDiagramFilter(filter);
    }

    /**
     * Adds new ExpressionFilter to diagram, set the table to the given value and select it
     * @todo move table type check into ExpressionFilterProperties
     * @param diagram - diagram to process
     * @param filterName - name of newly created filter (there should not be existing filter with such name)
     */
    private static void addDiagramQuickFilter(Diagram diagram, DataElementPath tablePath) throws WebException
    {
        TableDataCollection table = getDataElement(tablePath, TableDataCollection.class);
        Object referenceTypeValue = diagram.getAttributes().getValue(ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY);
        if(referenceTypeValue != null)
        {
            ReferenceType type = ReferenceTypeRegistry.optReferenceType(referenceTypeValue.toString());
            if(type != null && type != ReferenceTypeRegistry.getDefaultReferenceType())
            {
                ReferenceType tableType = ReferenceTypeRegistry.optReferenceType(table.getReferenceType());
                if(!type.getClass().isInstance(tableType))
                {
                    throw new WebException("EX_QUERY_INVALID_TABLE_TYPE", tablePath, tableType, type);
                }
            }
        }
        Set<String> filters = StreamEx.of(diagram.getFilterList()).map( DiagramFilter::getName ).toSet();
        String baseFilterName = table.getName();
        String filterName = baseFilterName;
        int i=0;
        while( filters.contains(filterName) )
        {
            filterName = baseFilterName + " " + ( ++i );
        }
        ExpressionFilter filter = new ExpressionFilter(filterName);
        filter.getProperties().setTable(tablePath);
        filter.setEnabled(true);
        disableExpressionFilters( diagram );
        // TODO: support undoManager
        diagram.setFilterList((DiagramFilter[])ArrayUtils.add(diagram.getFilterList(), filter));
        diagram.setDiagramFilter(filter);
    }


    /**
     * Removes filter from diagram by given name
     * @param diagram - diagram to process
     * @param filterName - name of filter to remove
     */
    private static void removeDiagramFilter(Diagram diagram, String filterName) throws WebException
    {
        DiagramFilter[] filterList = diagram.getFilterList();
        for( int i = 0; i < filterList.length; i++ )
        {
            DiagramFilter filter = filterList[i];
            if( ! ( filter instanceof ExpressionFilter ) )
                continue;
            if( filter.getName().equals(filterName) )
            {
                if( diagram.getFilter() == filter )
                    diagram.setDiagramFilter(null);
                diagram.setFilterList((DiagramFilter[])ArrayUtils.remove(filterList, i));
                return;
            }
        }
        throw new WebException("EX_QUERY_FILTER_NOT_FOUND", filterName);
    }


    /**
     * Selects diagram filter by given name
     * @param diagram - diagram to process
     * @param filterName - name of filter to select
     * @param out - OutputStream to write result to
     */
    private static void selectDiagramFilter(Diagram diagram, String filterName) throws WebException
    {
        disableExpressionFilters( diagram );
        if( TextUtil.isEmpty(filterName) )
        {
            diagram.setDiagramFilter(null);
            return;
        }
        DiagramFilter[] filterList = diagram.getFilterList();
        for( DiagramFilter filter : filterList )
        {
            if( ! ( filter instanceof ExpressionFilter ) )
                continue;
            if( filter.getName().equals(filterName) )
            {
                ( (ExpressionFilter)filter ).setEnabled( true );
                diagram.setDiagramFilter(filter);
                return;
            }
        }
        throw new WebException("EX_QUERY_FILTER_NOT_FOUND", filterName);
    }

    private static void disableExpressionFilters(Diagram diagram)
    {
        DiagramFilter[] filterList = diagram.getFilterList();
        for( DiagramFilter filter : filterList )
        {
            if( filter instanceof ExpressionFilter )
                ( (ExpressionFilter)filter ).setEnabled( false );
        }
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        Diagram diagram = arguments.getDataElement( Diagram.class );
        switch(arguments.getAction())
        {
            case "list":
                break;
            case "add":
                addDiagramFilter( diagram, arguments.getElementName( "name" ) );
                break;
            case "add_quick":
                addDiagramQuickFilter( diagram, arguments.getDataElementPath( "table" ) );
                break;
            case "remove":
                removeDiagramFilter( diagram, arguments.get( "name" ) );
                break;
            case "select":
                selectDiagramFilter( diagram, arguments.get( "name" ) );
                break;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION );
        }
        sendDiagramFilterList( diagram, response );
    }
}
