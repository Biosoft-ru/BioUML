package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.graph.Path;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.EllipseView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.view.FormulaViewBuilder;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.standard.type.Reaction;
import biouml.standard.type.Type;
import biouml.workbench.graph.OnePointFinder;

public class PathwaySimulationDiagramViewBuilder extends PathwayDiagramViewBuilder
{
    public static final int PORT_SIZE = 12;
    public static final int PORT_INDENT = 2;

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new PathwaySimulationDiagramViewOptions( null );
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( Type.MATH_EVENT.equals( node.getKernel().getType() ) )
            return createEventView( container, node, (PathwaySimulationDiagramViewOptions)options, g );

        if( Type.MATH_EQUATION.equals( node.getKernel().getType() ) )
            return createEquationView( container, node, (PathwaySimulationDiagramViewOptions)options, g );

        if( Type.MATH_STATE.equals( node.getKernel().getType() ) )
            return createStateView( container, node, (PathwaySimulationDiagramViewOptions)options, g );

        if( Type.MATH_FUNCTION.equals( node.getKernel().getType() ) )
            return createFunctionView( container, node, (PathwaySimulationDiagramViewOptions)options, g );

        if( Type.MATH_CONSTRAINT.equals( node.getKernel().getType() ) )
            return createConstraintView(container, node, (PathwaySimulationDiagramViewOptions)options, g, false);

        if( node.getKernel().getType().equals(Type.TYPE_TABLE) )
            return this.createTableView(container, node, (PathwaySimulationDiagramViewOptions)options, g);

        if( Util.isPort(node) )
            return createConnectionPortView( container, node, (PathwaySimulationDiagramViewOptions)options, g );

