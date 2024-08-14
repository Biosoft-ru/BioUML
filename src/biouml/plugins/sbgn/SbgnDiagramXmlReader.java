package biouml.plugins.sbgn;

import org.w3c.dom.Element;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.State;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlReader;
import biouml.standard.type.Base;
import biouml.standard.type.Specie;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataElementPath;

public class SbgnDiagramXmlReader extends DiagramXmlReader
{
    @Override
    public Base getKernel(Element element, Compartment compartment)
    {
        String name = element.getAttribute( KERNEL_REF_ATTR );
        if (!name.startsWith(STUB))
            return super.getKernel(element, compartment);
        String simpleName = DataElementPath.create( name ).getName();
        String type = element.getAttribute(KERNEL_TYPE_ATTR);
        if( SBGNPropertyConstants.entityTypes.contains(type) )
        {
            Specie specie = new Specie(null, simpleName, type, SbgnUtil.getSBGNTypes());
            Element attrElement = getElement( element, ATTRIBUTES_ELEMENT );
            if( attrElement != null )
                fillProperties( attrElement, specie.getAttributes(), null );
            return specie;
        }
        return super.getKernel(element, compartment);
    }
    
    @Override
    public Compartment createCompartment(Element compartmentElement, String id, Base kernel, Compartment origin)
    {
        Compartment compartment = new Compartment(origin, id, kernel);
       if (kernel instanceof Specie && !(origin.getKernel() instanceof Specie))
           compartment.setRole(new VariableRole(compartment));
       return compartment;
    }
    
    @Override
    public Node createNode(Element nodeElement, String id, Base kernel, Compartment origin)
    {
       Node node = new Node(origin, id, kernel);
       if( kernel.getType().equals(Type.MATH_EQUATION) )
       {
           node.setRole(new Equation(node, Equation.TYPE_SCALAR, "unknown", "0"));
           node.setShowTitle(false);
       }
       else if(  kernel.getType().equals(Type.MATH_EVENT) )
       {
           node.setRole(new Event(node));
       }
       else if(  kernel.getType().equals(Type.MATH_FUNCTION) )
       {
           node.setRole(new Function(node));
           node.setShowTitle(false);
       }
       else if(kernel.getType().equals(Type.MATH_STATE) )
       {
           State state = new State(node);
           state.addOnEntryAssignment(new Assignment("unknown", "0"), false);
           state.addOnExitAssignment(new Assignment("unknown", "0"), false);
           node.setRole(state);
           return node;
       }
       return node;
    }
}
