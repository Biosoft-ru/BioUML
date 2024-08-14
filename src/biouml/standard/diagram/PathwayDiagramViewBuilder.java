package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.awt.geom.RoundRectangle2D;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.PolylineView;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.EquivalentNodeGroup;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Biopolymer;
import biouml.standard.type.Protein;
import biouml.standard.type.ProteinProperties;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

/**
 * Default implementation of <code>DiagramViewBuilder</code>.
 */
public class PathwayDiagramViewBuilder extends DefaultDiagramViewBuilder
{
    private static final String DEPENDENCY_TYPE = "dependencyType";
    private static final String INCREASE = "increase";
    private static final String DECREASE = "decrease";

    // ////////////////////////////////////////////////////////////////
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new PathwayDiagramViewOptions(null);
    }

    @Override
    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g)
    {
        CompositeView diagramView = super.createDiagramView(diagram, g);

        createReactionTitles(diagram, diagramView, diagram.getViewOptions(), g);
        diagramView.updateBounds();

        return diagramView;
    }

    public void createReactionTitles(Compartment compartment, CompositeView diagramView, DiagramViewOptions diagramOptions, Graphics g)
    {
        // build reaction titles
        boolean showReactionName = false;
        if( diagramOptions instanceof PathwayDiagramViewOptions )
        {
            showReactionName = ( (PathwayDiagramViewOptions)diagramOptions ).showReactionName;
        }
        if( showReactionName )
        {
            for( DiagramElement de : compartment )
            {
                if( de.getKernel() instanceof Reaction )
                    createReactionTitle(de, diagramView, diagramOptions, g);

                else if( de instanceof Compartment )
                    createReactionTitles((Compartment)de, diagramView, diagramOptions, g);
            }
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Methods to build view for different kernel types
    //

    protected boolean createConceptCoreView(CompositeView container, Node node, String title, PathwayDiagramViewOptions options, Graphics g)
    {
        ComplexTextView view = new ComplexTextView( title, getTitleFont( node, options.conceptTitleFont ), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
        //view.setModel ( node );
        container.add(view);

        Rectangle r = container.getBounds();
        Point d = options.getTitleMargin();
        int[] x = new int[] {0, 10, r.width + d.x * 3, r.width + 10 + d.x * 3, r.width + d.x * 3, 10};
        int[] y = new int[] { ( r.height + d.y * 3 ) / 2, 0, 0, ( r.height + d.y * 3 ) / 2, ( r.height + d.y * 3 ), ( r.height + d.y * 3 )};
        container.add( new PolygonView( getBorderPen( node, options.conceptPen ), null, x, y ), CompositeView.X_CC | CompositeView.Y_CC,
                null );

        return false;
    }

    protected boolean createFunctionCoreView(CompositeView container, Node node, String title, PathwayDiagramViewOptions options,
            Graphics g)
    {
        ComplexTextView view = new ComplexTextView( title, getTitleFont( node, options.functionTitleFont ), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
        //view.setModel ( node );
        container.add(view);
        return false;
    }

    protected boolean createProcessCoreView(CompositeView container, Node node, String title, PathwayDiagramViewOptions options,
            Graphics g)
    {
        ComplexTextView view = new ComplexTextView( title, getTitleFont( node, options.processTitleFont ), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
        //view.setModel ( node );
        container.add(view);

        return false;
    }

    protected boolean createStateCoreView(CompositeView container, Node node, String title, PathwayDiagramViewOptions options,
            Graphics g)
    {
        ComplexTextView view = new ComplexTextView( title, getTitleFont( node, options.stateTitleFont ), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
        //view.setModel ( node );
        container.add(view);

        Point d = options.getTitleMargin();
        Rectangle r = container.getBounds();
        RoundRectangle2D.Float rect = new RoundRectangle2D.Float(0, 0, r.width + d.x * 3, r.height + d.y * 3, 10, 10);
        container.add( new BoxView( getBorderPen( node, options.statePen ), null, rect ), CompositeView.X_CC | CompositeView.Y_CC, null );

        return false;
    }

    protected boolean createCellCoreView(CompositeView container, Node node, DiagramViewOptions diagramOptions, Graphics g)
    {
        PathwayDiagramViewOptions options = (PathwayDiagramViewOptions)diagramOptions;
        Pen p = options.getDefaultPen();

        container.add(new EllipseView(p, getBrush(node, options.cellCytoplasmBrush), 0, 0, 35, 23));
        container.add(new EllipseView(p, options.cellNucleusBrush, 11, 5, 14, 14));
        return true;
    }

    protected boolean createGeneCoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g)
    {
        Pen p = getBorderPen(node, options.getDefaultPen());

        container.add(new BoxView(p, getBrush(node, options.geneBrush), 0, 0, 27, 13));
        container.add(new LineView(p, -6, 6, 0, 6));
        container.add(new LineView(p, 28, 6, 34, 6));

        PolylineView l = new PolylineView(p);
        l.addPoint(13, 0);
        l.addPoint(13, -9);
        l.addPoint(27, -9);
        container.add(l);

        l = new PolylineView(p);
        l.addPoint(22, -12);
        l.addPoint(27, -9);
        l.addPoint(22, -6);
        container.add(l);
        return hasTitle( node );
    }

    protected boolean createRNACoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g)
    {
        Pen p = getBorderPen(node, options.getDefaultPen());

        PolylineView l = new PolylineView(p);
        for( int i = 0; i < 9; i++ )
            l.addPoint(i * 3, i / 2 * 2 == i ? 0 : 5);

        container.add(l);
        return hasTitle( node );
    }

    protected boolean createProteinCoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g,
            String functionalState, String configuration, String modification)
    {
        Pen p = getBorderPen(node, options.getDefaultPen());

        // define used protein brush
        Brush b = getBrush(node, options.proteinBrush);
        Brush bP = getBrush(node, options.modifiedProteinBrush);

        if( functionalState != null )
        {
            if( functionalState.equals(ProteinProperties.STATE_ACTIVE) )
                b = getBrush(node, options.activeProteinBrush);
            else if( functionalState.equals(ProteinProperties.STATE_INACTIVE) )
                b = getBrush(node, options.inactiveProteinBrush);
        }

        if( configuration == null )
            configuration = ProteinProperties.UNKNOWN;
        if( modification == null )
            modification = ProteinProperties.MODIFICATION_NONE;

        if( configuration.equals(ProteinProperties.HOMODIMER) )
        {
            container.add(new EllipseView(p, b, 0, 0, 18, 30));
            container.add(new EllipseView(p, b, 12, 0, 18, 30));

            paintModification(modification, container, g, p, bP, 23, 1);
        }
        else if( configuration.equals(ProteinProperties.HETERODIMER) )
        {
            container.add(new EllipseView(p, b, 0, 0, 20, 20));
            container.add(new EllipseView(p, b, 12, 0, 18, 30));

            paintModification(modification, container, g, p, bP, 23, 1);
        }
        else if( configuration.equals(ProteinProperties.MULTIMER) || configuration.equals(ProteinProperties.COMPLEX) )
        {
            container.add(new EllipseView(p, b, 0, 0, 20, 20));
            container.add(new EllipseView(p, b, 20, 0, 20, 20));
            container.add(new EllipseView(p, b, 10, -17, 20, 20));

            paintModification(modification, container, g, p, bP, 23, 1);
        }
        else
        // monomer or unknown
        {
            container.add(new EllipseView(p, b, 0, 0, 30, 30));

            paintModification(modification, container, g, p, bP, 23, 1);
        }

        return hasTitle( node );
    }

    protected boolean createPhysicalEntityCoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g)
    {
        Pen p = getBorderPen(node, options.getDefaultPen());
        Brush b = getBrush(node, options.physicalEntityBrush);
        container.add(new EllipseView(p, b, 0, 0, 20, 20));
        return true;
    }

    private void paintModification(String modification, CompositeView container, Graphics g, Pen p, Brush bP, int xCenter, int yCenter)
    {
        String text = null;

        if( modification.equals(ProteinProperties.MODIFICATION_PHOSPHORYLATED) )
            text = "P";
        if( modification.equals(ProteinProperties.MODIFICATION_FATTY_ACYLATION) )
            text = "Fa";
        if( modification.equals(ProteinProperties.MODIFICATION_PRENYLATION) )
            text = "Pr";
        if( modification.equals(ProteinProperties.MODIFICATION_CHOLESTEROLATION) )
            text = "Ch";
        if( modification.equals(ProteinProperties.MODIFICATION_UBIQUITINATION) )
            text = "U";
        if( modification.equals(ProteinProperties.MODIFICATION_SUMOLATION) )
            text = "S";
        if( modification.equals(ProteinProperties.MODIFICATION_GLYCATION) )
            text = "Gy";
        if( modification.equals(ProteinProperties.MODIFICATION_GPI_ANCHOR) )
            text = "GPI";
        if( modification.equals(ProteinProperties.MODIFICATION_UNKNOWN) )
            text = "?";
        if( text != null )
        {
            ColorFont font = new ColorFont("Courier New", Font.BOLD, 10, Color.BLACK);
            FontMetrics fontMetrics = g.getFontMetrics(font.getFont());
            LineMetrics lineMetrics = fontMetrics.getLineMetrics(text, g);
            int width = 16;
            int height = 16;
            container.add(new EllipseView(p, bP, xCenter, yCenter, width, height));
            width = xCenter + width / 2 + 1 - fontMetrics.stringWidth(text) / 2;
            height = yCenter + height + 1 - (int)lineMetrics.getHeight() / 2;
            container.add(new TextView(text, new Point(width, height), View.LEFT | View.BASELINE, font, g));
        }
    }

    protected boolean createSubstanceCoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g)
    {
        Pen p = getBorderPen(node, options.getDefaultPen());

        RoundRectangle2D.Float rect = new RoundRectangle2D.Float(0, 0, 30, 30, 10, 10);
        container.add(new BoxView(p, getBrush(node, options.substanceBrush), rect));
        return hasTitle( node );
    }

    protected boolean createReactionCoreView(CompositeView container, Node node, PathwayDiagramViewOptions options, Graphics g)
    {
        Brush brush = getBrush(node, getReactionBrush(node.getKernel(), options));
        container.add(new EllipseView(null, brush, 0, 0, 7, 7));

        if( options.automaticallyLocateReactions )
        {
            node.setView(container);
            locateReaction(node, options, g);
        }

        return false;
    }

    protected void createReactionTitle(DiagramElement reaction, CompositeView diagramView, DiagramViewOptions options, Graphics g)
    {
        View reactionView = reaction.getView();
        if( reactionView != null )
        {
            Rectangle rBounds = reactionView.getBounds();
            View titleView = new ComplexTextView(reaction.getTitle(), getTitleFont(reaction, options.getNodeTitleFont()), options.getFontRegistry(),
                    ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
            Rectangle tBounds = titleView.getBounds();
            titleView.setLocation(rBounds.x + ( ( rBounds.width - tBounds.width ) / 2 ), rBounds.y + rBounds.height);
            diagramView.add(titleView);
        }
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
        PathwayDiagramViewOptions options = (PathwayDiagramViewOptions)viewOptions;

        Base kernel = node.getKernel();
        String type = kernel.getType();

        if( type.startsWith(Type.TYPE_CONCEPT) )
        {
            String title = node.getTitle();
            if( title == null )
                title = node.getName();

            if( Type.TYPE_CONCEPT.equals(type) )
                return createConceptCoreView(container, node, title, options, g);

            if( Type.TYPE_FUNCTION.equals(type) )
                return createFunctionCoreView(container, node, title, options, g);

            if( Type.TYPE_PROCESS.equals(type) )
                return createProcessCoreView(container, node, title, options, g);

            if( Type.TYPE_STATE.equals(type) )
                return createStateCoreView(container, node, title, options, g);
        }

        if( Type.TYPE_CELL.equals(type) )
            return createCellCoreView(container, node, options, g);

        if( Type.TYPE_GENE.equals(type) )
            return createGeneCoreView(container, node, options, g);

        if( Type.TYPE_RNA.equals(type) )
            return createRNACoreView(container, node, options, g);

        if( Type.TYPE_PROTEIN.equals(type) )
        {
            if( ! ( kernel instanceof Protein ) )
                return createProteinCoreView(container, node, options, g, null, null, null);
            else
            {
                Protein protein = (Protein)kernel;
                return createProteinCoreView(container, node, options, g, protein.getFunctionalState(), protein.getStructure(), protein
                        .getModification());
            }
        }

        if( Type.TYPE_SUBSTANCE.equals(type) )
            return createSubstanceCoreView(container, node, options, g);

        if( Type.TYPE_PHYSICAL_ENTITY.equals(type) )
            return createPhysicalEntityCoreView(container, node, options, g);

        if( isReaction(kernel) )
            return createReactionCoreView(container, node, options, g);

        return super.createNodeCoreView(container, node, options, g);
    }

    @Override
    public @Nonnull CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = super.createNodeView(node, options, g);

        Base kernel = node.getKernel();
        if( kernel instanceof Biopolymer )
        {
            String name = kernel.getName();
            int pos = name.indexOf(':');
            if( pos > 0 )
            {
                String speciesPrefix = name.substring(0, name.indexOf(':'));
                View species = new TextView("(" + speciesPrefix + ")", options.getNodeTitleFont(), g);
                view.add(species, CompositeView.X_CC | CompositeView.Y_BT, null);
            }
        }

        return view;
    }

    @Override
    public @Nonnull CompositeView createEquivalentNodeGroupView(EquivalentNodeGroup group, DiagramViewOptions options, Graphics g)
    {
        CompositeView view = super.createEquivalentNodeGroupView(group, options, g);

        Node node = group.getRepresentative();
        Base kernel = node.getKernel();
        if( kernel instanceof Biopolymer )
        {
            Base[] kernels = group.getKernels();

            String species = StreamEx.of( kernels ).map( Base::getName ).map( name -> name.substring( 0, name.indexOf( ':' ) ) ).distinct()
                    .joining( ",", "(", ")" );

            if( species.length() > "()".length() )
            {
                View speciesView = new TextView(species, options.getNodeTitleFont(), g);
                view.add(speciesView, CompositeView.X_CC | CompositeView.Y_BT, null);
            }
        }

        return view;
    }

    // /////////////////////////////////////////////////////////////////////////
    // Create edge view issues
    //

    /**
     * @pending take initial ports into account
     */
    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        if( edge.getKernel().getType().equals(Type.TYPE_DEPENDENCY) )
            return createDependencyView(edge, g);
        
        if( edge.getKernel().getType().equals(Base.TYPE_SEMANTIC_RELATION) )
            return createSemanticRelationView(edge, (PathwayDiagramViewOptions)viewOptions, g);

        // create reaction edges
        PathwayDiagramViewOptions options = (PathwayDiagramViewOptions)viewOptions;
        Pen pen = getBorderPen(edge, getRelationPen(edge, options));

        CompositeView view = new CompositeView();

        if( edge.getPath() == null )
            Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);

        SimplePath path = edge.getSimplePath();

        View arrow = new ArrowView(pen, getBrush(edge, options.reactionBrush), path, getRelationStartTip(edge, options), getRelationEndTip(edge, pen, options));
        arrow.setModel(edge);
        arrow.setActive(true);
        view.add(arrow);

        view.setModel(edge);
        view.setActive(false);

        return view;
    }

    /**
     * Adds offsets between edge and node, excluding reactions.
     */
    @Override
    protected boolean calculateInOut(Edge edge, Point in, Point out, int offsetStart, int offsetEnd)
    {
        int inOffset = offsetStart;
        int outOffset = offsetEnd;

        if( edge.getInput().getKernel().getType().equals(Base.TYPE_REACTION) )
            inOffset = 0;

        if( edge.getOutput().getKernel().getType().equals(Base.TYPE_REACTION) )
            outOffset = 0;

        return super.calculateInOut(edge, in, out, inOffset, outOffset);
    }

    public @Nonnull CompositeView createSemanticRelationView(Edge edge, PathwayDiagramViewOptions viewOptions, Graphics g)
    {
        SemanticRelation relation = (SemanticRelation)edge.getKernel();

        Pen pen = getBorderPen(edge, viewOptions.getSemanticRelationPen(relation.getRelationType(), relation.getParticipation()));
        Brush brush = getBrush(edge, viewOptions.getSemanticRelationBrush(relation.getRelationType()));

        ArrowView.Tip endTip = ArrowView.createSimpleTip(viewOptions.getSemanticRelationPen(relation.getRelationType(),
                PathwayDiagramViewOptions.DEFAULT), 10, 5);

        String titleString = edge.getTitle();
        if( titleString != null )
        {
            if( titleString.equalsIgnoreCase(INCREASE) )
            {
                endTip = ArrowView.createTriangleTip(viewOptions.getSemanticRelationPen(relation.getRelationType(),
                        PathwayDiagramViewOptions.DEFAULT), new Brush(Color.white), 10, 5);
            }
            else if( titleString.equalsIgnoreCase("decrease") )
            {
                endTip = ArrowView.createLineTip(viewOptions.getSemanticRelationPen(relation.getRelationType(),
                        PathwayDiagramViewOptions.DEFAULT), null, 0, 6);
            }
        }

        CompositeView view = new CompositeView();
        view.setModel(edge);

        Path path = edge.getPath();
        if( path == null || path.npoints < 2 )
        {
            path = new Path();
            Point in = new Point();
            Point out = new Point();
            if( !calculateInOut(edge, in, out) )
                return view;
            path.addPoint(in.x, in.y);
            path.addPoint(out.x, out.y);

            edge.setPath(path);
        }
        Point outPort = new Point(path.xpoints[path.npoints - 1], path.ypoints[path.npoints - 1]);
        Point inPort = new Point(path.xpoints[0], path.ypoints[0]);

        ArrowView arrow = new ArrowView(pen, brush, edge.getSimplePath(), null, endTip);
        arrow.setModel(edge);
        arrow.setActive(true);
        view.add(arrow);

        //create control point
        Point controlPoint = null;
        if( arrow.getPathView() != null )
            controlPoint = arrow.getPathView().getMiddlePoint();
        else
            //path contains of 2 points
            controlPoint = new Point( ( path.xpoints[0] + path.xpoints[1] ) / 2, ( path.ypoints[0] + path.ypoints[1] ) / 2);

        View title = null;
        if( titleString != null && titleString.length() > 0 )
        {
            //HACK: don't show title for some values
            if( showRelationTitle( titleString ) )
            {
                float a = ( outPort.y - inPort.y ) / ( outPort.x - inPort.x + 0.1f );
                int alignment = Math.abs(a) < 0.2?  CompositeView.X_CC | CompositeView.Y_TB: CompositeView.X_RL | CompositeView.Y_CC;
                Point middlePoint = new Point(controlPoint.x - 7, controlPoint.y);
                title = new TextView(titleString, middlePoint, alignment, getTitleFont(edge, viewOptions.relationTitleFont), g);
                title.setActive(true);
                title.setModel(edge);
            }
        }

        if( title != null )
            view.add(title);

        return view;
    }
    protected boolean showRelationTitle(String titleString)
    {
        return !titleString.equalsIgnoreCase( INCREASE ) && !titleString.equalsIgnoreCase( "decrease" )
                && !titleString.equalsIgnoreCase( "influence" );
    }
    private static Polygon[] dividePolygon(Polygon p)
    {
        if( p == null || p.npoints < 2 )
            return null;

        int n = p.npoints;

        double[] len = new double[n - 1]; // length of all the segment
        double totalLength = 0;
        for( int i = 0; i < n - 1; ++i )
        {
            double segLen = Math.sqrt( ( p.xpoints[i + 1] - p.xpoints[i] ) * ( p.xpoints[i + 1] - p.xpoints[i] )
                    + ( p.ypoints[i + 1] - p.ypoints[i] ) * ( p.ypoints[i + 1] - p.ypoints[i] ));
            len[i] = segLen;
            totalLength += segLen;
        }

        // Define segment to divide on
        double lenAfterDivided = 0;
        int toDivide = 0;
        for( int i = 0; i < n - 1; ++i )
        {
            lenAfterDivided += len[i];
            if( lenAfterDivided > totalLength / 2 )
            {
                toDivide = i;
                break;
            }
        }

        int xBeforeDivided = p.xpoints[toDivide];
        int yBeforeDivided = p.ypoints[toDivide];

        int xAfterDivided = p.xpoints[toDivide + 1];
        int yAfterDivided = p.ypoints[toDivide + 1];

        double dividedSegmentLen = len[toDivide];
        double lenBeforeDivided = lenAfterDivided - dividedSegmentLen;
        double fraction = ( totalLength / 2 - lenBeforeDivided ) / dividedSegmentLen;

        int divideX = (int) ( xBeforeDivided + fraction * ( xAfterDivided - xBeforeDivided ) );
        int divideY = (int) ( yBeforeDivided + fraction * ( yAfterDivided - yBeforeDivided ) );

        Polygon first = new Polygon();
        for( int i = 0; i <= toDivide; ++i )
            first.addPoint(p.xpoints[i], p.ypoints[i]);
        first.addPoint(divideX, divideY);

        Polygon second = new Polygon();
        second.addPoint(divideX, divideY);
        for( int i = toDivide + 1; i < n; ++i )
            second.addPoint(p.xpoints[i], p.ypoints[i]);

        return new Polygon[] {first, second};
    }

    // ////////////////////////////////////////////////////////////////
    // Reaction and relation issues
    //

    /**
     * Locate reaction
     */
    public void locateReaction(Node reactionNode, DiagramViewOptions options, Graphics g)
    {
        try
        {
            doLocateReaction(reactionNode, options, g);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not locate reaction: " + reactionNode.getKernel().getName() + "\n  error: " + t);
        }
    }

    protected void doLocateReaction(Node reactionNode, DiagramViewOptions options, Graphics g)
    {
        List<View> species = new ArrayList<>();
        boolean hasReactants = false;
        boolean hasProducts = false;

        // put views
        for(Edge edge : reactionNode.edges())
        {
            Base kernel = edge.getKernel();

            Node specie = edge.getOtherEnd( reactionNode );
            View view = specie.getView();
            if( view == null )
                view = createNodeView(specie, options, g);

            if( kernel instanceof SpecieReference )
            {
                SpecieReference ref = (SpecieReference)kernel;
                if( ref.isReactantOrProduct() )
                    species.add(view);

                if( ref.getRole().equals(SpecieReference.REACTANT) )
                    hasReactants = true;
                else if( ref.getRole().equals(SpecieReference.PRODUCT) )
                    hasProducts = true;
            }
            else
            {
                species.add(view);

                if( edge.getInput() == reactionNode )
                    hasProducts = true;
                else
                    hasReactants = true;
            }
        }

        // cretate reaction view if it is absents
        View reactionView = reactionNode.getView();
        if( reactionView == null )
            reactionView = createNodeView(reactionNode, options, g);

        // calculate geometric center for all reaction components
        int x = 0;
        int y = 0;
        int n = 0;

        for( View view : species )
        {
            Rectangle bounds = view.getBounds();
            x += bounds.x + bounds.width / 2;
            y += bounds.y + bounds.height / 2;
            n++;
        }

        Rectangle bounds = reactionView.getBounds();
        reactionNode.setLocation(x / n - bounds.width / 2, y / n - bounds.height / 2);

        int dx = 50;
        Point p = reactionNode.getLocation();
        if( hasReactants && !hasProducts )
            reactionNode.setLocation(p.x + dx, p.y);
        else if( !hasReactants && !hasProducts )
            reactionNode.setLocation(p.x - dx, p.y);
    }

    protected Brush getReactionBrush(Base reaction, PathwayDiagramViewOptions options)
    {
        return options.reactionBrush;
    }

    protected Pen getRelationPen(Edge edge, PathwayDiagramViewOptions options)
    {
        if( edge.getKernel() instanceof Stub.NoteLink )
            return options.getNoteLinkPen();

        Pen pen = options.reactionPen;

        Base kernel = edge.getKernel();
        if( kernel instanceof SpecieReference )
        {
            String type = ( (SpecieReference)kernel ).getModifierAction();
            if( type != null )
            {
                if( type.equals(SpecieReference.ACTION_CATALYST) )
                    pen = options.catalystActionPen;
                else if( type.equals(SpecieReference.ACTION_INHIBITOR) )
                    pen = options.inhibitorActionPen;
                else if( type.equals(SpecieReference.ACTION_SWITCH_ON) )
                    pen = options.switchOnActionPen;
                else if( type.equals(SpecieReference.ACTION_SWITCH_OFF) )
                    pen = options.switchOffActionPen;
            }
        }
        return pen;
    }

    protected ArrowView.Tip getRelationStartTip(Edge edge, PathwayDiagramViewOptions options)
    {
        return null;
    }

    protected ArrowView.Tip getRelationEndTip(Edge edge, Pen pen, PathwayDiagramViewOptions options)
    {
        ArrowView.Tip endTip = null;
        Base kernel = edge.getInput().getKernel();
        if( isReaction(kernel) )
            endTip = ArrowView.createArrowTip(pen, getBrush(edge, getReactionBrush(kernel, options)), 5, 10, 4);
        return endTip;
    }
    
    public @Nonnull CompositeView createDependencyView(Edge edge, Graphics g)
    {
        String depType = edge.getAttributes().getValueAsString(DEPENDENCY_TYPE);
        Pen pen = getBorderPen(edge, getDependencyPen(depType));

        if( edge.getPath() == null )
            Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);

        ArrowView arrow = new ArrowView(pen, new Brush(Color.white), edge.getSimplePath(), null, getDependencyTip(depType, pen));
        arrow.setActive(true);
        arrow.setModel(edge);
        return arrow;
    }
    
    public Pen getDependencyPen(String dependencyType)
    {
        if( INCREASE.equals(dependencyType) )
            return new Pen(1, Color.red);
        else if( DECREASE.equals(dependencyType) )
            return new Pen(1, Color.blue);
        return new Pen(1, Color.black);
    }
    
    public ArrowView.Tip getDependencyTip(String dependencyType, Pen pen)
    {
        if( INCREASE.equals(dependencyType) )
            return ArrowView.createTriangleTip(pen, new Brush(Color.white), 10, 5);
        else if( DECREASE.equals(dependencyType) )
            return ArrowView.createLineTip(pen, null, 3, 6);
        return ArrowView.createSimpleTip(pen, 10, 5);
    }

}
