package ru.biosoft.table;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.ProtectedSqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.columnbeans.Descriptor;
import ru.biosoft.table.datatype.DataType;

public class JavaScriptTable extends JavaScriptHostObjectBase
{
    public TableDataCollection create(String path) throws Exception
    {
        TableDataCollection t = TableDataCollectionUtils.createTableDataCollection(DataElementPath.create(path));
        if( ! ( t instanceof StandardTableDataCollection ) )
            CollectionFactoryUtils.save(t);
        return t;
    }
    
    public TableDataCollection query(String connectionHolder, String query) throws Exception
    {
        return query(connectionHolder, query, null);
    }

    public TableDataCollection query(String connectionHolder, String query, String nameListQuery) throws Exception
    {
        DataCollection<?> collection = CollectionFactory.getDataCollection(connectionHolder);
        if( collection == null )
            throw new IllegalArgumentException("Cannot find connection holder: " + connectionHolder);
        SqlConnectionHolder holder = new ProtectedSqlConnectionHolder(collection);
        return new SqlQueryTableDataCollection("", holder, query, nameListQuery);
    }

    public void addRow(TableDataCollection table, String name, Object[] data)
    {
        TableDataCollectionUtils.addRow(table, name, data);
    }

    /**
     * The method stores rows in memory and saves in the table when {@link JavaScriptTable#writeBatch(TableDataCollection)} method is called.
     */
    public void addBatchRow(TableDataCollection table, String name, Object[] data)
    {
        TableDataCollectionUtils.addRow(table, name, data, true);
    }
    
    public void writeBatch(TableDataCollection table)
    {
        table.finalizeAddition();
    }

    public void addColumn(TableDataCollection table, String name, String type)
    {
        table.getColumnModel().addColumn(name, DataType.fromString(type));
    }

    public void addDescriptor(TableDataCollection table, String name, String descriptorPath)
    {
        Descriptor de = DataElementPath.create(descriptorPath).getDataElement(Descriptor.class);
        table.getColumnModel().addColumn(name, de);
    }
    
    
    public void cleanRows(TableDataCollection table, String column, double startFrom, double keepEach)
    {
        int columnIndex = TableDataCollectionUtils.getColumnIndexes(table, new String[]{column})[0];
        int j = 1;
        for (int i=(int)startFrom; i< table.getSize(); i++)
        {           
            if (j == (int)keepEach)
            {
                j = 1;
                continue;               
            }
            j++;
            table.setValueAt(i, columnIndex, Double.NaN);       
        }
    }
    
    public void calcCumulative(TableDataCollection table, String column, String resultColumn, double startFrom, double keepEach)
    {
        if( !table.getColumnModel().hasColumn( resultColumn ) )
            table.getColumnModel().addColumn( resultColumn, DataType.Float );
        int[] indices = TableDataCollectionUtils.getColumnIndexes(table, new String[]{column, resultColumn});
        int columnIndex = indices[0];
        int resultColumnIndex = indices[1];     
        int j = 1;
        double value = 0;
        for (int i=(int)startFrom; i< table.getSize(); i++)
        {
            double addValue = (double)table.getValueAt( i, columnIndex );
            if( !Double.isNaN( addValue ) )
                value += addValue;
            if( j == keepEach )
            {
                j = 1;
                table.setValueAt( i, resultColumnIndex, value );
                value = 0;
                continue;
            }
            j++;
            table.setValueAt(i, resultColumnIndex, Double.NaN);
        }
    }
    
    public void calcCumulative(TableDataCollection table, String column, String resultColumn, double startFrom)
    {
        if( !table.getColumnModel().hasColumn( resultColumn ) )
            table.getColumnModel().addColumn( resultColumn, DataType.Float );
        int[] indices = TableDataCollectionUtils.getColumnIndexes(table, new String[]{column, resultColumn});
        int columnIndex = indices[0];
        int resultColumnIndex = indices[1];     
        double value = 0;
        for (int i=(int)startFrom; i< table.getSize(); i++)
        {
            double addValue = (double)table.getValueAt( i, columnIndex );
            if( !Double.isNaN( addValue ) )
                value += addValue;
                table.setValueAt( i, resultColumnIndex, value );
        }
    }
    
    public void calcDelta(TableDataCollection table, String column, String resultColumn, double distance)
    {
        if( !table.getColumnModel().hasColumn( resultColumn ) )
            table.getColumnModel().addColumn( resultColumn, DataType.Float );
        int[] indices = TableDataCollectionUtils.getColumnIndexes( table, new String[] {column, resultColumn} );
        int columnIndex = indices[0];
        int resultColumnIndex = indices[1];
        double delta = 0;
        double prevValue = (double)table.getValueAt(0, columnIndex);
        for (int i=1; i< table.getSize(); i++)
        {           
            double newValue = (double)table.getValueAt(i, columnIndex) ;
            if (Double.isNaN(prevValue))
                delta = newValue;
            else
                delta = newValue - prevValue;
            prevValue = newValue;
            table.setValueAt(i, resultColumnIndex, delta);
        }
    }
    
    public void calcSum(TableDataCollection table, String column, String column2, String resultColumn)
    {
        if( !table.getColumnModel().hasColumn( resultColumn ) )
            table.getColumnModel().addColumn( resultColumn, DataType.Float );
        int[] indices = TableDataCollectionUtils.getColumnIndexes( table, new String[] {column, column2, resultColumn} );
        int columnIndex = indices[0];
        int columnIndex2 = indices[1];
        int resultColumnIndex = indices[2];       
        for (int i=0; i< table.getSize(); i++)
        {           
            double val1 = (double)table.getValueAt(i, columnIndex);
            double val2 = (double)table.getValueAt(i, columnIndex2);
            double newValue = 0;
            if( !Double.isNaN( val1 ) )
                newValue = val1;
            if( !Double.isNaN( val2 ) )
                newValue += val2;
            if( Double.isNaN( val1 ) && Double.isNaN( val2 ) )
                newValue = Double.NaN;
            table.setValueAt(i, resultColumnIndex, newValue);
        }
    }
    
    public void thinOut(TableDataCollection table, String column, int step)
    {
        int index = TableDataCollectionUtils.getColumnIndexes( table, new String[] {column} )[0];

        int counter = 0;
        for( int i = 0; i < table.getSize(); i++ )
        {
            counter++;
            if( counter >= step )
            {
                counter = 0;
                continue;
            }
            table.setValueAt( i, index, Double.NaN );
        }
    }
    
    public double[] getColumn(TableDataCollection t, String colname)
    {
        return TableDataCollectionUtils.getColumn( t, colname );
    }
}
