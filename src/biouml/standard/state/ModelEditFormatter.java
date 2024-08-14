package biouml.standard.state;

import java.util.HashSet;
import java.util.Set;

import javax.swing.undo.UndoableEdit;

import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import biouml.model.Node;

/**
 * @author anna
 * This formatter should create string representation for Model changes
 * To add properties taken into account add property name to significantPropertiesNames
 * A specific branch in getPresentationString can be used to display property in some special way
 */
public class ModelEditFormatter implements UndoableEditFormatter
{

    private static final Set<String> significantPropertiesNames = new HashSet<>();
    static
    {
        significantPropertiesNames.add("initialValue");
        significantPropertiesNames.add("isConstant");
        significantPropertiesNames.add("boundaryCondition");
        significantPropertiesNames.add("formula");
    }

    @Override
    public String getPresentationName(UndoableEdit edit)
    {
        Node node = null;
        String suffix = "";
        if( ( edit instanceof DataCollectionAddUndo ) )
        {
            DataElement de = ( (DataCollectionAddUndo)edit ).getDataElement();
            if( de instanceof Node )
                node = (Node)de;
            suffix = "added";
        }
        else if( edit instanceof DataCollectionRemoveUndo )
        {
            DataElement de = ( (DataCollectionRemoveUndo)edit ).getDataElement();
            if( de instanceof Node )
                node = (Node)de;
            suffix = "removed";
        }
        if( node != null )
        {
            String name = node.getTitle() != null ? !node.getTitle().isEmpty() ? node.getTitle() : node.getName() : node.getName();
            return name + ": " + suffix;
        }
        if( edit instanceof StatePropertyChangeUndo )
        {
            StatePropertyChangeUndo pcu = (StatePropertyChangeUndo)edit;
            String name = pcu.getPropertyName();
            DataElementPath propertyPath = DataElementPath.create(name);
            String propertyName = propertyPath.getName();
            if( significantPropertiesNames.contains(propertyName) )
            {
                String owner;
                if( propertyPath.getParentPath().getName().isEmpty() || propertyPath.getParentPath().getName().equals("role")) //source is owner for property
                {
                    owner = pcu.getSource() instanceof DataElement ? ( (DataElement)pcu.getSource() ).getName() : "";
                }
                else
                //write element preceding to property name as owner for property
                {
                    owner = propertyPath.getParentPath().getName();
                }

                Object newValue = pcu.getNewValue();
                Object oldValue = pcu.getOldValue();

                if( newValue != null || oldValue != null )
                {
                    Class<?> type = null;
                    if( newValue != null )
                    {
                        type = newValue.getClass();
                    }
                    else
                    {
                        type = oldValue.getClass();
                    }
                    if( type.isPrimitive() || type.equals(String.class) || type.equals(Integer.class) || type.equals(Float.class)
                            || type.equals(Double.class) || type.equals(Long.class) || type.equals(Boolean.class)
                            || type.equals(Character.class) || type.equals(Byte.class) )
                    {
                        return getPresentationString(owner, propertyPath.getName(), newValue, oldValue);
                    }
                    else
                    {
                        if( owner.isEmpty() )
                            owner = propertyName;
                        else
                            owner += " (" + propertyName + ")";
                        return owner.isEmpty() + " changed";
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isSignificant(UndoableEdit edit)
    {
        if( ( edit instanceof DataCollectionAddUndo ) )
        {
            DataElement de = ( (DataCollectionAddUndo)edit ).getDataElement();
            return ( de instanceof Node );
        }
        else if( edit instanceof DataCollectionRemoveUndo )
        {
            DataElement de = ( (DataCollectionRemoveUndo)edit ).getDataElement();
            return ( de instanceof Node );
        }
        else if( edit instanceof StatePropertyChangeUndo )
        {
            StatePropertyChangeUndo pcu = (StatePropertyChangeUndo)edit;
            return significantPropertiesNames.contains(DataElementPath.create(pcu.getPropertyName()).getName());
        }
        return false;
    }

    private String getPresentationString(String ownerName, String propertyName, Object oldValue, Object newValue)
    {
        if( propertyName.equals("initialValue") )
            return ownerName + ": " + oldValue + " -> " + newValue;
        else if( propertyName.equals("isConstant") )
            return ownerName + ( Boolean.parseBoolean(newValue + "") ? " is constant" : "is not constant" );
        if( propertyName.equals("boundaryCondition") )
            return ownerName + ( Boolean.parseBoolean(newValue + "") ? " uses boundary condition" : "no boundary condition" );
        return ownerName + " (" + propertyName + "): " + oldValue + " -> " + newValue;
    }

}
