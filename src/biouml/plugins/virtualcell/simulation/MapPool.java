package biouml.plugins.virtualcell.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import biouml.plugins.virtualcell.core.Pool;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class MapPool extends Pool
{
    private Map<String, Double> values = new HashMap<>();
    public MapPool(String name)
    {
        super( name );
    }

    public double getValue(String name)
    {
        return values.get( name );
    }

    public void setValue(String name, double value)
    {
        values.put( name, value );
    }

    public void load(TableDataCollection tdc, String column)
    {
        int index = TableDataCollectionUtils.getColumnIndexes( tdc, new String[] {column} )[0];
        for( RowDataElement rde : tdc )
        {
            double value = (double)rde.getValues()[index];
            String name = rde.getName();
            values.put( name, value );
        }
    }

    public void save(DataElementPath dep, String column)
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( dep );
        tdc.getColumnModel().addColumn( column, DataType.Float );
        for( Entry<String, Double> e : values.entrySet() )
        {
            TableDataCollectionUtils.addRow( tdc, e.getKey(), new Object[] {e.getValue()} );

        }
    }
}