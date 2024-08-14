package biouml.plugins.simulation._test;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.TableElement.Variable;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.TableElementPreprocessor;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.MathDiagramType;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestTableElementPreprocessor extends TestCase
{
    public TestTableElementPreprocessor(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( TestTableElementPreprocessor.class.getName() );
        suite.addTest( new TestTableElementPreprocessor( "test" ) );
        return suite;
    }
    {

    }

    public void test() throws Exception
    {
        testTableElementPreprocessor( new double[] {0, 10, 20, 24, 30, 38, 45}, new double[] {1.06, 1.03, 0.97, 0.93, 0.8, 0.46, 0}, "y" );
        testTableElementPreprocessor( new double[] {0, 70, 100, 125, 160, 200, 240}, new double[] {1.04, 1.025, 1.0, 0.96, 0.88, 0.59, 0},
                "x" );
    }

    public void testTableElementPreprocessor(double[] time, double[] x, String varName) throws Exception
    {

        TableDataCollection table = createTable( time, x, varName );
        Diagram diagram = createDiagram( table );
        assertNotNull( diagram );

        TableElementPreprocessor preprocessor = new TableElementPreprocessor();
        Diagram resultDiagram = preprocessor.preprocess( diagram );
        assertNotNull( resultDiagram );

        assertEquals( resultDiagram.getNodes().length, 1 );

        Node piecewiseNode = resultDiagram.getNodes()[0];

        assertTrue( piecewiseNode.getRole() != null && piecewiseNode.getRole() instanceof Equation );

        SimulationResult result = new SimulationResult( null, "result" );

        SimulationEngine simulationEngine = new JavaSimulationEngine();
        simulationEngine.setDiagram( diagram );
        simulationEngine.setInitialTime( 0 );
        simulationEngine.setCompletionTime( time[time.length - 1] );
        simulationEngine.setTimeIncrement( 1 );
        simulationEngine.setOutputDir( "../out/" );
        simulationEngine.simulate( result );
        

        result.getValues();
        int index = result.getVariableMap().get( varName );
        double[][] interpolated = result.interpolateLinear( time );

        boolean error = false;
        for( int i = 0; i < time.length; i++ )
        {
            if( Math.abs( x[i] - interpolated[i][index] ) >= 1E-6 )
            {
                error = true;
                System.out.println( "At time " + time[i] + " expected: " + x[i] + ", was: " + interpolated[i][index] );
            }
        }

        assertTrue( !error );
    }


    public TableDataCollection createTable(double[] times, double[] vals, String varName)
    {
        TableDataCollection result = new StandardTableDataCollection( null, "test_table" + varName );
        result.getColumnModel().addColumn( new TableColumn( "time", Double.class ) );
        result.getColumnModel().addColumn( new TableColumn( varName, Double.class ) );

        for( int i = 0; i < times.length; i++ )
        {
            Object[] nextRow = {times[i], vals[i]};
            TableDataCollectionUtils.addRow( result, String.valueOf( i ), nextRow );
        }
        return result;
    }

    public Diagram createDiagram(TableDataCollection table) throws Exception
    {
        String columnName = null;
        for( TableColumn column : table.getColumnModel() )
        {
            if( !column.getName().equals( "time" ) )
                columnName = column.getName();
        }

        Diagram result = new MathDiagramType().createDiagram( null, "test_diagram", new DiagramInfo( null, "test_diagram" + columnName ) );

        Node node = new Node( result, new Stub( null, "table", Type.TYPE_TABLE_ELEMENT ) );
        TableElement role = new TableElement( node );
        node.setRole( role );

        role.setTable( table );
        role.setFormula( columnName+" ~ time" );
        for( Variable var : role.getVariables() )
        {
            var.setName( var.getColumnName() );
        }
        result.put( node );
        return result;
    }

}