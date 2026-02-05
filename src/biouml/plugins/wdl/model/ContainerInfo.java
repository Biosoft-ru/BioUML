package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContainerInfo implements Iterable<Object>
{
    private List<Object> objects = new ArrayList<>();

    public void addObject(Object obj)
    {
        objects.add(obj);
    }

    public Iterable<Object> getObjects()
    {
        return objects;
    }
    
    public int size()
    {
        return objects.size();
    }
    
    public boolean isEmpty()
    {
        return size() == 0;
    }

    @Override
    public Iterator<Object> iterator()
    {
        return objects.iterator();
    }
}