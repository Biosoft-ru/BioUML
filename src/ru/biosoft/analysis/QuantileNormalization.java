package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class QuantileNormalization extends AnalysisMethodSupport<QuantileNormalization.Parameters>
{
    public QuantileNormalization(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection inputTable = parameters.getInputTable().getDataElement( TableDataCollection.class );
        List<String> nameList = new ArrayList<>();
        double[][] data = loadTable( inputTable, nameList );

        int nCols = data.length;
        int nRows = data[0].length;
        
        int[][] ranks = new int[nCols][];
        for( int iCol = 0; iCol < nCols; iCol++ )
            ranks[iCol] = computeRanks( data[iCol] );
        
        double[] mean = new double[nRows];//mean[i] is the mean of values having rank=i
        int[] size = new int[nRows];
        for(int iCol = 0; iCol < nCols; iCol++)
            for(int iRow = 0; iRow < nRows; iRow++)
            {
                int r = ranks[iCol][iRow];
                double v = data[iCol][iRow];
                if(Double.isFinite( v ))
                {
                    mean[r] += v;
                    size[r]++;
                }
            }
        for(int i = 0; i < nRows; i++)
            if(size[i] == 0)
                mean[i] = Double.NaN;
            else
                mean[i] /= size[i];
        
        for(int iCol = 0; iCol < nCols; iCol++)
            for(int iRow = 0; iRow < nRows; iRow++)
                data[iCol][iRow] = mean[ranks[iCol][iRow]];
        
        TableDataCollection result = createTableLike( inputTable, parameters.getOutputTable() );
        fillTable(result, data, nameList);
        parameters.getOutputTable().save( result );
        
        return result;
    }

    private void fillTable(TableDataCollection result, double[][] data, List<String> nameList) throws Exception
    {
        int nCols = data.length;
        int nRows = data[0].length;
        for(int iRow = 0; iRow < nRows; iRow++)
        {
            RowDataElement row = new RowDataElement( nameList.get( iRow ), result );
            Object[] values = new Object[nCols];
            for(int iCol = 0; iCol < nCols; iCol++)
                values[iCol] = data[iCol][iRow];
            row.setValues( values  );
            result.addRow( row  );
        }
        result.finalizeAddition();
    }

    private TableDataCollection createTableLike(TableDataCollection source, DataElementPath targetPath)
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( targetPath );
        for(TableColumn col : source.getColumnModel())
            result.getColumnModel().addColumn( col );
        DataCollectionUtils.copyPersistentInfo( result, source );
        return result;
    }

    private double[][] loadTable(TableDataCollection table, List<String> nameList)
    {
        int nColumns = table.getColumnModel().getColumnCount();
        int nRows = table.getSize();
        double[][] result = new double[nColumns][nRows];
        int iRow = 0;
        for( RowDataElement row : table )
        {
            Object[] values = row.getValues();
            for( int i = 0; i < values.length; i++ )
            {
                if( values[i] instanceof Number )
                    result[i][iRow] = ( (Number)values[i] ).doubleValue();
                else
                    result[i][iRow] = Double.NaN;
            }
            iRow++;
            nameList.add( row.getName() );
        }
        return result;
    }

    private int[] computeRanks(double[] values)
    {
        Integer[] indices = new Integer[values.length];
        for( int i = 0; i < indices.length; i++ )
            indices[i] = i;
        Arrays.sort( indices, Comparator.comparingDouble( i -> values[i] ) );
        int[] ranks = new int[values.length];
        for( int i = 0; i < indices.length; i++ )
            ranks[indices[i]] = i;
        return ranks;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTable;
        @PropertyName ( "Input table" )
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

        private DataElementPath outputTable;
        @PropertyName ( "Output table" )
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
            property("outputTable").outputElement( TableDataCollection.class ).auto( "$inputTable$ quantile normalized" ).add();
        }
    }
}
