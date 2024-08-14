package biouml.model;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.util.DPSUtils;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
* Element which encapsulate diagram definition (not instance like in SubDiagram)
* @see SubDiagram
* @author Ilya
*/
@SuppressWarnings ( "serial" )
@PropertyName("Model definition")
@PropertyDescription("Element defining diagram to be included into another diagram.")
public class ModelDefinition extends DiagramContainer
{
    
   private DataElementPath diagramPath;
   final static public String REF_MODEL_DEFINITION = "refModelDefinition";

    @Override
    public DiagramElement get(String name)
    {
        return name.equals("_diagram_") ? diagram : super.get(name);
    }
    
    public ModelDefinition(DataCollection<?> origin, Diagram diagram, String name)
    {
        super( origin, diagram, new Stub( origin, name, Type.TYPE_MODEL_DEFINITION ) );
        
        markRefModelDefinition(diagram);
        diagramPath = diagram.getOrigin() instanceof TransformedDataCollection? diagram.getCompletePath(): null;
    }

    public void markRefModelDefinition(Diagram diagram)
    {
        DynamicProperty dp = DPSUtils.createHiddenReadOnly(REF_MODEL_DEFINITION, ModelDefinition.class, this);
        DPSUtils.makeTransient(dp);
        diagram.getAttributes().add(dp);
    }

    public static boolean isDefindInModelDefinition(Diagram diagram)
    {
        return  diagram.getAttributes().getProperty(ModelDefinition.REF_MODEL_DEFINITION) != null;
    }
    
    public static ModelDefinition getModelDefinition(Diagram diagram)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty(ModelDefinition.REF_MODEL_DEFINITION);
        if( dp == null )
            return null;
        return (ModelDefinition)dp.getValue();
    }

    public String getDiagramPath()
    {
        return diagramPath != null? diagramPath.toString(): getCompletePath().getChildPath( "_diagram_" ).toString();
    }

    @Override
    public @Nonnull ModelDefinition clone(Compartment newParent, String newName)
    {
        if( newParent == this )
            throw new IllegalArgumentException( "Can not clone compartment into itself, compartment=" + newParent );

        ModelDefinition result = new ModelDefinition( newParent, this.getDiagram(), newName );
        result.setNotificationEnabled( false );
        doClone( result );
        result.setNotificationEnabled( isNotificationEnabled() );
        return result;
    }
}
