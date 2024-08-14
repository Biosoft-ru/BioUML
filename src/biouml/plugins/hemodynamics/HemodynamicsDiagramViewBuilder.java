package biouml.plugins.hemodynamics;

import java.awt.Color;
import java.awt.Graphics;

import javax.annotation.Nonnull;

import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.PathwaySimulationDiagramViewBuilder;
import biouml.standard.type.Base;

/**
 * @author Ilya
 */
public class HemodynamicsDiagramViewBuilder extends PathwaySimulationDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new HemodynamicsDiagramViewOptions(null);
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        Base kernel = node.getKernel();
        String type = kernel.getType();

        if( HemodynamicsType.HEART.equals(type) )
        {
            return createHeartCoreView(container, node, (HemodynamicsDiagramViewOptions)viewOptions, g);
        }
        else if( HemodynamicsType.BIFURCATION.equals(type) )
        {
            return createBifurcationCoreView(container, node, (HemodynamicsDiagramViewOptions)viewOptions, g);
        }
        else if( HemodynamicsType.CONTROL_POINT.equals(type) )
        {
            return createControlPointCoreView( container, node, viewOptions, g );
        }
        return super.createNodeCoreView(container, node, viewOptions, g);
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        if (edge.getPath() == null)
            Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);
        
        Base kernel = edge.getKernel();
        String type = kernel.getType();
        
        CompositeView view = null;
        
        if( HemodynamicsType.VESSEL.equals(type) )
            view =  createArteryView(edge, (HemodynamicsDiagramViewOptions)viewOptions, g);
        else if (HemodynamicsType.CONTROL_LINK.equals(type) )
            view = createControlLinkView(edge, (HemodynamicsDiagramViewOptions)viewOptions, g);
        
        if (view != null)
        {
            view.setActive(true);
            view.setModel(edge);
            return view;
        }
        return super.createEdgeView(edge, viewOptions, g);
    }

    public boolean createHeartCoreView(CompositeView container, Node node, HemodynamicsDiagramViewOptions options, Graphics g)
    {
        EllipseView ellipse = new EllipseView(options.getDefaultPen(), new Brush(Color.red), 0, 0, 40, 40);
        container.add(ellipse);
        return true;
    }

    protected CompositeView createArteryView(Edge edge, HemodynamicsDiagramViewOptions options, Graphics g)
    {
        return new ArrowView(options.getVesselPen(), null, edge.getSimplePath(), 0, 0);
    }
    
    protected CompositeView createControlLinkView(Edge edge, HemodynamicsDiagramViewOptions options, Graphics g)
    {
        return new ArrowView(options.getDefaultPen(), null, edge.getSimplePath(), 0, 0);
    }

    protected boolean createBifurcationCoreView(CompositeView container, Node node, HemodynamicsDiagramViewOptions options, Graphics g)
    {
        EllipseView view = new EllipseView(options.getDefaultPen(), options.getBifurcationColor(), 0, 0, options.getBifurcationRadius(), options.getBifurcationRadius());
        container.add(view);
        return true;
    }

    protected boolean createControlPointCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        EllipseView ellipse = new EllipseView(options.getDefaultPen(), new Brush(Color.gray), 0, 0, 10, 10);
        container.add(ellipse);
        return true;
    }
}
