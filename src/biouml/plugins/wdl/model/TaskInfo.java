package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskInfo
{
    private String name;
    private List<ExpressionInfo> beforeCommand = new ArrayList<>();
    private List<ExpressionInfo> inputs = new ArrayList<>();
    private List<ExpressionInfo> outputs = new ArrayList<>();
    private MetaInfo meta = new MetaInfo();
    private Map<String, String> runtime = new HashMap<>();
    private CommandInfo command = new CommandInfo();

    public TaskInfo(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getMetaProperty(String name)
    {
        return meta.getProperty(name);
    }

    public void setMetaProperty(String name, String value)
    {
        meta.setProperty(name, value);
    }
    
    public CommandInfo getCommand()
    {
        return command;
    }

    public void setCommand(CommandInfo command)
    {
        this.command = command;
    }
    
    public List<ExpressionInfo> getInputs()
    {
        return List.copyOf(inputs);
    }
    
    public void addInputInfo(ExpressionInfo inputInfo)
    {
        this.inputs.add(inputInfo);
    }
    
    public List<ExpressionInfo> getOutputs()
    {
        return List.copyOf(outputs);
    }
    
    public void addOutputInfo(ExpressionInfo outputInfo)
    {
        this.outputs.add(outputInfo);
    }
    
    public List<ExpressionInfo> getBeforeCommand()
    {
         return List.copyOf(beforeCommand);
    }
    public void addBeforeCommand(ExpressionInfo expression)
    {
        this.beforeCommand.add(expression);
    }
    
    public Map<String, String> getRuntime()
    {
        return new HashMap<>(runtime);
    }

    public void setRuntime(String key, String value)
    {
        runtime.put(key, value);
    }
}
