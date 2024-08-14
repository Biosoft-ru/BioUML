
package biouml.plugins.cellml._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model._test.ViewTestCase;
import biouml.plugins.cellml.CellMLDiagramInfo;
import biouml.plugins.cellml.Species;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.swing.PropertyInspector;


/** Batch unit test for biouml.model package. */
public class RdfTest extends ViewTestCase
{
    /** Standart JUnit constructor */
    public RdfTest(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( "./biouml/plugins/cellml/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(RdfTest.class.getName());

        suite.addTest( new RdfTest("testDiagramRdf") );
        suite.addTest( new RdfTest("testSpeciesRdf") );

        return suite;
    }

    public void testDiagramRdf() throws Exception
    {
        Diagram diagram = CellMLReaderTest.testReadModel("two_reaction_model");
        CellMLDiagramInfo info = (CellMLDiagramInfo)diagram.getKernel();

        assertNotNull("Diagram info is not initialised.", info);

        DynamicPropertySet rdf = info.getRdf();
        assertNotNull("Diagram info RDF is not initialised.", rdf);

        PropertyInspector pi = new PropertyInspector();
        pi.explore(info);

        assertView(pi, "DiagramInfo: " + diagram.getName());
    }

    public void testSpeciesRdf() throws Exception
    {
        Diagram diagram = CellMLReaderTest.testReadModel("goldbeter_model_1991");
        Node node = (Node)diagram.get("C");

        assertNotNull("Species not found.", node);

        DynamicPropertySet rdf = ((Species)node.getKernel()).getRdf();
        assertNotNull("Species 'C' RDF is not initialised.", rdf);

        PropertyInspector pi = new PropertyInspector();
        pi.explore(node);

        assertView(pi, "Node C");
    }
}

