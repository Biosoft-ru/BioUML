package biouml.standard.simulation;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;


public class ScriptVariable extends ScriptableObject
{
    private int idx;
    private String name;
    private int count = 0;
    private SimulationResult result;
    private double min, max, sum, product;
    private int skipPoints;
    
    ScriptVariable(String name, int idx, SimulationResult result, int skipPoints)
    {
        this.idx = idx;
        this.name = name;
        this.result = result;
        this.skipPoints = skipPoints;
    }

    public Integer getIdx()
    {
        return idx;
    }

    public String getName()
    {
        return name;
    }

    void addValue(double val)
    {
        if(count == 0)
        {
            min = max = sum = product = val;
        } else
        {
            max = Math.max(val, max);
            min = Math.min(val, min);
            sum += val;
            product *= val;
        }
        count++;
    }
    
    int count()
    {
        return count;
    }

    @Override
    public String getClassName()
    {
        return "variable";
    }
    
    public int getSkipPoints()
    {
        return skipPoints;
    }

    @Override
    public Object get(int point, Scriptable start)
    {
        return idx == -1?result.getTime(point+skipPoints):result.getValue(point+skipPoints)[idx];
    }

    @Override
    public Object get(String name, Scriptable start)
    {
        if(name.equals("max")) return max;
        if(name.equals("min")) return min;
        if(name.equals("sum")) return sum;
        if(name.equals("product")) return product;
        if(name.equals("count")) return count;
        return null;
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        return index >= 0 && index < count;
    }

    @Override
    public boolean has(String name, Scriptable start)
    {
        return name.equals("max") || name.equals("min") || name.equals("sum") || name.equals("product") || name.equals("count");
    }

    @Override
    public void put(int index, Scriptable start, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(String name, Scriptable start, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String name)
    {
        throw new UnsupportedOperationException();
    }
}