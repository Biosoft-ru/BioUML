package biouml.plugins.lucene.web;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.lucene.queryparser.classic.ParseException;
import org.json.JSONArray;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyName;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.plugins.lucene.Formatter;
import biouml.plugins.lucene.LuceneProtocol;
import biouml.plugins.lucene.LuceneQuerySystem;
import biouml.plugins.lucene.LuceneUtils;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.SpecieReference;
import one.util.streamex.EntryStream;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.RowFilter;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.export.TableDPSWrapper;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.HtmlUtil;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.PropertyInfo;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class WebLuceneProvider extends WebJSONProviderSupport
{
    static final String BEANS_SEARCH_RESULT = "beans/searchResult";
    static final String BEANS_SEARCH_RESULT_FULL = "beans/searchResultFull";

    static class SearchResult
    {
        private final String query;
        private DynamicPropertySet[] dpsArray;

        public SearchResult(String query, DynamicPropertySet[] dpsArray)
        {
            this.query = query;
            this.dpsArray = dpsArray;
            normalize();
        }

        String getQuery()
        {
            return query;
        }

        StreamEx<ru.biosoft.access.core.DataElementPath> collections()
        {
            return StreamEx.of( dpsArray ).map( dps -> dps.getProperty( "Path" ) ).nonNull().map( property -> property.getValue() )
                    .select( DataElementPath.class ).map( DataElementPath::getParentPath ).without( DataElementPath.EMPTY_PATH );
        }

        void normalize()
        {
            // Collect all columns
            List<String> columns = StreamEx.of( dpsArray ).<DynamicProperty>flatMap( dps -> StreamEx.of( dps.spliterator() ) )
                    .map( DynamicProperty::getName ).remove( column -> column.startsWith( "__" ) ).distinct().without( "Score" )
                    .without( "completeName" ).toList();

            //order columns
            List<String> keyColumns = Arrays.asList( "Path", "Name", "Title", "Field", "Field data" );
            Collections.sort( columns, Comparator.comparingInt( col -> {
                int idx = keyColumns.indexOf( col );
                return idx == -1 ? Integer.MAX_VALUE : idx;
            } ) );

            List<DynamicPropertySet> resultList = new ArrayList<>();
            for( DynamicPropertySet dps : dpsArray )
            {
                DynamicPropertySet newDPS = new DynamicPropertySetSupport();
                for( String column : columns )
                {
                    DynamicProperty property = dps.getProperty( column );
                    if( property == null )
                    {
                        property = new DynamicProperty( column, String.class, "" );
                    }
                    newDPS.add( property );
                }
                resultList.add( newDPS );
            }
            this.dpsArray = resultList.toArray( new DynamicPropertySet[0] );
        }

        public StreamEx<String> fields()
        {
            return StreamEx.of( dpsArray ).map( dps -> dps.getProperty( "Field" ) ).nonNull()
                    .map( property -> property.getValue().toString() ).distinct();
        }

        public DynamicPropertySet[] getData()
        {
            return dpsArray;
        }
    }

    public static class SaveTableParameters extends OptionEx
    {
        private DataElementPathSet collections;
        private boolean addPath = true;
        private DataElementPath table;
        private PropertyInfo[] fields = new PropertyInfo[0];

        @PropertyName ( "Add column with element path" )
        public boolean isAddPath()
        {
            return addPath;
        }

        public void setAddPath(boolean addPath)
        {
            Object oldValue = this.addPath;
            this.addPath = addPath;
            firePropertyChange( "addPath", oldValue, this.addPath );
        }

        public DataElementPathSet getCollections()
        {
            return collections;
        }
        public void setCollections(DataElementPathSet collections)
        {
            Object oldValue = this.collections;
            this.collections = collections;
            firePropertyChange( "collections", oldValue, this.collections );
        }

        @PropertyName ( "Output table" )
        public DataElementPath getTable()
        {
            return table;
        }
        public void setTable(DataElementPath table)
        {
            Object oldValue = this.table;
            this.table = table;
            firePropertyChange( "table", oldValue, this.table );
        }

        @PropertyName ( "Additional columns" )
        public PropertyInfo[] getFields()
        {
            return fields;
        }

        public void setFields(PropertyInfo[] fields)
        {
            Object oldValue = this.fields;
            this.fields = fields;
            firePropertyChange( "fields", oldValue, this.fields );
        }

        StreamEx<PropertyInfo> availableFields()
        {
            return StreamEx.of( getCollections().stream() ).map( DataElementPath::optDataCollection ).nonNull()
                    .map( dc -> dc.stream().findFirst() )
                    .flatMap( StreamEx::of ).<PropertyInfo>flatMap( de -> Stream.of( BeanUtil.getRecursivePropertiesList( de ) ) ).sorted()
                    .distinct();
        }
    }

    public static class SaveTableParametersBeanInfo extends BeanInfoEx2<SaveTableParameters>
    {
        public SaveTableParametersBeanInfo()
        {
            super( SaveTableParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            addHidden( "collections" );
            property( "table" ).outputElement( TableDataCollection.class ).add();
            add( "addPath" );
            add( "fields", FieldsSelector.class );
        }
    }

    public static class FieldsSelector extends GenericMultiSelectEditor
    {
        @Override
        protected PropertyInfo[] getAvailableValues()
        {
            try
            {
                return ( (SaveTableParameters)getBean() ).availableFields().toArray( PropertyInfo[]::new );
            }
            catch( RuntimeException e )
            {
                return new PropertyInfo[0];
            }
        }
    }

    public static class SaveTableBeanProvider implements BeanProvider
    {
        @Override
        public SaveTableParameters getBean(String path)
        {
            SaveTableParameters parameters = new SaveTableParameters();
            SearchResult result = (SearchResult)WebServicesServlet.getSessionCache().getObject( BEANS_SEARCH_RESULT_FULL );
            if( result == null )
                throw new IllegalStateException( "No previous search result found" );
            String query = result.getQuery();
            String name = query == null ? "Search result" : "Search result (" + query.replaceAll( "\\W+", " " ).trim() + ")";
            DataElementPath basePath = DataElementPathDialog.getDefaultPath( TableDataCollection.class );
            parameters.setTable( basePath.getChildPath( name ).uniq() );
            parameters.setCollections( result.collections().toListAndThen( DataElementPathSet::new ) );
            Map<String, List<PropertyInfo>> nameToProperty = parameters.availableFields().groupingBy( PropertyInfo::getName );
            parameters.setFields( result.collections().map( DataElementPath::optDataCollection ).flatMap( LuceneUtils::indexedFields )
                    .distinct().flatCollection( nameToProperty::get ).toArray( PropertyInfo[]::new ) );
            return parameters;
        }
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        switch( arguments.getAction() )
        {
            case "search":
                SearchResult searchResult = search( arguments );
                sendSearchResult( response, searchResult );
                return;
            case "suggest":
                suggest( arguments, response );
                return;
            case "save":
                JSONArray jsonParams = arguments.getJSONArray( JSON_ATTR );
                SaveTableParameters parameters = new SaveTableBeanProvider().getBean( "" );
                JSONUtils.correctBeanOptions( parameters, jsonParams );
                saveSearchResult( parameters, arguments.get( "filter" ) );
                response.sendString( "ok" );
                return;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION );
        }
    }

    private void saveSearchResult(SaveTableParameters parameters, String filter) throws WebException
    {
        SearchResult result = (SearchResult)WebServicesServlet.getSessionCache().getObject( BEANS_SEARCH_RESULT_FULL );
        if( result == null )
            throw new WebException( "EX_QUERY_NO_SEARCH" );
        TableDataCollection table = wrap( result );
        TableDataCollection target = TableDataCollectionUtils.createTableDataCollection( parameters.getTable() );
        Filter<DataElement> rowFilter = Filter.INCLUDE_ALL_FILTER;
        if( filter != null )
        {
            rowFilter = new RowFilter( filter, table );
        }
        List<Class<?>> idxToType = new ArrayList<>();
        ColumnModel columnModel = target.getColumnModel();
        if( parameters.isAddPath() )
        {
            idxToType.add( DataElementPath.class );
            columnModel.addColumn( "Path", DataElementPath.class );
        }
        for( PropertyInfo pi : parameters.getFields() )
        {
            String name = pi.getDisplayName().replace( '/', ' ' );
            idxToType.add( String.class );
            columnModel.addColumn( name, String.class );
        }
        DataElementPathSet collections = new DataElementPathSet();
        for( RowDataElement row : table )
        {
            if( rowFilter.isAcceptable( row ) )
            {
                DataElementPath path = (DataElementPath)row.getValue( "Path" );
                collections.add( path.getParentPath() );
                List<Object> values = new ArrayList<>();
                if( parameters.isAddPath() )
                    values.add( path );
                DataElement de = path.optDataElement();
                for( PropertyInfo pi : parameters.getFields() )
                {
                    Object value = "";
                    try
                    {
                        if( de != null )
                        {
                            Object rawValue = BeanUtil.getBeanPropertyValue( de, pi.getName() );
                            value = convertValue( rawValue );
                            if(value != null)
                                idxToType.set( values.size(), value.getClass() );
                        }
                    }
                    catch( Exception e )
                    {
                    }
                    values.add( value );
                }
                TableDataCollectionUtils.addRow( target, row.getName(), values.toArray() );
            }
        }
        EntryStream.of(idxToType).forKeyValue( (idx, type) -> columnModel.getColumn( idx ).setType( DataType.fromClass( type ) ) );
        target.finalizeAddition();
        collections
                .stream()
                .map( path -> path.optDataCollection() )
                .filter( Objects::nonNull )
                .map( dc -> ReferenceTypeRegistry.optReferenceType( dc.getInfo()
                        .getProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY ) ) )
                .filter( Objects::nonNull ).distinct()
                .collect( MoreCollectors.onlyOne() ).ifPresent( referenceType -> target.setReferenceType( referenceType.getStableName() ) );
        CollectionFactoryUtils.save( target );
    }

    private static Object convertValue(Object rawValue)
    {
        if(rawValue instanceof DatabaseReference[])
        {
            return StreamEx.of((DatabaseReference[])rawValue).map( DatabaseReference::getAc ).toCollection( StringSet::new );
        }
        if(rawValue instanceof SpecieReference[])
        {
            return StreamEx.of((SpecieReference[])rawValue).map( SpecieReference::getName ).toCollection( StringSet::new );
        }
        return TextUtil.toString( rawValue );
    }

    private TableDataCollection wrap(SearchResult result)
    {
        DynamicPropertySet[] bean = result.getData();
        if( bean.length == 0 || bean[0].getProperty( "Path" ) == null )
        {
            return new TableDPSWrapper( bean );
        }
        TableDataCollection table = new StandardTableDataCollection( null, "Search result" );
        ColumnModel columnModel = table.getColumnModel();

        for( DynamicProperty dp : bean[0] )
        {
            if( dp.getName().equals( "Path" ) )
            {
                columnModel.addColumn( dp.getName(), dp.getDisplayName(), dp.getShortDescription(), DataElementPath.class, null );
            }
            else
            {
                columnModel.addColumn( dp.getName(), dp.getDisplayName(), dp.getShortDescription(), dp.getType(), null );
            }
        }
        for( DynamicPropertySet dps : bean )
        {
            RowDataElement rde = new RowDataElement( DataElementPath.create( dps.getValueAsString( "Path" ) ).getName(), table );
            List<Object> values = new ArrayList<>();
            for( TableColumn column : columnModel )
            {
                Object value = dps.getValue( column.getName() );
                if( value instanceof String )
                {
                    value = HtmlUtil.stripHtml( value.toString() );
                }
                values.add( value );
            }
            rde.setValues( values.toArray() );
            table.put( rde );
        }
        return table;
    }

    private void sendSearchResult(JSONResponse response, SearchResult result) throws IOException
    {
        //this is for compatibility; should be removed later
        WebServicesServlet.getSessionCache().addObject( BEANS_SEARCH_RESULT, new TableDPSWrapper(result.getData()), true );
        WebServicesServlet.getSessionCache().addObject( BEANS_SEARCH_RESULT_FULL, result, true );

        response.sendJSON( new JsonObject().add( "collections", getCollections( result ) ).add( "fields", getFields( result ) )
                .add( "results", result.getData().length ) );
    }

    private static JsonArray getCollections(SearchResult result)
    {
        JsonArray collJSON = new JsonArray();
        result.collections().map( String::valueOf ).distinct().forEach( collJSON::add );
        return collJSON;
    }

    private static JsonArray getFields(SearchResult result)
    {
        JsonArray fieldsJSON = new JsonArray();
        result.fields().forEach( fieldsJSON::add );
        return fieldsJSON;
    }

    private void suggest(BiosoftWebRequest arguments, JSONResponse response) throws IOException, WebException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade( arguments );
        String queryString = arguments.getString( LuceneProtocol.KEY_QUERY );
        String relativeName = arguments.getDataElementPath().getPathDifference( luceneFacade.getCollection().getCompletePath() );
        response.sendJSON( JsonUtils.fromCollection( luceneFacade.getSuggestions( queryString, relativeName ) ) );
    }

    private @Nonnull LuceneQuerySystem getLuceneFacade(BiosoftWebRequest arguments) throws WebException
    {
        DataCollection<?> dc = arguments.getDataCollection();
        DataCollection<?> luceneCollection = LuceneUtils.optLuceneParent( dc );
        if(luceneCollection != null)
        {
            LuceneQuerySystem luceneFacade = LuceneUtils.getLuceneFacade( luceneCollection );
            if( luceneFacade.testHaveLuceneDir() )
            {
                try
                {
                    if( luceneFacade.testHaveIndex() )
                        return luceneFacade;
                }
                catch( IOException e )
                {
                    throw ExceptionRegistry.translateException( e );
                }
            }
        }
        throw new WebException( "EX_QUERY_SEARCH_NOT_SUPPORTED", dc.getCompletePath() );
    }

    private SearchResult search(BiosoftWebRequest arguments) throws IOException, ParseException, IntrospectionException, WebException
    {
        LuceneQuerySystem luceneFacade = getLuceneFacade( arguments );
        String relativeName = arguments.getDataElementPath().getPathDifference( luceneFacade.getCollection().getCompletePath() );

        String queryString = arguments.getString( LuceneProtocol.KEY_QUERY );

        // formatter
        Formatter formatter = null;
        String formatterPrefix = arguments.get( LuceneProtocol.KEY_FORMATTER_PREFIX );
        if( formatterPrefix != null )
        {
            String formatterPostfix = arguments.get( LuceneProtocol.KEY_FORMATTER_POSTFIX );
            if( formatterPostfix != null )
                formatter = new Formatter( formatterPrefix, formatterPostfix );
        }

        int from = arguments.optInt( LuceneProtocol.KEY_FROM, 0 );
        int to = arguments.optInt( LuceneProtocol.KEY_TO, LuceneQuerySystem.MAX_DEFAULT_SEARCH_RESULTS_COUNT );

        DynamicPropertySet[] dpsArray = luceneFacade.searchRecursive( relativeName, queryString, formatter, from, to );
        return new SearchResult( queryString, dpsArray );
    }
}
