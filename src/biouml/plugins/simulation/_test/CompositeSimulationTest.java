package biouml.plugins.simulation._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import java.util.logging.LogManager;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Composite models (preprocessing) testing
 */
public class CompositeSimulationTest extends AbstractBioUMLTest
{
    static final String repositoryPath = "../data";
    static final String compositeModelsName = "databases/CompositeModel_test";
    static final String out = "../out_test";

    static final double DELTA = 0.0000001;

    public CompositeSimulationTest(String name)
    {
        super(name);
        File configFile = new File( "./biouml/plugins/simulation/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(CompositeSimulationTest.class.getName());
        suite.addTest(new CompositeSimulationTest("testCompositeModel_1"));
        suite.addTest(new CompositeSimulationTest("testCompositeModel_2"));
        suite.addTest(new CompositeSimulationTest("testCompositeModel_3"));
        suite.addTest(new CompositeSimulationTest("testCompositeModel_4"));
//        suite.addTest(new CompositeSimulationTest("testCompositeModel_5"));

        return suite;
    }

    public void testCompositeModel_1() throws Exception
    {
        processDiagramTesting(getDiagram("1_Composite_Preprocessor_Test"));
    }

    public void testCompositeModel_2() throws Exception
    {
        processDiagramTesting(getDiagram("2_Composite_Preprocessor_Test"));
    }

    public void testCompositeModel_3() throws Exception
    {
        processDiagramTesting(getDiagram("3_Composite_Preprocessor_Test"));
    }

    public void testCompositeModel_4() throws Exception
    {
        processDiagramTesting(getDiagram("4_Composite_Preprocessor_Test"));
    }

    public void testCompositeModel_5() throws Exception
    {
        processDiagramTesting(getDiagram("5_Composite_Preprocessor_Test"));
    }

    /**
     * Get diagram for testing
     */
    protected Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection db = CollectionFactory.getDataCollection(compositeModelsName);
        assertNotNull("Can not find collection " + compositeModelsName, db);
        assertTrue("Terget collection is not database: " + db.getCompletePath(), ( db instanceof Module ));
        DataCollection<Diagram> diagrams = ( (Module)db ).getDiagrams();
        assertNotNull("Can not find Diagrams in database " + db.getCompletePath(), diagrams);
        Diagram diagram = diagrams.get(name);
        assertNotNull("Can not find diagram: " + name, diagram);
        return diagram;
    }

    /**
     * Process diagram testing
     */
    protected void processDiagramTesting(Diagram diagram) throws Exception
    {
        JavaSimulationEngine simulationEngine = new JavaSimulationEngine();
        File outDir = new File(out);
        try
        {
            outDir.mkdirs();
            simulationEngine.setOutputDir(outDir.getAbsolutePath());
            simulationEngine.setDiagram(diagram);
            simulationEngine.setJobControl(new FunctionJobControl(null));
            File[] files = simulationEngine.generateModel(true);
            assertNotNull("Generate model error: " + diagram.getName(), files);

            SimulationResult result = new SimulationResult(null, "tmp");
            simulationEngine.initSimulationResult(result);
            ResultWriter currentResults = new ResultWriter(result);

            String msg = simulationEngine.simulate(files, new ResultListener[] {currentResults});
            if( msg != null && msg.length() > 1 )
            {
                fail("Simulation error: " + diagram.getName() + ": " + msg);
            }

            SimulationResult templateResult = null;
            try
            {
                templateResult = (SimulationResult) ( (DataCollection) ( (DataCollection)Module.getModule(diagram).get(Module.SIMULATION) )
                        .get(Module.RESULT) ).get(diagram.getName());
            }
            catch( Exception e )
            {
                fail(e.getMessage());
            }
            assertNotNull("Can not find template result", templateResult);
            assertTrue("Results are different", resultsAreEquals(currentResults.getResults(), templateResult));
        }
        finally
        {
            ApplicationUtils.removeDir(outDir);
        }
    }

    /**
     * Compare simulation results
     */
    protected boolean resultsAreEquals(SimulationResult result, SimulationResult templateResult)
    {
        Set<String> variableSet = result.getVariableMap().keySet();
        String[] variableNames = variableSet.toArray(new String[variableSet.size()]);
        double[][] resultArray = result.getValues(variableNames);
        double[][] templateResultArray = templateResult.getValues(variableNames);

        if( resultArray.length > 0 && ( resultArray.length == templateResultArray.length ) )
        {
            for( int i = 0; i < resultArray.length; i++ )
            {
                double[] value = resultArray[i];
                double[] templateValue = templateResultArray[i];
                {
                    if( value.length != templateValue.length )
                    {
                        return false;
                    }
                    for( int j = 0; j < value.length; j++ )
                    {
                        if( Math.abs(value[j] - templateValue[j]) > DELTA )
                        {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
}
