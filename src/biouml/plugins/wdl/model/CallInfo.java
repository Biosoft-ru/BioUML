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
    private String source = null;
    private String resultName = null;

    public String getSource()
    {
        return source;
    }
    public void setSource(String source)
    {
        this.source = source;
    }
    
    public String getTaskName()
    {
        return taskName;
    }
    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public String getAlias()
    {
        return alias;
    }
    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public Collection<InputInfo> getInputs()
    {
        return inputs;
    }
    public void setInputs(List<InputInfo> inputs)
    {
        this.inputs = inputs;
    }

    public void addInputInfo(InputInfo inputInfo)
    {
        this.inputs.add(inputInfo);
    }
   
    public Object getAttribute(String name)
    {
        return attributes.get(name);
    }
    public void setAttribute(String name, Object value)
    {
        this.attributes.put(name, value);
    }
    
    public String getResultName()
    {
        return resultName;
    }
    public void setResultName(String resultName)
    {
        this.resultName = resultName;
    }

    public String toString()
    {
        if( source != null )
            return "from" + source + " " + taskName + " as " + alias + " ( "
                    + StreamEx.of(getInputs()).map(in -> in.toString()).joining(",") + " )";
        return taskName + " as " + alias + " ( " + StreamEx.of(getInputs()).map(in -> in.toString()).joining(",") + " )";
    }
}