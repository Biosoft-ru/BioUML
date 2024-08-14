package biouml.plugins.hemodynamics._test;

import java.util.HashMap;
import java.util.Map;

import biouml.model.Diagram;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsEModel;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.Vessel;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class VesselAreaFitTest extends TestCase
{
    public VesselAreaFitTest(String name)
    {
        super( name );
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( VesselAreaFitTest.class.getName() );
        suite.addTest( new VesselAreaFitTest( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        Diagram d = getDiagram();
        System.out.println( "First iteration" );
        setReferencedPressure(d, 45);
        Map<String, Double> pressures = simulate( d, null );

//        for( int i = 1; i < 10; i++ )
//            pressures = simulate( d, pressures );

    }

    public Map<String, Double> simulate(Diagram diagram, Map<String, Double> pressures) throws Exception
    {
        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();

        if( pressures != null )
            setReferencedPressure( diagram, pressures );
        engine.setDiagram( diagram );
        engine.setInitialTime( 0 );
        engine.setTimeIncrement( 0.001 );
        engine.setCompletionTime( 20 );
        ((HemodynamicsOptions)engine.getSimulatorOptions()).setInputCondition( HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION_STRING );
        ((HemodynamicsOptions)engine.getSimulatorOptions()).setOutputCondition( HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION_STRING );
        ((HemodynamicsOptions)engine.getSimulatorOptions()).setUseFullPressureConservation( false );
        ArterialBinaryTreeModel model = engine.createModel();
        engine.simulate( model, new ResultListener[]{new ResultPlotPane(engine, null)} );
        return getPressures( model );

    }

    public void setReferencedPressure(Diagram diagram, Map<String, Double> values) throws Exception
    {
        HemodynamicsEModel emodel = diagram.getRole(HemodynamicsEModel.class);
        for( Vessel v : emodel.getVessels() )
            v.setReferencedPressure( values.get( v.getName() ) );
//        diagram.save();
    }
    
    public void setReferencedPressure(Diagram diagram, double value) throws Exception
    {
        HemodynamicsEModel emodel = diagram.getRole(HemodynamicsEModel.class);
        for( Vessel v : emodel.getVessels() )
            v.setReferencedPressure(value);
//        diagram.save();
    }

    public Map<String, Double> getPressures(ArterialBinaryTreeModel model)
    {
        Map<String, Double> result = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        model.vesselMap.forEach( (key, val) -> {
            result.put( key, val.getPressure()[0] );
            builder.append( val.getPressure()[0] + "\t" );
        });
        System.out.println( builder.toString() );
        return result;
    }

    public Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        return DataElementPath.create("databases/Virtual Human/Diagrams/Arterial Tree Brachial" ).getDataElement(Diagram.class);
    }
}
