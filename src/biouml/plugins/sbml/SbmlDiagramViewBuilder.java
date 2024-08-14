package biouml.plugins.sbml;

import java.awt.Graphics;

import javax.annotation.Nonnull;
import ru.biosoft.graphics.CompositeView;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.standard.diagram.CompositeDiagramViewBuilder;
import biouml.standard.diagram.PathwayDiagramViewBuilder;
import biouml.standard.type.Type;

public class SbmlDiagramViewBuilder extends CompositeDiagramViewBuilder
{
    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        if( edge.getKernel().getType().equals(Type.TYPE_DEPENDENCY) )
        {
            PathwayDiagramViewBuilder builder = new PathwayDiagramViewBuilder();
            return builder.createDependencyView(edge, g);
        }
        return super.createEdgeView(edge, viewOptions, g);
    }
}
