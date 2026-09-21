package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConditionalInfo
{
    private LinkedHashMap<String, ContainerInfo> ifBlocks = new LinkedHashMap<>();
    private Map<String, Set<String>> conditionArguments = new HashMap<>();
    
    private ContainerInfo elseBlock = new ContainerInfo();

    public void addCondition(String expression, Set<String> arguments)
    {
        if (ifBlocks.containsKey( expression ))
            return;
        ifBlocks.put( expression, new ContainerInfo() );
        conditionArguments.put( expression, arguments );
    }
    
    public Set<String> getArguments(String condition)
    {
        return conditionArguments.get( condition );
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

    public ContainerInfo getElse()
    {
        return elseBlock;
    }

    public void add(String condition, Object object)
    {
        ifBlocks.computeIfAbsent( condition, k -> new ContainerInfo() ).addObject( object );
    }
    
    public ContainerInfo get(String condition)
    {
        return ifBlocks.get( condition );
    }
}