        return super.createNodeCoreView( container, node, options, g );
    }

    @Override
    public @Nonnull
    CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    {
        CompositeView nodeView = super.createNodeView( node, options, g );

        if( options instanceof PathwaySimulationDiagramViewOptions && !Util.isBus(node))
        {
            // show variable value
            Role role = node.getRole();
            if( role instanceof VariableRole && ((PathwaySimulationDiagramViewOptions)options).showVariableValue )
            {
                VariableRole var = (VariableRole)role;
                TextView initialValueView = new TextView( Double.toString(var.getInitialValue()), getTitleFont(node, options.getNodeTitleFont()), g );
//                initialValueView.setActive(true);
                //initialValueView.setModel(var);
                nodeView.add( initialValueView, CompositeView.X_CC | CompositeView.Y_BC, options.getNodeTitleMargin() );
            }
        }
        return nodeView;
    }

    protected void createReactionTitle(DiagramElement reactionNode, CompositeView diagramView, PathwayDiagramViewOptions options, Graphics g)
    {
        super.createReactionTitle( reactionNode, diagramView, options, g );

        if( options instanceof PathwaySimulationDiagramViewOptions && ( (PathwaySimulationDiagramViewOptions)options ).showReactionRate )
        {
            View reactionView = reactionNode.getView();
            Rectangle rBounds = reactionView.getBounds();

            Equation equation = reactionNode.getRole(Equation.class);
            String formula = equation.getFormula();
            if( formula != null )
                formula = formula.trim();
            if( formula == null || formula.isEmpty() )
                formula = "--";

            TextView formulaView = new TextView( formula, getTitleFont(reactionNode, options.getNodeTitleFont()), g );

            Reaction reaction = (Reaction)reactionNode.getKernel();
            formulaView.setModel( reaction.getKineticLaw() );
            formulaView.setActive( true );

            Rectangle tBounds = formulaView.getBounds();
            formulaView.setLocation( rBounds.x + ( ( rBounds.width - tBounds.width ) / 2 ), rBounds.y + rBounds.height + 10 );
            diagramView.add( formulaView );
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // View for mathematical entities
    //

    protected FormulaViewBuilder formulaViewBuilder = new FormulaViewBuilder()
    {
        @Override
        protected void init()
        {
            defaultFont = new ColorFont( "Arial", Font.PLAIN, 11, Color.black );
            superscriptFont = new ColorFont( "Arial", Font.PLAIN, 9, Color.black );
            subscriptFont = new ColorFont( "Arial", Font.PLAIN, 9, Color.black );

            offset = new Point( 2, 0 );
            subscript = new Point( 1, -4 );
            superscript = new Point( 1, -4 );
        }
    };

    @Override
    protected @Nonnull CompositeView doCreateCompartmentView(Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( Util.isBlock( compartment ) )
            return createBlockView( compartment, options, g );

        return super.doCreateCompartmentView( compartment, options, g );
    }

    protected @Nonnull
    View createMathView(Role role, String math, PathwaySimulationDiagramViewOptions options, Graphics g)
    {
        if( !options.mathAsText )
        {
            try
            {
                Diagram diagram = Diagram.getDiagram( role.getDiagramElement() );
                EModel model = diagram.getRole(EModel.class);
                AstStart start = model.readMath2( math, role );

                //hack to present initial assignments as "x(0) = ..." instead of "x = ..."
                if( role instanceof Equation && ( (Equation)role ).isInitial() )
                {
                    AstFunNode funNode = (AstFunNode)start.jjtGetChild(0);
                    funNode.setFunction(new PredefinedFunction("(0) =", PredefinedFunction.ASSIGNMENT_PRIORITY, -1));
                }
                formulaViewBuilder.setMultiplySign( options.getMultiplySign() );
                return formulaViewBuilder.createView( start, g );
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE,  "Can not generate formula view, \r\n  math=" + math + "\r\n  error=" + t, t );
            }
        }
        return new TextView( math, options.formulaFont, g );
    }

    protected @Nonnull CompositeView createBlockView(Compartment block, DiagramViewOptions options, Graphics g)
    {
        CompositeView compartmentView = new CompositeView();

        ComplexTextView titleView = new ComplexTextView( "<b>" + block.getTitle() + "</b>", new Point( 0, 0 ), ComplexTextView.CENTER,
                getTitleFont(block, options.getCompartmentTitleFont()), options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, 200, g );

        compartmentView.add( titleView, CompositeView.CENTER );

        for( Node node : block.stream().select(Node.class) )
        {
            if( node.getRole() instanceof Equation )
            {
                CompositeView view = new CompositeView();
                createEquationView(view, node, (PathwaySimulationDiagramViewOptions)options, g, true);
                view.setModel(node);
                view.setActive(true);
                node.setView(view);
                compartmentView.add(view, CompositeView.X_LL | CompositeView.Y_BT);
            }
            else
            {
                log.log(Level.SEVERE, "Invalid node \"" + node.getTitle() + "\" with role \"" + node.getRole() + "\" inside block \"" + block.getTitle()
                        + "\". Only equations are allowed inside blocks.");
            }
        }
        Dimension size = block.getShapeSize();
        Rectangle bounds = compartmentView.getBounds();
        if( size.width < bounds.width )
            size.width = bounds.width;
        if( size.height < bounds.height )
            size.height = bounds.height;

        compartmentView.remove( titleView );
        compartmentView.add( titleView, CompositeView.CENTER & CompositeView.TOP );

        compartmentView.add( new BoxView( getBorderPen(block, options.getDefaultPen()), null, bounds.x, bounds.y, size.width, size.height ),
                CompositeView.X_CC | CompositeView.Y_TT );

        compartmentView.add( new LineView( options.getDefaultPen(), 0, 0, size.width, 0 ), CompositeView.X_CC | CompositeView.Y_TT,
                new Point( 0, titleView.getBounds().height ) );

        compartmentView.setModel( block );
        compartmentView.setActive( true );

        compartmentView.setLocation( block.getLocation() );
        block.setView( compartmentView );
        return compartmentView;
    }

    protected boolean createEventView(CompositeView eventView, Node node, PathwaySimulationDiagramViewOptions options, Graphics g)
    {
        Event event = (Event)node.getRole();
        if( event == null )
            return false;

        CompositeView when = new CompositeView();
        ColorFont font = options.formulaFont;
        when.add( new TextView( "when: ", font, g ) );
        when.add( createMathView( event, event.getTrigger(), options, g ), CompositeView.X_RL | CompositeView.Y_CC );
        eventView.add( when );

        if( !event.isTriggerInitialValue() )
        {
            TextView initial = new TextView( "even if on start", font, g );
            eventView.add( initial, CompositeView.X_LL | CompositeView.Y_BT, options.formulaOffset );
        }

        if( !event.isTriggerPersistent() )
        {
            TextView persistent = new TextView( "not persistent", font, g );
            eventView.add( persistent, CompositeView.X_LL | CompositeView.Y_BT, options.formulaOffset );
        }

        if( event.getDelay() != null && !"".equals( event.getDelay() ) && !"0".equals( event.getDelay() ) )
        {
            CompositeView delay = new CompositeView();
            delay.add( new TextView( "delay: ", font, g ) );
            delay.add( createMathView( event, event.getDelay(), options, g ), CompositeView.X_RL | CompositeView.Y_CC );
            eventView.add( delay, CompositeView.X_LL | CompositeView.Y_BT, options.formulaOffset );
        }

        if( event.getPriority() != null && !"".equals( event.getPriority() ) )
        {
            CompositeView priority = new CompositeView();
            priority.add( new TextView( "priority: ", font, g ) );
            priority.add( createMathView( event, event.getPriority(), options, g ), CompositeView.X_RL | CompositeView.Y_CC );
            eventView.add( priority, CompositeView.X_LL | CompositeView.Y_BT, options.formulaOffset );
        }

        Point offset = new Point( 0, options.formulaOffset.y * 2 );
        Rectangle r = eventView.getBounds();
        int y = r.y + r.height + options.formulaOffset.y;

        for( int i = 0; i < event.getEventAssignment().length; i++ )
        {
            Assignment a = event.getEventAssignment( i );
            eventView.add( createMathView( event, a.getVariable() + " = " + a.getMath(), options, g ), CompositeView.X_LL
                    | CompositeView.Y_BT, offset );

            if( i == 0 )
                offset = options.formulaOffset;
        }

        ColorFont titleFont = getTitleFont(node, options.getMathTitleFont());
        Pen pen = getBorderPen(node, options.getMathPen());
        String title = event.getDiagramElement().getTitle();
        if( title != null && title.trim().length() > 0 )
            eventView.add(new TextView(title, titleFont, g), CompositeView.X_CC | CompositeView.Y_TB);

        // add line separator
        r = eventView.getBounds();
        int d = options.borderOffset;
        eventView.add(new LineView(pen, r.x - d, y, r.x + r.width + d, y));

        // add border
        eventView.add(new BoxView(pen, null, new RoundRectangle2D.Float(r.x - d, r.y - d, r.width + d * 2, r.height + d * 2, 20, 20 ) ) );
        return false;
    }

    protected boolean createEquationView(CompositeView view, Node node, PathwaySimulationDiagramViewOptions options, Graphics g)
    {
        return createEquationView( view, node, options, g, false );
    }

    protected boolean createEquationView(CompositeView view, Node node, PathwaySimulationDiagramViewOptions options, Graphics g,
            boolean isInsideBlock)
    {
        Equation eq = (Equation)node.getRole();

        if( eq != null && (eq.getVariable() != null || eq.getType().equals( Equation.TYPE_ALGEBRAIC )) )
        {
            if( Equation.isScalar( eq.getType() ) )
                view.add( createMathView( eq, eq.getVariable() + " = " + eq.getFormula(), options, g ) );
            else if( eq.getType().equals( Equation.TYPE_INITIAL_ASSIGNMENT ) )
                view.add( createMathView( eq, eq.getVariable() + " = " + eq.getFormula(), options, g ) );
            else if( eq.getType().equals( Equation.TYPE_RATE ) )
                view.add( createMathView( eq, "diff(time," + eq.getVariable() + ") = " + eq.getFormula(), options, g ) );
            else
            {
                view.add( createMathView( eq, eq.getFormula(), options, g ) );
                view.add( new TextView( " = 0", formulaViewBuilder.getDefaultFont(), g ), CompositeView.X_RL | CompositeView.Y_CC );
            }
        }

        if (node.isShowTitle())
        {
            TextView textView = new TextView(node.getTitle(), getTitleFont(node, options.getMathTitleFont()), g);
            view.add(textView, CompositeView.X_LL | CompositeView.Y_TT, new Point(0, -textView.getBounds().height));
        }

        // add border
        int d = options.borderOffset;
        Rectangle r = view.getBounds();
        Pen pen = getBorderPen(node, options.getMathPen());
        if( !isInsideBlock )
            view.add(new BoxView(pen, null, new RoundRectangle2D.Float(r.x - d, r.y - d, r.width + d * 2, r.height + d * 2, 10, 10)));

        return false;
    }

    protected boolean createConstraintView(CompositeView view, Node node, PathwaySimulationDiagramViewOptions options, Graphics g,
            boolean isInsideBlock)
    {
        try
        {
            Constraint con = node.getRole(Constraint.class);
            view.add(createMathView(con, con.getFormula(), options, g));
            if( node.isShowTitle() )
            {
                TextView textView = new TextView(node.getTitle(), getTitleFont(node, options.getMathTitleFont()), g);
                view.add(textView, CompositeView.X_LL | CompositeView.Y_TT, new Point(0, -textView.getBounds().height));
            }
            // add border
            int d = options.borderOffset;
            Rectangle r = view.getBounds();
            Pen pen = getBorderPen(node, options.getMathPen());
            if( !isInsideBlock )
                view.add(new BoxView(pen, null, new RoundRectangle2D.Float(r.x - d, r.y - d, r.width + d * 2, r.height + d * 2, 10, 10)));
        }
        catch( Exception ex )
        {

        }
        return false;
    }


    protected boolean createFunctionView(CompositeView view, Node node, PathwaySimulationDiagramViewOptions options, Graphics g)
    {
        return createFunctionView( view, node, options, g, false );
    }

    protected boolean createFunctionView(CompositeView view, Node node, PathwaySimulationDiagramViewOptions options, Graphics g,
            boolean isInsideBlock)
    {
        Function fun = node.getRole(Function.class);
        view.add( createMathView ( fun, fun.getFormula(), options, g));

        if (node.isShowTitle())
        {
            TextView textView = new TextView(node.getTitle(), getTitleFont(node, options.getMathTitleFont()), g);
            view.add(textView, CompositeView.X_LL | CompositeView.Y_TT, new Point(0, -textView.getBounds().height));
        }
        // add border
        if( !isInsideBlock )
        {
            int d = options.borderOffset;
            Rectangle r = view.getBounds();
            Pen pen = getBorderPen(node, options.getMathPen());
            view.add(new BoxView(pen, null, new RoundRectangle2D.Float(r.x - d, r.y - d, r.width + d * 2, r.height + d * 2, 10, 10)));
        }
        return false;
    }
    
    public boolean createTableView(CompositeView container, Node node, PathwaySimulationDiagramViewOptions options, Graphics g)
    {
        Dimension d = node.getShapeSize();
        SimpleTableElement table = node.getRole( SimpleTableElement.class );
        VarColumn column = table.getArgColumn();
        VarColumn[] varColumns = table.getColumns();

        String argVariable = column.getVariable();
        String argColumn = column.getColumn();

        int offset = 5;

        TextView titleView = new TextView( node.getTitle(), getTitleFont( node, options.getNodeTitleFont() ), g );
        titleView.setLocation( offset, 0 );
        container.add( titleView );
        int yStart = titleView.getBounds().height + offset;

        TextView argColumnView = new TextView( argColumn, options.getDefaultFont(), g );
        TextView argView = new TextView( argVariable, options.getDefaultFont(), g );

        
        int size = varColumns.length;
        TextView[] columnViews = new TextView[size];
        TextView[] variableView = new TextView[size];
        LineView[] horizontalLine = new LineView[size];

        for( int i = 0; i < size; i++ )
        {
            VarColumn col = table.getColumns()[i];
            columnViews[i] = new TextView( col.getColumn(), options.getDefaultFont(), g );
            variableView[i] = new TextView( col.getVariable(), options.getDefaultFont(), g );
        }

        int cellheight = Math.max( argColumnView.getBounds().height, argView.getBounds().height );
        int cellWidth1 = argColumnView.getBounds().width;
        int cellWidth2 = argView.getBounds().width;

        for( int i = 0; i < size; i++ )
        {
            cellheight = Math.max( cellheight, columnViews[i].getBounds().height );
            cellheight = Math.max( cellheight, variableView[i].getBounds().height );
            cellWidth1 = Math.max( cellWidth1, columnViews[i].getBounds().width );
            cellWidth2 = Math.max( cellWidth2, variableView[i].getBounds().width );
        }
        cellheight += 2 * offset;
        int width = cellWidth1 + cellWidth2 + 4 * offset;
        int height = cellheight * ( size + 1 );

        argColumnView.setLocation( offset, yStart + offset );
        argView.setLocation( 3 * offset + cellWidth1, yStart + offset );

        for( int i = 0; i < size; i++ )
        {
            columnViews[i].setLocation( offset, yStart + cellheight * ( i + 1 ) + offset );
            variableView[i].setLocation( 3 * offset + cellWidth1, yStart + cellheight * (i+1)+ offset );
            horizontalLine[i] = new LineView( options.getDefaultPen(), 0, yStart + ( cellheight ) * (i+1), width, yStart + ( cellheight ) * (i+1));
        }

        container.add( new BoxView( options.getDefaultPen(), new Brush( Color.white ), 0, yStart, width, height ) );
        container.add( new LineView( options.getDefaultPen(), cellWidth1 + 2 * offset, yStart, cellWidth1 + 2 * offset, yStart+height ) );      
        container.add( argColumnView );
        container.add( argView );
        
        for( int i = 0; i < size; i++ )
        {
            container.add( columnViews[i] );
            container.add( variableView[i] );
            container.add( horizontalLine[i] );
        }
        d.width = width;
        d.height = height;

        return false;
    }

    protected boolean createStateView(CompositeView stateView, Node node, PathwaySimulationDiagramViewOptions options, Graphics g)
    {
        State state = node.getRole(State.class);

        View entry_1 = null;
        for( int i = 0; i < state.getOnEntryAssignment().length; i++ )
        {
            Assignment a = state.getOnEntryAssignment( i );
            View v = createMathView( state, a.getVariable() + " = " + a.getMath(), options, g );
            stateView.add( v, CompositeView.X_LL | CompositeView.Y_BT, options.formulaOffset );

            if( i == 0 )
                entry_1 = v;
        }

        View exit_1 = null;
        for( int i = 0; i < state.getOnExitAssignment().length; i++ )
        {
            Assignment a = state.getOnExitAssignment( i );
            View v = createMathView( state, a.getVariable() + " = " + a.getMath(), options, g );
            stateView.add( v, CompositeView.X_LL | CompositeView.Y_BT, options.formulaOffset );

            if( i == 0 )
                exit_1 = v;
        }

        if( entry_1 != null )
        {
            TextView onEntry = new TextView( "on entry: ", options.stateOnFont, g );
            Rectangle rOn = onEntry.getBounds();
            Rectangle r1 = entry_1.getBounds();
            onEntry.setLocation( r1.x - rOn.width - 10, r1.y + ( r1.height - rOn.height ) / 2 );
            stateView.add( onEntry );
        }

        if( exit_1 != null )
        {
            TextView onExit = new TextView( "on exit: ", options.stateOnFont, g );
            Rectangle rOn = onExit.getBounds();
            Rectangle r1 = exit_1.getBounds();
            onExit.setLocation( r1.x - rOn.width - 10, r1.y + ( r1.height - rOn.height ) / 2 );
            stateView.add( onExit );
        }

        // add title
        Point offset = new Point( 0, options.formulaOffset.y * 2 );
        Rectangle r = stateView.getBounds();
        int y = r.y - options.formulaOffset.y;
        stateView.add( new TextView( node.getTitle(), options.getMathTitleFont(), g ), CompositeView.X_CC | CompositeView.Y_TB, offset );

        // add line separator
        r = stateView.getBounds();
        int d = options.borderOffset;
        stateView.add( new LineView( options.getMathPen(), r.x - d, y, r.x + r.width + d * 2, y ) );

        // add border
        stateView.add( new BoxView( options.getMathPen(), null, new RoundRectangle2D.Float( r.x - d, r.y - d, r.width + d * 2,
                r.height + d * 2, 20, 20 ) ) );
        return false;
    }

    protected View createPortCoreView(Node node, PathwaySimulationDiagramViewOptions options)
    {
        Pen pen = getBorderPen( node, options.getNodePen() );
        PortOrientation orientation = Util.getPortOrientation(node);
        String type = node.getKernel().getType();
        if( type.equals(Type.TYPE_OUTPUT_CONNECTION_PORT) )
            return new PolygonView( pen, options.outputConnectionPortBrush, createPolygon( orientation, PORT_SIZE ) );
        else if( type.equals(Type.TYPE_INPUT_CONNECTION_PORT) )
            return new PolygonView( pen, options.getInputConnectionPortBrush(), createPolygon( orientation.opposite(), PORT_SIZE ) );
        return new EllipseView( pen, options.contactConnectionPortBrush, 0, 0, PORT_SIZE, PORT_SIZE );
    }

    protected boolean createConnectionPortView(CompositeView container, Node node, PathwaySimulationDiagramViewOptions options, Graphics g)
    {
        container.add(createPortCoreView(node, options));
        View title = new FormulaViewBuilder().createTitleView(node.getTitle(), getTitleFont(node, options.getNodeTitleFont()), g);
        switch( Util.getPortOrientation(node) )
        {
            case TOP:
                container.add(title, CompositeView.X_CC | CompositeView.Y_BT, new Point(0, 3) );
                break;
            case RIGHT:
                container.add(title, CompositeView.X_LR | CompositeView.Y_CC, new Point(3, 0));
                break;
            case BOTTOM:
                container.add(title, CompositeView.X_CC | CompositeView.Y_TB, new Point(0, 3));
                break;
            case LEFT:
                container.add(title, CompositeView.X_RL | CompositeView.Y_CC, new Point(3, 0));
                break;
        }
        return false;
    }

    public static Polygon createPolygon(PortOrientation orientation, int size)
    {
        Polygon polygon = new Polygon();
        polygon.addPoint(0, 0);

        switch( orientation )
        {
            case TOP:
            {
                polygon.addPoint(size, 0);
                polygon.addPoint(size / 2, -size);
                return polygon;
            }
            case BOTTOM:
            {
                polygon.addPoint(size, 0);
                polygon.addPoint(size / 2, size);
                return polygon;
            }
            case RIGHT:
            {
                polygon.addPoint(0, size);
                polygon.addPoint(size, size / 2);
                return polygon;
            }
            default:
            {
                polygon.addPoint(0, size);
                polygon.addPoint( -size, size / 2);
                return polygon;
            }
        }
    }

    public static Polygon createPolygon(PortOrientation orientation, boolean isOutput, int size)
    {
        Polygon polygon = new Polygon();

        polygon.addPoint( 0, 0 );

        if( ( orientation == PortOrientation.TOP && isOutput ) || ( orientation == PortOrientation.BOTTOM && !isOutput ) )
        {
            polygon.addPoint( size, 0 );
            polygon.addPoint( size / 2, -size );
        }
        else if( ( orientation == PortOrientation.BOTTOM && isOutput ) || ( orientation == PortOrientation.TOP && !isOutput ) )
        {
            polygon.addPoint( size, 0 );
            polygon.addPoint( size / 2, size );
        }
        else if( ( orientation == PortOrientation.RIGHT && isOutput ) || ( orientation == PortOrientation.LEFT && !isOutput ) )
        {
            polygon.addPoint( 0, size );
            polygon.addPoint( size, size / 2 );
        }
        else if( ( orientation == PortOrientation.LEFT && isOutput ) || ( orientation == PortOrientation.RIGHT && !isOutput ) )
        {
            polygon.addPoint( 0, size );
            polygon.addPoint( -size, size / 2 );
        }
        return polygon;
    }

    @Override
    public @Nonnull
    CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        if( edge.getKernel().getType().equals( Type.MATH_TRANSITION ) )
        {
            return createTransitionView( edge, (PathwaySimulationDiagramViewOptions)viewOptions, g );
        }
        return super.createEdgeView( edge, viewOptions, g );
    }

    protected @Nonnull
    CompositeView createTransitionView(Edge edge, PathwaySimulationDiagramViewOptions viewOptions, Graphics g)
    {
        Transition transition = edge.getRole( Transition.class );

        Pen pen = getBorderPen(edge ,viewOptions.transitionPen);
        Brush brush = getBrush(edge, viewOptions.transitionBrush);

        CompositeView view = new CompositeView();
        view.setModel( edge );

        Path path = edge.getPath();
        if( path == null || path.npoints < 2 )
        {
            path = new Path();
            Point in = new Point();
            Point out = new Point();
            if( !calculateInOut( edge, in, out ) )
                return view;
            path.addPoint( in.x, in.y );
            path.addPoint( out.x, out.y );

            edge.setPath( path );
        }

        Point outPort = new Point( path.xpoints[path.npoints - 1], path.ypoints[path.npoints - 1] );
        Point inPort = new Point( path.xpoints[0], path.ypoints[0] );

        ArrowView.Tip endTip = ArrowView.createSimpleTip( pen, 10, 5 );
        ArrowView arrow = new ArrowView( pen, brush, edge.getSimplePath(), null, endTip );
        arrow.setModel( edge );
        arrow.setActive( true );
        view.add( arrow );


        //create control point
        Point controlPoint = null;
        if( arrow.getPathView() != null )
            controlPoint = arrow.getPathView().getMiddlePoint();
        else
            controlPoint = new Point( ( path.xpoints[0] + path.xpoints[1] ) / 2, ( path.ypoints[0] + path.ypoints[1] ) / 2 ); //path contains of 2 points

        StringBuffer titleString = new StringBuffer();
        if( transition.getWhen() != null && transition.getWhen().length() > 0 )
        {
            titleString.append( "when: " );
            titleString.append( transition.getWhen() );
        }
        if( transition.getAfter() != null && transition.getAfter().length() > 0 )
        {
            if( titleString.length() > 0 )
            {
                titleString.append( "; " );
            }
            titleString.append( "after: " );
            titleString.append( transition.getAfter() );
        }
        View title = null;
        if( titleString.length() > 0 )
        {
            int alignment = 0;
            float a = ( outPort.y - inPort.y ) / ( outPort.x - inPort.x + 0.1f );
            if( Math.abs( a ) < 0.2 )
                alignment = CompositeView.X_CC | CompositeView.Y_TB;
            else
                alignment = CompositeView.X_RL | CompositeView.Y_CC;

            Point middlePoint = new Point( controlPoint.x - 7, controlPoint.y );
            title = new TextView( titleString.toString(), middlePoint, alignment, getTitleFont(edge, viewOptions.relationTitleFont), g );

            title.setActive( true );
            title.setModel( edge );
        }

        if( title != null )
            view.add( title );

        return view;
    }

    @Override
    public PortFinder getPortFinder(Node node)
    {
        if( Util.isPort(node) )
            return new OnePointFinder(Util.getPortOrientation(node).opposite(), getNodeBounds( node ));
        return super.getPortFinder(node);
    }
}
