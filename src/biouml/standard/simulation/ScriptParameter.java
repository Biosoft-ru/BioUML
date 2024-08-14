package biouml.standard.simulation;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author lan
 *
 */
public class ScriptParameter extends ScriptableObject
{
    private Object value;
    
    public ScriptParameter(Object value)
    {
        this.value = value;
    }

    @Override
    public String getClassName()
    {
        return "parameter";
    }

    @Override
    public Object get(int point, Scriptable start)
    {
        return value;
    }

    @Override
    public Object get(String name, Scriptable start)
    {
        if(name.equals("max") || name.equals("min") || name.equals("sum") || name.equals("product")) return value;
        return null;
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        return true;
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
