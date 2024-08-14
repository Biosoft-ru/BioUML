package biouml.standard.diagram;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.CollectionFactory;

public class NoteLinkEdgeCreator implements EdgeCreator
{
    @Override
    public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
    {
        if( !temporary && !( in.getKernel() instanceof Stub.Note ) && ! ( out.getKernel() instanceof Stub.Note ) )
            throw new IllegalArgumentException("One of selected nodes should be a note.");

        String name;
        if( temporary )
        {
            name = "stub";
        }
        else
        {
            Diagram diagram = Diagram.getDiagram(in);
            name = CollectionFactory.getRelativeName(in, diagram) + " -> " + CollectionFactory.getRelativeName(out, diagram);
        }
        return new Edge(new Stub.NoteLink(null, name), in, out);
    }
}