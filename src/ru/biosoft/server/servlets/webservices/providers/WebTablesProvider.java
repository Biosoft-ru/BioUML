package ru.biosoft.server.servlets.webservices.providers;

import static ru.biosoft.util.j2html.TagCreator.a;
import static ru.biosoft.util.j2html.TagCreator.div;
import static ru.biosoft.util.j2html.TagCreator.input;
import static ru.biosoft.util.j2html.TagCreator.option;
import static ru.biosoft.util.j2html.TagCreator.p;
import static ru.biosoft.util.j2html.TagCreator.select;
import static ru.biosoft.util.j2html.TagCreator.span;

import java.awt.Color;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.editors.PropertyEditorEx;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.model.Property.PropWrapper;
import com.developmentontheedge.beans.swing.table.BeanTableModelAdapter;
import com.developmentontheedge.beans.swing.table.Column;
import com.developmentontheedge.beans.swing.table.ColumnModel;
import com.developmentontheedge.beans.swing.table.ColumnWithSort;
import com.developmentontheedge.beans.swing.table.SortedTableModel;

import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.server.SqlEditorProtocol;
import biouml.plugins.server.access.AccessProtocol;
import biouml.standard.simulation.plot.Series;
import one.util.streamex.Joining;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.ReferenceTypeSupport;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.task.JobControlTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.ColorEditor;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.StackProgressJobControl;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.plugins.javascript.JavaScriptUtils;
import ru.biosoft.server.Response;
import ru.biosoft.server.Service;
import ru.biosoft.server.ServiceRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.table.BiosoftTableModel;
import ru.biosoft.table.ColumnEx;
import ru.biosoft.table.DescribedString;
import ru.biosoft.table.MessageStubTableDataCollection;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.RowFilter;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.table.columnbeans.Descriptor;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.document.editors.ExpressionEditor;
import ru.biosoft.table.document.editors.TableElement;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.treetable.TreeTableElement;
import ru.biosoft.util.ClassExtensionRegistry;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.ControlCodeGenerator;
import ru.biosoft.util.ObjectExtensionRegistry;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.Util;
import ru.biosoft.util.j2html.tags.ContainerTag;
import ru.biosoft.util.j2html.tags.DomContent;
import ru.biosoft.util.j2html.tags.Tag;

/**
 * Provides table functions
 */
public class WebTablesProvider extends WebProviderSupport
{
    protected static final Logger log = Logger.getLogger( WebTablesProvider.class.getName() );
    public static final String MAP_PATH = "../biouml/map";

    private static final ClassExtensionRegistry<TableResolver> resolverRegistry = new ClassExtensionRegistry<>(
            "ru.biosoft.server.servlets.webTableResolver", "type", TableResolver.class );

    @SuppressWarnings ( "rawtypes" )
    private static final ObjectExtensionRegistry<TableExporter> exporters = new ObjectExtensionRegistry<>(
            "ru.biosoft.server.servlets.tableExporter", "prefix", TableExporter.class );

    private static final ObjectExtensionRegistry<ControlCodeGenerator> controlCodeGenerators = new ObjectExtensionRegistry<>(
            "ru.biosoft.server.servlets.controlCodeGenerator", "prefix", ControlCodeGenerator.class );

    public static class TableQueryResponse
    {
        private static final String TABLE_CELL_PREFIX = "properties/tableCell/";
        private static final byte[] DOUBLE_ARRAY_START = "[[".getBytes( StandardCharsets.ISO_8859_1 );
        private static final byte[] DOUBLE_ARRAY_MIDDLE = "],[".getBytes( StandardCharsets.ISO_8859_1 );
        private static final byte[] DOUBLE_ARRAY_END = "]]".getBytes( StandardCharsets.ISO_8859_1 );
        private DataCollection<?> dc;
        private final TableResolver resolver;
        private final BiosoftWebRequest arguments;
        private final OutputStream out;
        private final JSONResponse response;
        private ColumnModel columnModel;
        private BiosoftTableModel tableModel;
        private int rowFrom, rowTo;

        public TableQueryResponse(DataCollection<?> dc, TableResolver resolver, BiosoftWebRequest arguments, OutputStream out)
        {
            this.dc = dc;
            this.resolver = resolver;
            this.arguments = arguments;
            this.out = out;
            this.response = new JSONResponse( out );
        }

        /**
         * Generate table structure and put to output stream
         * @throws WebException if some unexpected problem occurs
         */
        public void sendTableSceleton() throws WebException
        {
            try
            {
                StringBuilder html = new StringBuilder();
                String tableClass = "display";
                if( resolver == null || resolver.isTableEditable() )
                    tableClass += " editable_table";
                initModel( false );
                if( tableModel.isSortingSupported() )
                {
                    tableClass += " sortable_table";
                }

                html.append( "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"" + tableClass + "\"><thead><tr>" );
                for( int i = 0; i < columnModel.getColumns().length; i++ )
                {
                    if( !columnModel.getColumns()[i].getEnabled() )
                        continue;
                    html.append( "<th>" );
                    html.append( decorateColumnName( columnModel.getColumns()[i].getName() ) );
                    html.append( "</th>" );
                }
                html.append( "</tr></thead><tbody><tr><td colspan=\"" + ( columnModel.getColumns().length )
                        + "\" class=\"dataTables_empty\">Loading data from server</td></tr></tbody></table>" );

                response.sendString( html.toString() );
            }
            catch( Exception e )
            {
                throw new WebException( e, "EX_INTERNAL_CUSTOM", "send table", e.getMessage() );
            }
        }

