package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskInfo extends ExecutableInfo
{
    private List<ExpressionInfo> beforeCommand = new ArrayList<>();
    private Map<String, String> runtime = new HashMap<>();
    private CommandInfo command = new CommandInfo();

    public TaskInfo(String name)
    {
        super(name);
    }
    
    public String getMetaProperty(String name)
    {
        return getMeta().getProperty(name);
    }

    public void setMetaProperty(String name, String value)
    {
        getMeta().setProperty(name, value);
    }
    
    public CommandInfo getCommand()
    {
        return command;
    }

    public void setCommand(CommandInfo command)
    {
        this.command = command;
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
