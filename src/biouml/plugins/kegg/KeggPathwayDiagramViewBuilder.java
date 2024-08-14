package biouml.plugins.kegg;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import javax.annotation.Nonnull;

import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.kegg.type.Glycan;
import biouml.standard.diagram.PathwayDiagramViewBuilder;
import biouml.standard.diagram.PathwayDiagramViewOptions;
import biouml.standard.type.Base;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;

/**
 * KEGG patway diagram view builders has several tricks to get maximum
 * similarity with original human drawn diagrams.
 */
public class KeggPathwayDiagramViewBuilder extends PathwayDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new KeggPathwayDiagramViewOptions(null);
    }

    ///////////////////////////////////////////////////////////////////
    // Compound & Glycan title issues
    //

    /**
     * Here we have added special iteraton to generate titles for compounds
     * and glycans.
     */
    @Override
    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g)
    {
        CompositeView view = super.createDiagramView(diagram, g);
        createNodeTitleViews(diagram, diagram.getViewOptions(), g);
        view.updateBounds();
        return view;
    }

    protected void createNodeTitleViews(Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        for(DiagramElement de: compartment)
        {
            if( de instanceof Node )
                createNodeTitleView((Node)de, options, g);
        }
    }

    protected void createNodeTitleView(Node node, DiagramViewOptions options, Graphics g)
    {
        Base kernel = node.getKernel();
        if( ! ( kernel instanceof Substance ) && ! ( kernel instanceof Glycan ) )
            return;

        String title = kernel.getTitle();
        View titleView = createTitleView(title, options, g, 15);
        CompositeView view = (CompositeView)node.getView();
        view.add(titleView, CompositeView.X_CC | CompositeView.Y_BT);
    }

    protected View createTitleView(String title, DiagramViewOptions options, Graphics g, int maxStringLength)
    {
        CompositeView titleView = new CompositeView();

        for( int i = 0, j = 0; i < title.length(); i++ )
        {
            if( ( i - j > maxStringLength && ( title.charAt(i) == ' ' || title.charAt(i) == '-' || title.charAt(i) == ',' || title
                    .charAt(i) == '/' ) )
                    || i == title.length() - 1 )
            {
                ComplexTextView text = new ComplexTextView(title.substring(j, i + 1), options.getNodeTitleFont(),
                        options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
                titleView.add(text, CompositeView.X_CC | CompositeView.Y_BT);
                j = i + 1;
            }
        }

        return titleView;
    }

    ///////////////////////////////////////////////////////////////////
    // KEGG specific core views
    //

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        Base kernel = node.getKernel();
        if( Type.TYPE_DIAGRAM_REFERENCE.equals(kernel.getType()) )
            return createDiagramReferenceView(container, node, viewOptions, g);

        super.createNodeCoreView(container, node, viewOptions, g);
        return false;
    }

    protected boolean createDiagramReferenceView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        String title = node.getTitle();
        if( title.startsWith("TITLE:") )
            title = title.substring(6);
        View textView = createTitleView(title, viewOptions, g, 15);

        Rectangle bounds = textView.getBounds();
        Dimension prefferedSize = node.getShapeSize() != null ? node.getShapeSize() : new Dimension(0, 0);
        int width = (int)Math.max(prefferedSize.width, bounds.getWidth() + 10);
        int height = (int)Math.max(prefferedSize.height, bounds.getHeight() + 4);
        RectangularShape shape = new RoundRectangle2D.Float(0, 0, width, height, 25, 25);

        CompositeView view = new CompositeView();
        view.add(new BoxView(viewOptions.getDefaultPen(), null, shape));
        view.add(textView, CompositeView.X_CC | CompositeView.Y_CC);
        container.add(view);

        return false;
    }


    @Override
    protected boolean createSubstanceCoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g)
    {
        Pen p = options.getDefaultPen();
        container.add(new EllipseView(p, null, 0, 0, 8, 8));
        return false;
    }

    @Override
    protected boolean createProteinCoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g,
            String functionalState, String configuration, String modification)
    {
        Base kernel = node.getKernel();

        String nodeName = kernel.getName();
        if( nodeName.startsWith("EC ") )
        {
            nodeName = nodeName.substring(3);
        }
        else
        {
            nodeName = kernel.getTitle();
        }

        TextView text = new TextView(nodeName, options.getNodeTitleFont(), g);
        Rectangle bounds = text.getBounds();
        Dimension prefferedSize = node.getShapeSize() != null ? node.getShapeSize() : new Dimension(0, 0);
        int width = (int)Math.max(prefferedSize.width, bounds.getWidth() + 4);
        int height = (int)Math.max(prefferedSize.height, bounds.getHeight());

        container.add(new BoxView(options.getDefaultPen(), null, 0, 0, width, height));
        container.add(text, CompositeView.X_CC | CompositeView.Y_CC);

        return false;
    }

    /**
     * One more dirty tricks:
     * if enzyme view overlaps reaction view, then we are using enzyme view as a reaction view.
     */
    @Override
    public @Nonnull CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getKernel() != null && node.getKernel().getType().equals(Type.TYPE_REACTION) )
        {
            // try to find the enzyme node
            Node enzymeNode = node.edges().flatMap( Edge::nodes )
                .findFirst( n -> n.getKernel().getType().equals(Type.TYPE_PROTEIN) && n.getView().getBounds().contains(node.getLocation()) )
                .orElse( null );

            if( enzymeNode != null )
            {
                node.setView(enzymeNode.getView());
                return new CompositeView();
            }
        }

        return super.createNodeView(node, options, g);
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        Base input = edge.getInput().getKernel();
        Base output = edge.getOutput().getKernel();
        if( input instanceof Protein && output instanceof Reaction )
        {
            Rectangle rect = edge.getInput().getView().getBounds();
            Point location = edge.getOutput().getLocation();
            if( location.x >= rect.x && location.x <= rect.x + rect.width && location.y >= rect.y && location.y <= rect.y + rect.height )
                return new CompositeView();
        }
        return super.createEdgeView(edge, viewOptions, g);
    }
    
    @Override
    protected ArrowView.Tip getRelationEndTip(Edge edge, Pen pen, PathwayDiagramViewOptions options)
    {
        ArrowView.Tip endTip = null;
        Base kernel = edge.getInput().getKernel();
        if( isReaction(kernel) )
            endTip = ArrowView.createArrowTip(pen, getBrush(edge, getReactionBrush(kernel, options)), 10, 10, 4);
        return endTip;
    }
}
