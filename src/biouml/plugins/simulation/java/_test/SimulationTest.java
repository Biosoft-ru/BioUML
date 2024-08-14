package biouml.plugins.simulation.java._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class SimulationTest extends AbstractBioUMLTest
{
    public static final String SIMULATION_REPOSITORY_PATH = "../data/test/biouml/plugins/simulation/data";
    public static final String MODEL_NAME = "CellCycle_1991Gol";
    public static final String SIMULATION_RESULT_NAME =  "_test "+MODEL_NAME;
    
    static Diagram diagram;
    DataCollection resultDC;
    static SimulationResult result;
    static OdeSimulationEngine jse = new JavaSimulationEngine();

    public SimulationTest(String name)
    {
        super(name);
        File configFile = new File( "./biouml/plugins/simulation/java/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SimulationTest.class.getName());

        suite.addTest(new SimulationTest("testCreateDiagram"));
        suite.addTest(new SimulationTest("testGenerateCode"));
        suite.addTest(new SimulationTest("testSimulate"));
        suite.addTest(new SimulationTest("testSaveResult"));
        suite.addTest(new SimulationTest("testSimulateToPlot"));
        return suite;
    }

    public void testCreateDiagram() throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository( "../data" );
        assertNotNull("Wrong repository", repository);
        diagram = (Diagram)CollectionFactory.getDataCollection("databases/SBML model repository/Diagrams/CellCycle-1991Gol");
        assertNotNull("Diagram not loaded", diagram);
    }

    public void testGenerateCode() throws Exception
    {
        jse.setDiagram(diagram);
        jse.setOutputDir( AbstractBioUMLTest.getTestDir().getPath() );
        File[] modelFiles = jse.generateModel(true);
        assertNotNull(modelFiles);
    }

    public void testSimulate() throws Exception
    {
        File rightModel = new File(jse.getOutputDir() + "/" + MODEL_NAME+".java");
        DataCollection<?> repository = CollectionFactory.createRepository( SIMULATION_REPOSITORY_PATH );
        resultDC = CollectionFactory.getDataCollection("data/Data");
        result = new SimulationResult(resultDC, SIMULATION_RESULT_NAME);
        String success = jse.simulate(new File[] {rightModel}, result);
        System.out.println("Result: " + success);
    }

    public void testSaveResult() throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository( SIMULATION_REPOSITORY_PATH );
        resultDC = CollectionFactory.getDataCollection("data/Data");
        resultDC.put(result);
        resultDC.release(SIMULATION_RESULT_NAME);
    }

    public void testSimulateToPlot() throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);
        PlotInfo plotInfo = new PlotInfo(emodel);
        Curve c1 = new Curve("", "$cytoplasm.C", "C", emodel);
        Curve c2 = new Curve("", "$cytoplasm.M", "M", emodel);
        Curve c3 = new Curve("", "$cytoplasm.X", "X", emodel);
        plotInfo.setYVariables(new Curve[] {c1, c2, c3});
        DiagramUtility.setPlotsInfo(diagram, new PlotsInfo(emodel, new PlotInfo[] {plotInfo}));
        String success = jse.simulate(new ResultListener[] {new ResultPlotPane(jse, null, plotInfo)});
        System.out.println("Result: " + success);
    }
}
