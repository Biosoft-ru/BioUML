package biouml.model.xml._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.editor.ViewPane;

/**
 * Batch unit test for biouml.model package.
 */
public class TransformNotationDiagram extends TestCase
{
    private static final String repositoryPath = "../data";
    private static final String repositoryPath2 = "../data_resources";

    protected ViewPane viewPane;

    /** Standart JUnit constructor */
    public TransformNotationDiagram(String name)
    {
        super(name);
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(TransformNotationDiagram.class);
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TransformNotationDiagram.class.getName());
        suite.addTest(new TransformNotationDiagram("testDiagram"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testDiagram() throws Exception
    {
        DataCollection repository = CollectionFactory.createRepository(repositoryPath);
        DataCollection repository2 = CollectionFactory.createRepository(repositoryPath2);
        DataCollection diagrams = (DataCollection)CollectionFactory.getDataElement("databases/Biopath/Diagrams");
        
        assertNotNull( repository );
        assertNotNull( repository2 );
        assertNotNull( diagrams );

       // Diagram diagram = (Diagram)CollectionFactory.getDataElement("databases/test/Diagrams/DGR0343");
       // diagrams.put(diagram);
    }
}