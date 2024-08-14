package biouml.plugins.sbgn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram.PortOrientation;
import biouml.standard.diagram.CompositeDiagramViewBuilder;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import ru.biosoft.graph.CenterPointFinder;
import ru.biosoft.graph.OrientedPortFinder;
import ru.biosoft.graph.OrthogonalPathLayouter.Orientation;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graph.ShapeChanger;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.ArrowView.Tip;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.FigureView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.ShapeView;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.TruncatedView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

/**
 * @author Ilya
 * For the most part code is ported form "sbml-sbgn.xml" notation
 */
public class SbgnDiagramViewBuilder extends CompositeDiagramViewBuilder
{
    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getKernel() instanceof ConnectionPort )
        {
            return createPortView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        else if( node.getKernel().getType().equals(biouml.standard.type.Type.TYPE_REACTION) )
        {
            return createReactionView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        else if( node.getKernel().getType().equals(Type.TYPE_VARIABLE) )
        {
            return createVariableView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        else if( node.getKernel().getType().equals(Type.TYPE_UNIT_OF_INFORMATION) )
        {
            return createUnitOfInformationView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        else if( node.getKernel().getType().equals(Type.TYPE_SOURCE_SINK) )
        {
            return this.createSourceSinkView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        else if( node.getKernel().getType().equals(Type.TYPE_TABLE) )
        {
            return this.createTableView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        else if( node.getKernel().getType().equals(Type.TYPE_LOGICAL) )
        {
            return this.createLogicalView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        else if( node.getKernel().getType().equals(Type.TYPE_EQUIVALENCE) )
        {
            return this.createEquivalenceView(container, node, (SbgnDiagramViewOptions)options, g);
        }
        return super.createNodeCoreView(container, node, options, g);
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( compartment.getKernel().getType().equals(Type.TYPE_COMPLEX) )
        {
            return createComplexView(container, compartment, (SbgnDiagramViewOptions)options, g);
        }
        else if( compartment.getKernel().getType().equals(Type.TYPE_PHENOTYPE) )
        {
            return createPhenotypeView(container, compartment, (SbgnDiagramViewOptions)options, g);
        }
        else if( compartment.getKernel() instanceof Specie )
        {
            return createEntityView(container, compartment, (SbgnDiagramViewOptions)options, g);
        }
        else if( compartment.getKernel().getType().equals(Type.TYPE_COMPARTMENT) )
        {
            return createCompartmentView(container, compartment, (SbgnDiagramViewOptions)options, g);
        }
        return super.createCompartmentCoreView(container, compartment, options, g);
    }

    public boolean createPortView(CompositeView container, Node node, SbgnDiagramViewOptions options, Graphics g)
    {
        if( options.isBioUMLPorts() )
        {
            return super.createConnectionPortView( container, node, options, g );
        }
        Dimension d = new Dimension(15, 15);
        ColorFont font = getTitleFont(node, options.getPortTitleFont());
        ComplexTextView title = new ComplexTextView(node.getTitle(), font, options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_RIGHT, g,
                options.getNodeTitleLimit());
        Rectangle textRect = title.getBounds();
        d.width = Math.max(d.width, textRect.width + 3);
        d.height = Math.max(d.height, textRect.height + 3);
        PortOrientation orientation = (PortOrientation)node.getAttributes().getValue(PortOrientation.ORIENTATION_ATTR);

        String portType = node.getKernel().getType();
        Brush brush;
        if( portType.equals(biouml.standard.type.Type.TYPE_OUTPUT_CONNECTION_PORT) )
            brush = options.getOutputPortBrush();
        else if( portType.equals(biouml.standard.type.Type.TYPE_INPUT_CONNECTION_PORT) )
            brush = options.getInputPortBrush();
        else
            brush = options.getContactPortBrush();

        if( orientation.isVertical() )
            node.setShapeSize(new Dimension(d.width, d.height + d.width / 3));
        else
            node.setShapeSize(new Dimension(d.width + d.height / 3, d.height));

        int[] x = null;
        int[] y = null;
        Point titlePoint = null;
        switch( orientation )
        {
            case LEFT:
            {
                x = new int[] {0, d.width, d.width + d.height / 2, d.width, 0};
                y = new int[] {0, 0, d.height / 2, d.height, d.height};
                titlePoint = new Point( -d.height / 4, 0);
                break;
            }
            case TOP:
            {
                x = new int[] {0, d.width, d.width, d.width / 2, 0};
                y = new int[] {0, 0, d.height, 3 * d.height / 2, d.height};
                titlePoint = new Point(0, -d.height / 4);
                break;
            }
            case RIGHT:
            {
                x = new int[] { -d.height / 2, 0, d.width, d.width, 0};
                y = new int[] {d.height / 2, 0, 0, d.height, d.height};
                titlePoint = new Point(d.height / 4, 0);
                break;
            }
            default:
            {
                x = new int[] {0, d.width, d.width, d.width / 2, 0};
                y = new int[] {d.height, d.height, 0, -d.height / 2, 0};
                titlePoint = new Point(0, d.height / 4);
                break;
            }
        }
        container.add(new PolygonView(getBorderPen(node, options.getNodePen()), brush, x, y));
        container.add(title, CompositeView.X_CC | CompositeView.Y_CC, titlePoint);
        return false;
    }

    public boolean createComplexView(CompositeView container, Compartment compartment, SbgnDiagramViewOptions options, Graphics g)
    {
        Dimension d = new Dimension(compartment.getShapeSize());
        String multimerStr = getMultimerString(compartment);

        String cloneMarker = compartment.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_CLONE_MARKER);
        if( d.width < 50 && d.height < 50 )
        {
            d.width = 50;
            d.height = 50;
        }

        int multimerShift = 7;
        Brush brush = DefaultDiagramViewBuilder.getBrush(compartment, options.getComplexBrush());
        Pen pen = getBorderPen(compartment, options.getNodePen());

        int delta = Math.min(Math.min(15, d.width / 3), d.height / 3);
        int[] x = new int[] {delta, d.width - delta, d.width, d.width, d.width - delta, delta, 0, 0};
        int[] y = new int[] {0, 0, delta, d.height - delta, d.height, d.height, d.height - delta, delta};

        CompositeView baseView = new CompositeView();
        PolygonView polygon = new PolygonView(pen, brush, x, y);

        if( multimerStr != null )
        {
            PolygonView additional = new PolygonView(pen, brush, x, y);
            additional.move(multimerShift, multimerShift);
            baseView.add(additional);
            baseView.add(polygon);
            container.add(baseView);
            TextView mcount = new TextView(multimerStr, options.getNodeTitleFont(), g);
            BoxView multimerBox = new BoxView(pen, new Brush(Color.white), 25, -10, mcount.getBounds().width + 10,
                    mcount.getBounds().height + 4);
            container.add(multimerBox);
            container.add(mcount, CompositeView.X_LL | CompositeView.Y_TT, new Point(30, 2));
        }
        else
        {
            baseView.add(polygon);
            container.add(baseView);
        }

        if( cloneMarker != null )
        {
            Point markerTitleInsets = new Point(0, 0);
            TextView mTitle = new TextView(cloneMarker, options.getCloneFont(), g);
            View marker = new TruncatedView(new PolygonView(pen, options.getCloneBrush(), x, y));
            if( multimerStr != null )
            {
                View additionalMarker = new TruncatedView(new PolygonView(pen, options.getCloneBrush(), x, y));
                additionalMarker.move(multimerShift, multimerShift);
                container.add(additionalMarker);
                markerTitleInsets.y += multimerShift;
            }
            container.add(marker);
            container.add(mTitle, CompositeView.X_CC | CompositeView.Y_BB, markerTitleInsets);
        }

        ColorFont font = getTitleFont(compartment, options.getCustomTitleFont());

        String titleStr = compartment.getTitle();
        if( !titleStr.isEmpty() && compartment.isShowTitle() )
        {
            ComplexTextView title = createTitleView(titleStr, d, 0, font, options, ComplexTextView.TEXT_ALIGN_CENTER, g,
                    options.isShrinkNodeTitleSize());
            Point titleInsets = cloneMarker == null ? new Point(0, 10) : new Point(0, 22);
            if( multimerStr != null )
                titleInsets.x -= multimerShift / 2;

            int yMode = ( title.getBounds().height + 10 ) < d.height ? CompositeView.Y_BB : CompositeView.Y_TT;
            container.add(title, CompositeView.X_CC | yMode, titleInsets);
        }
        SbgnUtil.setCompartmentView(container, baseView, compartment);
        return false;
    }

    public static String getMultimerString(Node node)
    {
        Object multimerObj = node.getAttributes().getValue(SBGNPropertyConstants.SBGN_MULTIMER);
        String multimerStr = null;
        if( multimerObj instanceof Integer && (int)multimerObj > 1 )
            multimerStr = "N:" + (int)multimerObj;
        else if( multimerObj instanceof String )
            multimerStr = (String)multimerObj;
        return multimerStr;
    }

    public boolean createUnitOfInformationView(CompositeView container, Node node, SbgnDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        if( d.width == 0 && d.height == 0 )
            d = new Dimension(options.getUnitOfInformationSize());
        TextView title = new TextView(node.getTitle(), getTitleFont(node, options.getNodeTitleFont()), g);
        d.width = Math.max(d.width, title.getBounds().width + 10);
        d.height = Math.max(d.height, title.getBounds().height + 10);
        container.add(new BoxView(getBorderPen(node, options.getNodePen()), options.getUnitOfInformationBrush(), 0, 0, d.width, d.height));
        container.add(title, CompositeView.X_CC | CompositeView.Y_CC);
        return false;
    }



    public boolean createSourceSinkView(CompositeView container, Node node, SbgnDiagramViewOptions options, Graphics g)
    {
        Dimension d = options.getSourceSinkSize();
        Pen pen = getBorderPen(node, options.getNodePen());
        container.add(new EllipseView(pen, options.getSourceSinkBrush(), 0, 0, d.width, d.height));
        container.add(new LineView(pen, new Point(3, d.height + 3), new Point(d.width - 3, -3)));
        return false;
    }

    public boolean createLogicalView(CompositeView container, Node node, SbgnDiagramViewOptions options, Graphics g)
    {
        int d = options.getLogicalSize();
        Pen edgePen = getBorderPen(node, options.getEdgePen());
        Pen pen = getBorderPen(node, options.getNodePen());

        String type = node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR);
        TextView title = new TextView(type.toUpperCase(), getTitleFont(node, options.getNodeTitleFont()), g);
        int size = Math.max(title.getBounds().height, title.getBounds().width);
        d = Math.max(d, size);

        PortOrientation orientation = (PortOrientation)node.getAttributes().getValue(PortOrientation.ORIENTATION_ATTR);
        if( orientation.equals(PortOrientation.RIGHT) || orientation.equals(PortOrientation.LEFT) )
        {
            Dimension curD = node.getShapeSize();
            if( curD.width == 0 && curD.height == 0 )
                node.setShapeSize(new Dimension(d + 20, d));
            else
                d = Math.max(size, curD.height);

            container.add(new LineView(edgePen, 0, d / 2, node.getShapeSize().width - 2, d / 2));
            container.add(new EllipseView(pen, new Brush(Color.white), ( node.getShapeSize().width - d ) / 2, 0, d, d));
        }
        else if( orientation.equals(PortOrientation.TOP) || orientation.equals(PortOrientation.BOTTOM) )
        {
            Dimension curD = node.getShapeSize();
            if( curD.width == 0 && curD.height == 0 )
                node.setShapeSize(new Dimension(d, d + 20));
            else
                d = Math.max(size, curD.width);

            container.add(new LineView(edgePen, d / 2, 0, d / 2, node.getShapeSize().height - 2));
            container.add(new EllipseView(pen, new Brush(Color.white), 0, ( node.getShapeSize().height - d ) / 2, d, d));
        }

        container.add(title, CompositeView.X_CC | CompositeView.Y_CC);
        return false;
    }

    public boolean createEquivalenceView(CompositeView container, Node node, SbgnDiagramViewOptions options, Graphics g)
    {
        int d = options.getLogicalSize();
        int outlet = 10;
        Pen edgePen = getBorderPen(node, options.getEdgePen());
        Pen pen = getBorderPen(node, options.getNodePen());
        TextView title = new TextView("\u039e", getTitleFont(node, options.getNodeTitleFont()), g);
        d = Math.max(d, Math.max(title.getBounds().height, title.getBounds().width));
        PortOrientation orientation = (PortOrientation)node.getAttributes().getValue(PortOrientation.ORIENTATION_ATTR);
        if( orientation.equals(PortOrientation.RIGHT) || orientation.equals(PortOrientation.LEFT) )
        {
            container.add(new LineView(edgePen, 0, d / 2, d + outlet, d / 2));
            container.add(new EllipseView(pen, new Brush(Color.white), outlet / 2, 0, d, d));
        }
        else if( orientation.equals(PortOrientation.TOP) || orientation.equals(PortOrientation.BOTTOM) )
        {
            container.add(new LineView(edgePen, d / 2, 0, d / 2, d + outlet));
            container.add(new EllipseView(pen, new Brush(Color.white), 0, outlet / 2, d, d));
        }
        container.add(title, CompositeView.X_CC | CompositeView.Y_CC);
        node.setShapeSize(new Dimension(d, d));
        return false;
    }

    public boolean createVariableView(CompositeView container, Node node, SbgnDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        if( d.height == 0 && d.width == 0 )
            d = new Dimension(options.getVariableSize());
        TextView title = new TextView(node.getTitle(), getTitleFont(node, options.getNodeTitleFont()), g);
        d.width = Math.max(title.getBounds().width + 10, d.width);
        d.height = Math.max(title.getBounds().height + 10, d.height);
        d.width = Math.max(d.width, d.height);
        container.add(new BoxView(getBorderPen(node, options.getNodePen()), options.getVariableBrush(), 0, 0, d.width, d.height, d.height,
                d.height));
        container.add(title, CompositeView.X_CC | CompositeView.Y_CC);
        return false;
    }

    public boolean createCompartmentView(CompositeView container, Compartment compartment, SbgnDiagramViewOptions options, Graphics g)
    {
        Dimension d = compartment.getShapeSize();
        if( d.height == 0 && d.width == 0 )
            d = new Dimension();
        if( d.width == 0 && d.height == 0 )
        {
            d.width = 150;
            d.height = 150;
        }
        Brush brush = DefaultDiagramViewBuilder.getBrush(compartment, options.getCompartmentBrush());
        Pen pen = getBorderPen(compartment, options.getCompartmentPen());
        ColorFont font = getTitleFont(compartment, options.getCompartmentTitleFont());

        ComplexTextView title = new ComplexTextView(compartment.getTitle(), font, options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_RIGHT, g, d.width);
        int type = compartment.getShapeType();
        Point textPosition = new Point( ( d.width - title.getBounds().width ) - 5, 3);
        View baseView = null;
        if( type == Compartment.SHAPE_ELLIPSE )
        {
            textPosition = new Point(d.width / 2 - title.getBounds().width / 2, 3);
            baseView = new EllipseView(pen, brush, 0, 0, (float) ( d.width - pen.getWidth() ), (float) ( d.height - pen.getWidth() ));
        }
        else if( type == Compartment.SHAPE_ROUND_RECTANGLE )
        {
            RoundRectangle2D.Float rect = new RoundRectangle2D.Float(0, 0, (float) ( d.width - pen.getWidth() ),
                    (float) ( d.height - pen.getWidth() ), 40, 15);
            baseView = new BoxView(pen, brush, rect);

        }
        else if( type == Compartment.SHAPE_RECTANGLE )
        {
            RoundRectangle2D.Float rect = new RoundRectangle2D.Float(0, 0, (float) ( d.width - pen.getWidth() ),
                    (float) ( d.height - pen.getWidth() ), 0f, 0f);
            baseView = new BoxView(pen, brush, rect);
        }
        container.add(baseView);

        if( !compartment.getTitle().isEmpty() )
            container.add(title, CompositeView.X_LL | CompositeView.Y_TT, textPosition);

        if( baseView != null )
            SbgnUtil.setCompartmentView(container, baseView, compartment);

        return false;
    }

    public boolean createPhenotypeView(CompositeView container, Compartment node, SbgnDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 80;
            d.height = 40;
            node.setShapeSize(d);
        }
        Brush brush = DefaultDiagramViewBuilder.getBrush(node, options.getPhenotypeBrush());
        Pen pen = getBorderPen(node, options.getNodePen());
        ColorFont font = getTitleFont(node, options.getCustomTitleFont());
        int titleMargin = 15;
        ComplexTextView title = createTitleView(node.getTitle(), d, titleMargin, font, options, ComplexTextView.TEXT_ALIGN_CENTER, g,
                options.isShrinkNodeTitleSize());
        Rectangle textRect = title.getBounds();
        d.width = Math.max(d.width, textRect.width + titleMargin);
        d.height = Math.max(d.height, textRect.height + titleMargin);
        int xOffset = 15;
        int[] x = new int[] {0, xOffset, d.width - xOffset, d.width, d.width - xOffset, xOffset};
        int[] y = new int[] {d.height / 2, 0, 0, d.height / 2, d.height, d.height};
        container.add(new PolygonView(pen, brush, x, y));
        container.add(title, CompositeView.X_CC | CompositeView.Y_CC);
        SbgnUtil.setView(container, node);
        return false;
    }

    public boolean createReactionView(CompositeView container, Node node, SbgnDiagramViewOptions options, Graphics g)
    {
        Brush brush = new Brush(Color.white);
        Pen pen = getBorderPen(node, options.getNodePen());
        Pen edgePen = getBorderPen(node, options.getEdgePen());
        int xStart = 0;
        int yStart = 0;

        int d = options.getReactionSize();
        if( options.isOrientedReactions() )
        {
            PortOrientation orientation = Util.getPortOrientation(node);
            if( !orientation.isVertical() )
            {
                Dimension curD = node.getShapeSize();
                if( curD.width == 0 && curD.height == 0 )
                    node.setShapeSize(new Dimension(d + 20, d));
                container.add(new LineView(edgePen, 0, d / 2, node.getShapeSize().width, d / 2));
                xStart = ( node.getShapeSize().width - d ) / 2;
            }
            else
            {
                Dimension curD = node.getShapeSize();
                if( curD.width == 0 && curD.height == 0 )
                    node.setShapeSize(new Dimension(d, d + 20));
                container.add(new LineView(edgePen, d / 2, 0, d / 2, node.getShapeSize().height));
                yStart = ( node.getShapeSize().height - d ) / 2;
            }
        }
        else if( node.getShapeSize().width == 0 && node.getShapeSize().height == 0 )
        {
            node.setShapeSize(new Dimension(d, d));
        }

        String reactionType = node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_REACTION_TYPE);
        if( reactionType.equals(Type.TYPE_PROCESS) )
        {
            container.add(new BoxView(pen, brush, xStart, yStart, d, d));
        }
        else if( reactionType.equals(Type.TYPE_OMITTED_PROCESS) )
        {
            container.add(new BoxView(pen, brush, xStart, yStart, d, d));
            container.add(new PolygonView(pen, brush, new int[] {xStart + 3, xStart + 8}, new int[] {yStart + 3, yStart + 12}));
            container.add(new PolygonView(pen, brush, new int[] {xStart + 8, xStart + 13}, new int[] {yStart + 3, yStart + 12}));
        }
        else if( reactionType.equals(Type.TYPE_UNCERTAIN_PROCESS) )
        {
            container.add(new BoxView(pen, brush, xStart, yStart, d, d));
            container.add(new TextView("?", options.getNodeTitleFont(), g), CompositeView.X_CC | CompositeView.Y_TT);
        }
        else if( reactionType.equals(Type.TYPE_ASSOCIATION) )
        {
            container.add(new EllipseView(pen, new Brush(pen.getColor()), xStart, yStart, d, d));
        }
        else if( reactionType.equals(Type.TYPE_DISSOCIATION) )
        {
            int inset = d / 5;
            int size = d - 2 * inset;
            container.add(new EllipseView(pen, brush, xStart, yStart, d, d));
            container.add(new EllipseView(pen, brush, xStart + inset, yStart + inset, size, size));
        }

        if( node.isShowTitle() )
            createReactionTitle2(node, container, options, g);

        return false;
    }

    protected void createReactionTitle2(DiagramElement reaction, CompositeView diagramView, DiagramViewOptions options, Graphics g)
    {
        Node node = (Node)reaction;
        View titleView = new ComplexTextView(reaction.getTitle(), getTitleFont(reaction, options.getNodeTitleFont()),
                options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
        Rectangle tBounds = titleView.getBounds();
        titleView.setLocation(node.getLocation().x + ( node.getShapeSize().width - tBounds.width ) / 2,
                node.getLocation().y + node.getShapeSize().height);
        //dirty fix for situations where compartment is not created, like in layout
        //think of refactoring and don't add titleView to any 'parent' here
        if( reaction.getCompartment().getView() != null )
            ( (CompositeView)reaction.getCompartment().getView() ).add(titleView);
    }

    public boolean createEntityView(CompositeView container, Compartment node, SbgnDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 70;
            d.height = 40;
            node.setShapeSize(d);
        }

        Point titleInsets = new Point(0, 0);
        int multimerShift = 5;
        Pen pen = getBorderPen(node, options.getNodePen());
        String cloneMarker = node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_CLONE_MARKER);
        String multimerStr = getMultimerString(node);
        ColorFont font = getTitleFont(node, options.getCustomTitleFont());
        int titleMargin = 10;

        ComplexTextView title = null;
        if( node.isShowTitle() )
        {
            title = createTitleView(node.getTitle(), d, titleMargin, font, options, ComplexTextView.TEXT_ALIGN_CENTER, g,
                    options.isShrinkNodeTitleSize());
            Rectangle textRect = title.getBounds();
            d.width = Math.max(d.width, textRect.width + titleMargin);
            d.height = Math.max(d.height, textRect.height + titleMargin);
        }
        int w = d.width;
        int h = d.height;
        String entityType = node.getKernel().getType();
        CompositeView baseView = new CompositeView();
        if( entityType.equals(Type.TYPE_UNSPECIFIED) )
        {
            baseView.add(new EllipseView(pen, getBrush(node, options.getUnspecifiedBrush()), 0, 0, w, h));
            if( SbgnUtil.isClone(node) )
                baseView.add(new TruncatedView(new EllipseView(pen, options.getCloneBrush(), 0, 0, w, h)));
            container.add(baseView);
        }
        else if( entityType.equals(Type.TYPE_SIMPLE_CHEMICAL) )
        {
            multimerShift = 5;
            Brush brush = getBrush(node, options.getSimpleChemicalBrush());
            if( multimerStr != null )
                baseView.add(new BoxView(pen, brush, new RoundRectangle2D.Float(multimerShift, multimerShift, w, h, h, h)));
            baseView.add(new BoxView(pen, brush, new RoundRectangle2D.Float(0, 0, w, h, h, h)));
            if( SbgnUtil.isClone(node) )
            {
                if( multimerStr != null )
                    baseView.add(new TruncatedView(new BoxView(pen, options.getCloneBrush(),
                            new RoundRectangle2D.Float(multimerShift, multimerShift, w, h, h, h))));
                baseView.add(new TruncatedView(new BoxView(pen, options.getCloneBrush(), new RoundRectangle2D.Float(0, 0, w, h, h, h))));
            }
            container.add(baseView);
            if( multimerStr != null )//TODO: info box should be separate diagram element
            {
                TextView mcount = new TextView(multimerStr, options.getNodeTitleFont(), g);
                int labelXOffset = h / 2 - 5;
                BoxView multimerBox = new BoxView(pen, new Brush(Color.white), labelXOffset, -10, mcount.getBounds().width + 10,
                        mcount.getBounds().height + 4);
                container.add(multimerBox);
                container.add(mcount, CompositeView.X_LL | CompositeView.Y_TT, new Point(labelXOffset + 5, 2));
                titleInsets.y += ( multimerBox.getBounds().height / 2 - multimerShift ) / 2;
                titleInsets.x -= 3 / 2;
            }
        }
        else if( entityType.equals(Type.TYPE_MACROMOLECULE) )
        {
            Brush brush = DefaultDiagramViewBuilder.getBrush(node, options.getMacromoleculeBrush());
            int round = Math.max(Math.min(Math.min(d.width, h) / 3, 20), 2);
            if( multimerStr != null )
            {
                baseView.add(new BoxView(pen, brush, new RoundRectangle2D.Float(multimerShift, multimerShift, d.width, h, round, round)));
                baseView.add(new BoxView(pen, brush, new RoundRectangle2D.Float(0, 0, d.width, h, round, round)));
                container.add(baseView);
                TextView mcount = new TextView(multimerStr, options.getNodeTitleFont(), g);
                BoxView multimerBox = new BoxView(pen, new Brush(Color.white), 10, -10, mcount.getBounds().width + 10,
                        mcount.getBounds().height + 4);
                container.add(multimerBox);
                container.add(mcount, CompositeView.X_LL | CompositeView.Y_TT, new Point(15, 2));

                titleInsets.y += ( multimerBox.getBounds().height / 2 - multimerShift ) / 2;
                titleInsets.x -= multimerShift / 2;
            }
            else
            {
                baseView.add(new BoxView(pen, brush, new RoundRectangle2D.Float(0, 0, d.width, h, round, round)));
                container.add(baseView);
            }

            if( SbgnUtil.isClone(node) )
            {
                TextView mTitle = new TextView(cloneMarker, options.getCloneFont(), g);
                Point markerTitleInsets = new Point(titleInsets.x, 0);
                ShapeView marker = new TruncatedView(
                        new BoxView(pen, options.getCloneBrush(), new RoundRectangle2D.Float(0, 0, d.width, h, round, round)));
                if( multimerStr != null )
                {
                    container.add(new TruncatedView(new BoxView(pen, options.getCloneBrush(),
                            new RoundRectangle2D.Float(multimerShift, multimerShift, d.width, h, round, round))));
                    markerTitleInsets.y += multimerShift;
                }
                container.add(marker);
                container.add(mTitle, CompositeView.X_CC | CompositeView.Y_BB, markerTitleInsets);
            }
        }
        else if( entityType.equals(Type.TYPE_NUCLEIC_ACID_FEATURE) )
        {
            Brush brush = getBrush(node, options.getNucleicBrush());
            int[] x = new int[] {0, w, w, w, w - 10, 10, 0, 0};
            int[] y = new int[] {0, 0, h - 10, h, h, h, h, h - 10};
            int[] t = new int[] {0, 0, 0, 1, 0, 0, 1, 0};
            if( multimerStr != null )
            {
                FigureView additionalFig = new FigureView(pen, brush, x, y, t);
                additionalFig.move(multimerShift, multimerShift);
                baseView.add(additionalFig);
                baseView.add(new FigureView(pen, brush, x, y, t));
                container.add(baseView);
                TextView mcount = new TextView(multimerStr, options.getNodeTitleFont(), g);
                BoxView multimerBox = new BoxView(pen, new Brush(Color.white), 10, -10, mcount.getBounds().width + 10,
                        mcount.getBounds().height + 4);
                container.add(multimerBox);
                container.add(mcount, CompositeView.X_LL | CompositeView.Y_TT, new Point(15, 2));

                titleInsets.y += ( multimerBox.getBounds().height / 2 - multimerShift ) / 2;
                titleInsets.x -= multimerShift / 2;
            }
            else
            {
                baseView.add(new FigureView(pen, brush, x, y, t));
                container.add(baseView);
            }

            if( SbgnUtil.isClone(node) )
            {
                Point markerTitleInsets = new Point(titleInsets.x, 0);
                TextView mTitle = new TextView(cloneMarker, options.getCloneFont(), g);
                if( multimerStr != null )
                {
                    TruncatedView marker = new TruncatedView(new FigureView(pen, options.getCloneBrush(), x, y, t));
                    marker.move(multimerShift, multimerShift);
                    container.add(marker);
                    markerTitleInsets.y += multimerShift;
                }
                container.add(new TruncatedView(new FigureView(pen, options.getCloneBrush(), x, y, t)));
                container.add(mTitle, CompositeView.X_CC | CompositeView.Y_BB, markerTitleInsets);
            }
        }
        else if( entityType.equals(Type.TYPE_PERTURBING_AGENT) )
        {
            int[] x = new int[] {10, 0, w, w - 10, w, 0};
            int[] y = new int[] {h / 2, 0, 0, h / 2, h, h};
            baseView.add(new PolygonView(pen, getBrush(node, options.getPerturbingBrush()), x, y));
            container.add(baseView);
            if( SbgnUtil.isClone(node) )
                container.add(new TruncatedView(new PolygonView(pen, options.getCloneBrush(), x, y)));
        }

        if( node.isFixed() )
        {
            View pinView = createPinView();
            container.add(pinView, CompositeView.X_RR | CompositeView.Y_TT, new Point(5, 5));
        }

        if( title != null )
            container.add(title, CompositeView.X_CC | CompositeView.Y_CC, titleInsets);

        SbgnUtil.setCompartmentView(container, baseView, node);
        return false;
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = null;

        if( edge.getPath() == null )
            Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);

        if( edge.getKernel() instanceof SpecieReference || edge.getKernel() != null
                && ( edge.getKernel().getType().equals(Type.TYPE_PRODUCTION) || edge.getKernel().getType().equals(Type.TYPE_CONSUMPTION) )
                || edge.getKernel().getType().equals(Type.TYPE_REGULATION) )
            view = createReactionEdgeView(edge, (SbgnDiagramViewOptions)options, g);
        else if( edge.getKernel() != null && Type.TYPE_PORTLINK.equals(edge.getKernel().getType()) )
            view = createPortLinkView(edge, (SbgnDiagramViewOptions)options, g);
        else if( edge.getKernel().getType().equals(Base.TYPE_NOTE_LINK) )
            view = createNoteLinkView(edge, (SbgnDiagramViewOptions)options, g);
        else if( edge.getKernel().getType().equals(Type.TYPE_EQUIVALENCE_ARC) )
            view = createLogicalLinkView(edge, (SbgnDiagramViewOptions)options, g);
        else if( edge.getKernel().getType().equals(Type.TYPE_LOGIC_ARC) )
            view = createLogicalLinkView(edge, (SbgnDiagramViewOptions)options, g);

        if( view != null )
        {
            SbgnUtil.setView(view, edge);
            return view;
        }
        return super.createEdgeView(edge, options, g);
    }

    public CompositeView createNoteLinkView(Edge edge, SbgnDiagramViewOptions options, Graphics g)
    {
        return new ArrowView(getBorderPen(edge, options.getNoteLinkPen()), new Brush(Color.white), edge.getSimplePath(), 0, 0);
    }

    public CompositeView createLogicalLinkView(Edge edge, SbgnDiagramViewOptions options, Graphics g)
    {
        return new ArrowView(getBorderPen(edge, options.getEdgePen()), new Brush(Color.white), edge.getSimplePath(), 0, 0);
    }

    public CompositeView createPortLinkView(Edge edge, SbgnDiagramViewOptions options, Graphics g)
    {
        return new ArrowView(getBorderPen(edge, options.getEdgePen()), new Brush(Color.white), edge.getSimplePath(), 0, 0);
    }

    public CompositeView createReactionEdgeView(Edge edge, SbgnDiagramViewOptions options, Graphics g)
    {
        Brush brush = new Brush(Color.white);

        Reaction reaction = Util.isReaction(edge.getInput()) ? (Reaction)edge.getInput().getKernel()
                : Util.isReaction(edge.getOutput()) ? (Reaction)edge.getOutput().getKernel() : null;

        SimplePath path = edge.getSimplePath();

        Pen pen = getBorderPen(edge, options.getEdgePen());
        String role = ( edge.getKernel() instanceof SpecieReference ) ? ( (SpecieReference)edge.getKernel() ).getRole()
                : edge.getKernel().getType();

        boolean inputTip = false;
        if( role.equals(SpecieReference.PRODUCT) || role.equals(Type.TYPE_PRODUCTION) )
        {
            if( Util.isReaction(edge.getOutput()) )
                inputTip = true;
            String edgeType = edge.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_EDGE_TYPE);
            Tip tip = null;
            if( Type.TYPE_INHIBITION.equals(edgeType) )
                tip = createDefaultLineTip(brush, pen, true);
            else
            {
                tip = createDefaultTriangleTip(pen, new Brush(pen.getColor()));
                brush = options.getEdgeTipBrush();
            }
            return new ArrowView(pen, brush, path, inputTip ? tip : null, inputTip ? null : tip);
        }
        else if( role.equals(SpecieReference.REACTANT) || role.equals(Type.TYPE_CONSUMPTION) )
        {
            if( reaction != null && reaction.isReversible() )
            {
                if( Util.isReaction(edge.getOutput()) )
                    inputTip = true;
                Tip tip = createDefaultTriangleTip(pen, new Brush(pen.getColor()));
                return new ArrowView(pen, options.getEdgeTipBrush(), path, inputTip ? tip : null, inputTip ? null : tip);
            }
            return new ArrowView(pen, brush, path, 0, 0);
        }
        else if( role.equals(SpecieReference.MODIFIER) || role.equals(Type.TYPE_REGULATION) )
        {
            String edgeType = Type.TYPE_CATALYSIS;
            if( Type.TYPE_LOGIC_ARC.equals( edge.getAttributes().getValueAsString( SBGNPropertyConstants.SBGN_EDGE_TYPE ) ) )
            {
                edgeType = Type.TYPE_LOGIC_ARC;
            }
            else if ( edge.getKernel() instanceof SpecieReference
                    && ((SpecieReference) edge.getKernel()).getModifierAction() != null )
                edgeType = ( (SpecieReference)edge.getKernel() ).getModifierAction(); //try to use speciereference field instead of attribute
            else
                edgeType = edge.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_EDGE_TYPE);

            if( Util.isReaction(edge.getInput()) )
                inputTip = true;
            Tip tip = null;

            switch( edgeType )
            {
                case Type.TYPE_MODULATION:
                    tip = ArrowView.createDiamondTip(pen, brush, 5, 10, 5);
                    break;
                case Type.TYPE_STIMULATION:
                    tip = createDefaultTriangleTip(pen, brush);
                    break;
                case Type.TYPE_CATALYSIS:
                    tip = ArrowView.createEllipseTip(pen, brush, 6);
                    break;
                case Type.TYPE_INHIBITION:
                    brush = new Brush(pen.getColor());
                    tip = createDefaultLineTip(brush, pen, false);
                    break;
                case Type.TYPE_NECCESSARY_STIMULATION:
                    tip = ArrowView.createTriggerTip(pen, brush, 19, 8, 4, 3);
                    break;
                case Type.TYPE_EQUIVALENCE_ARC:
                case Type.TYPE_LOGIC_ARC:
                    tip = ArrowView.createSimpleTip(pen, 0, 0);
                    break;
                default:
                    tip = ArrowView.createSimpleTip(pen, 0, 0);
            }

            if( tip != null )
                return new ArrowView(pen, brush, path, inputTip ? tip : null, inputTip ? null : tip);
        }
        return new ArrowView(new Pen(3, Color.RED), new Brush(Color.red), path, null, ArrowView.createTriangleTip(pen, brush, 15, 7));
    }

    protected Tip createDefaultLineTip(Brush brush, Pen pen, boolean isProduct)
    {
        int w = isProduct ? 3 : 5;
        return ArrowView.createLineTip(pen, brush, w, 8);
    }

    protected Tip createDefaultTriangleTip(Pen pen, Brush brush)
    {
        return ArrowView.createTriangleTip(pen, brush, 15, 5);
    }

    @Override
    public boolean calculateInOut(Edge edge, Point in, Point out)
    {
        super.calculateInOut(edge, in, out, 0, 0);

        Diagram diagram = Diagram.getDiagram(edge);

        Node inNode = edge.getInput();
        Node outNode = edge.getOutput();

        SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)diagram.getViewOptions();
        if( edge.getKernel() instanceof SpecieReference
                || ( edge.getKernel() instanceof Stub && ( Type.TYPE_PRODUCTION.equals(edge.getKernel().getType())
                        || Type.TYPE_CONSUMPTION.equals(edge.getKernel().getType())
                        || Type.TYPE_REGULATION.equals(edge.getKernel().getType()) ) )
                || Util.isPortLink(edge) || Type.TYPE_EQUIVALENCE_ARC.equals(edge.getKernel().getType()) )
        {
            if( ( Util.isReaction(inNode) && options.isOrientedReactions() ) || SbgnUtil.isLogical(inNode) || Util.isPort(inNode)
                    || SbgnUtil.isEquivalence(inNode) )
                selectPoint(in, out, inNode, edge);
            if( ( Util.isReaction(outNode) && options.isOrientedReactions() ) || SbgnUtil.isLogical(outNode) || Util.isPort(outNode)
                    || SbgnUtil.isEquivalence(outNode) )
                selectPoint(out, in, outNode, edge);
        }
        return true;
    }

    public void selectPoint(Point p, Point otherPoint, Node node, Edge edge)
    {
        Rectangle reactionBounds = getNodeBounds(node);
        PortOrientation orientation = (PortOrientation)node.getAttributes().getValue(ConnectionPort.PORT_ORIENTATION);
        Point left = new Point(reactionBounds.x, reactionBounds.y + reactionBounds.height / 2);
        Point right = new Point(reactionBounds.x + reactionBounds.width, reactionBounds.y + reactionBounds.height / 2);
        Point down = new Point(reactionBounds.x + reactionBounds.width / 2, reactionBounds.y + reactionBounds.height);
        Point up = new Point(reactionBounds.x + reactionBounds.width / 2, reactionBounds.y);
        Point input = null;
        Point output = null;
        Point[] aux = null;
        switch( orientation )
        {
            case LEFT:
            {
                input = right;
                output = left;
                aux = new Point[] {up, down};
                break;
            }
            case RIGHT:
            {
                input = left;
                output = right;
                aux = new Point[] {up, down};
                break;
            }
            case TOP:
            {
                input = down;
                output = up;
                aux = new Point[] {left, right};
                break;
            }
            default:
            {
                input = up;
                output = down;
                aux = new Point[] {left, right};
                break;
            }
        }

        if( SbgnUtil.isLogical(node) )
        {
            if( edge.getKernel() instanceof SpecieReference )
                p.setLocation(input);
            else
                p.setLocation(output);
            return;
        }
        else if( SbgnUtil.isEquivalence(node) )
        {
            if( SbgnUtil.isSubType(edge.getOtherEnd(node)) )
                p.setLocation(input);
            else
                p.setLocation(output);
            return;
        }
        else if( Util.isPort(node) )
        {
            if( Util.isPortLink(edge) )
                p.setLocation(input);
            return;
        }


        String role;
        if( edge.getKernel() instanceof SpecieReference )
            role = ( (SpecieReference)edge.getKernel() ).getRole();
        else
        {
            role = edge.getKernel().getType();
            role = Type.TYPE_CONSUMPTION.equals(role) ? SpecieReference.REACTANT
                    : Type.TYPE_PRODUCTION.equals(role) ? SpecieReference.PRODUCT : role;
        }
        switch( role )
        {
            case SpecieReference.REACTANT:
            {
                p.setLocation(input.x, input.y);
                break;
            }
            case SpecieReference.PRODUCT:
            {
                p.setLocation(output.x, output.y);
                break;
            }
            default:
            {
                int index = findNearestPoint(aux, otherPoint);
                p.setLocation(aux[index].x, aux[index].y);
                break;
            }
        }
    }

    public static int findNearestPoint(Point[] points, Point p)
    {
        if( points.length == 0 )
            return -1;
        int result = 0;
        double distance = p.distance(points[0]);
        for( int i = 1; i < points.length; i++ )
        {
            double nextDistance = p.distance(points[i]);
            if( nextDistance < distance )
            {
                distance = nextDistance;
                result = i;
            }
        }
        return result;
    }

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new SbgnDiagramViewOptions();
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        SbgnDiagramViewOptions options = ( (SbgnDiagramViewOptions)Diagram.getDiagram(node).getViewOptions() );
        if( Util.isReaction(node) && !options.isOrientedReactions() )
        {
            Rectangle bounds = getNodeBounds(node);
            return new CenterPointFinder(bounds);
        }
        else if( Util.isReaction(node) )
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
        else if( SbgnUtil.isLogical(node) )
        {
            OrientedPortFinder portFinder = new OrientedPortFinder();
            for( Edge e : node.edges() )
            {
                Node otherNode = e.getOtherEnd(node);
                if( Util.isReaction(otherNode) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.TOP);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.RIGHT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.LEFT);
                }
                else if( Util.isModifier(e) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.LEFT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.TOP);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.RIGHT);
                }
            }
            return portFinder;
        }
        else if( Util.isPort(node) )
        {
            OrientedPortFinder portFinder = new OrientedPortFinder();
            for( Edge e : node.edges() )
            {
                if( Util.isPortLink(e) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.LEFT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.TOP);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.RIGHT);
                }
            }
            return portFinder;
        }
        else if( SbgnUtil.isEquivalence(node) )
        {
            OrientedPortFinder portFinder = new OrientedPortFinder();
            for( Edge e : node.edges() )
            {
                Node otherNode = e.getOtherEnd(node);
                if( SbgnUtil.isSubType(otherNode) )
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.LEFT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.TOP);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.RIGHT);
                }
                else
                {
                    portFinder.addPort(e, Orientation.TOP, Orientation.TOP);
                    portFinder.addPort(e, Orientation.RIGHT, Orientation.RIGHT);
                    portFinder.addPort(e, Orientation.BOTTOM, Orientation.BOTTOM);
                    portFinder.addPort(e, Orientation.LEFT, Orientation.LEFT);
                }
            }
            return portFinder;
        }
        return super.getPortFinder(node);
    }

    @Override
    public ShapeChanger getShapeChanger(Node node)
    {
        //TODO: refactor - this method use the same code as create view for reaction, port and logical operator
        if( Util.isReaction(node) )
        {
            SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)Diagram.getDiagram(node).getViewOptions();
            if( !options.isOrientedReactions() )
                return null;

            int size = options.getReactionSize();
            int longSize = size + 20;
            ShapeChanger changer = new ShapeChanger();
            changer.setSize(PortOrientation.TOP.toString(), new Dimension(size, longSize));
            changer.setSize(PortOrientation.BOTTOM.toString(), new Dimension(size, longSize));
            changer.setSize(PortOrientation.LEFT.toString(), new Dimension(longSize, size));
            changer.setSize(PortOrientation.RIGHT.toString(), new Dimension(longSize, size));
            return changer;
        }
        else if( SbgnUtil.isLogical(node) )
        {
            SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)Diagram.getDiagram(node).getViewOptions();
            int d = options.getLogicalSize();
            String type = node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR);
            TextView title = new TextView(type.toUpperCase(), getTitleFont(node, options.getNodeTitleFont()),
                    ApplicationUtils.getGraphics());
            int size = Math.max(title.getBounds().height, title.getBounds().width);
            size = Math.max(d, size);
            int longSize = size + 20;
            ShapeChanger changer = new ShapeChanger();
            changer.setSize(PortOrientation.TOP.toString(), new Dimension(size, longSize));
            changer.setSize(PortOrientation.BOTTOM.toString(), new Dimension(size, longSize));
            changer.setSize(PortOrientation.LEFT.toString(), new Dimension(longSize, size));
            changer.setSize(PortOrientation.RIGHT.toString(), new Dimension(longSize, size));
            return changer;
        }
        else if( Util.isPort(node) )
        {
            SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)Diagram.getDiagram(node).getViewOptions();

            Dimension dim = new Dimension(15, 15);
            ColorFont font = getTitleFont(node, options.getPortTitleFont());
            TextView title = new TextView(node.getTitle(), font, ApplicationUtils.getGraphics());
            Rectangle textRect = title.getBounds();
            dim.width = Math.max(dim.width, textRect.width + 10);
            dim.height = Math.max(dim.height, textRect.height + 10);
            ShapeChanger changer = new ShapeChanger();
            changer.setSize(PortOrientation.TOP.toString(), new Dimension(dim.width, dim.height + dim.width / 2));
            changer.setSize(PortOrientation.BOTTOM.toString(), new Dimension(dim.width, dim.height + dim.width / 2));
            changer.setSize(PortOrientation.LEFT.toString(), new Dimension(dim.width + dim.height / 2, dim.height));
            changer.setSize(PortOrientation.RIGHT.toString(), new Dimension(dim.width + dim.height / 2, dim.height));
            return changer;
        }
        return null;
    }

    public static ComplexTextView createTitleView(String text, @Nonnull Dimension d, int margin, ColorFont font, DiagramViewOptions options,
            int textAlignment, Graphics g, boolean shrinkToNodeSize)
    {
        int maxStringSize = options.getNodeTitleLimit();
        Map<String, ColorFont> fontRegistry = options.getFontRegistry();
        if( !shrinkToNodeSize )
            return new ComplexTextView(text, font, fontRegistry, textAlignment, g, maxStringSize);

        String baseText = text;
        int length = text.length();
        int limit = (int)Math.max(0, Math.min(d.getWidth() - margin, maxStringSize));
        ComplexTextView titleView = new ComplexTextView(text, font, fontRegistry, textAlignment, g, limit);
        while( text.length() > 3 && ( titleView.getBounds().getHeight() > d.getHeight() - margin ) )
        {
            length--;
            text = baseText.substring(0, length).concat("...");
            titleView = new ComplexTextView(text, font, fontRegistry, textAlignment, g, limit);
        }
        return titleView;
    }
}
