package ru.biosoft.math.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.PathView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.AstPiece;
import ru.biosoft.math.model.AstPiecewise;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;

public class FormulaViewBuilder
{
    protected ColorFont defaultFont = new ColorFont( "Arial", Font.BOLD, 14, Color.black );
    public ColorFont getDefaultFont()
    {
        return defaultFont;
    }

    protected ColorFont superscriptFont = new ColorFont( "Arial", Font.BOLD, 10, Color.black );
    protected ColorFont subscriptFont = new ColorFont( "Arial", Font.BOLD, 10, Color.black );
    protected ColorFont errorFont = new ColorFont( "Arial", Font.BOLD, 14, Color.red );

    protected Point offset = new Point( 3, 0 );
    protected Point subscript = new Point( 1, -7 );
    protected Point superscript = new Point( 1, -7 );

    /** Current center horizontal line used for alignment. */
    protected int y;

    /**
     * Offset for the last created view relative center line.
     * It is used by align functions to align new view.
     */
    protected int dy;

    protected Pen defaultPen = new Pen( 1, Color.black );
    protected Pen boldPen = new Pen( 2, Color.black );

    protected String multiplySign = "×";

    public FormulaViewBuilder()
    {
        init();
    }

    public void setMultiplySign(String sign)
    {
        multiplySign = sign;
    }

    /** Convenience method to be used i anonymous subclasses. */
    protected void init()
    {
    }

    public @Nonnull CompositeView createView(AstStart start, Graphics g)
    {
        CompositeView view = new CompositeView();

        if( start == null )
            return view;

        y = 0;
        dy = 0;

        for( Node node : Utils.children( start ) )
        {
            view.add( createNodeView( node, g ), CompositeView.X_RL | CompositeView.Y_CC, offset );
        }

        return view;
    }

    ///////////////////////////////////////////////////////////////////
    // Utilities
    //

    public static boolean needParenthis(AstFunNode node)
    {
        if( ! ( node.jjtGetParent() instanceof AstFunNode ) )
            return false;

        AstFunNode parent = (AstFunNode)node.jjtGetParent();
        Function f1 = parent.getFunction();
        Function f2 = node.getFunction();

        if( f1.getPriority() == Function.FUNCTION_PRIORITY || f1.getName().equals( "=" ) || f1.getName().equals( "/" ) )
            return false;

        if( f2.getName().equals( "u-" ) && parent.jjtGetChild( 0 ) != node )
            return true;

        if( f2.getName().equals( "u-" ) )
        {
            if( parent.jjtGetParent() instanceof AstFunNode )
            {
                Function f0 = ( (AstFunNode)parent.jjtGetParent() ).getFunction();
                if( f0.getPriority() == Function.PLUS_PRIORITY && parent.jjtGetParent().jjtGetChild( 0 ) != parent )
                    return true;
            }
        }

        if( f1.getPriority() > f2.getPriority() )
            return true;

        return false;
    }

    /**
     * Creates view for variable or function name.
     *
     * '_' character indicates that subscript should be used for last part of expression.
     * if title starts with '[' and ends with ']' - this means that this is a concentration, those symbols should not be subscripted
     *
     * @pending whether we should use different fonts if expression has several levels of
     * subscript?
     */
    public @Nonnull View createTitleView(String title, ColorFont font, Graphics g)
    {
        if( title.indexOf( '_' ) < 1 )
            return new TextView( title, font, g );

        boolean addBracket = false;
        boolean addQuote = false;

        if( title.startsWith( "[" ) && title.endsWith( "]" ) )
        {
            title = title.substring( 0, title.length() - 1 );
            addBracket = true;
        }
        if( title.endsWith( "\"" ) )
        {
            title = title.substring( 0, title.length() - 1 );
            addQuote = true;
        }
        CompositeView view = new CompositeView();
        int height = 0;
        StringTokenizer tokensDot = new StringTokenizer( title, "." );
        while( tokensDot.hasMoreTokens() )
        {
            String next = tokensDot.nextToken();
            StringTokenizer tokens = new StringTokenizer( next, "_" );
            int i = 0;
            int yOffset = 7;
            while( tokens.hasMoreTokens() )
            {
                if( i == 0 )
                {
                    TextView innerView = new TextView( tokens.nextToken(), defaultFont, g );
                    view.add( innerView, CompositeView.X_RL | CompositeView.Y_UN, new Point( 1, 0 ) );
                }
                else
                {
                    TextView innerView = new TextView( tokens.nextToken(), subscriptFont, g );
                    view.add( innerView, CompositeView.X_RL | CompositeView.Y_UN, new Point( 1, yOffset ) );
                    yOffset += 7;
                }
                i++;
                if( view.size() == 1 )
                    height = view.getBounds().height;
            }

            if( tokensDot.hasMoreTokens() )
            {
                TextView innerView = new TextView( ".", defaultFont, g );
                view.add( innerView, CompositeView.X_RL | CompositeView.Y_UN, new Point( 1, 0 ) );
            }
            dy = ( height - view.getBounds().height ) / 2;
        }

        if( addQuote )
        {
            TextView innerView = new TextView( "\"", defaultFont, g );
            view.add( innerView, CompositeView.X_RL | CompositeView.Y_UN, new Point( 1, 0 ) );
        }

        if( addBracket )
        {
            TextView innerView = new TextView( "]", defaultFont, g );
            view.add( innerView, CompositeView.X_RL | CompositeView.Y_UN, new Point( 1, 0 ) );
        }
        return view;
    }