        /**
         * Generate and send JSON with table data in "dataTables" format
         */
        public void sendTableData(String completeName, boolean readMode)
        {
            JSONObject root = new JSONObject();
            try
            {
                DataElement de = getDataElement( completeName, resolver );
                ReferenceType baseType = null;
                if( de instanceof DataCollection && ( (DataCollection<?>)de ).getInfo().getProperty( DataCollectionConfigConstants.URL_TEMPLATE ) != null )
                {
                    baseType = new MapperType( (DataCollection<?>)de );
                }
                applyFilter();
                initModel( true );
                String sEcho = TextUtil2.nullToEmpty( arguments.get( "sEcho" ) );

                JSONArray aaData = new JSONArray();

                DataElementPath basePath = DataElementPath.create( completeName );
                for( int i = rowFrom; i < rowTo; i++ )
                {
                    JSONArray row = new JSONArray();
                    DataElementPath rowPath = basePath.getChildPath( tableModel.getRealValue( i, 0 ).toString() );
                    String rowId = null;
                    Object rowBean = null;
                    if( tableModel instanceof BeanTableModelAdapter )
                    {
                        rowBean = ( (BeanTableModelAdapter)tableModel ).getModelForRow( i );
                    }
                    if( arguments.get( "add_row_id" ) != null )
                    {
                        try
                        {
                            String name = tableModel.getRowName( i );

                            if( resolver != null && name != null )
                            {
                                rowId = resolver.getRowId( de, name );
                            }
                            else
                            {
                                rowId = name;
                            }

                        }
                        catch( Exception e )
                        {
                        }
                    }
                    Column[] columns = columnModel.getColumns();
                    int columnNum = 0;
                    for( int j = 0; j < tableModel.getColumnCount(); j++ )
                    {
                        while( !columns[columnNum].getEnabled() )
                            columnNum++;
                        Column column = columns[columnNum++];
                        ReferenceType type = null;
                        boolean displayTitle = false;
                        if( column instanceof ColumnEx )
                        {
                            ColumnEx columnEx = (ColumnEx)column;
                            displayTitle =  Boolean.valueOf( columnEx.getValue( ColumnEx.DISPLAY_TITLE ) );
                            type = ReferenceTypeRegistry.optReferenceType( columnEx
                                    .getValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY ) );
                        }
                        if( ( type == null || type == ReferenceTypeRegistry.getDefaultReferenceType() ) && j == 0 )
                        {
                            type = baseType;
                        }
                        
                        String cellPath = TABLE_CELL_PREFIX + rowPath.getChildPath( String.valueOf( j - 1 ) );
                        String cellId = i + ":" + j;
                        Object value = tableModel.getValueAt( i, j );
                        boolean readOnly = ( j == 0 );
                        Tag<?> content = null;
                        if( value instanceof Property )
                        {
                            Property prop = (Property)value;
                            Class<?> c = prop.getPropertyEditorClass();
                            readOnly = prop.isReadOnly();
                            FeatureDescriptor descriptor = prop.getDescriptor();
                            value = prop.getValue();
                            if( c != null && !readOnly )
                            {
                                content = getEditableControlCode( value, readOnly || readMode, cellId, cellPath, c, rowBean, descriptor );
                            }
                        }
                        if(content == null)
                        {
                            content = getControlCode( value, readOnly || readMode, cellId, cellPath, type, displayTitle );
                        }
                        row.put( content.withCondData( rowId != null, "id", rowId ).render() );
                        rowId = null;
                    }

                    aaData.put( row );
                }

                root.put( "sEcho", sEcho );
                root.put( "iTotalRecords", tableModel.getRowCount() );
                root.put( "iTotalDisplayRecords", tableModel.getRowCount() );
                root.put( "aaData", aaData );

                try (OutputStreamWriter writer = new OutputStreamWriter( out, StandardCharsets.UTF_8 ))
                {
                    root.write( writer );
                }
                out.close();
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Can not send 'dataTables' data: " + ExceptionRegistry.log( e ) );
            }
        }

        /**
         * Generate and send JSON with table data in "dataTables" format
         * @throws IOException, WebException
         */
        public void sendRawData() throws IOException
        {
            applyFilter();
            initModel( true );
            int columnCount = tableModel.getColumnCount();
            List<ByteArrayOutputStream> entries = new ArrayList<>( columnCount );
            List<Writer> entryWriters = new ArrayList<>( columnCount );
            for( int j = 0; j < columnCount; j++ )
            {
                ByteArrayOutputStream entry = new ByteArrayOutputStream();
                entries.add( entry );
                Writer entryWriter = new OutputStreamWriter( entry, StandardCharsets.UTF_8 );
                entryWriters.add( entryWriter );
            }
            for( int i = rowFrom; i < rowTo; i++ )
            {
                for( int j = 0; j < columnCount; j++ )
                {
                    Object value = tableModel.getRealValue( i, j );
                    String valueStr;
                    if( value == null )
                        valueStr = "\"\"";
                    else if( value instanceof Number )
                    {
                        try
                        {
                            valueStr = JSONObject.numberToString( (Number)value );
                        }
                        catch( JSONException e )
                        {
                            valueStr = "null";
                        }
                    }
                    else if( value instanceof Boolean )
                        valueStr = ( (Boolean)value ).toString();
                    else if( value instanceof StringSet )
                        valueStr = new JSONArray( (Collection<?>)value ).toString();
                    else if( value instanceof View )
                        try
                        {
                            valueStr = ( (View)value ).toJSON().toString();
                        }
                        catch( JSONException e )
                        {
                            valueStr = "\"\"";
                        }
                    else
                        valueStr = JSONObject.quote( value.toString() );
                    Writer entryWriter = entryWriters.get( j );
                    if( i > rowFrom )
                        entryWriter.write( ',' );
                    entryWriter.write( valueStr );
                }
            }
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            boolean firstEntry = true;
            result.write( DOUBLE_ARRAY_START );
            while( !entries.isEmpty() )
            {
                ByteArrayOutputStream entry = entries.remove( 0 );
                Writer writer = entryWriters.remove( 0 );
                writer.flush();
                if( !firstEntry )
                    result.write( DOUBLE_ARRAY_MIDDLE );
                entry.writeTo( result );
                firstEntry = false;
            }
            result.write( DOUBLE_ARRAY_END );
            response.sendJSON( result );
        }

