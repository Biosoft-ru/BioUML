package biouml.model.dynamics;

import java.util.Map;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * This class represents a context in which mathematical expressions are calculated via {@link MathCalculator}.
 * Context contains variables mapped to their values. Context can be derived from another context.
 * In this case get method will first check current context then parent context.
 * @author lan
 */
public class MathContext
{
    private MathContext parent;
    private final TObjectDoubleMap<String> map = new TObjectDoubleHashMap<>();
    
    public MathContext(MathContext parent)
    {
        this.parent = parent;
    }
    
    public MathContext(Map<String, Double> source)
    {
        map.putAll(source);
    }
    
    public MathContext()
    {
    }

    public boolean contains(String name)
    {
        return map.containsKey(name) || (parent != null && parent.contains(name));
    }
    
    public double get(String name, double defaultValue)
    {
        if(map.containsKey(name))
            return map.get(name);
        if(parent != null)
            return parent.get(name, defaultValue);
        return defaultValue;
    }

    public void put(String name, double value)
    {
        map.put(name, value);
    }
}
