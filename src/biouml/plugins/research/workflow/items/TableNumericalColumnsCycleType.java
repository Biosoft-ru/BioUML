package biouml.plugins.research.workflow.items;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

/**
 * Cycle which iterates over all children of given collection
 * @author lan
 */
public class TableNumericalColumnsCycleType implements CycleType
{

    @Override
    public String getName()
    {
        return "Table columns (numerical only)";
    }

    @Override
    public int getCount(String expression)
    {
        DataCollection dc = CollectionFactory.getDataCollection(expression);
        if(dc == null) throw new IllegalArgumentException("Collection not found: "+expression);
        if(!(dc instanceof TableDataCollection)) throw new IllegalArgumentException("Collection is not a table: "+expression);
        int count = 0;
        for(TableColumn column: ((TableDataCollection)dc).getColumnModel())
        {
            if(column.getType().isNumeric()) count++;
        }
        return count;
    }

    @Override
    public String getValue(String expression, int number)
    {
        DataCollection dc = CollectionFactory.getDataCollection(expression);
        if(dc == null) throw new IllegalArgumentException("Collection not found: "+expression);
        if(!(dc instanceof TableDataCollection)) throw new IllegalArgumentException("Collection is not a table: "+expression);
        TableDataCollection tdc = (TableDataCollection)dc;
        int count = 0;
        for(TableColumn column: tdc.getColumnModel())
        {
            if(column.getType().isNumeric())
            {
                if(count == number) return column.getName();
                count++;
            }
        }
        throw new IllegalArgumentException("Out of range");
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