        public void sendCellData() throws WebException, IOException
        {
            String[] cellId = TextUtil2.split( arguments.getString( "cellId" ), ':' );
            if( cellId.length < 2 )
                throw new WebException( "EX_QUERY_PARAM_INVALID", "cellId" );
            int row;
            int column;
            try
            {
                row = Integer.parseInt( cellId[0] );
                column = Integer.parseInt( cellId[1] );
            }
            catch( NumberFormatException e )
            {
                throw new WebException( "EX_QUERY_PARAM_INVALID", "cellId" );
            }
            applyFilter();
            initModel( true );
            tableModel.setRange( row, row + 1 );
            
            Object value = tableModel.getRealValue( row, column );
            Column columnObj = columnModel.getColumns( column );
            ReferenceType type = null;
            boolean displayTitle = false;
            if( columnObj instanceof ColumnEx )
            {
                ColumnEx columnEx = (ColumnEx)columnObj;
                displayTitle =  Boolean.valueOf( columnEx.getValue( ColumnEx.DISPLAY_TITLE ) );
                type = ReferenceTypeRegistry.optReferenceType( columnEx
                        .getValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY ) );
            }
            Tag<?> result;
            if(type != null)
                result = p().withRawHtml( getCodeForReferenceTyped( value, row + ":" + column, type, displayTitle, Integer.MAX_VALUE ) );
            else
                result = p().withText( TextUtil2.insertBreaks( String.valueOf( value ) ) );
            response.sendString( result.toString() );
        }

        /**
         * Edit cell in table and send new result
         * @throws WebException
         */
        public void sendChangeData(String completeName, JSONArray jsonArray, JSONArray rowIds) throws WebException
        {
            try
            {
                initModel( true );

                if( tableModel != null )
                {
                    if( rowIds != null )
                    {
                        boolean isValid = validateTableRowIds( completeName, rowIds );
                        if( !isValid )
                        {
                            response.error("Table was modified by another user or from another source. Please, repeat your changes and try to save table again." );
                            return;
                        }
                    }

                    SessionCache cache = WebServicesServlet.getSessionCache();
                    Object changedElementInCache = cache.getObject( completeName );
                    if( changedElementInCache != null )
                    {
                        cache.setObjectChanged( completeName, changedElementInCache );
                    }

                    Set<DataElement> changedRows = new HashSet<>();
                    for( int i = 0; i < jsonArray.length(); i++ )
                    {
                        JSONObject property = (JSONObject)jsonArray.get( i );
                        String cellId = (String)property.get( "name" );
                        Object value = property.get( "value" );
                        int ind = cellId.lastIndexOf( ':' );
                        if( ind != -1 )
                        {
                            try
                            {
                                int row = Integer.parseInt( cellId.substring( 0, ind ) );
                                int column = Integer.parseInt( cellId.substring( ind + 1 ) );
                                Object oldValue = tableModel.getValueAt( row, column );
                                if( oldValue instanceof Property )
                                {
                                    Property valueProperty = (Property)oldValue;
                                    try
                                    {
                                        PropertyEditor editor = (PropertyEditor)valueProperty.getPropertyEditorClass().newInstance();
                                        editor.setValue( valueProperty.getValue() );
                                        if( editor instanceof PropertyEditorEx )
                                        {
                                            if( tableModel instanceof BeanTableModelAdapter )
                                            {
                                                Object rowBean = ( (BeanTableModelAdapter)tableModel ).getModelForRow( row );
                                                if( rowBean != null )
                                                    ( (PropertyEditorEx)editor ).setBean( rowBean );
                                            }
                                            ( (PropertyEditorEx)editor ).setDescriptor( valueProperty.getDescriptor() );
                                        }
                                        editor.setAsText( value.toString() );
                                        value = editor.getValue();
                                    }
                                    catch( Exception e1 )
                                    {
                                    }
                                    Object oldVal = valueProperty.getValue();
                                    try
                                    {
                                        if( oldVal instanceof Integer )
                                            value = Integer.parseInt( value.toString() );
                                        else if( oldVal instanceof Double )
                                            value = Double.parseDouble( value.toString() );
                                        else if( oldVal instanceof Boolean )
                                        {
                                            if( value instanceof String )
                                            {
                                                value = Boolean.valueOf( (String)value ) || value.equals( "checked" );
                                            }
                                            else if( ! ( value instanceof Boolean ) )
                                            {
                                                value = false;
                                            }
                                        }
                                    }
                                    catch( NumberFormatException e )
                                    {
                                    }
                                    if( ( oldVal != null || value != null )
                                            && ( oldVal == null || value == null || !oldVal.equals( value ) ) )
                                    {
                                        ( (Property)oldValue ).setValue( value );
                                        Object owner = ( (Property)oldValue ).getOwner();
                                        if( owner instanceof PropWrapper )
                                        {
                                            owner = ( (PropWrapper)owner ).getOwner();
                                        }
                                        if( owner instanceof DataElement )
                                        {
                                            changedRows.add( (DataElement)owner );
                                        }
                                    }
                                }
                                else
                                {
                                    tableModel.setValueAt( value, row, column );
                                }
                            }
                            catch( NumberFormatException e )
                            {
                                response.error( "Incorrect cell ID" );
                            }
                        }
                    }
                    for( ru.biosoft.access.core.DataElement row : changedRows )
                    {
                        try
                        {
                            if( row instanceof RowDataElement )
                            {
                                @SuppressWarnings ( "unchecked" )
                                DataCollection<RowDataElement> origin = (DataCollection<RowDataElement>)row.getOrigin();
                                origin.put( (RowDataElement)row );
                            }
                        }
                        catch( Exception e )
                        {
                        }
                    }
                }
                {
                    DataElement de = CollectionFactory.getDataElement( completeName );
                    if( de instanceof TableDataCollection )
                    {
                        ( (TableDataCollection)de ).getCompletePath().save( de );
                    }
                }
                response.sendString( "" );
            }
            catch( Exception e )
            {
                throw new WebException( e, "EX_INTERNAL_CUSTOM", "Update table data", e.getMessage() );
            }
        }


        //Check if number of rows was not changed and rows have the same ids
        //Otherwise table might have been modified from other source of by another user and can not be changed to avoid data inconsistency
        //TODO: think of better way to merge changes, now cell values changed by another user will be overwritten without warning
        private boolean validateTableRowIds(String completeName, JSONArray rowIds) throws WebException
        {
            if( rowIds.length() > tableModel.getRowCount() )
                return false;

            DataElement de = getDataElement( completeName, resolver );

            for( int i = 0; i < rowIds.length(); i++ )
            {
                JSONArray property = (JSONArray)rowIds.get( i );
                String cellId = (String)property.get( 0 );
                String rowId = (String)property.get( 1 );
                int ind = cellId.lastIndexOf( ':' );
                if( ind != -1 )
                {
                    int row = Integer.parseInt( cellId.substring( 0, ind ) );
                    String realRowId = null;
                    String name = tableModel.getRowName( row );
                    if( resolver != null && name != null )
                    {
                        realRowId = resolver.getRowId( de, name );
                    }
                    else
                    {
                        realRowId = name;
                    }
                    if( !realRowId.equals( rowId ) )
                        return false;
                }
            }
            return true;
        }

        @SuppressWarnings ( {"unchecked", "rawtypes"} )
        public void exportFilteredTable(DataCollection<?> table, DataElementPath path, WebJob wj) throws WebException, IOException
        {
            //String type = "Regular"; //TODO
            //stream
            TableExporter<?, ?> exporter = exporters.stream().filter( exp -> exp.getSupportedCollectionType().isInstance( table ) )
                    .findFirst().orElseThrow( () -> new WebException( "EX_INTERNAL_CUSTOM", "Save filtered table",
                            "Table type is not supported: " + table.getClass().getSimpleName() ) );
            //TableExporter<?, ?> exporter = exporters.getExtension( table.getClass().getName() );
            //            TableExporter<?, ?> exporter = EntryStream.of(exporters).filterKeys( cls -> cls.isInstance( table ) )
            //                .values().findFirst().orElseThrow( () -> new WebException("EX_INTERNAL_CUSTOM", "Save filtered table",
            //                        "Table type is not supported: "+table.getClass().getSimpleName()));
            //            if( exporter == null )
            //                throw new WebException( "EX_INTERNAL_CUSTOM", "Save filtered table",
            //                        "Table type is not supported: " + table.getClass().getSimpleName() );
            String filterExpr = arguments.getOrDefault( "filter", "true" );
            StackProgressJobControl sjc = new StackProgressJobControl( java.util.logging.Logger.getLogger( log.getName() ) )
            {
                @Override
                protected void doRun() throws JobControlException
                {
                    ((TableExporter)exporter).export( table, path, filterExpr, this );
                }
            };
            if(wj != null)
            {
                wj.setJobControl( sjc );
            }
            TaskPool.getInstance().submit( new JobControlTask( "Export filtered table", sjc ));
            response.sendString( "Table export started" );
        }

        private void applyFilter()
        {
            String filterStr = arguments.get( "filter" );
            if( filterStr != null )
            {
                Filter<DataElement> filter = new RowFilter( filterStr, dc );
                dc = new FilteredDataCollection<>( null, "", dc, filter, null, new Properties() );
            }
        }

        private static Tag<?> getEditableControlCode(Object value, boolean readOnly, String id, String path, Class<?> editorClass,
                Object rowBean, FeatureDescriptor descriptor)
        {
            if( value == null )
            {
                return getEmptyControlCode();
            }

            if( readOnly )
            {
                return getControlCode( value, readOnly, id, path, null, false );
            }
            String[] tags = null;
            PropertyEditor editor = null;
            try
            {
                editor = (PropertyEditor)editorClass.newInstance();
                editor.setValue( value );
                if( editor instanceof PropertyEditorEx )
                {
                    if( rowBean != null )
                        ( (PropertyEditorEx)editor ).setBean( rowBean );
                    ( (PropertyEditorEx)editor ).setDescriptor( descriptor );
                }
                tags = editor.getTags();
            }
            catch( Exception e1 )
            {
            }
            if( tags != null )
            {
                Set<String> values = new HashSet<>();
                boolean multiselect = false;
                if( value.getClass().isArray() )
                {
                    for( Object o : (Object[])value )
                    {
                        String valueStr = o.toString();
                        values.add( valueStr );
                    }
                    multiselect = true;
                }
                else
                {
                    String valueStr = editor != null ? editor.getAsText() : value.toString();
                    values.add( valueStr );
                }

                Tag<?> selectTag = select().withClass( "cellControl" ).withId( id )
                    .with( StreamEx.of(tags).map(
                                tag -> option().withValue( tag ).condAttr( values.contains( tag ), "selected", null ).withText( tag ) )
                                .toList() );
                if( multiselect )
                {
                    selectTag.attr( "multiple", "multiple" );
                    selectTag.attr( "size", String.valueOf( tags.length ) );
                }
                return selectTag;
            }
            else if( ExpressionEditor.class.isAssignableFrom( editorClass ) )
            {
                return p().withClass( "cellControl" ).attr( "style", "white-space:nowrap;" )
                    .with( input().withType( "text" ).withId( id ).withValue( value.toString() ),
                            input().withType( "image" ).withSrc( "icons/edit.gif" ).withValue( "Edit" )
                            .attr( "onclick", "getExpressionDialog('" + id+ "', this);" ));
            }

            else if( PenEditor.class.isAssignableFrom( editorClass ) && rowBean != null
                    && Series.class.isAssignableFrom( rowBean.getClass() ) )
            {
                //TODO: use ControlCodeGenerator
                String plotPath = path.substring( TABLE_CELL_PREFIX.length() );
                String name = ( (Series)rowBean ).getName();
                String parentPath = "plotseriespen/" + plotPath + ";" + name;
                return p().withClass( "cellControl" ).attr( "style", "white-space:nowrap;" )
                        .with( input().withType( "image" ).withSrc( "icons/edit.gif" ).withValue( "Edit" ).attr( "onclick",
                                "createBeanEditorDialog('Plot line specification', '" + parentPath
                                        + "', function(){}, true);" ) );
            }
            else if( PenEditor.class.isAssignableFrom( editorClass ) && rowBean != null && rowBean instanceof DataElement
                    && ( Curve.class.isAssignableFrom( rowBean.getClass() ) || Experiment.class.isAssignableFrom( rowBean.getClass() ) ) )
            {
                //TODO: rewrite the code, use ControlCodeGenerator
                //TODO: create specific web control for any pen (processing as json) and avoid bean mediator 
                String fullPath = path.substring( TABLE_CELL_PREFIX.length() );
                String name = ( (DataElement)rowBean ).getName();
                Option plotParent = ( (Option)rowBean ).getParent();
                if( plotParent != null && plotParent instanceof PlotInfo )
                {
                    String plotName = ( (PlotInfo)plotParent ).getTitle();
                    String varType = Curve.class.isAssignableFrom( rowBean.getClass() ) ? "curve" : "experiment";
                    String parentPath = "plotseriespen/" + fullPath + ";" + plotName + ";" + name + ";" + varType;
                    return p().withClass( "cellControl" ).attr( "style", "white-space:nowrap;" )
                            .with( input().withType( "image" ).withSrc( "icons/edit.gif" ).withValue( "Edit" ).attr( "onclick",
                                    "createBeanEditorDialog('Plot line specification', '" + parentPath + "', function(){}, true);" ) );
                }
                else
                    return getControlCode( value, readOnly, id, path, null, false );

            }
            else if( ColorEditor.class.isAssignableFrom(editorClass) && !readOnly )
            {
                Color color = ((Color) value);
                return input().withType("button").withClass("cellControl").withClass("color-picker-button").withId(id)
                        .attr("style",
                                "width:100px; height:12px; border: 1px solid black; background-color:rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ");")
                        .attr("onclick", "changeColorFromTable(this)");
            }
            else
            {
                return getControlCode( value, readOnly, id, path, null, false );
            }
        }

        private void initModel(boolean sort)
        {
            columnModel = TableDataCollectionUtils.getColumnModel( dc );
            tableModel = TableDataCollectionUtils.getTableModel( dc, columnModel );
            if( sort )
                sortTableModel();
            this.rowFrom = arguments.optInt( "iDisplayStart" );
            this.rowTo = this.rowFrom + arguments.optInt( "iDisplayLength", -1 );
            if( this.rowTo == -1 || this.rowTo > tableModel.getRowCount() )
                this.rowTo = tableModel.getRowCount();
            tableModel.setRange( rowFrom, rowTo );
        }

        private void sortTableModel()
        {
            if( !tableModel.isSortingSupported() )
                return;
            int cNumber = arguments.optInt( "iSortCol_0" );
            boolean dir = !"desc".equals( arguments.get( "sSortDir_0" ) );
            for( int i = 0; i <= cNumber && i < columnModel.getColumns().length; i++ )
            {
                if( !columnModel.getColumns()[i].getEnabled() )
                    cNumber++;
            }
            // Invalid column is supplied for sorting
            if( cNumber >= columnModel.getColumns().length )
                return;

            for( Column col : columnModel.getColumns() )
            {
                if( col instanceof ColumnWithSort )
                    ( (ColumnWithSort)col ).setSorting( ColumnWithSort.SORTING_NONE );
            }
            ColumnWithSort column = (ColumnWithSort)columnModel.getColumns( cNumber );
            if( dir )
            {
                column.setSorting( ColumnWithSort.SORTING_ASCENT );
            }
            else
            {
                column.setSorting( ColumnWithSort.SORTING_DESCENT );
            }

            try
            {
                ( (SortedTableModel)tableModel ).sort();
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        /**
         * @param name
         * @return column-name ready to insert into HTML
         */
        private static String decorateColumnName(String name)
        {
            return StringEscapeUtils.escapeHtml( name ).replaceFirst( "^-", "&#x2011;" );
        }

        protected String[] getColumnNames(BiosoftWebRequest arguments) throws WebException
        {
            initModel( false );
            String[] innerNames = arguments.getStrings( "jsoncols" );
            List<String> realNames = new ArrayList<>();
            int length = columnModel.getColumns().length;
            for( String innerName : innerNames )
            {
                try
                {
                    int index = Integer.parseInt( innerName );
                    if( index >= 0 && index < length )
                        realNames.add( columnModel.getColumns( index ).getName() );
                    else
                        throw new Exception( "Incorrect column index: " + innerName );
                }
                catch( Exception e )
                {
                    //TODO: process
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            return realNames.toArray( new String[realNames.size()] );
        }

        private static class MapperType extends ReferenceTypeSupport
        {
            private final DataElementPath parent;
            private final String urlTemplate;

            public MapperType(DataCollection<?> parent)
            {
                this.parent = DataElementPath.create( parent );
                urlTemplate = this.parent.getDataElement( ru.biosoft.access.core.DataCollection.class ).getInfo().getProperty( DataCollectionConfigConstants.URL_TEMPLATE );
            }

            @Override
            public int getIdScore(String id)
            {
                return SCORE_NOT_THIS_TYPE;
            }

            @Override
            public String getObjectType()
            {
                return "Data element";
            }

            @Override
            public String getURL(String id)
            {
                if( urlTemplate != null && urlTemplate.startsWith( "de:" ) )
                {
                    return urlTemplate.replace( "$id$", id );
                }
                return MAP_PATH + "?de=" + TextUtil2.encodeURL( parent.getChildPath( id ).toString() );
            }
        }
    }

    public interface TableExporter<E extends DataElement, C extends DataCollection<E>>
    {
        void export(C originalCollection, DataElementPath targetPath, String filterExpression, StackProgressJobControl jc);
        Class<?> getSupportedCollectionType();

    }

    private static void checkFilter(DataCollection<?> dc, String filter) throws WebException
    {
        try
        {
            if( TextUtil2.isEmpty( filter ) )
                return;
            new RowFilter( filter, dc );
        }
        catch( Exception e )
        {
            throw new WebException( e, "EX_INPUT_FILTER_ERROR", e.getMessage() );
        }
    }

    protected static final DecimalFormat decFormat, expFormat;
    static
    {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols( Locale.US );
        decimalFormatSymbols.setNaN( "" );
        decFormat = new DecimalFormat( "#.#####" );
        decFormat.setDecimalFormatSymbols( decimalFormatSymbols );
        expFormat = new DecimalFormat( "#.####E0" );
        expFormat.setDecimalFormatSymbols( decimalFormatSymbols );
    }

    public static Tag<?> getControlCode(Object value, boolean readOnly, String id, String path, ReferenceType type, boolean displayTitle)
    {
        
        if( value == null )
        {
            if( readOnly )
                return getEmptyControlCode();
            return input().withType( "text" ).withClass( "cellControl" ).withId( id )
                    .withValue( "" );
        }


        if( value instanceof Throwable )
        {
            return span().withClass( "table_error" ).withText( ( (Throwable)value ).getMessage() );
        }
        else if( value instanceof Boolean )
        {
            return input().withType( "checkbox" ).condAttr( readOnly, "disabled", null )
                    .condAttr( ( (Boolean)value ).booleanValue(), "checked", null ).withClass( "cellControl" ).withId( id );
        }
        else if( value instanceof Chart )
        {
            if( ( (Chart)value ).isEmpty() )
                return getEmptyControlCode();
            return input().withType( "button" ).withClass( "ui-state-default" ).withValue( "View" ).attr( "onclick",
                    "showImage('" + StringEscapeUtils.escapeJavaScript( path ) + "')" );
        }
        else if( value instanceof CompositeView )
        {
            String json = null;
            try
            {
                json = ( (CompositeView)value ).toJSON().toString();
            }
            catch( JSONException e )
            {
                log.log( Level.SEVERE, "Can't get cell value", e );
                return p().withText( "error" );
            }
            long uid = Util.getUniqueId();
            return div().attr( "width", "200px" ).withId( "viewer_" + uid )
                    .with( span().withClass( "table_script_node" ).withText( "showViewPane('viewer_" + uid + "', '" + json + "')" ) );
        }
        else if( value instanceof DescribedString )
        {
            DescribedString describedString = (DescribedString)value;
            ContainerTag summary = div().withClass( "summaryText" ).withText( describedString.getTitle() );
            Color color = describedString.getColor();
            if( color != null )
            {
                summary = div().withClass( "alternativeView" ).with( summary, div().attr( "style",
                        "width:20px;height:20px;background-color:" + ColorUtils.colorToString( color ) + ";border:1px solid #aaa;" ) );
            }
            return div().with( summary, div().withClass( "hiddenDetails" ).withRawHtml( describedString.getHtml() ) );
        }
        else if( value instanceof StringSet )
        {
            StringSet stringSet = (StringSet)value;
            String fullList = String.join( ", ", stringSet );
            if( fullList.length() <= 100 )
                return span().withText( fullList );
            BiFunction<ContainerTag, String, ContainerTag> addToggler = (tag, label) -> tag.withText( " " )
                    .with( span().withClass( "clickable" ).attr( "onclick", "BioUMLTable.toggleCellContent(this)" ).withText( label ) );
            String shortContent = stringSet.stream().collect( Joining.with( ", " ).maxChars( 100 ).cutAtWord().ellipsis( "..." ) );
            return span().with( addToggler.apply( span().withText( shortContent ), "(more)" ),
                    addToggler.apply( span().attr( "style", "display:none" ).withText( fullList ), "(less)" ) );
        }
        else if( ( value instanceof DataElementPath || value instanceof DataElementPathSet ) && readOnly )
        {
            DataElementPathSet paths;
            if( value instanceof DataElementPath )
            {
                paths = new DataElementPathSet();
                paths.add( (DataElementPath)value );
            }
            else
            {
                paths = (DataElementPathSet)value;
            }
            long uid = Util.getUniqueId();
            StringBuilder sb = new StringBuilder();
            for( DataElementPath dep : paths )
            {
                String iconId = IconFactory.getIconId( dep );
                String title = getTitle( dep, dep.getName() );
                sb.append( "showDataElementLink('viewer_" + uid + "', '" + StringEscapeUtils.escapeJavaScript( dep.toString() ) + "','"
                        + ( iconId == null ? "" : StringEscapeUtils.escapeJavaScript( iconId ) ) + "','"
                        + StringEscapeUtils.escapeJavaScript( title ) + "', " + dep.exists() + ");" );
            }
            return div().withId( "viewer_" + uid ).with( span().withClass( "table_script_node" ).withText( sb.toString() ) );
        }
        else if( value instanceof DynamicPropertySet )
        {
            return getEmptyControlCode();
        }
        else if( value instanceof Pen )
        {
            //TODO: move to control code generator
            Color color = ( (Pen)value ).getColor();
            long uid = Util.getUniqueId();
            return div().attr( "style", "width:100px; height:12px; border:1px solid black; background-color:rgb(" + color.getRed() + ","
                    + color.getGreen() + "," + color.getBlue() + ");" ).withId( "viewer_" + uid );
        }
        else
        {
            ControlCodeGenerator cg = controlCodeGenerators.stream().filter( ccg -> ccg.getSupportedItemType().isInstance( value ) )
                    .findFirst().orElse( null );
            if( cg != null )
            {
                try
                {
                    return cg.getControlCode( value );
                }
                catch( Exception e )
                {
                    log.log( Level.SEVERE, "Can't get control code from generator " + cg.getClass().getName(), e );
                    return p().withText( "error" );
                }
            }

            if( readOnly
                    || ! ( value instanceof String || value instanceof Number || value instanceof DataElementPath || value instanceof DataElementPathSet ) )
            {
                String valueStr;
                if( type != null )
                {
                    valueStr = getCodeForReferenceTyped( value, id, type, displayTitle, 400 );
                }
                else
                {
                    valueStr = TextUtil2.nullToEmpty( TextUtil2.toString( value ) );
                    valueStr = getSubstringWithTags( valueStr );
                    valueStr = TextUtil2.insertBreaks( valueStr );
                    valueStr = valueStr.replace( "\n", "<br>" );
                    if( value instanceof Float || value instanceof Double )
                    {
                        double num = ( (Number)value ).doubleValue();
                        valueStr = span()
                                .attr( "title", value.toString() )
                                .withText(
                                        ( num == 0 ? "0" : ( Math.abs( num ) >= 1e6 || Math.abs( num ) < 1e-3 ) ? expFormat.format( num )
                                                : decFormat.format( num ) ) ).render();
                    }
                }
                return p().withClass( "cellControl" ).withId( id ).withRawHtml( valueStr );
            }
            String valueStr = TextUtil2.nullToEmpty( value.toString() );
            return input().withType( "text" ).withClass( "cellControl" ).withId( id ).withValue( valueStr );
        }
    }

    private static String getSubstringWithTags(String value)
    {
        if( value.length() > 600 )
        {
            int startTag = value.indexOf( "<" );
            if( startTag != -1 )
            {
                Document taggedStr = Jsoup.parse( value );
                String innerStr = taggedStr.text();
                if( innerStr.length() > 600 )
                    return value.substring( 0, Math.min( 600, startTag ) )
                            + " <span class='clickable' onclick='displayTableCell(this)'>(more)</span>";
                else
                    return value;
            }
            else
                return value.substring( 0, 600 ) + " <span class='clickable' onclick='displayTableCell(this)'>(more)</span>";
        }
        return value;
    }

    public static String getCodeForReferenceTyped(Object value, String cellId, ReferenceType type, boolean displayTitle, int maxLength)
    {
        String valueStr;
        String[] values = cellId.endsWith( ":0" ) ? new String[] {value.toString()} : TableDataCollectionUtils.splitIds( value
                .toString() );
        List<DomContent> elements = new ArrayList<>();
        int originalLength = 0;
        for( int i = 0; i < values.length; i++ )
        {
            String url = type.getURL( values[i] );
            DataElementPath dePath = type.getPath( values[i] );
            String shownText = values[i];
            if( displayTitle && dePath != null && dePath.exists() )
                shownText = getTitle( dePath, values[i] );

            originalLength += shownText.length() + 2;
            elements.add( wrapLink(shownText, url) );
            if( originalLength > maxLength && i < values.length - 2 )
            {
                elements.add( span().withClass( "clickable" ).attr( "onclick", "displayTableCell(this)" ).withText( "(more)" ) );
                break;
            }
        }
        valueStr = StreamEx.of(elements).joining( ", " );
        return valueStr;
    }

    protected static DomContent wrapLink(String value, String url)
    {
        if( url == null )
            return span(value).attr( "style", "white-space: nowrap" );
        if( url.startsWith( "de:" ) )
            return a().withHref( "#" )
                    .attr( "onclick", "openDocument('" + StringEscapeUtils.escapeJavaScript( url.substring( 3 ) ) + "');return false;" )
                    .attr( "style", "white-space: nowrap" )
                    .withText( value );

        return a().withTarget( "_blank" )
                .attr( "style", "white-space: nowrap" )
                .withHref( url )
                .withText( value );
    }

    public static String getTitle(DataElementPath path, String defaultValue)
    {
        DataCollection<?> parent = path.optParentCollection();
        if(parent != null)
        {
            QuerySystem querySystem = parent.getInfo().getQuerySystem();
            if(querySystem != null)
            {
                Index<?> index = querySystem.getIndex( "title" );
                if(index != null)
                {
                    Object titleObj = index.get( path.getName() );
                    if(titleObj != null)
                        return titleObj.toString();
                }
            }
        }
        return defaultValue;
    }

    public static ContainerTag getEmptyControlCode()
    {
        return p().withClass( "cellControl" ).withRawHtml( "&nbsp;" );
    }

    public static void sendTableColumns(DataCollection<?> dc, JSONResponse response) throws IOException
    {
        JSONArray columnNames = new JSONArray();
        ColumnModel columnModel = TableDataCollectionUtils.getColumnModel( dc );

        for( Column column : columnModel.getColumns() )
        {
            JSONObject columnEntry = new JSONObject();
            try
            {
                columnEntry.put( "name", column.getName() );
                columnEntry.put( "jsName", JavaScriptUtils.getValidName( column.getName() ) );
                if( column instanceof TableColumn )
                {
                    TableColumn tableColumn = (TableColumn)column;
                    columnEntry.put( "type", tableColumn.getType().toString() );
                    String refType = tableColumn.getValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY );
                    if( refType != null )
                        columnEntry.put( "referenceType", refType );
                }
                if( !column.getEnabled() )
                    columnEntry.put( "hidden", true );
            }
            catch( JSONException e )
            {
            }
            columnNames.put( columnEntry );
        }
        response.sendJSON( columnNames );
    }

    public static void sendSortOrder(DataCollection<?> dc, JSONResponse response) throws IOException
    {
        JSONObject root = new JSONObject();
        ColumnModel columnModel = TableDataCollectionUtils.getColumnModel( dc );
        Column[] columns = columnModel.getColumns();
        int columnNumber = 0;
        String dir = "asc";
        boolean isSorterd = false;
        for( Column column : columns )
        {
            ColumnWithSort col = (ColumnWithSort)column;
            if( col.getSorting() != ColumnWithSort.SORTING_NONE )
            {
                isSorterd = true;
                if( col.getSorting() == ColumnWithSort.SORTING_ASCENT )
                {
                    dir = "asc";
                }
                else
                {
                    dir = "desc";
                }
                break;
            }
            if( col.getEnabled() )
                columnNumber++;
        }
        if( !isSorterd )
            columnNumber = 0;
        try
        {
            root.put( "columnNumber", columnNumber );
            root.put( "direction", dir );
        }
        catch( JSONException e )
        {
        }
        response.sendJSON( root );
    }

    /**
     * Return table clone using session
     */
    public static DataCollection<?> getTable(String completeName, TableResolver resolver) throws Exception
    {
        Object de = null;
        if( resolver instanceof SqlQueryTableResolver )
        {
            de = resolver.getTable( null );
        }
        else
        {
            de = WebBeanProvider.getBean( completeName );
            if( de instanceof DataElement && resolver != null )
            {
                de = resolver.getTable( (DataElement)de );
            }
        }
        if( de instanceof DataCollection )
        {
            return (DataCollection<?>)de;
        }
        if( ! ( resolver instanceof SqlQueryTableResolver ) )
        {
            Object table = WebBeanProvider.getBean( completeName );
            if( table instanceof TreeTableElement )
            {
                return ( (TreeTableElement)table ).getTable();
            }
            if( table instanceof DataCollection )
            {
                return (DataCollection<?>)table;
            }
        }
        return null;
    }

    protected static DataElement getDataElement(String completeName, TableResolver resolver)
    {
        if( resolver instanceof SqlQueryTableResolver )
        {
            return null;
        }
        Object deObj = WebBeanProvider.getBean( completeName );
        if( deObj instanceof DataElement )
        {
            return (DataElement)deObj;
        }
        return null;
    }

    protected static TableDataCollection getTableDataCollection(DataCollection<?> dc) throws WebException
    {
        if( ! ( dc instanceof TableDataCollection ) )
            throw new WebException( "EX_QUERY_NOT_REAL_TABLE", dc.getCompletePath() );
        return (TableDataCollection)dc;
    }

    protected static TableDataCollection getMutableTableDataCollection(DataCollection<?> dc) throws WebException
    {
        TableDataCollection tdc = getTableDataCollection( dc );
        if( !tdc.isMutable() )
            throw new WebException( "EX_ACCESS_READ_ONLY", dc.getCompletePath() );
        return tdc;
    }

    private static void addColumn(TableDataCollection dc) throws WebException
    {
        try
        {
            dc.getColumnModel().addColumn( String.class );
            CollectionFactoryUtils.save( dc );
        }
        catch( Throwable e )
        {
            throw new WebException( e, "EX_INTERNAL_CUSTOM", "Add column", e.getMessage() );
        }
    }

    /**
     * Creates descriptor-based column
     * @throws WebException
     */
    private static void addDescriptorColumn(TableDataCollection dc, Descriptor descriptor) throws WebException
    {
        try
        {
            TableColumn column = dc.getColumnModel().addColumn( descriptor );
            dc.recalculateColumn( column.getName() );
        }
        catch( Throwable e )
        {
            throw new WebException( e, "EX_INTERNAL_CUSTOM", "Add descriptor", e.getMessage() );
        }
    }

    private static void removeColumn(TableDataCollection dc, String[] columnNames, JSONResponse response) throws WebException
    {
        try
        {
            ru.biosoft.table.ColumnModel colModel = dc.getColumnModel();
            List<String> nonRemovable = new ArrayList<>();
            for( String name : columnNames )
            {
                int index = colModel.optColumnIndex( name );
                if( index == -1 )
                {
                    nonRemovable.add( name );
                }
                else
                {
                    colModel.removeColumn( index );
                }
            }
            String respStr = "";
            if( !nonRemovable.isEmpty() )
            {
                respStr = "Column" + ( ( nonRemovable.size() > 1 ) ? "s " : " " ) + String.join( ", ", nonRemovable )
                        + " can not be removed";
            }
            CollectionFactoryUtils.save( dc );
            response.sendString( respStr );
        }
        catch( Throwable e )
        {
            throw new WebException( e, "EX_INTERNAL_CUSTOM", "Remove column", e.getMessage() );
        }
    }

    private static void convertExpressionsToValues(TableDataCollection tdc, String[] columnNames) throws WebException
    {
        ru.biosoft.table.ColumnModel colModel = tdc.getColumnModel();
        List<String> toProcess = new ArrayList<>();
        for( String colName : columnNames )
        {
            TableColumn column = colModel.getColumn( colName );
            if( !column.isExpressionEmpty() && !column.isExpressionLocked() )
                toProcess.add( colName );
        }
        if( toProcess.isEmpty() )
        {
            throw new WebException( "EX_QUERY_NO_COLUMNS" );
        }
        try
        {
            for( String name : tdc.getNameList().toArray( new String[tdc.getSize()] ) )
            {
                RowDataElement rde = tdc.get( name );
                for( String colName : toProcess )
                {
                    rde.setValue( colName, rde.getValue( colName ) );
                }
                tdc.put( rde );
            }
            for( String colName : columnNames )
            {
                colModel.getColumn( colName ).setExpression( "" );
            }
            CollectionFactoryUtils.save( tdc );
        }
        catch( Throwable e )
        {
            throw new WebException( e, "EX_INTERNAL_CUSTOM", "Convert to values", e.getMessage() );
        }
    }

    /**
     * @param path
     * @param data
     * @param columns
     */
    private void createTable(DataElementPath path, JSONArray data, JSONArray columns) throws WebException
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( path );
        try
        {
            for( int i = 0; i < columns.length(); i++ )
            {
                JSONObject columnJSON;
                String name;
                columnJSON = columns.getJSONObject( i );
                name = columnJSON.getString( "name" );
                DataType type = DataType.fromString( columnJSON.optString( "type" ) );
                ReferenceType referenceType = ReferenceTypeRegistry.optReferenceType( columnJSON.optString( "referenceType" ) );
                if( i == 0 )
                {
                    if( type.isNumeric() )
                    {
                        table.getInfo().getProperties().setProperty( TableDataCollection.INTEGER_IDS, String.valueOf( true ) );
                    }
                    if( referenceType != null )
                    {
                        table.setReferenceType( referenceType.toString() );
                    }
                }
                else
                {
                    TableColumn column = table.getColumnModel().addColumn( name, type );
                    if( referenceType != null )
                    {
                        column.setValue( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, referenceType.toString() );
                    }
                }
            }
        }
        catch( JSONException e )
        {
            throw new WebException( e, "EX_QUERY_PARAM_INVALID_VALUE", "columns" );
        }
        try
        {
            int nRows = data.getJSONArray( 0 ).length();
            int nColumns = data.length() - 1;
            for( int i = 0; i < nRows; i++ )
            {
                Object[] values = new Object[nColumns];
                for( int j = 0; j < nColumns; j++ )
                    values[j] = table.getColumnModel().getColumn( j ).getType().convertValue( data.getJSONArray( j + 1 ).getString( i ) );
                TableDataCollectionUtils.addRow( table, data.getJSONArray( 0 ).getString( i ), values, true );
            }
            table.finalizeAddition();
            path.save( table );
            WebSession.getCurrentSession().pushRefreshPath( path );
        }
        catch( JSONException e )
        {
            throw new WebException( e, "EX_QUERY_PARAM_INVALID_VALUE", "data" );
        }
    }

    public static class SqlQueryTableResolver extends TableResolver
    {
        protected String sqlQuery;
        protected int start;
        protected int length;
        protected boolean addToJournal;

        public SqlQueryTableResolver(String sqlquery, int start, int length, boolean addToJournal)
        {
            this.start = start;
            this.length = length;
            this.sqlQuery = sqlquery;
            this.addToJournal = addToJournal;
        }

        @Override
        public DataCollection<?> getTable(DataElement de) throws Exception
        {
            TaskInfo taskInfo = null;
            Journal journal = JournalRegistry.getCurrentJournal();
            if( journal != null )
            {
                taskInfo = journal.getEmptyAction();
                taskInfo.setType( TaskInfo.SQL );
                taskInfo.setData( getQuery() );
            }
            try
            {
                Service service = ServiceRegistry.getService( SqlEditorProtocol.SQL_EDITOR_SERVICE );
                Map<String, String> map = new HashMap<>();
                map.put( SecurityManager.SESSION_ID, SecurityManager.getSession() );
                map.put( SqlEditorProtocol.KEY_QUERY, sqlQuery );
                map.put( SqlEditorProtocol.KEY_START, Integer.toString( start ) );
                map.put( SqlEditorProtocol.KEY_LENGTH, Integer.toString( length ) );
                SQLResponse response = new SQLResponse();
                if( service != null )
                {
                    service.processRequest( SqlEditorProtocol.DB_EXECUTE, map, response );
                }

                if( response.getError() != null )
                {
                    throw new Exception( response.getError() );
                }

                if( response.getJsonString() != null )
                {
                    JSONObject json = new JSONObject( response.getJsonString() );
                    JSONArray jsonColumns = json.getJSONArray( "columns" );
                    JSONObject jsonData = json.getJSONObject( "data" );

                    StandardTableDataCollection tableDataCollection = new StandardTableDataCollection( null, new Properties() );
                    for( int i = 0; i < jsonColumns.length(); i++ )
                    {
                        tableDataCollection.getColumnModel().addColumn( jsonColumns.getString( i ), String.class );
                    }
                    int rowCnt = 0;
                    while( rowCnt < start + length )
                    {
                        Object rowValues[] = new Object[jsonColumns.length()];
                        if( rowCnt >= start )
                        {
                            String key = Integer.toString( rowCnt );
                            if( !jsonData.has( key ) )
                            {
                                break;
                            }
                            JSONArray jsonRow = jsonData.getJSONArray( key );
                            for( int i = 0; i < jsonRow.length(); i++ )
                            {
                                rowValues[i] = jsonRow.getString( i );
                            }
                        }
                        TableDataCollectionUtils.addRow( tableDataCollection, Integer.toString( rowCnt ), rowValues );
                        rowCnt++;
                    }
                    return tableDataCollection;
                }
            }
            finally
            {
                if( journal != null && taskInfo != null )
                {
                    taskInfo.setEndTime();
                    journal.addAction( taskInfo );
                }
            }
            return null;
        }

        public static class SQLResponse extends Response
        {
            protected String error = null;
            protected String jsonString = null;

            public SQLResponse()
            {
                super( null, null );
            }

            @Override
            public void error(String message) throws IOException
            {
                error = message;
            }

            @Override
            public void send(byte[] message, int format) throws IOException
            {
                jsonString = new String( message, "UTF-16BE" );
            }

            public String getError()
            {
                return error;
            }

            public String getJsonString()
            {
                return jsonString;
            }
        }

        public String getQuery()
        {
            return sqlQuery;
        }
    }

    /**
     * Table resolver for column structure.
     * Is used in Columns view part
     */

    public static class ColumnsTableResolver extends TableResolver
    {
        protected TableResolver baseResolver;
        public ColumnsTableResolver(TableResolver baseResolver)
        {
            this.baseResolver = baseResolver;
        }

        @Override
        public DataCollection<?> getTable(DataElement de) throws Exception
        {
            if( baseResolver != null )
            {
                de = baseResolver.getTable( de );
            }
            TableDataCollection dc = de.cast( TableDataCollection.class );
            VectorDataCollection<TableElement> columns = new VectorDataCollection<>( "Columns", TableElement.class, null );
            columns.put( new TableElement( dc, -1 ) );
            int columnCount = dc.getColumnModel().getColumnCount();
            for( int i = 0; i < columnCount; i++ )
            {
                columns.put( new TableElement( dc, i ) );
            }
            return columns;
        }
    }



    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        JSONResponse response = new JSONResponse( resp );
        String action = arguments.getAction();

        if( action.equals( "createTable" ) )
        {
            DataElementPath path = arguments.getDataElementPath();
            JSONArray data = arguments.getJSONArray( "data" );
            JSONArray columns = arguments.getJSONArray( "columns" );
            createTable( path, data, columns );
            response.sendString( "ok" );
            return;
        }

        TableResolver resolver = null;
        String dePath = arguments.get( AccessProtocol.KEY_DE );
        String query = arguments.get( "query" );
        String path = null;
        if( dePath == null && query == null )
        {
            throw new WebException( "EX_QUERY_PARAM_MISSING_BOTH", "de", "query" );
        }

        if( query != null )
        {
            int from = arguments.optInt( "iDisplayStart", 0 );
            int count = arguments.optInt( "iDisplayLength", 1 );
            resolver = new SqlQueryTableResolver( query, from, count, action.equals( "sceleton" ) );
            path = "SqlQueryResult";
        }
        else if( dePath != null )
        {
            path = dePath;

            String type = arguments.get( "type" );
            if( type != null )
            {
                Class<? extends TableResolver> resolverClass = resolverRegistry.getExtension( type );
                if( resolverClass != null )
                {
                    resolver = resolverClass.getConstructor( BiosoftWebRequest.class ).newInstance( arguments );
                }
            }
            String type2 = arguments.get( "type2" );
            if( type2 != null )
            {
                if( type2.equals( "columns" ) )
                {
                    resolver = new ColumnsTableResolver( resolver );
                }
            }

            if( resolver == null )
            {
                DataElement de = CollectionFactory.getDataElement( path );
                int maxPriority = 0;
                for( Class<? extends TableResolver> resolverClass : resolverRegistry )
                {
                    TableResolver curResolver;
                    try
                    {
                        curResolver = resolverClass.getConstructor( BiosoftWebRequest.class ).newInstance( arguments );
                    }
                    catch( Exception e )
                    {
                        continue;
                    }
                    int priority = curResolver.accept( de );
                    if( priority > maxPriority )
                    {
                        maxPriority = priority;
                        resolver = curResolver;
                    }
                }
            }

            String cachedResolverName = arguments.get( "cached" );
            if( cachedResolverName != null )
            {
                //try to get table resolver from cache
                resolver = (TableResolver)WebServicesServlet.getSessionCache().getObject( cachedResolverName );
            }
        }
        boolean isReadMode = arguments.getBoolean( "read" );
        DataCollection<?> dc;
        try
        {
            dc = getTable( path, resolver );
        }
        catch( Exception e )
        {
            if( action.equals( "sceleton" ) || action.equals( "datatables" ) )
            {
                dc = new MessageStubTableDataCollection( "Unable to get table: " + ExceptionRegistry.log( e ) );
            }
            else
            {
                throw new WebException( "EX_QUERY_NO_TABLE_RESOLVED", path, ExceptionRegistry.log( e ) );
            }
        }
        if( dc == null )
        {
            throw new WebException( "EX_QUERY_NO_TABLE", path );
        }
        TableQueryResponse queryResponse = new TableQueryResponse( dc, resolver, arguments, resp.getOutputStream() );
        if( action.equals( "sceleton" ) )
        {
            queryResponse.sendTableSceleton();
        }
        else if( action.equals( "datatables" ) )
        {
            queryResponse.sendTableData( path, isReadMode );
        }
        else if( action.equals( "rawdata" ) )
        {
            queryResponse.sendRawData();
        }
        else if( action.equals( "cell" ) )
        {
            queryResponse.sendCellData();
        }
        else if( action.equals( "checkFilter" ) )
        {
            checkFilter( dc, arguments.get( "filter" ) );
            response.sendString( "ok" );
        }
        else if( action.equals( "change" ) )
        {
            queryResponse.sendChangeData( path, arguments.getJSONArray( "data" ), arguments.optJSONArray( "rowids" ) );
        }
        else if( action.equals( "columns" ) )
        {
            String colaction = arguments.get( "colaction" );
            if( colaction == null )
            {
                sendTableColumns( dc, response );
            }
            else if( "add".equals( colaction ) )
            {
                addColumn( getMutableTableDataCollection( dc ) );
                response.sendString( "ok" );
            }
            else if( "addDescriptor".equals( colaction ) )
            {
                Descriptor de = arguments.getDataElement( Descriptor.class, "descriptor" );
                addDescriptorColumn( getMutableTableDataCollection( dc ), de );
                response.sendString( "ok" );
            }
            else if( "remove".equals( colaction ) )
            {
                removeColumn( getMutableTableDataCollection( dc ), queryResponse.getColumnNames( arguments ), response );
            }
            else if( "toValues".equals( colaction ) )
            {
                convertExpressionsToValues( getMutableTableDataCollection( dc ), queryResponse.getColumnNames( arguments ) );
                response.sendString( "ok" );
            }
            else
            {
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "colaction" );
            }
        }
        else if( action.equals( "export" ) )
        {
            String jobID = arguments.get("jobID");
            WebJob wj = WebJob.getWebJob( jobID );
            queryResponse.exportFilteredTable( dc, arguments.getDataElementPath( "exportTablePath" ), wj );
        }
        else if( action.equals( "sortOrder" ) )
        {
            sendSortOrder( dc, response );
        }
        else
            throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION );
    }
}
