package biouml.plugins.research.workflow.items;

/**
 * Cycle which is specified by lower and upper bound like "2..5" -> [2,3,4,5]
 * @author lan
 */
public class RangeCycleType implements CycleType
{
    @Override
    public String getName()
    {
        return "Range (first[,second]..last)";
    }
    
    @Override
    public int getCount(String expression)
    {
        return new Range( expression ).getCount();
    }

    @Override
    public String getValue(String expression, int number)
    {
        return new Range( expression ).getValueAt( number );
    }
    
    @Override
    public String toString()
    {
        return getName();
    }
}
