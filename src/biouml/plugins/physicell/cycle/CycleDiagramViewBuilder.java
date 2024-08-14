package biouml.plugins.physicell.cycle;

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
import biouml.plugins.physicell.PhaseProperties;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.ArrowView.Tip;
import ru.biosoft.graphics.font.ColorFont;

public class CycleDiagramViewBuilder extends DefaultDiagramViewBuilder
{

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( CycleConstants.TYPE_PHASE.equals( node.getKernel().getType() ) )
        {
            return createPhaseView( container, node, (CycleDiagramViewOptions)options, g );
        }
        return super.createNodeCoreView( container, node, options, g );
    }

    public boolean createPhaseView(CompositeView container, Node node, CycleDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        ColorFont titleFont = getTitleFont( node, options.getNodeTitleFont() );
        ColorFont font = getTitleFont( node, options.getDefaultFont() );

        TextView title = new TextView( node.getTitle(), titleFont, g );

        Rectangle textRect = title.getBounds();
        int height = Math.max( d.height, textRect.height );
        int width = Math.max( d.width, textRect.width );
        node.setShapeSize( new Dimension( d.width, d.height ) );
        PhaseProperties phase = node.getRole( PhaseProperties.class );
        if( phase.isDivisionAtExit() )
        {
            ComplexTextView divisionTitle = new ComplexTextView( "Division on exit", font, options.getFontRegistry(),
                    CompositeView.X_LL | CompositeView.Y_TT, g, options.getNodeTitleLimit() );
            container.add( divisionTitle, CompositeView.X_LL | CompositeView.Y_TT, new Point( 10, 20 ) );
            width = Math.max( width, divisionTitle.getBounds().width + 10 );
        }
        else if( phase.isRemovalAtExit() )
        {
            ComplexTextView dieTitle = new ComplexTextView( "Removal on exit", font, options.getFontRegistry(),
                    CompositeView.X_LL | CompositeView.Y_TT, g, options.getNodeTitleLimit() );
            container.add( dieTitle, CompositeView.X_LL | CompositeView.Y_TT, new Point( 10, 40 ) );
            width = Math.max( width, dieTitle.getBounds().width + 10 );
        }
        container.add( new BoxView( options.getNodePen(), null, new RoundRectangle2D.Float( 0, 0, width, height, 20, 20 ) ) );
        container.add( title, CompositeView.X_CC | CompositeView.Y_TT );
        return false;
    }

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new CycleDiagramViewOptions( null );
    }

    @Override
    protected Class<? extends DefaultDiagramViewBuilder> getResourcesRoot()
    {
        return CycleDiagramViewBuilder.class;
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = null;

        if( edge.getPath() == null )
            Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );

        if( CycleConstants.TYPE_TRANSITION.equals( edge.getKernel().getType() ) )
            view = createTransitionEdgeView( edge, (CycleDiagramViewOptions)options, g );

        if( view != null )
        {
            view.setModel( edge );
            view.setActive( true );
            edge.setView( view );
            return view;
        }
        return super.createEdgeView( edge, options, g );
    }

    public CompositeView createTransitionEdgeView(Edge edge, CycleDiagramViewOptions options, Graphics g)
    {
        Brush brush = getBrush( edge, new Brush( Color.white ) );
        SimplePath path = edge.getSimplePath();
        Pen pen = getBorderPen( edge, options.getTransitionPen() );
        Tip tip = createDefaultTriangleTip( pen, brush );
        return new ArrowView( pen, brush, path, null, tip );
    }

    protected Tip createDefaultTriangleTip(Pen pen, Brush brush)
    {
        return ArrowView.createTriangleTip( pen, brush, 15, 5 );
    }
}