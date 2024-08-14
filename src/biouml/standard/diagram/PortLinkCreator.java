package biouml.standard.diagram;

import javax.annotation.Nonnull;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;

public class PortLinkCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        if( !temporary && !Util.isPort(in) && !Util.isPort(out) )
            throw new IllegalArgumentException("One of selected nodes should be a port.");
        return new Edge(new Stub(null, in.getName() + " -> " + out.getName(), "portlink"), in, out);
    }
}