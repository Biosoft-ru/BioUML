package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.columnbeans.ColumnNamesSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

@ClassIcon("resources/SuperAnnotateTable.png")
public class SuperAnnotateTable extends AnalysisMethodSupport<SuperAnnotateTable.SuperAnnotateTableParameters>
{

    public SuperAnnotateTable(DataCollection<?> origin, String name)
    {
        super( origin, name, new SuperAnnotateTableParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();

        TableDataCollection input = parameters.getInputTable().getDataElement( TableDataCollection.class );
        String idColumnName = parameters.getIdColumn();
        if( idColumnName == null )
            throw new IllegalArgumentException( "Column with IDs to annotate by must be specified." );
        ColumnModel columnModel = input.getColumnModel();
        if( !useRowNameAsId( columnModel, parameters.getIdColumn() ) )
        {
            TableColumn idColumn = columnModel.getColumn( idColumnName );
            Class<?> type = idColumn.getType().getType();
            if( !String.class.isAssignableFrom( type ) && !StringSet.class.isAssignableFrom( type ) )
                throw new IllegalArgumentException( "Column with IDs to annotate by must be Text or Set." );
        }
    }

    private static boolean useRowNameAsId(ColumnModel cm, String columnName)
    {
        if( ColumnNameSelector.ID_COLUMN_SHORT.equals( columnName ) )
            return !cm.hasColumn( ColumnNameSelector.ID_COLUMN_SHORT );
        return ColumnNameSelector.ID_COLUMN_FULL.equals( columnName );
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 2 );
        TableDataCollection input = parameters.getInputTable().getDataElement( TableDataCollection.class );
        ColumnModel inputColumnModel = input.getColumnModel();
        String idColumnName = parameters.getIdColumn();
        boolean useRowName = useRowNameAsId( inputColumnModel, idColumnName );
        int idColumnNumber = inputColumnModel.optColumnIndex( idColumnName );

        log.info( "Creating result table stub." );
        TableDataCollection result = initResultTable( input );

        if( input.isEmpty() )
        {
            log.info( "Input table is empty." );
            return result;
        }

        RowDataElement rdeTest = input.stream().findAny().get();
        int initSize = rdeTest.getValues().length;
        String idColumnType = useRowName ? String.class.getName() : rdeTest.getValues()[idColumnNumber].getClass().getName();
        boolean isStringSet = StringSet.class.getName().equals( idColumnType );

        jobControl.popProgress();
        if( jobControl.isStopped() )
        {
            dropIncomleteResult();
            return null;
        }
        jobControl.pushProgress( 2, 5 );

        log.info( "Adding new columns to result table." );
        boolean hasMultipleIds = checkMultipleIds( input, isStringSet, useRowName, idColumnNumber );
        int addSize = createNewColumnsAndMapping( inputColumnModel, result, isStringSet, hasMultipleIds );
        jobControl.popProgress();

        if( jobControl.isStopped() )
        {
            dropIncomleteResult();
            return null;
        }

        log.info( "Annotation started." );
        jobControl.pushProgress( 5, 90 );
        jobControl.forCollection( DataCollectionUtils.asCollection( input, RowDataElement.class ), (RowDataElement rde) -> {
            Object[] oldValues = rde.getValues();
            Object idsValue = useRowName ? rde.getName() : oldValues[idColumnNumber];
            List<Object> annotations = fillAnnotationValues( idsValue, isStringSet );

            Object[] newValues = new Object[initSize + addSize];
            for( int i = 0; i < initSize; i++ )
                newValues[i] = oldValues[i];
            //size of annotations equals to the total number of ColumnMapper elements (equals to addSize)
            for( int i = 0; i < addSize; i++ )
                newValues[i + initSize] = annotations.get( i );

            TableDataCollectionUtils.addRow( result, rde.getName(), newValues, true );
            return true;
        } );

        jobControl.popProgress();
        if( jobControl.isStopped() )
        {
            dropIncomleteResult();
            return null;
        }
        jobControl.pushProgress( 90, 100 );

        log.info( "Saving results (this may take a while)..." );
        result.finalizeAddition();
        parameters.getOutputTable().save( result );

        jobControl.popProgress();
        return result;
    }

