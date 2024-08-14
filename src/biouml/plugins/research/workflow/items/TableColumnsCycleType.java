package biouml.plugins.research.workflow.items;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.table.TableDataCollection;

/**
 * Cycle which iterates over all children of given collection
 * @author lan
 */
public class TableColumnsCycleType implements CycleType
{

    @Override
    public String getName()
    {
        return "Table columns";
    }

    @Override
    public int getCount(String expression)
    {
        DataCollection dc = CollectionFactory.getDataCollection(expression);
        if(dc == null) throw new IllegalArgumentException("Collection not found: "+expression);
        if(!(dc instanceof TableDataCollection)) throw new IllegalArgumentException("Collection is not a table: "+expression);
        return ((TableDataCollection)dc).getColumnModel().getColumnCount();
    }

    @Override
    public String getValue(String expression, int number)
    {
        DataCollection dc = CollectionFactory.getDataCollection(expression);
        if(dc == null) throw new IllegalArgumentException("Collection not found: "+expression);
        if(!(dc instanceof TableDataCollection)) throw new IllegalArgumentException("Collection is not a table: "+expression);
        TableDataCollection tdc = (TableDataCollection)dc;
        if(tdc.getColumnModel().getColumnCount() == 0) throw new IllegalArgumentException("Table has no columns: "+expression);
        try
        {
            return tdc.getColumnModel().getColumn(number).getName();
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException("Unable to fetch column name of "+expression);
        }
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
