package biouml.plugins.bionetgen.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.RoundRectangle2D;

import javax.annotation.Nonnull;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.PathwaySimulationDiagramViewBuilder;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.util.TextUtil2;

public class BionetgenDiagramViewBuilder extends PathwaySimulationDiagramViewBuilder
{
    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( ! ( options instanceof BionetgenDiagramViewOptions ) )
            return super.createNodeCoreView( container, node, options, g );

        if( BionetgenUtils.isMoleculeType( node ) )
        {
            return createMoleculeTypeView( container, node, (BionetgenDiagramViewOptions)options, g );
        }
        else if( BionetgenUtils.isMoleculeComponent( node ) )
        {
            return createMoleculeComponentView( container, node, (BionetgenDiagramViewOptions)options, g );
        }
        else if( BionetgenUtils.isReaction( node ) )
        {
            return createReactionView( container, (BionetgenDiagramViewOptions)options );
        }
        else if( BionetgenUtils.isEquation( node ) )
        {
            return super.createEquationView( container, node, (BionetgenDiagramViewOptions)options, g );
        }
        return super.createNodeCoreView( container, node, options, g );
    }

    private boolean createReactionView(CompositeView container, BionetgenDiagramViewOptions options)
    {
        container.add( new BoxView( options.getDefaultPen(), new Brush( Color.white ), 0, 0, 10, 10 ) );
        return false;
    }

    private boolean createMoleculeComponentView(CompositeView container, Node node, BionetgenDiagramViewOptions options, Graphics g)
    {
        Dimension d = options.getMoleculeComponentDefaultSize();
        TextView title = new TextView( node.getTitle(), options.getMoleculeComponentTitleFont(), g );
        int width = Math.max( d.width, title.getBounds().width + 4 );
        int height = Math.max( d.height, title.getBounds().height );
        node.setShapeSize( new Dimension( width, height ) );
        container.add( new BoxView( options.getDefaultPen(), options.getMoleculeComponentBrush(), 0, 0, width, height ) );
        container.add( title, CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    private boolean createMoleculeTypeView(CompositeView container, Node node, BionetgenDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 100;
            d.height = 60;
            node.setShapeSize( d );
        }
        container.add( new PolygonView( options.getDefaultPen(), options.getMoleculeTypeBrush(), new int[] {0, 15, d.width - 15, d.width,
                d.width - 15, 15}, new int[] {d.height / 2, 0, 0, d.height / 2, d.height, d.height} ) );
        container.add( new ComplexTextView( node.getTitle(), options.getMoleculeTypeTitleFont(), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, g, d.width ), CompositeView.X_CC | CompositeView.Y_CC );
        return false;
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( ! ( options instanceof BionetgenDiagramViewOptions ) )
            return super.createCompartmentCoreView( container, compartment, options, g );

        if( BionetgenUtils.isSpecies( compartment ) )
        {
            return createSpeciesView( container, compartment, (BionetgenDiagramViewOptions)options );
        }
        else if( BionetgenUtils.isMolecule( compartment ) )
        {
            return createMoleculeView( container, compartment, (BionetgenDiagramViewOptions)options, g );
        }
        else if( BionetgenUtils.isObservable( compartment ) )
        {
            return createObservableView( container, compartment, (BionetgenDiagramViewOptions)options, g );
        }
        return super.createCompartmentCoreView( container, compartment, options, g );
    }

    private boolean createObservableView(CompositeView container, Compartment compartment, BionetgenDiagramViewOptions options, Graphics g)
    {
        Dimension d = compartment.getShapeSize();
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 70;
            d.height = 70;
            compartment.setShapeSize( d );
        }
        container.add( new EllipseView( options.getDefaultPen(), options.getObservableBrush(), 0, 0, d.width, d.height ) );
        container.add( new ComplexTextView( compartment.getTitle(), options.getObservableTitleFont(), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, g, d.width ), CompositeView.X_CC | CompositeView.Y_CC );
        setView( container, compartment );
        return false;
    }

    private boolean createMoleculeView(CompositeView container, Compartment compartment, BionetgenDiagramViewOptions options, Graphics g)
    {
        Dimension d = compartment.getShapeSize();
        if( d.width <= 0 || d.height <= 0 )
        {
            d.width = 70;
            d.height = 70;
            compartment.setShapeSize( d );
        }
        float round = Math.max( Math.min( Math.min( d.width, d.height ) / 3f, 20 ), 2f );
        container.add( new BoxView( options.getDefaultPen(), options.getMoleculeBrush(), new RoundRectangle2D.Float( 0, 0, d.width,
                d.height, round, round ) ) );
        container.add( new ComplexTextView( compartment.getTitle(), options.getMoleculeTitleFont(), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, g, d.width ), CompositeView.X_CC | CompositeView.Y_CC );
        setView( container, compartment );
        return false;
    }

    private boolean createSpeciesView(CompositeView container, Compartment compartment, BionetgenDiagramViewOptions options)
    {
        Dimension d = compartment.getShapeSize();
        String graphStr = compartment.getAttributes().getValueAsString( BionetgenConstants.GRAPH_ATTR );
        if( graphStr != null && !graphStr.isEmpty() )
        {
            int size = TextUtil2.split( graphStr, '.' ).length;
            int width = (int)Math.sqrt( size );
            if( width * width != size )
                width++;
            if( d.width <= 0 || d.height <= 0 )
                d = new Dimension( 130 * width, 80 * width );
        }

        int angleSize = Math.min( Math.min( 7, d.width / 3 ), d.height / 3 );
        int[] xpoints = new int[] {angleSize, d.width - angleSize, d.width, d.width, d.width - angleSize, angleSize, 0, 0};
        int[] ypoints = new int[] {0, 0, angleSize, d.height - angleSize, d.height, d.height, d.height - angleSize, angleSize};
        container.add( new PolygonView( options.getDefaultPen(), options.getSpeciesBrush(), xpoints, ypoints ) );

        compartment.setShapeSize( d );

        setView( container, compartment );
        return false;
    }

    public static void setView(CompositeView container, DiagramElement de)
    {
        if( container == null )
            return;
        container.setModel( de );
        container.setActive( true );
        if( de instanceof Node )
            container.setLocation( ( (Node)de ).getLocation() );
        de.setView( container );
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        if( ! ( options instanceof BionetgenDiagramViewOptions ) )
            return super.createEdgeView( edge, options, g );

        if( edge.getPath() == null )
            Diagram.getDiagram( edge ).getType().getSemanticController().recalculateEdgePath( edge );

        CompositeView view;
        if( BionetgenUtils.isBngEdge( edge ) )
            view = new ArrowView( new Pen( 1, Color.black ), null, edge.getSimplePath(), 0, 0 );
        else
            view = createReactionEdgeView( edge, (BionetgenDiagramViewOptions)options );

        setView( view, edge );
        return view;
    }

    public @Nonnull CompositeView createReactionEdgeView(Edge edge, BionetgenDiagramViewOptions options)
    {
        SimplePath path = edge.getSimplePath();

        if( BionetgenUtils.isReaction( edge.getInput() ) )
            return new ArrowView( options.getEdgePen(), options.getEdgeTipBrush(), path, null, ArrowView.createTriangleTip(
                    options.getEdgePen(), new Brush( Color.black ), 15, 5 ) );
        else
            return new ArrowView( options.getEdgePen(), new Brush( Color.white ), path, 0, 0 );
    }

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new BionetgenDiagramViewOptions();
    }
}
