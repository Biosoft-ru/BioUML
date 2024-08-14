package biouml.plugins.sbgn.extension;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import javax.annotation.Nonnull;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.ArrowView.Tip;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.ImageView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolylineView;
import ru.biosoft.graphics.font.ColorFont;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramViewBuilder;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbgn.Type;

public class SbgnExDiagramViewBuilder extends SbgnDiagramViewBuilder
{
    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        if( options instanceof SbgnDiagramViewOptions
                && biouml.standard.type.Type.TYPE_SEMANTIC_RELATION.equals( edge.getKernel().getType() ) )
        {
            SbgnDiagramViewOptions sbgnOptions = (SbgnDiagramViewOptions)options;
            if( edge.getPath() == null )
                Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );

            Pen pen = getBorderPen( edge, sbgnOptions.getEdgePen() );
            Brush brush = getBrush( edge, sbgnOptions.getEdgeTipBrush() );


            String edgeType = edge.getAttributes().getValueAsString( SBGNPropertyConstants.SBGN_EDGE_TYPE );

            ArrowView arrow = new ArrowView( pen, null, edge.getSimplePath(), null, generateTip( edgeType, pen, brush ) );

            if( !edge.getTitle().isEmpty() )
            {
                ColorFont titleFont = getTitleFont( edge, sbgnOptions.getCustomTitleFont() );
                ComplexTextView text = new ComplexTextView( edge.getTitle(), titleFont, sbgnOptions.getFontRegistry(),
                        ComplexTextView.TEXT_ALIGN_CENTER, 15, g );

                Point insets;
                if( arrow.getPathView() != null )
                    insets = arrow.getPathView().getMiddlePoint();
                else
                {
                    insets = new Point( ( edge.getInPort().x + edge.getOutPort().x ) / 2 - 7,
                            ( edge.getInPort().y + edge.getOutPort().y ) / 2 );
                }
                arrow.add( text, CompositeView.X_UN | CompositeView.Y_UN, insets );
            }