    protected void addAligned(CompositeView cv, Node node, int mode, Point offset, Graphics g)
    {
        dy = 0;
        View view = createNodeView( node, g );
        align( view );
        cv.add( view, mode | CompositeView.Y_UN, offset );
    }

    protected void addAligned(CompositeView cv, View view, int mode, Point offset)
    {
        dy = 0;
        align( view );
        cv.add( view, mode | CompositeView.Y_UN, offset );
    }

    protected void align(View view)
    {
        Rectangle r = view.getBounds();
        view.setLocation( r.x, y - r.height / 2 - dy );
    }

    ///////////////////////////////////////////////////////////////////
    // Create views for different node types
    //

    public @Nonnull View createNodeView(Node node, Graphics g)
    {
        try
        {
            if( node instanceof AstConstant )
                return createConstantView( (AstConstant)node, g );

            if( node instanceof AstVarNode )
                return createVariableView( (AstVarNode)node, g );

            if( node instanceof AstFunNode )
                return createFunctionView( (AstFunNode)node, g );

            if( node instanceof AstFunctionDeclaration )
                return createFunctionDeclarationView( (AstFunctionDeclaration)node, g );

            if( node instanceof AstPiecewise )
                return createPiecewiseView( (AstPiecewise)node, g );

        }
        catch( Throwable t )
        {
            new TextView( node.toString() + " - " + t.getMessage(), errorFont, g );
        }

        return new TextView( node.toString(), errorFont, g );
    }

    public @Nonnull View createConstantView(AstConstant node, Graphics g)
    {
        String text = node.getName();
        if( text == null )
            text = node.getValue() != null ? node.getValue().toString() : "?";
        if( text.endsWith( ".0" ) )
            text = text.substring( 0, text.length() - 2 );
        return new TextView( text, defaultFont, g );
    }

    public @Nonnull View createVariableView(AstVarNode node, Graphics g)
    {
        return createTitleView( node.getName(), defaultFont, g );
    }

