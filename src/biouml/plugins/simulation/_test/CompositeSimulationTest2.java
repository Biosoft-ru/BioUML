package biouml.plugins.simulation._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.simulation.SimulationResult;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Composite models (preprocessing) testing
 */
public class CompositeSimulationTest2 extends AbstractBioUMLTest
{
    static final String repositoryPath = "../data";
    static final String compositeSBML = "databases/SBML composite";
    static final String simpleSBML = "databases/SBML modules";
    //    static final String composite = "databases/Composite"
    static final String out = "../out_test";

    static final double DELTA = 0.0000001;

    public CompositeSimulationTest2(String name)
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
        TestSuite suite = new TestSuite(CompositeSimulationTest2.class.getName());
        suite.addTest(new CompositeSimulationTest2("test"));
        return suite;
    }

    public void test() throws Exception
    {
//        processDiagramTesting(compositeSBML, "test", simpleSBML);
//        processDiagramTesting(compositeSBML, "Composite apoptosis model comparts", simpleSBML);
        processDiagramTesting(compositeSBML, "Composite apoptosis model part1", simpleSBML);
//        processDiagramTesting(compositeSBML, "Composite apoptosis model3", simpleSBML);
//        processDiagramTesting(compositeSBML, "Composite apoptosis model part4", simpleSBML);
//        processDiagramTesting(compositeSBML, "Composite apoptosis model part5", simpleSBML);
//        processDiagramTesting(compositeSBML, "NF-kB module", simpleSBML);
//        processDiagramTesting(compositeSBML, "p53 module", simpleSBML);
//        processDiagramTesting(compositeSBML, "EGF module", simpleSBML);
//        processDiagramTesting(compositeSBML, "Composite apoptosis model", simpleSBML);
    }



    public void processDiagramTesting(Diagram diagram) throws Exception
    {
        processDiagramTesting(diagram, diagram.getOrigin());
    }

    public void processDiagramTesting(String collectionPath, String name, String outputPath) throws Exception
    {
        processDiagramTesting(getDiagram(collectionPath, name), getDiagramsCollection(outputPath));
    }

    public void processDiagramTesting(Diagram diagram, DataCollection dc) throws Exception
    {
        CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
        Diagram result = preprocessor.preprocess(diagram);
        dc.put(result);
    }

    protected DataCollection getDiagramsCollection(String collectionPath) throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection db = CollectionFactory.getDataCollection(collectionPath);
        assertNotNull("Can not find collection " + collectionPath, db);
        assertTrue("Terget collection is not database: " + db.getCompletePath(), ( db instanceof Module ));
        DataCollection diagrams = ( (Module)db ).getDiagrams();
        assertNotNull("Can not find Diagrams in database " + db.getCompletePath(), diagrams);
        return diagrams;
    }

    /**
     * Get diagram for testing
     */
    protected Diagram getDiagram(String collectionPath, String name) throws Exception
    {
        DataCollection db = getDiagramsCollection(collectionPath);
        DataElement diagram = db.get(name);
        assertNotNull("Can not find diagram: " + name, diagram);
        return (Diagram)diagram;
    }

    //    /**
    //     * Get diagram for testing
    //     */
    //    protected Diagram getDiagram(String name) throws Exception
    //    {
    //        return getDiagram(compositeModelsName, name);
    //    }

    //    /**
    //     * Process diagram testing
    //     */
    //    protected void processDiagramTesting(Diagram diagram) throws Exception
    //    {
    //        JavaSimulationEngine simulationEngine = new JavaSimulationEngine();
    //        File outDir = new File(out);
    //        try
    //        {
    //            outDir.mkdirs();
    //            simulationEngine.setOutputDir(outDir.getAbsolutePath());
    //            simulationEngine.writeUtilityFiles(simulationEngine.getOutputDir(), true);
    //            simulationEngine.setDiagram(diagram);
    //            simulationEngine.setJobControl(new FunctionJobControl(null));
    //            File[] files = simulationEngine.generateModel();
    //            assertNotNull("Generate model error: " + diagram.getName(), files);
    //
    //            SimulationResult result = new SimulationResult(null, "tmp");
    //            simulationEngine.initSimulationResult(result);
    //            ResultWriter currentResults = new ResultWriter(result);
    //
    //            String msg = simulationEngine.simulate(files, new ResultListener[] {currentResults});
    //            if( msg != null && msg.length() > 1 )
    //            {
    //                fail("Simulation error: " + diagram.getName() + ": " + msg);
    //            }
    //
    //            SimulationResult templateResult = null;
    //            try
    //            {
    //                templateResult = (SimulationResult) ( (DataCollection) ( (DataCollection)Module.getModule(diagram).get(Module.SIMULATION) )
    //                        .get(Module.RESULT) ).get(diagram.getName());
    //            }
    //            catch( Exception e )
    //            {
    //                fail(e.getMessage());
    //            }
    //            assertNotNull("Can not find template result", templateResult);
    //            assertTrue("Results are different", resultsAreEquals(currentResults.getResults(), templateResult));
    //        }
    //        finally
    //        {
    //            ApplicationUtils.removeDir(outDir);
    //        }
    //    }

    /**
     * Compare simulation results
     */
    protected boolean resultsAreEquals(SimulationResult result, SimulationResult templateResult)
    {
        double[][] resultArray = result.getValues();
        double[][] templateResultArray = templateResult.getValues();
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
