package biouml.plugins.research.workflow.items;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.WildcardPathSet;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.TextUtil;
import ru.biosoft.workbench.editors.FileSelector;

/**
 * Represents data type which WorkflowVariable can have
 * @author lan
 */
public class VariableType
{
    private Class<?> type;
    private Class<?> editorType;
    private String name;
    
    public static final String TYPE_AUTOOPEN = "Data element (auto-open)";
    public static final String TYPE_TEMPORARY = "Data element (temporary)";
    
    /**
     * Allowed types
     */
    static final VariableType[] VARIABLE_TYPES = {
        new VariableType(String.class, "String"),
        new VariableType(String[].class, "Multiple strings"),
        new VariableType(DataElementPath.class, "Data element"),
        new VariableType(DataElementPath.class, TYPE_TEMPORARY),
        new VariableType(DataElementPath.class, TYPE_AUTOOPEN),
        new VariableType(DataElementPathSet.class, "Multiple elements"),
        new VariableType(WildcardPathSet.class, "Multiple elements (wildcard)"),
        new VariableType(Species.class, "Species"),
        new VariableType(Double.class, "Float number"),
        new VariableType(Integer.class, "Integer number"),
        new VariableType(Boolean.class, "Boolean"),
        new VariableType(FileItem.class, FileSelector.class, "File"),
    };
    
    VariableType(Class<?> type, Class<?> editorType, String name)
    {
        super();
        this.type = type;
        this.editorType = editorType;
        this.name = name;
    }
    
    VariableType(Class<?> type, String name)
    {
        this(type, null, name);
    }
    
    @Override
    public String toString()
    {
        return name;
    }

    public Class<?> getTypeClass()
    {
        return type;
    }

    public Class<?> getEditorType()
    {
        return editorType;
    }
    
    /**
     * Construct object of given type from String
     * @param data String to construct from
     * @return constructed object or null in case of any error
     */
    public Object fromString(String data)
    {
        return TextUtil.fromString(getTypeClass(), data);
    }

    /**
     * Returns human-readable type name
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Returns VariableType object by given human-readable type name
     */
    public static VariableType getType(String typeName)
    {
        for(VariableType type: VARIABLE_TYPES)
        {
            if(type.getName().equals(typeName)) return type;
        }
        return VARIABLE_TYPES[0];
    }
    
    public static VariableType getTypeOrNull(String typeName)
    {
        for( VariableType type : VARIABLE_TYPES )
            if( type.getName().equals( typeName ) )
                return type;
        return null;
    }

    public static VariableType getType(Class<?> typeClass)
    {
        for(VariableType type: VARIABLE_TYPES)
        {
            if(type.getTypeClass().equals(typeClass)) return type;
        }
        return VARIABLE_TYPES[0];
    }
}