    public @Nonnull View createFunctionView(AstFunNode node, Graphics g)
    {
        Function f = node.getFunction();

        // process special cases
        if( DefaultParserContext.DIVIDE.equals( f.getName() ) )
            return createDivisionView( node, g );

        if( DefaultParserContext.DIFF.equals( f.getName() ) )
            return createDifferentiationView( node, g );

        if( DefaultParserContext.POWER.equals( f.getName() ) )
            return createPowerView( node, g );
        // end of special cases

        CompositeView view = new CompositeView();
        View funcView = null;
        // operator
        if( f.getPriority() != Function.FUNCTION_PRIORITY )
        {
            String name = f.getName();
            if( name.equals( "u-" ) )
                name = "-";
            else if( name.equals( "==" ) )
                name = "=";
            else if( name.equals( "*" ) )
                name = multiplySign;

            funcView = new TextView( name, defaultFont, g );
            y = funcView.getBounds().y + funcView.getBounds().height / 2;

            view.add( funcView );

            // unary operator
            if( f.getNumberOfParameters() == 1 )
            {
                if( node.jjtGetNumChildren() == 1 )
                    addAligned( view, node.jjtGetChild( 0 ), CompositeView.X_RL, offset, g );
            }

            // binary operator
            else
            {
                if( node.jjtGetNumChildren() == 2 )
                {
                    addAligned( view, node.jjtGetChild( 0 ), CompositeView.X_LR, offset, g );
                    addAligned( view, node.jjtGetChild( 1 ), CompositeView.X_RL, offset, g );
                }
            }
        }

        // function
        else
        {
            // we are using '(' as center for alignment
            View centerView = new TextView( "(", defaultFont, g );
            y = centerView.getBounds().y + centerView.getBounds().height / 2;

            view.add( centerView );

            dy = 0;
            funcView = createTitleView( f.getName(), defaultFont, g );
            align( funcView );
            view.add( funcView, CompositeView.X_LR | CompositeView.Y_UN, null );

            int n = node.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
            {
                addAligned( view, node.jjtGetChild( i ), CompositeView.X_RL, null, g );

                if( i < n - 1 )
                    addAligned( view, new TextView( ", ", defaultFont, g ), CompositeView.X_RL, null );
            }

            addAligned( view, new TextView( ")", defaultFont, g ), CompositeView.X_RL, null );

            funcView = centerView;
        }

        dy = funcView.getBounds().y + funcView.getBounds().height / 2 - view.getBounds().y - view.getBounds().height / 2;

        boolean parenthis = needParenthis( node );
        if( parenthis )
        {
            view.add( new TextView( "(", defaultFont, g ), CompositeView.X_LR | CompositeView.Y_CC, new Point( 0, dy ) );
            view.add( new TextView( ")", defaultFont, g ), CompositeView.X_RL | CompositeView.Y_CC, new Point( 0, dy ) );
        }

        return view;
    }

    public @Nonnull View createDivisionView(AstFunNode node, Graphics g)
    {
        CompositeView view = new CompositeView();

        view.add( createNodeView( node.jjtGetChild( 0 ), g ) );
        int h = view.getBounds().height;

        view.add( createNodeView( node.jjtGetChild( 1 ), g ), CompositeView.X_CC | CompositeView.Y_BT, new Point( 0, 5 ) );

        addHorisontalLine( view, h );

        dy = ( 2 * h + 5 - view.getBounds().height ) / 2;

        return view;
    }

    public @Nonnull View createDifferentiationView(AstFunNode node, Graphics g)
    {
        CompositeView view = new CompositeView();

        // first child should be variable name
        AstVarNode dx = (AstVarNode)node.jjtGetChild( 1 );
        view.add( new TextView( "d ", defaultFont, g ) );
        String name = dx.getName();
        if( name.startsWith( "[" ) && name.endsWith( "]" ) )
            name = name.substring( 1, name.length() - 1 );
        view.add( createTitleView( name, defaultFont, g ), CompositeView.X_RL | CompositeView.Y_TT, null );
        //        addAligned( view, new TextView( ")", defaultFont, g ), CompositeView.X_RL, null );
        int h = view.getBounds().height;

        // second node also should be variable name
        AstVarNode dt = (AstVarNode)node.jjtGetChild( 0 );
        String tname = dt.getName();
        view.add( createTitleView( "d " + tname, defaultFont, g ), CompositeView.X_CC | CompositeView.Y_BT, new Point( 0, 5 ) );

        addHorisontalLine( view, h );
        dy = ( 2 * h + 5 - view.getBounds().height ) / 2;
        return view;
    }

    protected void addHorisontalLine(CompositeView view, int h)
    {
        view.add( new LineView( defaultPen, 0, 0, view.getBounds().width + 5, 0 ), CompositeView.X_CC | CompositeView.Y_TT,
                new Point( 0, h + 2 ) );
        view.add( new BoxView( null, null, 0, 0, view.getBounds().width + 7, 0 ), CompositeView.X_CC | CompositeView.Y_TT,
                new Point( 0, h + 2 ) );
    }

    protected @Nonnull View createPowerView(AstFunNode node, Graphics g)
    {
        CompositeView view = new CompositeView();

        dy = 0;
        view.add( createNodeView( node.jjtGetChild( 0 ), g ) );
        int center = view.getBounds().y + view.getBounds().height / 2 + dy;

        ColorFont oldFont = defaultFont;
        defaultFont = superscriptFont;
        view.add( createNodeView( node.jjtGetChild( 1 ), g ), CompositeView.X_RL | CompositeView.Y_TB, superscript );
        defaultFont = oldFont;

        dy = center - view.getBounds().y - view.getBounds().height / 2;
        return view;
    }

