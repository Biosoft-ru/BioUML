package biouml.plugins.simulation;

import javax.annotation.Nonnull;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysis.Util.CubicSpline;
import ru.biosoft.analysis.Util.LinearSpline;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil2;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.TableElement.SplineType;
import biouml.model.dynamics.TableElement.Variable;
import biouml.model.dynamics.util.EModelHelper;

public class TableElementPreprocessor extends Preprocessor
{    
    SemanticController controller;
    
    @Override
    public Diagram preprocess(Diagram diagram)
    {
        if( !accept( diagram ) )
            return diagram;

        controller = diagram.getType().getSemanticController();
        processTableElements( diagram );
        return diagram;
    }

    public void processTableElements(Compartment compartment)
    {
        for( Node node : compartment.getNodes() )
        {
            if( node.getRole() instanceof TableElement )
            {
                try
                {
                    compartment.remove( node.getName() );
                    processTableElement(compartment, node );
                }
                catch( Exception ex )
                {
                    throw new BiosoftCustomException( ex,  "Error during node " + node.getName() + " preprocessing: " + ex.getMessage() );
                }
            }
            else if (node.getRole() instanceof SimpleTableElement)
            {
                try
                {
                    compartment.remove( node.getName() );
                    processSimpleTableElement(compartment, node );
                }
                catch( Exception ex )
                {
                    throw new BiosoftCustomException( ex,  "Error during node " + node.getName() + " preprocessing: " + ex.getMessage() );
                }
            }
            else if( node instanceof Compartment )
            {
                processTableElements( (Compartment)node );
            }
        }
    }

    public static void processSimpleTableElement(@Nonnull Compartment compartment, Node node) throws Exception
    {
        SimpleTableElement tableElement = node.getRole( SimpleTableElement.class );
        TableDataCollection table = tableElement.getTable();
        if( table == null )
            throw new Exception( "Please specufy table data collection for element " + node.getTitle() );

        VarColumn argColumn = tableElement.getArgColumn();
        VarColumn[] columns = tableElement.getColumns();

        String argName = argColumn.getVariable();
        String argColumnName = argColumn.getColumn();
        double[] arg = TableDataCollectionUtils.getColumn( table, argColumnName );

        SemanticController controller = Diagram.getDiagram( compartment).getType().getSemanticController();
        
        for( int i = 0; i < columns.length; i++ )
        {
            VarColumn var = columns[i];
            String varName = var.getVariable();
            String columnName = var.getColumn();
            double[] values = TableDataCollectionUtils.getColumn( table, columnName );
            String rightHandSide = generatePiecewise( argName, arg, values );
            Equation eq = new Equation( null, Equation.TYPE_SCALAR, varName );
            eq.setFormula( rightHandSide );
            DiagramElement de = controller.createInstance( compartment, Equation.class, node.getLocation(), eq ).getElement();
            compartment.put( de );
        }
    }
    
