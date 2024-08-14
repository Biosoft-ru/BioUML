package biouml.plugins.bionetgen._test;

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import biouml.model.Diagram;

import com.developmentontheedge.application.ApplicationUtils;

public class BionetgenDiagramGeneratorTest extends TestCase
{
    public BionetgenDiagramGeneratorTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BionetgenDiagramGeneratorTest.class.getName());

        suite.addTest(new BionetgenDiagramGeneratorTest("testDiagram"));

        return suite;
    }

    protected static final String testDirectory = "../src/biouml/plugins/bionetgen/_test/test_suite/models/";
    protected static final DataElementPath COLLECTION_NAME = DataElementPath.create("databases/my_database/Diagrams");

    protected Diagram generateDiagram(String modelName, boolean needLayout) throws Exception
    {
        return BionetgenTestUtility.generateDiagram(testDirectory + modelName + ".bngl", modelName, needLayout);
    }

    @SuppressWarnings ( "all" )
    public void testDiagram() throws Exception
    {
        List<String> testList = ApplicationUtils.readAsList(new File(testDirectory + "testList"));
        BionetgenTestUtility.initPreferences();
        DataCollection collection = COLLECTION_NAME.getDataCollection();
        for( String test : testList )
        {
            Diagram diagram = generateDiagram(test, true);
            collection.put(diagram);
        }
    }
}
