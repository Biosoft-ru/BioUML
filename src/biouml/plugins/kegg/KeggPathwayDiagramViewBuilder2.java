package biouml.plugins.kegg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.graph.OrientedPortFinder;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graph.ShapeChanger;
import ru.biosoft.graph.OrthogonalPathLayouter.Orientation;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.ArrowView.Tip;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.font.ColorFont;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram.PortOrientation;
import biouml.standard.diagram.Util;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub.ConnectionPort;

/**
 * ViewBuilder is using in kegg_recon graphical notation.
 * Is necessary for correct layout of edges
 */
public class KeggPathwayDiagramViewBuilder2 extends DefaultDiagramViewBuilder
{
    @Override
    public boolean calculateInOut(Edge edge, Point in, Point out)
    {
        super.calculateInOut(edge, in, out);

        Node inNode = edge.getInput();
        Node outNode = edge.getOutput();

        if( edge.getKernel() instanceof SpecieReference )
        {
            if( Util.isReaction(inNode) )
                selectPoint(in, out, inNode, edge);
            if( Util.isReaction(outNode) )
                selectPoint(out, in, outNode, edge);
        }
        return true;
    }

    public void selectPoint(Point p, Point otherPoint, Node node, Edge edge)
    {
        Rectangle reactionBounds = getNodeBounds(node);
        PortOrientation orientation = (PortOrientation)node.getAttributes().getValue(ConnectionPort.PORT_ORIENTATION);

        int middleY = reactionBounds.height / 2;
        int middleX = reactionBounds.width / 2;

        Point left = new Point(reactionBounds.x, reactionBounds.y + middleY);
        Point right = new Point(reactionBounds.x + reactionBounds.width, reactionBounds.y + middleY);
        Point down = new Point(reactionBounds.x + middleX, reactionBounds.y + reactionBounds.height);
        Point up = new Point(reactionBounds.x + middleX, reactionBounds.y);
        Point input = null;
        Point output = null;
        switch( orientation )
        {
            case LEFT:
            {
                input = right;
                output = left;
                break;
            }
            case RIGHT:
            {
                input = left;
                output = right;
                break;
            }
            case TOP:
            {
                input = down;
                output = up;
                break;
            }
            default:
            {
                input = up;
                output = down;
                break;
            }
        }

        String role = ( (SpecieReference)edge.getKernel() ).getRole();

        if( role.equals(SpecieReference.REACTANT) )
            p.setLocation(input.x, input.y);
        else
            p.setLocation(output.x, output.y);
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        if( Util.isReaction(node) )
        {
            OrientedPortFinder portFinder = new OrientedPortFinder();
            for( Edge e : node.edges() )
            {
                if( Util.isProduct(e) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.TOP);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.RIGHT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.LEFT);
                }
                else if( Util.isReactant(e) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.LEFT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.TOP);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.RIGHT);
                }
                else if( Util.isModifier(e) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.LEFT);
                    portFinder.addPort(e, Orientation.TOP, Orientation.RIGHT);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.TOP);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.LEFT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.RIGHT);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.TOP);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.BOTTOM);
                }
            }
            return portFinder;
        }
        return super.getPortFinder(node);
    }

    @Override
    public Rectangle getNodeBounds(Node node)
    {
        if( node.getView() == null )
        {
            Diagram diagram = Diagram.getDiagram(node); //here we have to get original XmlDiagramTypeViewBuilder to create node view
            diagram.getType().getDiagramViewBuilder().createNodeView(node, diagram.getViewOptions(), ApplicationUtils.getGraphics());
        }
        Rectangle result = node.getView().getBounds();
        result.setLocation(node.getLocation());
        return result;
    }

    @Override
    public ShapeChanger getShapeChanger(Node node)
    {
        //TODO: refactor - this method use the same code as create view for reaction
        if( Util.isReaction(node) )
        {
            long modifiers = KeggUtil.getModifiers(node);
                            
            int gridX = 80;
            int gridY = 32;
            int insets = 6;
            int offsets = 10;

            Dimension horizontal;
            Dimension vertical;
            
            if (modifiers == 0)
            {
                return null;
            }
            if( modifiers == 2 )
            {
                horizontal = new Dimension(gridX, 2 * gridY + insets);
                vertical = new Dimension(2 * gridX + insets, gridY);
            }
            else if( modifiers == 1 )
            {
                horizontal = new Dimension(gridX, gridY);
                vertical = new Dimension(gridX, gridY);
            }
            else
            {
                horizontal = new Dimension(2 * gridX + insets, 2 * gridY + insets);
                vertical = new Dimension(2 * gridX + insets, 2 * gridY + insets);
            }

            horizontal.setSize(horizontal.width + 2*offsets, horizontal.height);
            vertical.setSize(vertical.width, vertical.height + 2*offsets);

            ShapeChanger changer = new ShapeChanger();
            changer.setSize(PortOrientation.TOP.toString(), vertical);
            changer.setSize(PortOrientation.BOTTOM.toString(), vertical);
            changer.setSize(PortOrientation.LEFT.toString(), horizontal);
            changer.setSize(PortOrientation.RIGHT.toString(), horizontal);
            return changer;
        }
        return null;
    }
    
    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( Util.isReaction(node) )
            return createReactionView(container, node, options, g);
        else if( "molecule-substance".equals(node.getKernel().getType()) )
            return createMoleculeView(container, node, options, g);
        else if ("diagram-reference".equals(node.getKernel().getType()) )
                return createDiagramReference(container, node, options, g);
        return super.createNodeCoreView(container, node, options, g);
    }

    public boolean createDiagramReference(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        if (d == null)
            d = new Dimension(60,30);
        Brush brush = getBrush(node, new Brush(Color.white));
        Pen pen = getBorderPen(node,  options.getDefaultPen());
        ColorFont font = getTitleFont(node, new ColorFont("Arial", Font.BOLD, 16, Color.black));
        ComplexTextView title = new ComplexTextView(node.getTitle(), font, options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, d.width/10, g);
        RoundRectangle2D.Float rect2 = new RoundRectangle2D.Float(0, 0, d.width, d.height, 10, 10);
        container.add(new BoxView(pen, brush, rect2));
        container.add(title, CompositeView.X_CC | CompositeView.Y_CC, new Point(0, 0));
        return false;
    }

    public boolean createMoleculeView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getKernel().getAttributes().getValue("StopMolecule") != null )
        {
            node.setVisible(false);
            return false;
        }
        Dimension d = new Dimension(16, 16);
        Brush brush = getBrush(node, new Brush(Color.white));
        Pen pen = getBorderPen(node, options.getDefaultPen());
        EllipseView ellipse = new EllipseView(pen, brush, 0, 0, d.width, d.height);
        container.add(ellipse, CompositeView.X_CC | CompositeView.Y_CC, new Point( 0, 0));
        ColorFont font = getTitleFont(node, options.getDefaultFont());
        TextView title = new TextView(node.getTitle(), font, g);
        container.add(title, CompositeView.X_CC | CompositeView.Y_TB, new Point(0, 0));
        return false;
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        if( edge.getPath() == null )
            Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);

        if( edge.getKernel() instanceof SpecieReference )
        {
            if( ! ( (SpecieReference)edge.getKernel() ).isReactantOrProduct() )
                return new CompositeView();
            CompositeView view = new CompositeView();
            Pen pen = getBorderPen(edge, options.getDefaultPen());
            Brush brush = getBrush(edge, options.getConnectionBrush());
            SimplePath path = edge.getSimplePath();
            boolean fromReaction = Util.isReaction(edge.getInput());
            Reaction reaction = fromReaction ? (Reaction)edge.getInput().getKernel() : (Reaction)edge.getOutput().getKernel();

            Tip tip = Util.isProduct(edge) || ( Util.isReactant(edge) && reaction.isReversible() )
                    ? ArrowView.createTriangleTip(pen, brush, 10, 3) : null;

            ArrowView arrow = fromReaction ? new ArrowView(pen, brush, path, null, tip) : new ArrowView(pen, brush, path, tip, null);
            arrow.setModel(edge);
            arrow.setActive(true);
            view.add(arrow);

            view.setModel(edge);
            view.setActive(false);

            return view;
        }
        return super.createEdgeView(edge, options, g);
    }

    public boolean createReactionView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Pen pen = getBorderPen(node, options.getDefaultPen());
        PortOrientation orientation = Util.getPortOrientation(node);

        boolean horizontal = !orientation.isVertical();
        Map<String, Brush> ecClasses = new HashMap<>();
        Map<String, Brush> classTitles = new HashMap<>();
        int nClasses = 0;
        Brush brush = getBrush(node, new Brush(Color.white));
        Brush refBrush = brush;
        Edge[] edges = node.getEdges();
        String maxHighlight = "Default";
        for( int i = 0; i < edges.length; i++ )
        {
            Edge edge = edges[i];
            if( ! ( edge.getKernel() instanceof SpecieReference ) || ( (SpecieReference)edge.getKernel() ).isReactantOrProduct() )
                continue;
            Node modifier = edge.getOtherEnd(node);
            String nodeHighlight = modifier.getPredefinedStyle();
            Brush boxBrush = getBrush(modifier, brush);

            if( "highlight3".equals(nodeHighlight) )
            {
                maxHighlight = nodeHighlight;
                refBrush = boxBrush;
            }
            else if( "highlight1".equals(nodeHighlight) && !"highlight3".equals(maxHighlight) )
            {
                maxHighlight = nodeHighlight;
                refBrush = boxBrush;
            }
            else if( "highlight2".equals(nodeHighlight) && !"highlight1".equals(maxHighlight) && !"highlight3".equals(maxHighlight) )
            {
                maxHighlight = nodeHighlight;
                refBrush = boxBrush;
            }

            String titleStr = modifier.getAttributes().getValueAsString("EC");
            if( titleStr != null && !titleStr.isEmpty() )
            {
                if( !ecClasses.containsKey(titleStr) )
                {
                    ecClasses.put(titleStr, boxBrush);
                    nClasses++;
                    if( nClasses >= 4 )
                        break;
                }
            }
            else
            {
                titleStr = modifier.getTitle();
                classTitles.put(titleStr, boxBrush);
            }
        }
        for( String title : classTitles.keySet() )
        {
            if( nClasses >= 4 )
                break;
            nClasses++;
            String oldTitle = title;
            if( title.length() > 8 )
                title = title.substring(0, 8);
            ecClasses.put(title, classTitles.get(oldTitle));
        }

        Reaction reaction = (Reaction)node.getKernel();
        boolean ref = false;
        Map<String, Brush> ecRefs = new HashMap<>();
        int nRefs = 0;
        DatabaseReference[] dbrefs = reaction.getDatabaseReferences();
        if( dbrefs != null )
        {
            for( int i = 0; i < dbrefs.length; i++ )
            {
                //TODO: remove this HACK
                if( dbrefs[i].getDatabaseName().equals("MIR:00000004") && dbrefs[i].getId().indexOf(":") == -1 )
                {
                    ref = true;
                    ecRefs.put(dbrefs[i].getId(), refBrush);
                    nRefs++;
                    if( nRefs >= 4 )
                        break;
                }
            }
        }
        if( ref )
        {
            ecClasses = ecRefs;
            nClasses = nRefs;
        }

        Dimension d = new Dimension(80, 32); //size of single box
        int i = 0;
        int insets = 6;

        ColorFont font = getTitleFont(node, options.getDefaultFont());

        Map<String, TextView> titles = new HashMap<>();
        for( String ecClass : ecClasses.keySet() )
        {
            TextView title = new TextView(ecClass, font, g);
            titles.put(ecClass, title);
            Rectangle titleBox = title.getBounds();
            d.width = Math.max(d.width, titleBox.width + 6);
            d.height = Math.max(d.height, titleBox.height + 6);
        }

        CompositeView boxes = new CompositeView();
        for( Map.Entry<String, Brush> ecClassEntry : ecClasses.entrySet() )
        {
            int x = 0;
            int y = ( d.height + insets ) * i;
            if( nClasses == 4 && i >= 2 )
            {
                x = d.width + insets;
                y = ( d.height + insets ) * ( i - 2 );
            }

            if( nClasses == 2 && i > 0 )
            {
                if( !horizontal )
                {
                    y = 0;
                    x = d.width + insets;
                }
            }

            if( nClasses == 3 )
            {
                if( horizontal )
                {
                    if( i == 0 )
                        x = ( d.width + insets ) / 2;
                    else if( i == 2 )
                        x = d.width + insets;
                    if( i == 2 )
                        y = d.height + insets;
                }
                else
                {
                    if( i == 0 )
                    {
                        x = d.width + insets;
                        y = 0;
                    }
                    else if( i == 1 )
                    {
                        y = d.height / 2 + insets;
                        x = 0;
                    }
                    else if( i == 2 )
                    {
                        x = d.width + insets;
                        y = d.height + insets;
                    }
                }
            }
            CompositeView boxContainer = new CompositeView();
            String ecClass = ecClassEntry.getKey();
            Brush br = ecClassEntry.getValue();
            if( br == null )
                br = brush;
            boxContainer.add(new BoxView(pen, br, 0, 0, d.width, d.height));
            boxContainer.add(titles.get(ecClass), CompositeView.X_CC | CompositeView.Y_CC, new Point(0, 0));
            boxContainer.move(x, y);
            boxes.add(boxContainer);
            i++;
        }

        if( nClasses > 0 )
        {
            int height = boxes.getBounds().height;
            int width = boxes.getBounds().width;
            SimplePath p;
            if( horizontal )
            {
                p = new SimplePath(new Point( -10, height / 2), new Point(width + 10, height / 2));
            }
            else
            {
                p = new SimplePath(new Point(width / 2, -10), new Point(width / 2, height + 10));
            }
            //we need this for correct work of ExpressionFilter
            container.add( new BoxView( new Pen( 1f, Color.white ), new Brush( Color.white ), 0, 0, width, height ) );

            container.add(new ArrowView(pen, brush, p, null, null));
            container.add(boxes);
        }

        if( nClasses == 0 )
            container.add(new EllipseView(pen, brush, 0, 0, 5, 5));
        return false;
    }
}
