package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptInfo
{
    private String name;
    private Map<String, WorkflowInfo> workflows = new HashMap<>();
    private Map<String, TaskInfo> tasks = new HashMap<>();
    private Map<String, ImportInfo> imports = new HashMap<>();
    private List<StructInfo> structs = new ArrayList<>();
    
    private List<InputInfo> inputs = new ArrayList<>();
    
    public ScriptInfo(String name)
    {
        this.name = name;
    }
    public void addWorkflow(WorkflowInfo workflow)
    {
        workflows.put(workflow.getName(), workflow);
    }

    public void addTask(TaskInfo task)
    {
        tasks.put(task.getName(), task);
    }

    public Set<String> getWorkflowNames()
    {
        return workflows.keySet();
    }

    public Set<String> getTaskNames()
    {
        return tasks.keySet();
    }

    public WorkflowInfo getWorkflow(String name)
    {
        return workflows.get(name);
    }

    public TaskInfo getTask(String name)
    {
        return tasks.get(name);
    }
    
    public void addInput(InputInfo inputInfo)
    {
        inputs.add(inputInfo);
    }
    
    public Iterable<InputInfo> getInputs()
    {
        return inputs;
    }

    public void addImport(ImportInfo importInfo)
    {
        imports.put(importInfo.getSource() , importInfo);
    }

    public Iterable<ImportInfo> getImports()
    {
        return imports.values();
    }
    
    public ImportInfo getImport(String taskName)
    {
        return imports.get( taskName );
    }
    
    public void addStruct(StructInfo structInfo)
    {
        structs.add(structInfo);
    }

    public Iterable<StructInfo> getStructs()
    {
        return structs;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
}
