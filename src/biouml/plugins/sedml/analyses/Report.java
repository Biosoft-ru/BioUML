package biouml.plugins.sedml.analyses;

import java.util.LinkedHashMap;
import java.util.Map;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class Report extends GenerateReport<Report.Parameters>
{
    public Report(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        return generateTable();
    }
    
    private TableDataCollection generateTable()
    {
        DataElementPath resultPath = parameters.getOutputTable();
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( resultPath );
        
        Map<String, double[]> table = new LinkedHashMap<>();
        for( Column column : parameters.getColumns() )
        {
            Map<String, double[]> values = evaluateExpression( column.getExpression() );
            for( Map.Entry<String, double[]> entry : values.entrySet() )
                table.put( ( entry.getKey() + column.getName() ).replace( '/', '_' ), entry.getValue() );
        }
        
        for( String colName : table.keySet() )
            result.getColumnModel().addColumn( colName, DataType.Float );
        
        if(!table.isEmpty())
        {
            int nRows = table.values().iterator().next().length;
            for( int i = 0; i < nRows; i++ )
            {
                int row = i;
                Object[] rowValues = StreamEx.ofValues( table ).map( col->col[row] ).toArray();
                TableDataCollectionUtils.addRow( result, String.valueOf( i ), rowValues, true );
            }
        }
        
        result.finalizeAddition();
        resultPath.save( result );
        return result;
    }
    

    public static class Parameters extends GenerateReportParameters
    {
        private Column[] columns;
        private DataElementPath outputTable;

        public Parameters()
        {
            setColumns( new Column[] {new Column()} );
        }

        @PropertyName ( "Columns" )
        @PropertyDescription ( "Columns." )
        public Column[] getColumns()
        {
            return columns;
        }
        public void setColumns(Column[] columns)
        {
            Object oldValue = this.columns;
            this.columns = columns;
            for(Column column : columns)
                column.setParent( this );
            firePropertyChange( "columns", oldValue, columns );
        }

        @PropertyName ( "Output table" )
        @PropertyDescription ( "Output table." )
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
    
    public static class ParametersBeanInfo extends GenerateReportParametersBeanInfo<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            add( "columns" );
            property( "outputTable" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
