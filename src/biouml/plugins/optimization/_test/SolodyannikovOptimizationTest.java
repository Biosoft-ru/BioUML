package biouml.plugins.optimization._test;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Diagram;
import biouml.plugins.optimization.ExperimentalTableSupport.WeightMethod;
import biouml.plugins.optimization.OptimizationConstraint;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.optimization.OptimizationExperiment.ExperimentType;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class SolodyannikovOptimizationTest extends AbstractBioUMLTest
{
    public SolodyannikovOptimizationTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SolodyannikovOptimizationTest.class);
        return suite;
    }

    public void testSres() throws Exception
    {
        SRESOptMethod method = new SRESOptMethod(null, "sresTest");
        method.getParameters().setNumOfIterations(500);
        method.getParameters().setRandomSeed(1);
 
        List<Parameter> params = new ArrayList<>();
        params.add(new Parameter("BR_HeartCenter", 6, 5, 7));
        params.add(new Parameter("Reactivity_HeartCenter", 2, 1.5, 2.5));
        params.add(new Parameter("Stress_HeartCenter", 0.0015, 0.001,  0.002));
        params.add(new Parameter("Conductivity_Arterial", 11.7, 8, 20));
        params.add(new Parameter("Arterial_Tone", 120, 100, 140));
        params.add(new Parameter("k_1", 0.6, 0.4, 1));
        params.add(new Parameter("k_2", 20, 15, 25));
        params.add(new Parameter("S_1", 22, 18, 16));
        params.add(new Parameter("S_2", 0.25, 0.2, 0.3));
        params.add(new Parameter("S_3", 0.2, 0.1, 0.35));
        
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(loadDiagram());
        engine.setOutputDir("../out");
        engine.setCompletionTime(1E3);
       
//        OptimizationProblem problem = new SingleExperimentParameterEstimation(engine, createSteadyStateExperiment(), params,
//                createConstraints());
//
//        method.setOptimizationProblem(problem);
//
//        double[] solution = method.getSolution();

//        System.out.println(solution[0]);
    }


    private static OptimizationExperiment createSteadyStateExperiment()
    {
        TableDataCollection tdc = new StandardTableDataCollection(null, "d");
        tdc.getColumnModel().addColumn("CardiacOutput_Minute", Double.class);
        TableDataCollectionUtils.addRow(tdc, "0", new Object[] {5});
        OptimizationExperiment experiment = new OptimizationExperiment("exp", tdc);

        experiment.setExperimentType(ExperimentType.toString(ExperimentType.STEADY_STATE));
        experiment.setWeightMethod(WeightMethod.toString(WeightMethod.MEAN_SQUARE));
        experiment.initWeights();
        return experiment;
    }
    private static Diagram loadDiagram() throws Exception
    {
        String repositoryPath = "../data";
        CollectionFactory.createRepository(repositoryPath);
        return DataElementPath.create("databases/agentmodel_test/Diagrams/HeartModel").getDataElement(Diagram.class);
    }

    private List<OptimizationConstraint> createConstraints()
    {
        //Constarints
        List<OptimizationConstraint> constraints = new ArrayList<>();
        OptimizationConstraint constr = new OptimizationConstraint();
        constr.setInitialTime(0);
        constr.setCompletionTime(1E4);
        constr.setFormula("Pressure_Arterial < 120");
        constraints.add(constr);
        
        constr = new OptimizationConstraint();
        constr.setInitialTime(0);
        constr.setCompletionTime(1E4);
        constr.setFormula("Pressure_Arterial > 80");
        constraints.add(constr);
        
        constr = new OptimizationConstraint();
        constr.setInitialTime(0);
        constr.setCompletionTime(1E4);
        constr.setFormula("Pulse < 90");
        constraints.add(constr);
        
        constr = new OptimizationConstraint();
        constr.setInitialTime(0);
        constr.setCompletionTime(1E4);
        constr.setFormula("Pulse > 70");
        constraints.add(constr);
        
        return constraints;
    }
    
}
