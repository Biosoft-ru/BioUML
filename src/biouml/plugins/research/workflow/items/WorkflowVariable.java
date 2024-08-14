package biouml.plugins.research.workflow.items;

import java.beans.PropertyDescriptor;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.lang.StringUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.model.Compartment;
import biouml.model.Node;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * Represents workflow variable
 * (item which have type and value and can be referenced from expressions or linked to analysis parameters)
 * @author lan
 */
public abstract class WorkflowVariable extends WorkflowItem
{
    public static final String VARIABLE_TYPE = "variable-type";
    private static final PropertyDescriptor VARIABLE_TYPE_PD = StaticDescriptor.create(VARIABLE_TYPE);
    public static final String VARIABLE_AUTO_OPEN = "variable-auto-open";
    
    public WorkflowVariable(Node node, Boolean canSetName)
    {
        super(node, canSetName);
    }
    
    public VariableType getType()
    {
        Object type = getNode().getAttributes().getValue(VARIABLE_TYPE);
        return VariableType.getType(type==null?null:(String)type);
    }
    
    public void setType(VariableType type)
    {
        startTransaction("Set type");
        getNode().getAttributes().add(new DynamicProperty(VARIABLE_TYPE_PD, String.class, type.getName()));
        completeTransaction();
        firePropertyChange("type", null, type);
        firePropertyChange("*", null, null);
    }
    
    public String getValueString()
    {
        Object value;
        try
        {
            value = getValue();
        }
        catch( Exception e )
        {
            return "Error: "+e.getMessage();
        }
        if(value == null)
            return null;
        if(value.getClass().isArray())
            return StringUtils.join( (Object[])value, ", " );
        return value.toString();
    }
    
    /**
     * Implementation-dependent method to obtain variable value
     * Value type should have the same type as returned by getType().getClass()
     * @throws Exception if some problem occurred during value fetching/calculation
     */
    public abstract Object getValue() throws Exception;

    @Override
    public void setName(String name)
    {
        String oldName = getNode().getName();
        super.setName(name);
        String newName = getNode().getName();
        if(!newName.equals(oldName))
        {
            // Update variable references
            try
            {
                Deque<Compartment> compartments = new ArrayDeque<>();
                compartments.add((Compartment)getNode().getOrigin());
                while(!compartments.isEmpty())
                {
                    Compartment compartment = compartments.pollFirst();
                    for( Node node : compartment.getNodes() )
                    {
                        try
                        {
                            if(node instanceof Compartment && node.getKernel().getType().equals(Type.ANALYSIS_CYCLE)
                                    && !((Compartment)node).contains(oldName) && !((Compartment)node).contains(newName))
                            {   // Add compartment into consideration if there's no variable inside which overrides the current one
                                compartments.add((Compartment)node);
                            }
                            WorkflowItem item = WorkflowItemFactory.getWorkflowItem(node);
                            if( item instanceof WorkflowExpression )
                            {
                                ((WorkflowExpression)item).updateReference(oldName, newName);
                            }
                        }
                        catch( Exception e )
                        {
                        }
                    }
                }
            }
            catch( Exception e )
            {
            }
        }
    }
    
    public boolean isAutoOpen()
    {
        Object typeObj = getNode().getAttributes().getValue(VARIABLE_TYPE);
        VariableType type = VariableType.getType(typeObj.toString());
        if( type.getTypeClass().isAssignableFrom(DataElementPath.class) && type.getName().equals(VariableType.TYPE_AUTOOPEN) )
        {
            return true;
        }
        Object autoOpen = getNode().getAttributes().getValue(VARIABLE_AUTO_OPEN);
        if( autoOpen != null && autoOpen.equals(true) ) //old-property compatibility
        {
            setType(VariableType.getType(VariableType.TYPE_AUTOOPEN));
            return true;
        }
        return false;
    }

    public boolean isTemporary()
    {
        Object typeObj = getNode().getAttributes().getValue(VARIABLE_TYPE);
        VariableType type = VariableType.getType(typeObj.toString());
        return ( type.getTypeClass().isAssignableFrom(DataElementPath.class) && type.getName().equals(VariableType.TYPE_TEMPORARY) );
    }
}
