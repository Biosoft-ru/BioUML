package biouml.plugins.research.workflow.items;

import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.journal.ProjectUtils;
import ru.biosoft.util.FileItem;
import com.developmentontheedge.beans.PropertiesDPS;

import biouml.model.Diagram;

import com.developmentontheedge.application.Application;

public abstract class SystemVariable extends WorkflowVariable
{
    private static final long serialVersionUID = 1L;
    private static Map<String, SystemVariable> systemVars = new HashMap<>();
    private String name;
    
    static
    {
        systemVars.put("project", new SystemVariable("project")
        {
            @Override
            public Object getValue() throws Exception
            {
                return JournalRegistry.getProjectPath();
            }
        });
        systemVars.put("preferences", new SystemVariable("preferences")
        {
            @Override
            public Object getValue() throws Exception
            {
                return Application.getPreferences();
            }
        });
        systemVars.put("importPath", new SystemVariable("importPath")
        {
            @Override
            public Object getValue() throws Exception
            {
                String key = DataElementImporter.PREFERENCES_IMPORT_DIRECTORY;
                return new FileItem(Application.getPreferences().getStringValue(key, "."));
            }
        });
        systemVars.put("dbPaths", new SystemVariable("dbPaths")
        {
            @Override
            public Object getValue() throws Exception
            {
                Properties properties = new Properties();
                properties.putAll(ProjectUtils.getPreferredDatabasePaths());
                return new PropertiesDPS(properties, true);
            }
        });
        systemVars.put("dbVersions", new SystemVariable("dbVersions")
        {
            @Override
            public Object getValue() throws Exception
            {
                Properties properties = new Properties();
                properties.putAll(ProjectUtils.getPreferredDatabaseVersions());
                return new PropertiesDPS(properties, true);
            }
        });
    }
    
    private static SystemVariable getWorkflowPathVariable(final Diagram workflow)
    {
        return new SystemVariable("workflowPath")
        {
            @Override
            public Object getValue() throws Exception
            {
                return DataElementPath.create(workflow);
            }
        };
    }
    
    public static WorkflowVariable getVariable(String name, Diagram workflow)
    {
        if(name.equals("workflowPath"))
            return getWorkflowPathVariable(workflow);
        return systemVars.get(name);
    }
    
    public static Collection<SystemVariable> getVariables(Diagram workflow)
    {
        List<SystemVariable> list = new ArrayList<>(systemVars.values());
        list.add(getWorkflowPathVariable(workflow));
        return list;
    }

    protected SystemVariable(String name)
    {
        super(null, false);
        this.name = name;
    }

    @Override
    public VariableType getType()
    {
        try
        {
            return VariableType.getType(getValue().getClass());
        }
        catch( Exception e )
        {
            return VariableType.getType("");
        }
    }

    @Override
    public void setType(VariableType type)
    {
        throw new RuntimeException(new UnmodifiableClassException());
    }

    @Override
    public String getName()
    {
        return name;
    }
}
