package ru.biosoft.analysis;

import java.util.Properties;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
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
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon("resources/TransformTable.png")
public class TableTransform extends AnalysisMethodSupport<TableTransform.Parameters>
{

    public TableTransform(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        ColumnGroup columnGroup = parameters.getColumnGroup();
        TableDataCollection table = columnGroup.getTable();


        TableDataCollection outTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        DataCollectionUtils.copyPersistentInfo( outTable, table );

        ColumnModel cm = table.getColumnModel();
        ColumnModel outCM = outTable.getColumnModel();
        for( int i = 0; i < cm.getColumnCount(); i++ )
        {
            TableColumn col = cm.getColumn( i );
            outCM.addColumn( outCM.cloneTableColumn( col ) );
        }

        int[] columns = new int[columnGroup.getColumns().length];
        for( int i = 0; i < columns.length; i++ )
        {
            columns[i] = cm.getColumnIndex( columnGroup.getColumns()[i].getName() );
            TableColumn col = outCM.getColumn( columns[i] );
            col.setType( DataType.Float );
        }

        Operation operation = Operation.valueOf( parameters.getOperation() );
        for( RowDataElement rde : table )
        {
            RowDataElement transformedRDE = new RowDataElement( rde.getName(), outTable );
            Object[] values = rde.getValues();
            Object[] newValues = new Object[values.length];
            for( int i = 0; i < values.length; i++ )
                newValues[i] = values[i];
            for( int i = 0; i < columns.length; i++ )
            {
                int colIdx = columns[i];
                if( newValues[colIdx] != null )
                    newValues[colIdx] = operation.apply( (Number)newValues[colIdx] );
                else
                    newValues[colIdx] = Double.NaN;
            }
            transformedRDE.setValues( newValues );
            outTable.addRow( transformedRDE );
        }

        outTable.finalizeAddition();
        parameters.getOutputTable().save( outTable );
        return outTable;
    }

    public static enum Operation
    {
        Log2 ( x -> Math.log( x.doubleValue() ) / Math.log( 2 ) ),
        Log10 ( x -> Math.log10( x.doubleValue() ) ),
        Pow2 ( x -> Math.pow( 2, x.doubleValue() ) ),
        Pow10 ( x -> Math.pow( 10, x.doubleValue() ) ),
        Exp ( x -> Math.exp( x.doubleValue() ) );

        private Function<Number, Number> function;
        private Operation(Function<Number, Number> fun)
        {
            this.function = fun;
        }

        public Number apply(Number x)
        {
            return function.apply( x );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private ColumnGroup columnGroup;

        public Parameters()
        {
            setColumnGroup( new ColumnGroup( this ) );
        }
        @PropertyName("Input table")
        public ColumnGroup getColumnGroup()
        {
            return columnGroup;
        }
        public void setColumnGroup(ColumnGroup group)
        {
            columnGroup = group;
            if( columnGroup != null )
            {
                columnGroup.setParent( this );
                columnGroup.setNumerical( true );
            }
            firePropertyChange( "*", null, null );
        }

        private String operation = Operation.Log2.toString();
        @PropertyName("Operation")
        public String getOperation()
        {
            return operation;
        }
        public void setOperation(String operation)
        {
            Object oldValue = this.operation;
            this.operation = operation;
            firePropertyChange( "operation", oldValue, operation );
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

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read( properties, prefix );
            String columnGroupStr = properties.getProperty( prefix + "columnGroup" );
            if( columnGroupStr != null )
            {
                columnGroup = ColumnGroup.readObject( this, columnGroupStr );
            }
        }

        @Override
        public @Nonnull String[] getInputNames()
        {
            return new String[] {"columnGroup/tablePath"};
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
            add( "columnGroup" );
            property( "operation" ).tags( StreamEx.of( Operation.values() ).map( x->x.toString() ).toArray( String[]::new ) ).add();
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$columnGroup/tablePath$ transformed" ).add();
        }
    }
}
