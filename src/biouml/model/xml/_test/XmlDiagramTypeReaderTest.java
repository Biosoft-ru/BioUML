package biouml.model.xml._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramViewBuilder;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class XmlDiagramTypeReaderTest extends TestCase
{
    static String repositoryPath = "../data_resources";

    static
    {
        File configFile = new File( "./biouml/model/xml/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public XmlDiagramTypeReaderTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(XmlDiagramTypeReaderTest.class.getName());

        suite.addTest(new XmlDiagramTypeReaderTest("testInitRepository"));
        suite.addTest(new XmlDiagramTypeReaderTest("testReadArterialtreeNotation"));
        suite.addTest(new XmlDiagramTypeReaderTest("testViewFunction"));

        return suite;
    }

    protected String toString(Object[] array)
    {
        if( array == null )
            return "null";

        return StreamEx.of( array ).joining( ", ", "[" + array.length + ": ", "]" );
    }

    public void testInitRepository() throws Exception
    {
        DataCollection repository = CollectionFactory.createRepository(repositoryPath);
        assertNotNull("Can not initialise repository", repository);
    }

    public void testReadArterialtreeNotation() throws Exception
    {
        DataCollection<XmlDiagramType> dc = XmlDiagramType.getTypesCollection();
        assertNotNull("Can not read parent", dc);
        System.out.println("names: " + dc.getNameList());

        XmlDiagramType xdt = dc.get("arterialTree.xml");
        assertNotNull("Can not read diagram type", xdt);

        Object[] nodeTypes = xdt.getNodeTypes();
        System.out.println("Node types: " + toString(nodeTypes));
        assertEquals("node types", "[3: heart, junction, control-point]", toString(nodeTypes));

        Object[] edgeTypes = xdt.getEdgeTypes();
        System.out.println("Edge types: " + toString(edgeTypes));
        assertEquals("edge types", "[1: vessel]", toString(edgeTypes));

        XmlDiagramViewBuilder dvb = xdt.getXmlDiagramViewBuilder();
        System.out.println("Diagram view builder: " + dvb);
    }

    public void testViewFunction() throws Exception
    {


    }
}
