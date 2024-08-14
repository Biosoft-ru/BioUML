package biouml.plugins.research.workflow.items;

/**
 * Type of cycle
 * @author lan
 */
public interface CycleType
{
    /**
     * @return user-friendly name
     */
    public String getName();
    
    /**
     * Returns number of iterations by specified expression
     * @param expression - expression defining the cycle
     * @return number of iterations
     */
    public int getCount(String expression);
    
    /**
     * Returns value by specified expression on specified iteration
     * @param expression - expression defining the cycle
     * @param number - iteration number
     * @return value
     */
    public String getValue(String expression, int number);
    
}
