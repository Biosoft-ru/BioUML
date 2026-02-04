package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

public class CallInfo
{
    private Map<String, Object> attributes = new HashMap<>();
    private List<InputInfo> inputs = new ArrayList<>();
    private String taskName;
    private String alias;
    
    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public void setInputs(List<InputInfo> inputs)
    {
        this.inputs = inputs;
    }

    public Collection<InputInfo> getInputs()
    {
        return inputs;
    }
    
    public String getTaskName()
    {
        return taskName;
    }
    
    public String getAlias()
    {
        return alias;
    }
    
    public void addInputInfo(InputInfo inputInfo)
    {
        this.inputs.add(inputInfo);
    }
    
    public void setAttribute(String name, Object value)
    {
        this.attributes.put(name, value);
    }
    
    public Object getAttribute(String name)
    {
        return attributes.get(name);
    }
    
    public String toString()
    {
        return taskName+ " as "+alias+" ( " + StreamEx.of(getInputs()).map(in->in.toString()).joining(",")+" )";
    }
}