    //Check if String column with ids to annotate contains multiple ids 
    private boolean checkMultipleIds(TableDataCollection input, boolean isStringSet, boolean useRowName, int idColumnNumber)
    {
        if( useRowName )
            return false;
        if( isStringSet )
            return true;
        for( RowDataElement rde : input )
        {
            Object idsValue = rde.getValues()[idColumnNumber];
            String[] ids = TableDataCollectionUtils.splitIds( (String)idsValue );
            if( ids.length > 1 )
                return true;
        }
        return false;
    }

    private void dropIncomleteResult()
    {
        parameters.getOutputTable().remove();
    }

    private List<Object> fillAnnotationValues(@CheckForNull Object idsValue, boolean isStringSet)
    {
        List<Object> annotations = new ArrayList<>();
        for( SourceTable sourceTable : parameters.getAnnotationTables() )
        {
            if( sourceTable.getTable() == null )
                continue;
            Map<String, Map<String, String>> annotationCache = sourceTable.getCache();
            List<ColumnMapper> mapping = sourceTable.getColumnMapping();

            for( ColumnMapper cm : mapping )
            {
                Map<String, String> cache = annotationCache.computeIfAbsent( cm.newName, name -> new HashMap<>() );
                Object annotatioValue;
                if( idsValue == null )
                {
                    annotations.add( null );
                    continue;
                }
                if( isStringSet )
                {
                    annotatioValue = constructAnnotations( cache, (StringSet)idsValue, sourceTable.getTable(), cm.columnIndex );
                }
                else
                {
                    String[] ids = TableDataCollectionUtils.splitIds( (String)idsValue );
                    annotatioValue = constructAnnotations( cache, Arrays.asList( ids ), sourceTable.getTable(), cm.columnIndex ).stream()
                            .joining( "," );
                }
                annotations.add( annotatioValue );
            }
        }
        return annotations;
    }

    private int createNewColumnsAndMapping(ColumnModel inputColumnModel, TableDataCollection result, boolean isStringSet,
            boolean hasMultipleIds)
    {
        Set<String> existingColumns = inputColumnModel.stream().map( tc -> tc.getName() ).toSet();
        int addSize = 0;
        for( SourceTable st : parameters.getAnnotationTables() )
        {
            TableDataCollection table = st.getTable();
            if( table == null )
                continue;
            String[] columns = st.getAnnotationColumns();
            for( String columnName : columns )
            {
                TableColumn tc = table.getColumnModel().getColumn( columnName );
                if( !tc.getType().isNumeric() && !StringSet.class.getName().equals( tc.getType().getType().getName() )
                        && !String.class.getName().equals( tc.getType().getType().getName() ) )
                {
                    log.warning( "Analysis can not be applied to the column '" + columnName + "' from the table '" + table.getName()
                            + "' due to it type. It will be skipped." );
                    continue;
                }

                String newName = getUniqueColumnName( columnName, table.getName(), existingColumns );
                existingColumns.add( newName );
                int annotIndex = table.getColumnModel().getColumnIndex( columnName );
                st.addColumnMapping( columnName, newName, annotIndex );
                addSize++;
                if( isStringSet )
                    result.getColumnModel().addColumn( newName, StringSet.class );
                else if( hasMultipleIds )
                    result.getColumnModel().addColumn( newName, String.class );
                else
                    result.getColumnModel().addColumn( newName, tc.getType() );
            }
        }
        return addSize;
    }

    private @Nonnull StringSet constructAnnotations(Map<String, String> cache, Collection<String> ids, TableDataCollection tdc,
            int columnIndex)
    {
        StringSet annotationSet = new StringSet();
        for( String id : ids )
        {
            String annotationStr = cache.computeIfAbsent( id.trim(), idStr -> getAnnotation( idStr, tdc, columnIndex ) );
            if( !annotationStr.isEmpty() )
                annotationSet.add( annotationStr );
        }
        return annotationSet;
    }

    private String getAnnotation(String id, TableDataCollection tdc, int columnIndex)
    {
        try
        {
            RowDataElement rde = tdc.get( id );
            if( rde == null || columnIndex == -1 )
                return "";
            return rde.getValues()[columnIndex].toString();
        }
        catch( Exception e )
        {
            return "";
        }
    }