    private void processTableElement(@Nonnull Compartment compartment, Node node) throws Exception
    {
        TableElement tableElement = node.getRole( TableElement.class );

        TableDataCollection table = tableElement.getTable();

        if( table == null )
            throw new Exception( "Please specufy table data collection for element " + node.getTitle() );

        String formula = tableElement.getFormula();

        String leftCol = null;
        String rightCol = null;
        String[] vars = TextUtil2.split( formula, '~' );
        if( vars.length != 2 )
            throw new Exception( "Illegal formula in table element " + node.getName() + " : " + formula );
        vars[0] = vars[0].trim();
        vars[1] = vars[1].trim();


        Variable[] tableVars = tableElement.getVariables();
        for( Variable var : tableVars )
        {
            if( var.getName().equals( vars[0] ) )
                leftCol = var.getColumnName();
            else if( var.getName().equals( vars[1] ) )
                rightCol = var.getColumnName();
        }

        if( leftCol == null || rightCol == null )
            throw new Exception( "Illegal formula in table element " + node.getName() + " : " + formula );

        double[] timeValues = TableDataCollectionUtils.getColumn( table, rightCol );
        double[] values = TableDataCollectionUtils.getColumn( table, leftCol );

        if( timeValues == null || values == null )
            throw new Exception( "Error during data from table = " + table.getName() + " processing, probbaly data is missing" );

        int[] pos = Util.sort( timeValues );
        double[] newValues = new double[values.length];
        for( int i = 0; i < values.length; i++ )
            newValues[i] = values[pos[i]];

        if( tableElement.isCycled() )
        {
            double maxValue = timeValues[values.length - 1];
            double minValue = timeValues[0];
            double length = maxValue - minValue;
            EModel emodel = compartment.getRole( EModel.class );
            String newArg = EModelHelper.generateUniqueVariableName( emodel, vars[1] );
            Equation addiditonalEquation = new Equation( null, Equation.TYPE_SCALAR, newArg );
            addiditonalEquation.setFormula( "mod( " + vars[1] + " - " + minValue + "+ (Math.round(" + minValue + "/" + length + ")+1)*"
                    + length + ", " + length + ")" );
            vars[1] = newArg;
            DiagramElement de = controller.createInstance( compartment, Equation.class, node.getLocation(), addiditonalEquation )
                    .getElement();
            compartment.put( de );

            for( int i = 0; i < timeValues.length; i++ )
                timeValues[i] -= minValue;
        }

        Equation eq = new Equation( null, Equation.TYPE_SCALAR, vars[0] );
        
		int ind = SplineType.getSplineTypes().indexOf(tableElement.getSplineType());
		SplineType type = SplineType.values()[ind];

        switch( type )
        {
            case CUBIC:
            	CubicSpline cs = new CubicSpline(timeValues, newValues);
    			eq.setFormula( generateFormula( cs, vars[1] ) );
                break;

            case LINEAR:
            	LinearSpline ls = new LinearSpline(timeValues, newValues);
    			eq.setFormula( generateFormula( ls, vars[1] ) );
                break;
        }

        DiagramElement de = controller.createInstance( compartment, Equation.class, node.getLocation(), eq ).getElement();
        compartment.put( de );
    }

    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.stream( Node.class ).anyMatch( node -> node.getRole() instanceof TableElement || node.getRole() instanceof SimpleTableElement);
    }

    public static String generateFormula(CubicSpline spline, String argName)
    {
        double[] x = spline.getArgument();
        double[] a = spline.getA();
        double[] b = spline.getB();
        double[] c = spline.getC();
        double[] d = spline.getD();

        StringBuffer formula = new StringBuffer( "piecewise(" );

        //for constant extrapolation
        formula.append( "(" );
        formula.append( argName );
        formula.append( "<" );
        formula.append( x[0] );
        formula.append( ")=>" );
        formula.append( a[0] );
        formula.append( ";" );

        for( int i = 0; i < x.length; i++ )
        {
            StringBuffer condition = new StringBuffer();
            condition.append( "(" );
            condition.append( argName );
            condition.append( ">=" );
            condition.append( x[i] );
            condition.append( ")" );
            if( i < x.length - 1 )
            {
                condition.append( "&&(" );
                condition.append( argName );
                condition.append( "<" );
                condition.append( x[i + 1] );
                condition.append( ")" );
            }

            String diff = ( x[i] == 0 ) ? argName : "(" + argName + "-" + x[i] + ")";

            StringBuffer value = new StringBuffer();
            if( a[i] != 0 )
            {
                value.append( a[i] );
            }
            if( b[i] != 0 )
            {
                if( b[i] > 0 )
                    value.append( "+" );
                value.append( b[i] );
                value.append( "*" );
                value.append( diff );
            }
            if( c[i] != 0 )
            {
                if( c[i] > 0 )
                    value.append( "+" );
                value.append( c[i] );
                value.append( "*" );
                value.append( diff );
                value.append( "^2" );
            }
            if( d[i] != 0 )
            {
                if( d[i] > 0 )
                    value.append( "+" );
                value.append( d[i] );
                value.append( "*" );
                value.append( diff );
                value.append( "^3" );
            }
            if( value.length() == 0 )
                value.append( "0" );

            if( i != x.length - 1 )
                value.append( ";" );
            else
                value.append( ")" );

            formula.append( condition );
            formula.append( "=>" );
            formula.append( value );
        }
        return formula.toString();
    }

    public static String generateFormula(LinearSpline spline, String argName)
    {
        double[] x = spline.getArgument();
        double[] a = spline.getA();
        double[] b = spline.getB();

        StringBuffer formula = new StringBuffer( "piecewise(" );

        //for constant extrapolation
        formula.append( "(" );
        formula.append( argName );
        formula.append( "<" );
        formula.append( x[0] );
        formula.append( ")=>" );
        formula.append( a[0] );
        formula.append( ";" );

        for( int i = 0; i < x.length; i++ )
        {
            StringBuffer condition = new StringBuffer();
            condition.append( "(" );
            condition.append( argName );
            condition.append( ">=" );
            condition.append( x[i] );
            condition.append( ")" );
            if( i < x.length - 1 )
            {
                condition.append( "&&(" );
                condition.append( argName );
                condition.append( "<" );
                condition.append( x[i + 1] );
                condition.append( ")" );
            }

            String diff = ( x[i] == 0 ) ? argName : "(" + argName + "-" + x[i] + ")";

            StringBuffer value = new StringBuffer();
            if( a[i] != 0 )
            {
                value.append( a[i] );
            }
            if( b[i] != 0 )
            {
                if( b[i] > 0 )
                    value.append( "+" );
                value.append( b[i] );
                value.append( "*" );
                value.append( diff );
            }
            if( value.length() == 0 )
                value.append( "0" );

            if( i != x.length - 1 )
                value.append( ";" );
            else
                value.append( ")" );

            formula.append( condition );
            formula.append( "=>" );
            formula.append( value );
        }
        return formula.toString();
    }

    public static String generatePiecewise(String argName, double[] time, double[] values)
    {
        StringBuffer result = new StringBuffer();
        result.append( "piecewise( " );               
        double curValue = Double.NaN;        
        for (int i=0; i<time.length - 1; i++)
        {
            double nextValue = values[i];
            if (Double.isNaN( nextValue ))
                continue;            
            else if (!Double.isNaN( curValue ) && nextValue != curValue)// value changed from this time point            
                result.append( argName+" < "+time[i] + " => "+ curValue +"; " );
            curValue = nextValue;
        }
        result.append( curValue +" );");
        return result.toString();
    }
}