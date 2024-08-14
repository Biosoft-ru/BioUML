package biouml.plugins.hemodynamics._test;

import biouml.model.Diagram;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.InitialValuesCalculator;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class TestInitialValuesCalculator extends TestCase
{
    public TestInitialValuesCalculator(String name)
    {
        super( name );
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( TestInitialValuesCalculator.class.getName() );
        suite.addTest( new TestInitialValuesCalculator( "test" ) );
        return suite;
    }


    public static void test() throws Exception
    {
        Diagram diagram = getDiagram();
        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram( diagram );
        ArterialBinaryTreeModel model = engine.createModel();

        model.inputPressure = 100;
        model.outputPressure = 70;
        InitialValuesCalculator calculator = new InitialValuesCalculator();
        calculator.calculate( model, HemodynamicsOptions.PRESSURE_INITIAL_CONDITION,
                HemodynamicsOptions.PRESSURE_INITIAL_CONDITION );
    }

    public static Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        return DataElementPath.create( "databases/Virtual Human/Diagrams/Arterial Tree test2" ).getDataElement( Diagram.class );
    }
}