    private static String getUniqueColumnName(String newColumn, String tableName, Set<String> existingNames)
    {
        String defaultName = newColumn;
        if( !existingNames.contains( defaultName ) )
            return defaultName;
        defaultName += " (" + tableName + ")";
        int i = 1;
        String result = defaultName;
        while( existingNames.contains( result ) )
            result = defaultName + "_" + i;
        return result;
    }

    private @Nonnull TableDataCollection initResultTable(TableDataCollection input)
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        ColumnModel columnModel = input.getColumnModel();
        ColumnModel resColumnModel = result.getColumnModel();
        for( int i = 0; i < columnModel.getColumnCount(); i++ )
        {
            TableColumn col = columnModel.getColumn( i );
            resColumnModel.addColumn( resColumnModel.cloneTableColumn( col ) );
        }
        ReferenceTypeRegistry.copyCollectionReferenceType( result, input );
        return result;
    }

    @SuppressWarnings ( "serial" )
    public static class SourceTable extends Option implements JSONBean
    {
        public SourceTable()
        {
        }
        public SourceTable(Option parent)
        {
            super( parent );
        }


        private DataElementPath tablePath;
        private String[] annotationColumns;

        @PropertyName ( "Source table path" )
        public DataElementPath getTablePath()
        {
            return tablePath;
        }
        public void setTablePath(DataElementPath tablePath)
        {
            DataElementPath oldValue = this.tablePath;
            this.tablePath = tablePath;
            firePropertyChange( "tablePath", oldValue, tablePath );
        }

        @PropertyName ( "Source annotation columns" )
        public @Nonnull String[] getAnnotationColumns()
        {
            return annotationColumns != null ? annotationColumns : new String[0];
        }
        public void setAnnotationColumns(String[] annotationColumns)
        {
            String[] oldValue = this.annotationColumns;
            this.annotationColumns = annotationColumns;
            firePropertyChange( "annotationColumns", oldValue, annotationColumns );
        }

        private TableDataCollection table = null;
        public @CheckForNull TableDataCollection getTable()
        {
            if( tablePath == null )
                return null;
            if( table == null )
                table = tablePath.getDataElement( TableDataCollection.class );
            return table;
        }
        private final Map<String, Map<String, String>> annotationCache = new HashMap<>();
        public Map<String, Map<String, String>> getCache()
        {
            return annotationCache;
        }
        private final List<ColumnMapper> mapping = new ArrayList<>();
        public void addColumnMapping(String initName, String newName, int columnIndex)
        {
            mapping.add( new ColumnMapper( initName, newName, columnIndex ) );
        }
        public List<ColumnMapper> getColumnMapping()
        {
            return mapping;
        }

        @Override
        public String toString()
        {
            JSONObject obj = new JSONObject();
            if( tablePath != null )
                obj.put( "tablePath", tablePath.toString() );
            if( annotationColumns != null )
            {
                JSONArray columns = new JSONArray();
                for( String col : annotationColumns )
                    columns.put( col );
                obj.put( "columns", columns );
            }
            return obj.toString();
        }

        public static SourceTable[] readObjects(String jsonStr)
        {
            if( jsonStr == null )
                return new SourceTable[0];
            List<SourceTable> result = new ArrayList<>();
            JSONArray list = new JSONArray( jsonStr );
            for( int j = 0; j < list.length(); j++ )
            {
                SourceTable st = new SourceTable();
                JSONObject obj = new JSONObject( list.optString( j ) );
                String path = obj.optString( "tablePath" );
                if( !path.isEmpty() )
                    st.setTablePath( DataElementPath.create( path ) );
                JSONArray columns = obj.optJSONArray( "columns" );
                if( columns != null )
                {
                    String[] cols = new String[columns.length()];
                    for( int i = 0; i < columns.length(); i++ )
                        cols[i] = columns.getString( i );
                    st.setAnnotationColumns( cols );
                }
                result.add( st );
            }
            return result.toArray( new SourceTable[0] );
        }
    }

    public static class SourceTableBeanInfo extends BeanInfoEx2<SourceTable>
    {
        public SourceTableBeanInfo()
        {
            super( SourceTable.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "tablePath" ).inputElement( TableDataCollection.class ).canBeNull().add();
            add( ColumnNamesSelector.registerSelector( "annotationColumns", beanClass, "tablePath" ) );
        }
    }

    @SuppressWarnings ( "serial" )
    public static class SourceTables extends Option
    {
        private SourceTable[] tables;
        @PropertyName ( "Tables to annotate with" )
        public @Nonnull SourceTable[] getTables()
        {
            return tables != null ? tables : new SourceTable[0];
        }
        public void setTables(SourceTable[] annotationTables)
        {
            SourceTable[] oldValue = this.tables;
            this.tables = annotationTables;
            if( annotationTables != null )
                for( SourceTable st : annotationTables )
                    if( st != null )
                        st.setParent( this );
            firePropertyChange( "tables", oldValue, annotationTables );
            firePropertyChange( "*", null, null );
        }
    }

    public static class SourceTablesBeanInfo extends BeanInfoEx2<SourceTables>
    {
        public SourceTablesBeanInfo()
        {
            super( SourceTables.class );
            this.setSubstituteByChild( true );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "tables" );
        }
    }

    public static class ColumnMapper
    {
        final String initialName;
        final String newName;
        final int columnIndex;
        public ColumnMapper(String initialName, String newName, int columnIndex)
        {
            this.initialName = initialName;
            this.newName = newName;
            this.columnIndex = columnIndex;
        }
    }

    @SuppressWarnings ( "serial" )
    public static class SuperAnnotateTableParameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTable;
        private String idColumn;
        private SourceTable[] annotationTables;
        @PropertyName ( "Tables to annotate with" )
        public SourceTable[] getAnnotationTables()
        {
            return annotationTables != null ? annotationTables : new SourceTable[0];
        }

        public void setAnnotationTables(SourceTable[] annotationTables)
        {
            SourceTable[] oldValue = this.annotationTables;
            this.annotationTables = annotationTables;
            if( annotationTables != null )
                for( SourceTable st : annotationTables )
                    if( st != null )
                        st.setParent( this );
            firePropertyChange( "annotationTables", oldValue, annotationTables );
            firePropertyChange( "*", null, null );
        }

        private DataElementPath outputTable;

        @PropertyName ( "Target table" )
        public DataElementPath getInputTable()
        {
            return inputTable;
        }

        public void setInputTable(DataElementPath inputTable)
        {
            DataElementPath oldValue = this.inputTable;
            this.inputTable = inputTable;
            firePropertyChange( "inputTable", oldValue, inputTable );
        }

        @PropertyName ( "Column with IDs to annotate by" )
        public String getIdColumn()
        {
            return idColumn;
        }
        public void setIdColumn(String idColumn)
        {
            String oldValue = this.idColumn;
            this.idColumn = idColumn;
            firePropertyChange( "idColumn", oldValue, idColumn );
        }

        @PropertyName ( "Result table" )
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }
        public void setOutputTable(DataElementPath outputTable)
        {
            DataElementPath oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read( properties, prefix );
            String value = properties.getProperty( prefix + "annotationTables" );
            if( value != null )
            {
                annotationTables = SourceTable.readObjects( value );
                if( annotationTables != null )
                {
                    if( annotationTables != null )
                        for( SourceTable st : annotationTables )
                            if( st != null )
                                st.setParent( this );
                }
            }
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            super.write( properties, prefix );

            JSONArray list = new JSONArray();
            if( annotationTables != null )
            {
                for( SourceTable st : getAnnotationTables() )
                {
                    list.put( st.toString() );
                }
            }
            properties.put( prefix + "annotationTables", list.toString() );

        }
    }

    public static class SuperAnnotateTableParametersBeanInfo extends BeanInfoEx2<SuperAnnotateTableParameters>
    {
        public SuperAnnotateTableParametersBeanInfo()
        {
            super( SuperAnnotateTableParameters.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "inputTable" ).inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerTextOrSetSelector( "idColumn", beanClass, "inputTable", true, true ) );
            property( "annotationTables" ).structureChanging().add();
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$inputTable$ annotated" ).add();
        }
    }
}
