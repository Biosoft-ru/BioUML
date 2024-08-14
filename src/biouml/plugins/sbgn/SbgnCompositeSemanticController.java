package biouml.plugins.sbgn;

import java.awt.Point;
import java.util.logging.Level;
import javax.annotation.Nonnull;

import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramType;
import biouml.model.ModelDefinition;
import biouml.model.SubDiagram;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.SimpleBusProperties;
import biouml.standard.type.DiagramInfo;

/** @author Ilya */
public class SbgnCompositeSemanticController extends SbgnSemanticController
{

    @PropertyName ( "Model definition properties" )
    @PropertyDescription ( "Model definition properties." )
    public static class ModelDefinitionProperties extends SbgnElementProperties
    {
        private boolean composite;

        public ModelDefinitionProperties(String name)
        {
            super(Type.TYPE_MODEL_DEFINITION, name);
            setProperties(SbgnSemanticController.getDPSByType(Type.TYPE_MODEL_DEFINITION));
        }

        @PropertyName ( "Composite model" )
        @PropertyDescription ( "If true then created model definition will be composite." )
        public boolean isComposite()
        {
            return composite;
        }
        public void setComposite(boolean composite)
        {
            this.composite = composite;
        }

        @Override
        public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
        {
            DiagramType type = composite ? new SbgnCompositeDiagramType() : new SbgnDiagramType();
            String name = getName();
            ModelDefinition modelDefinition = new ModelDefinition(compartment, type.createDiagram(null, name, new DiagramInfo(name)), name);
            getProperties().forEach(dp -> modelDefinition.getAttributes().add(dp));
            return new DiagramElementGroup( modelDefinition );
        }
    }

    public static class ModelDefinitionPropertiesBeanInfo extends SbgnElementPropertiesBeanInfo
    {
        public ModelDefinitionPropertiesBeanInfo()
        {
            super(ModelDefinitionProperties.class);
        }

        @Override
        public void initProperties()
        {
            add("name");
            add("composite");
            add("properties");
        }
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, String name, Point point, Object properties)
    {
        try
        {
            if( type.equals(SubDiagram.class) )
            {
                if( properties instanceof Diagram )
                {
                    SubDiagram subDiagram = new SubDiagram(compartment, (Diagram)properties, name);
                    setNeccessaryAttributes(subDiagram);
                    compartment.put(subDiagram);
                    return new DiagramElementGroup( subDiagram );
                }
            }
            else if( type.equals(ModelDefinition.class) )
            {
                if( properties instanceof Diagram )
                {
                    ModelDefinition definition = new ModelDefinition(compartment, (Diagram)properties, name);
                    setNeccessaryAttributes(definition);
                    compartment.put(definition);
                    return new DiagramElementGroup( definition );
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While creating instance of type " + type.toString(), e);
        }
        return super.createInstance(compartment, type, name, point, properties);
    }

    public String generateUniqueName(@Nonnull Compartment compartment, Object type, Object properties) throws Exception
    {
        if( type.equals(SubDiagram.class) && properties instanceof Diagram )
            return generateUniqueNodeName(compartment, ( (Diagram)properties ).getName() + "_subDiagram");
        else if( type.equals(ModelDefinition.class) )
            return generateUniqueNodeName(compartment, ( (Diagram)properties ).getName());
        throw new IllegalArgumentException("Unknown element type, can not generate name");
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, Object properties)
    {
        try
        {
            if( type.equals(SubDiagram.class) || type.equals(ModelDefinition.class) )
            {
                String uniqueName = generateUniqueName(compartment, type, properties);
                return createInstance(compartment, type, uniqueName, point, properties);
            }
            else
            {
                return super.createInstance(compartment, type, point, properties);
            }

        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While creating instance of type " + type.toString(), e);
        }
        return null;
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type.equals( SubDiagram.class.getName() ) )
            return new SbgnSubDiagramProperties( Diagram.getDiagram( compartment ) );
        else if (type.equals( Bus.class ))
            return new SimpleBusProperties( Diagram.getDiagram( compartment ) );
        if( ModelDefinition.class.equals(type) )
            return new ModelDefinitionProperties(generateUniqueNodeName(Diagram.getDiagram(compartment), Type.TYPE_MODEL_DEFINITION));
        return super.getPropertiesByType(compartment, type, point);
    }

    @Override
    public boolean createByParent(Object type)
    {
        if( type instanceof Class )
            return !ModelDefinition.class.equals(type);
        return false;
    }

    @Override
    public boolean canAccept(Compartment parent, DiagramElement de)
    {
        if( parent instanceof DiagramContainer )
            return false;
        return super.canAccept(parent, de);
    }

}
