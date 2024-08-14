package biouml.plugins.physicell;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

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
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.ArrowView.Tip;
import ru.biosoft.graphics.font.ColorFont;

public class PhysicellDiagramViewBuilder extends DefaultDiagramViewBuilder
{

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( PhysicellConstants.TYPE_CELL_DEFINITION.equals( node.getKernel().getType() ) )
        {
            return createCellDefinitionView( container, node, (PhysicellDiagramViewOptions)options, g );
        }
        else if( PhysicellConstants.TYPE_SUBSTRATE.equals( node.getKernel().getType() ) )
        {
            return createSubstanceView( container, node, (PhysicellDiagramViewOptions)options, g );
        }
        return super.createNodeCoreView( container, node, options, g );
    }

    public boolean createCellDefinitionView(CompositeView container, Node node, PhysicellDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        ColorFont font = getTitleFont( node, options.getNodeTitleFont() );
        ComplexTextView title = new ComplexTextView( node.getTitle(), font, options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, g,
                options.getNodeTitleLimit() );
        Rectangle textRect = title.getBounds();
        int height = Math.max( d.height, textRect.height );
        int width = Math.max( d.width, textRect.width + 20 );
        int size = Math.max( height, width );
        Brush brush = getBrush( node, options.getCellDefinitionBrush() );
        node.setShapeSize( new Dimension( d.width, d.height ) );
        container.add( new EllipseView( getBorderPen( node, options.getNodePen() ), brush, 0, 0, size, size ) );
        container.add( title, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new PhysicellDiagramViewOptions( null );
    }

    @Override
    protected Class<? extends DefaultDiagramViewBuilder> getResourcesRoot()
    {
        return PhysicellDiagramViewBuilder.class;
    }

    public boolean createSubstanceView(CompositeView container, Node node, PhysicellDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        ColorFont font = getTitleFont( node, options.getNodeTitleFont() );
        ComplexTextView title = new ComplexTextView( node.getTitle(), font, options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_LEFT, g,
                options.getNodeTitleLimit() );
        Rectangle textRect = title.getBounds();
        int height = Math.max( d.height, textRect.height + 20 );
        int width = Math.max( d.width, textRect.width + 20 );
        Brush brush = getBrush( node, options.getSubstanceBrush() );
        node.setShapeSize( new Dimension( d.width, d.height ) );
        container.add( new BoxView( getBorderPen( node, options.getNodePen() ), brush, 0, 0, width, height ) );
        container.add( title, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = null;

        if( edge.getPath() == null )
            Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );

        if( PhysicellConstants.TYPE_SECRETION.equals( edge.getKernel().getType() ) )
            view = createSecretionEdgeView( edge, (PhysicellDiagramViewOptions)options, g );

        if( PhysicellConstants.TYPE_CHEMOTAXIS.equals( edge.getKernel().getType() ) )
            view = createChemotaxisEdgeView( edge, (PhysicellDiagramViewOptions)options, g );

        if( PhysicellConstants.TYPE_INTERACTION.equals( edge.getKernel().getType() ) )
            view = createInteractionEdgeView( edge, (PhysicellDiagramViewOptions)options, g );

        if( PhysicellConstants.TYPE_TRANSFORMATION.equals( edge.getKernel().getType() ) )
            view = createTransformationEdgeView( edge, (PhysicellDiagramViewOptions)options, g );

        if( view != null )
        {
            view.setModel( edge );
            view.setActive( true );
            edge.setView( view );
            return view;
        }
        return super.createEdgeView( edge, options, g );
    }

    public CompositeView createInteractionEdgeView(Edge edge, PhysicellDiagramViewOptions options, Graphics g)
    {
        Brush brush = getBrush( edge, new Brush( options.getInteractionPen().getColor() ) );
        SimplePath path = edge.getSimplePath();
        Pen pen = getBorderPen( edge, options.getInteractionPen() );
        Tip tip = createDefaultTriangleTip( pen, brush );
        return new ArrowView( pen, brush, path, null, tip );
    }

    public CompositeView createChemotaxisEdgeView(Edge edge, PhysicellDiagramViewOptions options, Graphics g)
    {
        Brush brush = getBrush( edge, new Brush( options.getChemotaxisPen().getColor() ) );
        SimplePath path = edge.getSimplePath();
        Pen pen = getBorderPen( edge, options.getChemotaxisPen() );
        Tip tip = createDefaultTriangleTip( pen, brush );
        return new ArrowView( pen, brush, path, tip, null );
    }

    public CompositeView createSecretionEdgeView(Edge edge, PhysicellDiagramViewOptions options, Graphics g)
    {
        Brush brush = getBrush( edge, new Brush( options.getSecretionPen().getColor() ) );
        SimplePath path = edge.getSimplePath();
        Pen pen = getBorderPen( edge, options.getSecretionPen() );
        Tip tip = createDefaultTriangleTip( pen, brush );
        return new ArrowView( pen, brush, path, null, tip );
    }

    public CompositeView createTransformationEdgeView(Edge edge, PhysicellDiagramViewOptions options, Graphics g)
    {
        Brush brush = getBrush( edge, new Brush( options.getTransformationPen().getColor() ) );
        SimplePath path = edge.getSimplePath();
        Pen pen = getBorderPen( edge, options.getTransformationPen() );
        Tip tip = createDefaultTriangleTip( pen, brush );
        return new ArrowView( pen, brush, path, null, tip );
    }

    protected Tip createDefaultTriangleTip(Pen pen, Brush brush)
    {
        return ArrowView.createTriangleTip( pen, brush, 15, 5 );
    }
}