            SbgnUtil.setView( arrow, edge );
            return arrow;
        }
        return super.createEdgeView( edge, options, g );
    }

    @Override
    public boolean createEntityView(CompositeView container, Compartment node, SbgnDiagramViewOptions options, Graphics g)
    {
        if( !node.getKernel().getType().equals( biouml.plugins.sbgn.Type.TYPE_NUCLEIC_ACID_FEATURE ) )
            return super.createEntityView( container, node, options, g );

        if( "miRNA".equals( node.getAttributes().getValueAsString( "nodeType" ) ) )
            return createMiRNAView( container, node, options, g );
        else
            return createGeneView( container, node, options, g );

    }

    public static int minMax(int val, int min, int max)
    {
        return Math.min( Math.max( val, min ), max );
    }

    public boolean createGeneView(CompositeView container, Compartment node, SbgnDiagramViewOptions options, Graphics g)
    {

        Dimension d = node.getShapeSize();
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 70;
            d.height = 40;
        }

        ComplexTextView title = null;
        int coreWidth = 40;
        int coreHeight = 30;

        if( node.isShowTitle() && !node.getTitle().isEmpty() )
        {
            ColorFont font = getTitleFont( node, options.getCustomTitleFont() );
            title = createTitleView( node.getTitle(), d, 10, font, options, ComplexTextView.TEXT_ALIGN_CENTER, g,
                    options.isShrinkNodeTitleSize() );
            Rectangle textRect = title.getBounds();

            int titleMargin = 10;

            coreWidth = textRect.width + titleMargin;
            coreHeight = textRect.height + titleMargin;
        }

        int minOutlet = 15;
        int maxOutlet = 40;
        int minArrowHeight = 10;
        int maxArrowHeight = 30;
        int newWidth = Math.max( d.width, coreWidth + 2 * minOutlet );
        int newHeight = Math.max( d.height, coreHeight + minArrowHeight + 5 );

        int outletW = minMax( ( newWidth - coreWidth ) / 2, minOutlet, maxOutlet );
        int arrowH = minMax( newHeight - coreHeight - 5, minArrowHeight, maxArrowHeight );

        int boxW = Math.max( newWidth - 2 * outletW, coreWidth );
        int boxH = Math.max( newHeight - arrowH - 5, coreHeight );

        int mainMiddleH = 5 + arrowH + boxH / 2;
        Pen p = getBorderPen( node, options.getNodePen() );

        int penOffset = (int)Math.ceil( p.getWidth() / 2 );

        container.add( new BoxView( p, getBrush( node, options.getNucleicBrush() ), outletW, arrowH, boxW, boxH ) );

        if( title != null )
            container.add( title, CompositeView.X_CC | CompositeView.Y_CC, new Point( 0, 0 ) );
        container.add( new LineView( p, penOffset, mainMiddleH, outletW - penOffset, mainMiddleH ) );
        container.add( new LineView( p, outletW + boxW + penOffset, mainMiddleH, newWidth - penOffset, mainMiddleH ) );

        int halfway = outletW + (int) ( boxW * 0.8 );

        PolylineView l = new PolylineView( p );
        l.addPoint( newWidth / 2, arrowH );
        l.addPoint( newWidth / 2, 5 );
        l.addPoint( halfway, 5 );
        container.add( l );

        l = new PolylineView( p );
        l.addPoint( halfway - 7, 0 );
        l.addPoint( halfway, 5 );
        l.addPoint( halfway - 7, 10 );
        container.add( l );
        SbgnUtil.setView( container, node );

        return false;
    }

    public boolean createMiRNAView(CompositeView container, Compartment node, SbgnDiagramViewOptions options, Graphics g)
    {
        try
        {
            String iconId = ClassLoading.getResourceLocation( getClass(), "resources/miRNA.png" );
            Image image = IconFactory.getIconById( iconId ).getImage();//TODO: use ImageDescriptor
            ImageView view = new ImageView( image, 0, 0 );
            view.setPath( iconId );
            container.add( view );
        }
        catch( Exception ex )
        {

        }
        ComplexTextView title = new ComplexTextView( node.getTitle(), getTitleFont( node, options.getNodeTitleFont() ),
                options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, g, options.getNodeTitleLimit() );
        container.add( title, CompositeView.X_CC | CompositeView.Y_BT, new Point( 0, 5 ) );

        SbgnUtil.setView( container, node );
        return false;
    }

    @Override
    protected Class<? extends SbgnDiagramViewBuilder> getResourcesRoot()
    {
        return SbgnDiagramViewBuilder.class;
    }

    protected Tip generateTip(String edgeType, Pen pen, Brush brush)
    {
        if( Type.TYPE_MODULATION.equals( edgeType ) )
        {
            return ArrowView.createDiamondTip( pen, brush, 5, 10, 5 );
        }
        else if( Type.TYPE_STIMULATION.equals( edgeType ) )
        {
            return createDefaultTriangleTip( pen, brush );
        }
        else if( Type.TYPE_CATALYSIS.equals( edgeType ) )
        {
            return ArrowView.createEllipseTip( pen, brush, 6 );
        }
        else if( Type.TYPE_INHIBITION.equals( edgeType ) )
        {
            return createDefaultLineTip( new Brush( pen.getColor() ), pen, false );
        }
        else if( Type.TYPE_NECCESSARY_STIMULATION.equals( edgeType ) )
        {
            return ArrowView.createTriggerTip( pen, brush, 19, 8, 4, 3 );
        }
        else if( Type.TYPE_EQUIVALENCE_ARC.equals( edgeType ) || Type.TYPE_LOGIC_ARC.equals( edgeType ) )
        {
            return ArrowView.createSimpleTip( pen, 0, 0 );
        }
        else
        {
            return ArrowView.createSimpleTip( pen, 0, 0 ); //default
        }
    }
}
