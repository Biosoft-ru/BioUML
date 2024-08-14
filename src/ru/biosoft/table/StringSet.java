package ru.biosoft.table;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import one.util.streamex.StreamEx;

import org.json.JSONArray;

public class StringSet implements Set<String>,Serializable
{
    private static final long serialVersionUID = 1L;
    private final Set<String> values;
    
    private static JSONArray createJSONArray(String jsonString)
    {
        try
        {
            return new JSONArray(jsonString);
        }
        catch(Exception e)
        {
            return new JSONArray();
        }
    }
    
    public StringSet()
    {
        values = new LinkedHashSet<>();
    }
    
    public StringSet(Collection<? extends String> collection)
    {
        values = new LinkedHashSet<>( collection );
    }
    
    public StringSet(JSONArray array)
    {
        Set<String> vals = new LinkedHashSet<>();
        for(int i=0; i<array.length(); i++)
        {
            vals.add(array.optString(i));
        }
        values = vals;
    }
    
    public StringSet(String jsonString)
    {
        this(createJSONArray(jsonString));
    }

    @Override
    public boolean add(String element)
    {
        return values.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends String> list)
    {
        return values.addAll(list);
    }

    @Override
    public void clear()
    {
        values.clear();
    }

    @Override
    public boolean contains(Object element)
    {
        return values.contains(element);
    }

    @Override
    public boolean containsAll(Collection<?> element)
    {
        return values.containsAll(element);
    }

    @Override
    public boolean equals(Object obj)
    {
        return values.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return values.hashCode();
    }

    @Override
    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    @Override
    public Iterator<String> iterator()
    {
        return values.iterator();
    }

    @Override
    public boolean remove(Object element)
    {
        return values.remove(element);
    }

    @Override
    public boolean removeAll(Collection<?> list)
    {
        return values.removeAll(list);
    }

    @Override
    public boolean retainAll(Collection<?> list)
    {
        return values.retainAll(list);
    }

    @Override
    public int size()
    {
        return values.size();
    }

    @Override
    public Object[] toArray()
    {
        return values.toArray();
    }

    @Override
    public <T> T[] toArray(T[] arr)
    {
        return values.toArray(arr);
    }
    
    public String[] toStringArray()
    {
        return values.toArray(new String[values.size()]);
    }
    
    public JSONArray toJSON()
    {
        JSONArray result = new JSONArray();
        Iterator<String> iter = iterator();
        while(iter.hasNext())
        {
            result.put(iter.next());
        }
        return result;
    }
    
    @Override
    public String toString()
    {
        return toJSON().toString();
    }

    @Override
    public StreamEx<String> stream()
    {
        return StreamEx.of(values);
    }
}
