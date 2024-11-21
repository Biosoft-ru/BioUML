package biouml.plugins.virtualcell.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

import javax.annotation.Nonnull;

import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.font.ColorFont;

public class VirtualCellDiagramViewBuilder extends DefaultDiagramViewBuilder
{

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getRole() instanceof TableCollectionDataSetProperties )
            return createDataSetView( container, node, (VirtualCellDiagramViewOptions)options, g );
        else if( node.getRole() instanceof ProcessProperties )
            return createProcessView( container, node, (VirtualCellDiagramViewOptions)options, g );
        return super.createNodeCoreView( container, node, options, g );
    }

    public boolean createDataSetView(CompositeView container, Node node, VirtualCellDiagramViewOptions options, Graphics g)
    {
        Dimension d = new Dimension( node.getShapeSize() );
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 70;
            d.height = 40;
            node.setShapeSize( d );
        }

        Brush brush = DefaultDiagramViewBuilder.getBrush( node, options.getDatasetBrush() );
        Pen pen = getBorderPen( node, options.getNodePen() );

        int xOffset = 15;
        int[] x = new int[] {0, xOffset, d.width - xOffset, d.width, d.width - xOffset, xOffset};
        int[] y = new int[] {d.height / 2, 0, 0, d.height / 2, d.height, d.height};

        CompositeView baseView = new CompositeView();
        PolygonView polygon = new PolygonView( pen, brush, x, y );
        baseView.add( polygon );
        container.add( baseView );

        ColorFont font = getTitleFont( node, options.getNodeTitleFont() );

        String titleStr = node.getTitle();
        if( !titleStr.isEmpty() && node.isShowTitle() )
        {
            ComplexTextView title = new ComplexTextView( node.getTitle(), font, options.getFontRegistry(),
                    ComplexTextView.TEXT_ALIGN_CENTER, g, Integer.MAX_VALUE );

            int yMode = ( title.getBounds().height + 10 ) < d.height ? CompositeView.Y_BB : CompositeView.Y_TT;
            container.add( title, CompositeView.X_CC | yMode, new Point( 0, 0 ) );
        }
        baseView.setModel( node );
        baseView.setActive( true );
        container.setModel( node );
        container.setLocation( node.getLocation() );
        node.setView( container );
        return false;
    }

    public boolean createProcessView(CompositeView container, Node node, VirtualCellDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 70;
            d.height = 40;
            node.setShapeSize( d );
        }

        Pen pen = getBorderPen( node, options.getNodePen() );

        ColorFont font = getTitleFont( node, options.getNodeTitleFont() );
        int titleMargin = 10;

        ComplexTextView title = null;
        if( node.isShowTitle() )
        {
            title = new ComplexTextView( node.getTitle(), font, options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, g,
                    Integer.MAX_VALUE );
            Rectangle textRect = title.getBounds();
            d.width = Math.max( d.width, textRect.width + titleMargin );
            d.height = Math.max( d.height, textRect.height + titleMargin );
        }

        CompositeView baseView = new CompositeView();

        Brush brush = DefaultDiagramViewBuilder.getBrush( node, options.getProcessBrush() );
        int round = Math.max( Math.min( Math.min( d.width, d.height ) / 3, 20 ), 2 );

        baseView.add( new BoxView( pen, brush, new RoundRectangle2D.Float( 0, 0, d.width, d.height, round, round ) ) );
        container.add( baseView );

        if( title != null )
            container.add( title, CompositeView.X_CC | CompositeView.Y_CC, new Point( 0, 0 ) );

        baseView.setModel( node );
        baseView.setActive( true );
        container.setModel( node );
        container.setLocation( node.getLocation() );
        node.setView( container );

        return false;
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {

        if( edge.getPath() == null )
            Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );
        Pen pen = getBorderPen( edge, options.getConnectionPen() );
        Brush brush = new Brush( Color.white );
        CompositeView view = new ArrowView( pen, brush, edge.getSimplePath(), null, ArrowView.createTriangleTip( pen, brush, 15, 5 ) );

        edge.setView( view );
        view.setModel( edge );
        view.setActive( true );
        return view;

        //        return super.createEdgeView( edge, options, g );
    }
    
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new VirtualCellDiagramViewOptions(null);
    }
}
