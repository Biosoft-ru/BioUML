package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptInfo
{
    private Map<String, WorkflowInfo> workflows = new HashMap<>();
    private Map<String, TaskInfo> tasks = new HashMap<>();
    private List<ImportInfo> imports = new ArrayList<>();
    private List<StructInfo> structs = new ArrayList<>();

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

    public void addImport(ImportInfo importInfo)
    {
        imports.add(importInfo);
    }

    public Iterable<ImportInfo> getImports()
    {
        return imports;
    }
    
    public void addStruct(StructInfo structInfo)
    {
        structs.add(structInfo);
    }

    public Iterable<StructInfo> getStructs()
    {
        return structs;
    }
}
