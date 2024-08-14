package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon ( "resources/MergeTableColumns.gif" )
public class MergeTableColumns extends AnalysisMethodSupport<MergeTableColumns.Parameters>
{
    public MergeTableColumns(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        if(parameters.getTableColumns().getColumns().length < 2)
            throw new IllegalArgumentException("Select at least two columns for merging");
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection table = parameters.getTableColumns().getTable();
        TableDataCollection out = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        DataCollectionUtils.copyPersistentInfo( out, table );
        
        ColumnModel origColModel = table.getColumnModel();
        ColumnModel outColModel = out.getColumnModel();
        
        boolean[] merge = new boolean[origColModel.getColumnCount()];
        int mergeColumnCount = 0;
        for(Column col : parameters.getTableColumns().getColumns())
        {
            int colId = origColModel.getColumnIndex( col.getName() );
            merge[colId] = true;
            mergeColumnCount++;
        }
        
        outColModel.addColumn( parameters.getMergedColumnName(), DataType.Float );
        int otherColumnCount = 0;
        for(int i = 0; i < merge.length; i++)
            if(!merge[i])
            {
                TableColumn origCol = origColModel.getColumn( i );
                outColModel.addColumn( origCol.getName(), origCol.getType() );
                otherColumnCount++;
            }
        
        int totalRows = table.getSize();
        int processedRows = 0;
        
        for(RowDataElement origRow : table)
        {
            Object[] origValues = origRow.getValues();
            
            
            double[] valuesToMerge = new double[mergeColumnCount];
            Object[] newValues = new Object[1 + otherColumnCount];
            int otherIdx = 1;
            int mergeIdx = 0;
            for(int i = 0; i < merge.length; i++)
                if(merge[i])
                {
                    valuesToMerge[mergeIdx++] = ((Number)origValues[i]).doubleValue();
                }else
                {
                    newValues[otherIdx++] = origValues[i];
                }
            newValues[0] = parameters.getAggregator().aggregate( valuesToMerge );
                
            RowDataElement newRow = new RowDataElement( origRow.getName(), out );
            newRow.setValues( newValues );
            out.addRow( newRow );
            
            processedRows++;
            jobControl.setPreparedness( (int)( processedRows*100d / totalRows ) );
        }
        
        out.finalizeAddition();
        parameters.getOutputTable().save( out );
        return out;
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        public Parameters()
        {
            tableColumns = new ColumnGroup( this );
            tableColumns.setNumerical( true );
            setAggregator(NumericAggregator.getAggregators()[0]);
        }
        
        private ColumnGroup tableColumns;
        @PropertyName("Table columns")
        public ColumnGroup getTableColumns()
        {
            return tableColumns;
        }
        public void setTableColumns(ColumnGroup tableColumns)
        {
            Object oldValue = this.tableColumns;
            this.tableColumns = tableColumns;
            firePropertyChange( "tableColumns", oldValue, tableColumns );
        }

        private boolean ignoreNaNInAggregator;
        @PropertyName ( "Ignore empty values" )
        public boolean isIgnoreNaNInAggregator()
        {
            return ignoreNaNInAggregator;
        }
        public void setIgnoreNaNInAggregator(boolean ignoreNaNInAggregator)
        {
            boolean oldValue = this.ignoreNaNInAggregator;
            this.ignoreNaNInAggregator = ignoreNaNInAggregator;
            firePropertyChange( "ignoreNaNInAggregator", oldValue, ignoreNaNInAggregator );
            if( aggregator != null )
                aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
        }

        private NumericAggregator aggregator;
        public NumericAggregator getAggregator()
        {
            return aggregator;
        }
        public void setAggregator(NumericAggregator aggregator)
        {
            if( aggregator != null )
                aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
            Object oldValue = this.aggregator;
            this.aggregator = aggregator;
            firePropertyChange("aggregator", oldValue, aggregator);
        }
        
        private String mergedColumnName = "Merged";
        @PropertyName("Merged column name")
        @PropertyDescription("The name of merged column in output table")
        public String getMergedColumnName()
        {
            return mergedColumnName;
        }
        public void setMergedColumnName(String mergedColumnName)
        {
            Object oldValue = this.mergedColumnName;
            this.mergedColumnName = mergedColumnName;
            firePropertyChange( "mergedColumnName", oldValue, mergedColumnName );
        }

        private DataElementPath outputTable;
        @PropertyName("Output table")
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }
        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
        }
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("tableColumns");
            property( "ignoreNaNInAggregator" ).descriptionRaw( "Ignore empty values during aggregator work" ).add();
            property("aggregator").simple().editor( NumericAggregatorEditor.class ).add();
            add("mergedColumnName");
            property("outputTable").outputElement( TableDataCollection.class ).auto( "$tableColumns/tablePath$ merged" ).add();
        }
    }
}
