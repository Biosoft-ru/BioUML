package biouml.plugins.research.workflow.items;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

/**
 * Cycle which iterates over all children of given collection
 * @author lan
 */
public class CollectionCycleType implements CycleType
{

    @Override
    public String getName()
    {
        return "All elements in collection";
    }

    @Override
    public int getCount(String expression)
    {
        DataCollection dc = CollectionFactory.getDataCollection(expression);
        if(dc == null) throw new IllegalArgumentException("Collection not found: "+expression);
        return dc.getSize();
    }

    @Override
    public String getValue(String expression, int number)
    {
        DataCollection dc = CollectionFactory.getDataCollection(expression);
        if(dc == null) throw new IllegalArgumentException("Collection not found: "+expression);
        if(dc.isEmpty()) throw new IllegalArgumentException("Collection is empty: "+expression);
        try
        {
            return DataElementPath.create(expression).getChildPath((String)dc.getNameList().get(number)).toString();
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException("Unable to fetch child of "+expression);
        }
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
