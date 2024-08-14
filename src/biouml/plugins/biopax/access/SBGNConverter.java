package biouml.plugins.biopax.access;

import java.awt.Dimension;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.model.xml.XmlDiagramViewOptions;
import biouml.plugins.biopax.BioPAXModuleType;
import biouml.plugins.biopax.BioPAXSQLModuleType;
import biouml.plugins.biopax.BioPAXTextModuleType;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.type.Base;
import biouml.standard.type.Complex;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;

/**
 * @author lan
 *
 */
public class SBGNConverter extends DiagramTypeConverterSupport
{

    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        convertCompartment(diagram, diagram);
        ReferenceType type = ReferenceTypeRegistry.getReferenceType(Module.getModule(diagram));
        if(type != null)
            diagram.getAttributes().add(new DynamicProperty(ReferenceTypeRegistry.REFERENCE_TYPE_PD, String.class, type.toString()));

        if( diagram.getViewOptions() instanceof XmlDiagramViewOptions )
        {
            DynamicPropertySet options = ( (XmlDiagramViewOptions)diagram.getViewOptions() ).getOptions();
            options.setValue( "customTitleFont", new ColorFont( "Arial", 0, 12 ) );
            options.setValue( "nodeTitleFont", new ColorFont( "Arial", 0, 12 ) );
            options.setValue( "nodeTitleLimit", 100 );
        }

        return diagram;
    }
    
    protected void convertCompartment(Compartment compartment, Diagram diagram) throws Exception
    {
        for(Node node: compartment.getNodes())
        {
            DiagramElement[] elements = convertDiagramElement(node, diagram);
            if(elements != null)
            {
                compartment.remove(node.getName());
                for(DiagramElement element: elements)
                {
                    compartment.put(element);
                }
            }
        }
    }

    @Override
    public DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram) throws Exception
    {
        Base base = de.getKernel();
        if(base == null) return null;
        Module module = Module.optModule(base);
        if(module == null) return null;
        if(!(module.getType() instanceof BioPAXSQLModuleType) && !(module.getType() instanceof BioPAXTextModuleType)
                && !(module.getType() instanceof BioPAXModuleType)) return null;
        
        if(de instanceof Node)
        {
            Node node = null;
            XmlDiagramType xmlDiagramType = (XmlDiagramType)diagram.getType();
            String typeStr = xmlDiagramType.getKernelTypeName(base.getClass());
            if( typeStr == null )
                typeStr = xmlDiagramType.getDefaultTypeName();
            if( typeStr == null )
                typeStr = "";
            boolean isCompartment = xmlDiagramType.checkCompartment(typeStr);
            if( isCompartment )
            {
                node = new Compartment(de.getOrigin(), base);
                node.setShapeSize(new Dimension(0, 0));//reset dimension
            }
            else
            {
                node = new Node(de.getOrigin(), base);
            }
            SemanticController sc = xmlDiagramType.getSemanticController();
            if( sc instanceof XmlDiagramSemanticController )
            {
                DynamicPropertySet attributes = ( (XmlDiagramSemanticController)sc ).createAttributes(typeStr);
                for(DynamicProperty attribute : attributes)
                {
                    node.getAttributes().add(attribute);
                }
                node = (Node) ( (XmlDiagramSemanticController)sc ).getPrototype().validate((Compartment)node.getOrigin(), node);
            }
            // Copied from biouml.plugins.biopax.reader.BioPAXReader.addXmlNodeProperties(Node)
            // TODO: merge duplicated code
            if( base instanceof Reaction )
            {
                String xmlType = "process";
                String type = (String)base.getAttributes().getValue("Type");
                if( type.equals("ComplexAssembly") )
                {
                    xmlType = "association";
                }
                else if( type.equals("Degradation") )
                {
                    xmlType = "dissociation";
                }
                node.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, xmlType));
            }
            else if( node instanceof Compartment && ( base instanceof Stub || base instanceof Complex ) )
            {
                node.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, "complex"));
                node.setShowTitle( false );
            }
            else if( base.getOrigin() != null )
            {
                String entityType = "macromolecule";
                if( base instanceof Substance )
                {
                    entityType = "simple chemical";
                }
                node.getAttributes().add(new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, "entity"));
                node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, entityType));
            }
            return new DiagramElement[] {node};
        }
        return null;
    }
}
