package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon ( "resources/table_imputation.gif" )
public class TableImputation extends AnalysisMethodSupport<TableImputation.Parameters>
{

    public TableImputation(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters());
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection outTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        TableDataCollection inputTable = parameters.getInputTable().getDataElement(TableDataCollection.class);
        
        ColumnModel inCM = inputTable.getColumnModel();
        ColumnModel outCM = outTable.getColumnModel();
        List<Integer> numericalColumns = new ArrayList<>();
        for(int i = 0; i < inCM.getColumnCount(); i++)
        {
            TableColumn col = inCM.getColumn( i );
            outCM.addColumn( col );
            if( col.getType().isNumeric() )
                numericalColumns.add( i );
        }
        
        for(RowDataElement row : inputTable)
        {
            Object[] values = row.getValues();
            values = imputeRow( values, numericalColumns );
            if(values == null)
                continue;
            RowDataElement newRow = new RowDataElement( row.getName(), outTable );
            newRow.setValues( values );
            outTable.addRow( newRow );
        }
        outTable.finalizeAddition();
        DataCollectionUtils.copyPersistentInfo(outTable, inputTable);
        parameters.getOutputTable().save( outTable );
        
        return outTable;
    }

    //TODO: 1) save real column type (to save integer values for columns with integer values
    //      2) rework in a better way
    private Object[] imputeRow(Object[] values, List<Integer> numericalColumns)
    {
        double mean = 0;
        int numberCount = 0, finiteCount = 0;
        
        for( int i : numericalColumns )
        {
            numberCount++;
            if( values[i] instanceof Number )
            {
                double v = ( (Number)values[i] ).doubleValue();
                if( Double.isFinite( v ) )
                {
                    finiteCount++;
                    mean += v;
                }
            }
        }
        if(numberCount == finiteCount)
            return values;
        
        if(finiteCount < parameters.getMinFiniteCount())
            return null;
        
        mean /= finiteCount;
        Object[] res = new Object[values.length];
        for( int i : numericalColumns )
        {
            if(values[i] instanceof Number)
            {
                double v = ((Number)values[i]).doubleValue();
                if(!Double.isFinite( v ))
                    res[i] = mean;
                else
                    res[i] = v;
            }
            else
                res[i] = mean;
        }
        
        return res;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTable;
        @PropertyName("Input table")
        public DataElementPath getInputTable()
        {
            return inputTable;
        }
        public void setInputTable(DataElementPath inputTable)
        {
            Object oldValue = this.inputTable;
            this.inputTable = inputTable;
            firePropertyChange( "inputTable", oldValue, inputTable );
        }
        
        private int minFiniteCount = 2;
        @PropertyName("Min finite count")
        @PropertyDescription("Minimal number of finite values in a row to be used for imputation, otherwise row will be removed/")
        public int getMinFiniteCount()
        {
            return minFiniteCount;
        }
        public void setMinFiniteCount(int minFiniteCount)
        {
            int oldValue = this.minFiniteCount;
            this.minFiniteCount = minFiniteCount;
            firePropertyChange( "minFiniteCount", oldValue, minFiniteCount );
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
            property("inputTable").inputElement( TableDataCollection.class ).add();
            add("minFiniteCount");
            property("outputTable").outputElement( TableDataCollection.class ).add();
        }
    }
}
