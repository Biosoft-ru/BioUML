package biouml.plugins.agentmodeling;

import org.w3c.dom.Element;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.model.util.DiagramXmlReader;
import biouml.standard.type.Base;

public class AgentModelDiagramXmlReader extends DiagramXmlReader
{
    @Override
    public Base getKernel(Element element, Compartment compartment)
    {
        return super.getKernel(element, compartment);
    }

    @Override
    public Compartment createCompartment(Element element, String id, Base kernel, Compartment origin)
    {
        Compartment c = super.createCompartment(element, id, kernel, origin);
        if( AgentModelSemanticController.isDynamicModule(c) )
            AgentModelSemanticController.addDynamicProperties(c);
        return c;
    }
    
    @Override
    protected Compartment readSubDiagram(Element element, Compartment origin)
    {
        Compartment c = super.readSubDiagram(element, origin);
        AgentModelSemanticController.addDynamicProperties(c);
        return c;
    }

    @Override
    public Node createNode(Element element, String id, Base kernel, Compartment origin)
    {
        Node n = super.createNode(element, id, kernel, origin);
        if( AgentModelSemanticController.isDynamicModule(n) )
            AgentModelSemanticController.addDynamicProperties(n);
        return n;
    }
}
