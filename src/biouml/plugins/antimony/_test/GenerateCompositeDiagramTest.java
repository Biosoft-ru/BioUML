package biouml.plugins.antimony._test;

import java.io.File;
import junit.framework.TestSuite;

import com.developmentontheedge.application.ApplicationUtils;

public class GenerateCompositeDiagramTest extends AntimonyTest
{

    public GenerateCompositeDiagramTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(GenerateCompositeDiagramTest.class.getName());

        suite.addTest(new GenerateCompositeDiagramTest("generateCompositeDiagram"));
        return suite;
    }

    public void generateCompositeDiagram() throws Exception
    {
        String FILE_PATH = "biouml/plugins/antimony/_test/example_5/Models.txt";
        for( String model : ApplicationUtils.readAsList( new File( FILE_PATH ) ) )
        {
            antimonyDiagram = null;
            String antimonyText = ApplicationUtils.readAsString( new File( model ) );
            preprocess( antimonyText );
            assertFalse( antimonyDiagram == null );
        }
    }
}
