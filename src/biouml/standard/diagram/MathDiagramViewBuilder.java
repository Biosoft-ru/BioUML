package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.TableElement.Variable;
import biouml.standard.type.Type;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.TextView;

public class MathDiagramViewBuilder extends PathwaySimulationDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new MathDiagramViewOptions(null);
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( Type.TYPE_TABLE_ELEMENT.equals(node.getKernel().getType()) )
            return createTableEntityCoreView(container, node, options, g);

        return super.createNodeCoreView(container, node, options, g);
    }

    @Override
    protected boolean createEquationView(CompositeView view, Node node, PathwaySimulationDiagramViewOptions options, Graphics g,
            boolean isInsideBlock)
    {
        Equation eq = (Equation)node.getRole();

        boolean simple = ( ( (MathDiagramViewOptions)options ).getEquationStyle() ).equals(EquationStyle.SIMPLE_NAME);
        if( eq != null && eq.getVariable() != null )
        {
            String equationText;
            if( Equation.isScalar(eq.getType()) || eq.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT) )
            {
                equationText = eq.getVariable();
                if( !simple )
                    equationText += " = " + eq.getFormula();
                view.add(createMathView(eq, equationText, options, g));
            }
            else if( eq.getType().equals(Equation.TYPE_RATE) )
            {
                if (simple)
                    equationText = eq.getVariable();
                else
                {
                    equationText = "diff(time," + eq.getVariable() + ")";
                if( !simple )
                    equationText += " = " + eq.getFormula();
                }
                view.add(createMathView(eq, equationText, options, g));
            }
            else
            {
                view.add(createMathView(eq, eq.getFormula(), options, g));
                view.add(new TextView(" = 0", formulaViewBuilder.getDefaultFont(), g), CompositeView.X_RL | CompositeView.Y_CC);
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
        if( !isInsideBlock )
        {

            view.add( new BoxView( getBorderPen( node, options.getMathPen() ), null,
                    new RoundRectangle2D.Float( r.x - d, r.y - d, r.width + d * 2, r.height + d * 2,
                    10, 10)));
        }

        return false;
    }

    public boolean createTableEntityCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        TableElement te = node.getRole( TableElement.class );
        if( te.getTable() == null )
        {
            TextView header = new TextView( "Please specify table!", options.getDefaultFont(), g );
            header.setLocation( 5, 5 );

            int width = header.getBounds().width + 10;
            int headerheight = header.getBounds().height;
            int height = headerheight*3+15;
            container.add( new BoxView( options.getDefaultPen(), new Brush( Color.white ), 0, 0, width, height ) );

            //vertical
            container.add( new LineView( options.getDefaultPen(), width/2f, headerheight+5, width/2f, height ) );

            //horizontal
            container.add( new LineView( options.getDefaultPen(), 0, headerheight+5, width, headerheight+5 ) );
            container.add( new LineView( options.getDefaultPen(), 0, headerheight*2+10, width, headerheight*2+10 ) );

            container.add( header );
            return false;
        }
        Variable[] variables = te.getVariables();
        String formula = te.getFormula();
        if( formula == null )
            formula = "No formula";
        TextView header = new TextView( formula, options.getDefaultFont(), g );
        header.setLocation( 0, 0 );
        container.add( header );

        int size = variables.length;
        LineView[] verticalLines = new LineView[size - 1];
        TextView[] colNames = new TextView[size];
        TextView[] varNames = new TextView[size];
        int yStart = header.getBounds().height;
        int y = 0;
        int x = 0;
        for( int i = 0; i < size; i++ )
        {
            String colName = variables[i].getColumnName();
            String varName = variables[i].getName();

            colNames[i] = new TextView( colName, options.getDefaultFont(), g );
            varNames[i] = new TextView( varName, options.getDefaultFont(), g );

            x += 5;
            y = colNames[i].getBounds().height + 10 + yStart;
            colNames[i].setLocation( x, 5+ yStart );
            varNames[i].setLocation( x, 5+ yStart );
            x += Math.max( colNames[i].getBounds().width, varNames[i].getBounds().width) + 5;
            if( i < size - 1 )
                verticalLines[i] = new LineView( options.getDefaultPen(), x, yStart, x, y * 3 + 15 );
        }

        for( int i = 0; i < size; i++ )
        {
            varNames[i].setLocation( varNames[i].getBounds().getLocation().x, y+5 );
        }

        container.add( new BoxView( options.getDefaultPen(), new Brush( Color.white ), 0, yStart, x, y * 3 + 15 - yStart ) );

        for( int i = 0; i < size - 1; i++ )
        {
            container.add(colNames[i] );
            container.add(varNames[i] );
            container.add( verticalLines[i]);
        }
        container.add( colNames[size-1] );
        container.add( varNames[size-1] );

        container.add( new LineView(  options.getDefaultPen(), 0, y, x, y ));
        container.add( new LineView(  options.getDefaultPen(), 0, 2*y+5, x, 2*y+5 ));
        return false;
    }
    
    @Override
    public boolean forbidCustomImage(Node node)
    {
        return true;
    }
}