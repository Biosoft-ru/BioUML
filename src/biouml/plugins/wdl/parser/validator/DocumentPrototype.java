package biouml.plugins.wdl.parser.validator;

import java.util.HashMap;
import java.util.Map;

public class DocumentPrototype extends Scope
{
    private String version;
    private Map<String, Scope> structures = new HashMap<String, Scope>();
    private WorkflowPrototype wf;
    private Map<String, TaskPrototype> tasks = new HashMap<String, TaskPrototype>();
    private Map<String, ImportPrototype> imports = new HashMap<String, ImportPrototype>();


    public DocumentPrototype(String name, String version)
    {
        super(name);
        this.version = version;
    }


    public void addStruct(Scope struct) throws Exception
    {
        if( structures.get(struct.getName()) != null )
            throw new Exception("Duplicate structure \"" + struct.getName() + "\"");
        this.structures.put(struct.getName(), struct);
    }

    public void addTask(TaskPrototype task) throws Exception
    {
        if( tasks.get(task.getName()) != null )
            throw new Exception("Duplicate task \"" + task.getName() + "\"");
        this.tasks.put(task.getName(), task);

    }

    public Map<String, TaskPrototype> getTasks()
    {
        return tasks;
    }

    public void setWorkflow(WorkflowPrototype wf)
    {
        this.wf = wf;
    }

    public WorkflowPrototype getWorkflow()
    {
        return wf;
    }

    public void addImport(ImportPrototype imp) throws Exception
    {
        if( imports.get(imp.getName()) != null )
            throw new Exception("Duplicate namespace imported \"" + imp.getName() + "\"");
        this.imports.put(imp.getName(), imp);

    }


}

class Scope extends NamedPrototype
{
    Map<String, Field> fields = new HashMap<String, Field>();

    public Scope(String name, NamedPrototype parent)
    {
        super(name, parent);
    }

    public Scope(String name)
    {
        super(name, null);
    }

    public void addField(Field field) throws Exception
    {
        if( field.getName() != null && fields.get(field.getName()) != null )
            throw new Exception("Duplicate field \"" + name + "\"");
        this.fields.put(field.getName(), field);
    }

    public Field getField(String name) throws Exception
    {
        Field field = fields.get(name);
        if( field == null )
            throw new Exception(getClass().getSimpleName() + " " + getName() + " doesn't contain the field \"" + name + "\"");

        return field;
    }
}

class NamedPrototype
{
    String name;
    NamedPrototype parent;


    public NamedPrototype(String name, NamedPrototype parent)
    {
        this.name = name;
        this.parent = parent;
    }

    public String getName()
    {
        return name;
    }

    public NamedPrototype getParent()
    {
        return parent;
    }

}
