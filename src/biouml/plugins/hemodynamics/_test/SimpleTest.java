package biouml.plugins.hemodynamics._test;

import java.util.HashMap;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsObserver;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.SimpleVessel;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class SimpleTest extends AbstractBioUMLTest
{
    private static final double START_SEARCH_TIME = 0;
    private static final double COMPLETION_TIME = 3;

    private static final Object R_RADIAL = "R. Radial";;

    private static final Object AORTAL = "Ascending Aorta";
    private static final Object L_RADIAL = "L. Radial";

    public SimpleTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(SimpleTest.class.getName());
        suite.addTest(new SimpleTest("test"));
        return suite;
    }

    Map<String, Double> R = new HashMap<>();
    
  
    //TODO: more sophisticated test
    public void test() throws Exception
    {
        Diagram diagram = getDiagram("Arterial Tree new WP notmatched");
//        Diagram diagram = getDiagram("Arterial Tree WP");
        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        HemodynamicsModelSolver solver = (HemodynamicsModelSolver)engine.getSimulator();
        EModel emodel = diagram.getRole(EModel.class);
//        emodel.getVariable("HR").setInitialValue(60);
//        emodel.getVariable("vesselSegments").setInitialValue(300);
//        emodel.getVariable("outputFlow").setInitialValue(0);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
        engine.setCompletionTime(20);
engine.setTimeIncrement(0.001);
        HemodynamicsObserver observer = new HemodynamicsObserver();
        observer.skipCycles = 0;
        observer.searchTime = 0.1;
        
        ArterialBinaryTreeModel atm = engine.createModel();
        
        for( SimpleVessel v : atm.vessels )
        {
            if (v.left == null && v.right == null)
                continue;
           
            System.out.println(v.getTitle()+"\t"+ calcR(v, v.left, v.right)+"\t"+calcRWeighted(v, v.left, v.right));
        }
        engine.simulate(atm, new ResultListener[] {new ResultPlotPane(engine, null)});
    }
    
    public double calcR(SimpleVessel v, SimpleVessel v1, SimpleVessel v2)
    {
        double y = v.unweightedArea / Math.sqrt(v.beta / ( 2 * Math.sqrt(v.unweightedArea) ));
        double yLeft = v1 == null? 0: v1.unweightedArea / Math.sqrt(v1.beta / ( 2 * Math.sqrt(v1.unweightedArea) ));
        double yRight = v2 == null? 0: v2.unweightedArea / Math.sqrt(v2.beta / ( 2 * Math.sqrt(v2.unweightedArea) ));
        double r = ( y - yLeft - yRight ) / ( y + yLeft + yRight );
        return r;
    }
    
    public double calcRWeighted(SimpleVessel v, SimpleVessel v1, SimpleVessel v2)
    {
        double y = v.area[0] / Math.sqrt(v.beta / ( 2 * Math.sqrt(v.area[0] ) ));
        double yLeft = v1 == null? 0: v1.area[0]  / Math.sqrt(v1.beta / ( 2 * Math.sqrt(v1.area[0] ) ));
        double yRight = v2 == null? 0: v2.area[0]  / Math.sqrt(v2.beta / ( 2 * Math.sqrt(v2.area[0] ) ));
        double r = ( y - yLeft - yRight ) / ( y + yLeft + yRight );
        return r;
    }
    
    public double calcR2(SimpleVessel v, SimpleVessel v1, SimpleVessel v2)
    {
        double y = v.unweightedArea * Math.sqrt(v.unweightedArea) / v.beta;
        double yLeft = v1 == null? 0:  v1.unweightedArea * Math.sqrt(v1.unweightedArea) / v1.beta;
        double yRight = v2 == null? 0:  v2.unweightedArea * Math.sqrt(v2.unweightedArea) / v2.beta;
        double r = ( y - yLeft - yRight ) / ( y + yLeft + yRight );
        return r;
    }
    
    public Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository("../data");
        return DataElementPath.create("databases/Virtual Human/Diagrams/"+name).getDataElement(Diagram.class);
    }

}