    public @Nonnull View createFunctionDeclarationView(AstFunctionDeclaration node, Graphics g)
    {
        CompositeView view = new CompositeView();

        // warning: this is modified copy & paste fragment from functionView
        // we are using it to create f(x, y) view
        // may be we need some refactoring here

        // we are using '(' as center for alignment
        View centerView = new TextView( "(", defaultFont, g );
        y = centerView.getBounds().y + centerView.getBounds().height / 2;

        view.add( centerView );

        dy = 0;
        View funcView = createTitleView( node.getName(), defaultFont, g );
        align( funcView );
        view.add( funcView, CompositeView.X_LR | CompositeView.Y_UN, null );

        int n = node.jjtGetNumChildren() - 1;
        for( int i = 0; i < n; i++ )
        {
            addAligned( view, node.jjtGetChild( i ), CompositeView.X_RL, null, g );

            if( i < n - 1 )
                addAligned( view, new TextView( ", ", defaultFont, g ), CompositeView.X_RL, null );

        }
        addAligned( view, new TextView( ")", defaultFont, g ), CompositeView.X_RL, null );
        addAligned( view, new TextView( " = ", defaultFont, g ), CompositeView.X_RL, null );

        addAligned( view, node.jjtGetChild( n ), CompositeView.X_RL, null, g );

        dy = centerView.getBounds().y + centerView.getBounds().height / 2 - view.getBounds().y - view.getBounds().height / 2;

        return view;
    }

    ///////////////////////////////////////////////////////////////////
    // Piecewise issues
    //

    public @Nonnull View createPiecewiseView(AstPiecewise node, Graphics g)
    {
        CompositeView view = new CompositeView();
        int n = node.jjtGetNumChildren();

        // calculate the maximum value width, views are stored in the array
        int maxWidth = 0;
        View[] values = new View[n];
        int[] valueDy = new int[n];
        for( int i = 0; i < n; i++ )
        {
            dy = 0;
            AstPiece piece = (AstPiece)node.jjtGetChild( i );
            values[i] = createNodeView( piece.getValue(), g );
            valueDy[i] = dy;
            maxWidth = Math.max( maxWidth, values[i].getBounds().width );
        }

        for( int i = 0; i < n; i++ )
        {
            View pieceView = createPieceView( (AstPiece)node.jjtGetChild( i ), values[i], valueDy[i], maxWidth, g );
            view.add( pieceView, CompositeView.X_LL | CompositeView.Y_BT, new Point( 0, 5 ) );
        }

        // create brace
        int h = view.getBounds().height;
        GeneralPath path = new GeneralPath();
        path.moveTo( 5, -5 );
        path.quadTo( 0, -5, 0, 0 );
        path.lineTo( 0, h / 2.0 - 7 );
        path.quadTo( 0, h / 2.0 - 1, -3, h / 2.0 );
        path.quadTo( 0, h / 2.0 + 1, 0, h / 2.0 + 7 );
        path.lineTo( 0, h );
        path.quadTo( 0, h + 4, 5, h + 4 );

        view.add( new PathView( boldPen, path ), CompositeView.X_LR | CompositeView.Y_CC, offset );

        // space delimiter
        view.add( new BoxView( null, null, 0, 0, 3, 0 ), CompositeView.X_LR | CompositeView.Y_CC, null );

        dy = 0;
        return view;
    }

    public View createPieceView(AstPiece node, View valueView, int valueDy, int conditionOffset, Graphics g)
    {
        CompositeView view = new CompositeView();

        String text = "if ";
        if( node.getCondition() == null )
            text = "otherwise";
        View textView = new TextView( text, defaultFont, g );
        view.add( textView );
        y = textView.getBounds().y + textView.getBounds().height / 2;

        valueView.setLocation( - ( conditionOffset + 10 ), 0 );
        dy = valueDy;
        align( valueView );
        view.add( valueView );

        if( node.getCondition() != null )
            addAligned( view, node.getCondition(), CompositeView.X_RL, offset, g );

        return view;
    }
}
