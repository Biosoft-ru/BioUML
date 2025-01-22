package biouml.plugins.virtualcell.simulation;

import biouml.plugins.virtualcell.core.Pool;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TablePool extends Pool
{
    private final String columnName = "Value";
    boolean shouldBeSaved = false;
    private TableDataCollection tdc;
    private int columnIndex;

    public TablePool(String name, TableDataCollection tdc)
    {
        super( name );
        this.tdc = tdc;
        this.columnIndex = TableDataCollectionUtils.getColumnIndexes( tdc, new String[] {columnName} )[0];
    }
    
  

    @Override
    public RowDataElement get(String name)
    {
        try
        {
            return tdc.get( name );
        }
        catch( Exception ex )
        {
            return null;
        }
    }

    @Override
    public RowDataElement put(DataElement de)
    {
        if( de instanceof RowDataElement )
        {
            tdc.put( (RowDataElement)de );
            return (RowDataElement)de;
        }
        return null;
    }

    public double getValue(String name)
    {
        return (double)get( name ).getValues()[columnIndex];
    }

    public void setValue(String name, double value)
    {
        RowDataElement rde = get(name);
        if (rde == null)
        {
            TableDataCollectionUtils.addRow( tdc, name, tdc.columns().map( c -> c.getType().getDefaultValue() )
                    .toArray(), false );
            rde = get(name);
        }
        rde.getValues()[columnIndex] = value;
    }
}