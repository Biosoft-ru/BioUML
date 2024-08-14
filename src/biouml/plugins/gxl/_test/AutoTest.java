package biouml.plugins.gxl._test;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataCollection;
import biouml.model.Diagram;
import biouml.plugins.gxl.GxlReader;
import biouml.plugins.gxl.GxlWriter;

public class AutoTest extends TestCase
{
    public AutoTest( String name )
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( AutoTest.class.getName() );
        
        suite.addTest(new AutoTest("testReadDiagram"));
        suite.addTest(new AutoTest("testWriteDiagram"));
        
        return suite;
    }
    
    static Diagram diagram;
    public void testReadDiagram() throws Exception
    {
        File fileOrig = new File("./biouml/plugins/gxl/_test/test.xml");
        diagram = (new GxlReader()).readDiagram(fileOrig, (DataCollection)null);
    }

    public void testWriteDiagram() throws Exception
    {
        File fileResult = new File("./biouml/plugins/gxl/_test/test_.xml");
        (new GxlWriter()).writeDiagram(fileResult, diagram);
        fileResult.deleteOnExit();
        fileResult.delete();
    }
    
}