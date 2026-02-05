package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ConditionalInfo
{
    private LinkedHashMap<String, ContainerInfo> ifBlocks = new LinkedHashMap<>();
    private ContainerInfo elseBlock = new ContainerInfo();

    public void addCondition(String expression)
    {
        ifBlocks.put( expression, new ContainerInfo() );
    }

    public List<String> getConditions()
    {
        return new ArrayList<>( ifBlocks.keySet() );
    }

    public void addElse(Object object)
    {
        elseBlock.addObject( object );
    }

    public boolean hasElse()
    {
        return !elseBlock.isEmpty();
    }

    public Iterable<Object> getElse()
    {
        return elseBlock.getObjects();
    }

    public void add(String condition, Object object)
    {
        ifBlocks.computeIfAbsent( condition, k -> new ContainerInfo() ).addObject( object );
    }
    
    public Iterable<Object> get(String condition)
    {
        return ifBlocks.get( condition ).getObjects();
    }
}