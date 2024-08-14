package biouml.model._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.LogManager;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.InternalException;
import ru.biosoft.workbench.Framework;

public class TestDiagramToXML extends AbstractBioUMLTest
{
    /** Standard JUnit constructor */
    public TestDiagramToXML(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( "./biouml/standard/diagram/_test/log.lcf" );
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
        TestSuite suite = new TestSuite(TestDiagramToXML.class.getName());

        suite.addTest(new TestDiagramToXML("testNullDiagram"));
        suite.addTest(new TestDiagramToXML("testLoadModule"));
        suite.addTest(new TestDiagramToXML("testDiagram_Reaction"));

        return suite;
    }

    static Module module;
    public void testLoadModule() throws Exception
    {
        DataCollection<?> root = CollectionFactory.createRepository( "../data/test/biouml/model" );
        assertNotNull("Can not load repository", root);
        Framework.initRepository("../data/test/biouml/model");

        module = (Module)root;
        assertNotNull("Can not load module", module);
    }


    public void testNullDiagram() throws Exception
    {
        new File("../temp").mkdirs();
        File file = new File("../temp/testNullDiagram.xml");

        try (FileOutputStream fos = new FileOutputStream( file ))
        {
            new DiagramXmlWriter( fos ).write(null);
        }
        catch( InternalException e )
        {
            return; // ok, this exception should be thrown
        }
        finally
        {
            file.delete();
        }

        fail("InternalException should be thrown");
    }

    public void testDiagram(String diagramName) throws Exception
    {
        File fileOrig = new File("../data/test/biouml/model/Diagrams/" + diagramName + ".orig");
        Diagram diagram = DiagramXmlReader.readDiagram(fileOrig.getName(), new FileInputStream(fileOrig), null, null, module);

        File fileResult = new File("../data/test/biouml/model/Diagrams/" + diagramName + ".result");
        DiagramXmlWriter writer = new DiagramXmlWriter(new FileOutputStream(fileResult));
        writer.write(diagram);

        assertFileEquals(fileOrig, fileResult);

        diagram = DiagramXmlReader.readDiagram(fileResult.getName(), new FileInputStream(fileResult), null, null, module);

        assertNotNull("Valid diagram", diagram);
    }

    public void testDiagram_Reaction() throws Exception
    {
        testDiagram("reaction");
    }
}
