package ru.biosoft.galaxy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test description for Galaxy method
 */
public class GalaxyMethodTest
{
    protected ParametersContainer parameters = new ParametersContainer();
    protected Map<String, Object> attributes = new HashMap<>();
    protected Map<String, ResultComparator> comparators = new HashMap<>();

    public void setParameters(ParametersContainer parameters)
    {
        this.parameters = parameters;
    }

    public ParametersContainer getParameters()
    {
        return parameters;
    }
    
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    /**
     * @param name - name of output parameter
     * @return comparator for output parameter
     */
    public ResultComparator getComparator(String name)
    {
        return comparators.get(name);
    }

    /**
     * @param comparator - comparator for output parameter
     * @param name - output parameter name
     */
    public void setComparator(ResultComparator comparator, String name)
    {
        this.comparators.put(name, comparator);
    }
    
    public Map<String, ResultComparator> getComparators()
    {
        return Collections.unmodifiableMap(comparators);
    }
}
