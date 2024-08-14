package biouml.plugins.research.workflow.items;

import ru.biosoft.access.core.DataElementPathSet;

/**
 * @author lan
 *
 */
public class SetCycleType implements CycleType
{

    @Override
    public String getName()
    {
        return "Elements in set";
    }

    @Override
    public int getCount(String expression)
    {
        if(expression == null || expression.isEmpty()) throw new IllegalArgumentException("Cycle expression is not specified");
        return new DataElementPathSet(expression).size();
    }

    @Override
    public String getValue(String expression, int number)
    {
        DataElementPathSet pathSet = new DataElementPathSet(expression);
        return pathSet.stream().skip( number ).findFirst().get().toString();
    }
    
    @Override
    public String toString()
    {
        return getName();
    }
}
