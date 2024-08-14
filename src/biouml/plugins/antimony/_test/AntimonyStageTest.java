package biouml.plugins.antimony._test;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.antimony.Antimony;
import biouml.plugins.antimony.astparser_v2.AstStart;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class AntimonyStageTest extends AbstractBioUMLTest
{
    final static DataElementPath COLLECTION_NAME = DataElementPath.create("databases/FluxBalance/Diagrams");
    final static DataElementPath NEW_COLLECTION_NAME = DataElementPath.create("databases/FluxBalance/Diagrams");

    final static String DIAGRAM_NAME = "ffn";

    final static String FILE_PATH = "biouml/plugins/antimony/astparser/_test/antimony.txt";

    Diagram diagram;
    Antimony antimony;
    String antimonyText;
    AstStart astStart;

    public AntimonyStageTest(String name)
    {
        super(name);
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        try
        {
            String repositoryPath = "../data";
            CollectionFactory.createRepository(repositoryPath);
            diagram = COLLECTION_NAME.getChildPath(DIAGRAM_NAME).getDataElement(Diagram.class);
            antimony = new Antimony(diagram);
            antimony.createAst();

            File file = new File(FILE_PATH);
            antimonyText = ApplicationUtils.readAsString(file);
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        astStart = antimony.generateAstFromText(antimonyText);
        StringBuffer dump = new StringBuffer();
        astStart.dump(dump, "");
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(AntimonyStageTest.class.getName());
        suite.addTest(new AntimonyStageTest("antimonyToAst"));
        suite.addTest(new AntimonyStageTest("astToDiagram"));
        suite.addTest(new AntimonyStageTest("diagramToAst"));
        suite.addTest(new AntimonyStageTest("astToAntimony"));
        return suite;
    }

    public void antimonyToAst() throws Exception
    {
    }

    public void astToDiagram() throws Exception
    {
        assertNotNull(astStart);
        Diagram changedDiagram = antimony.generateDiagram(astStart, false);
        String repositoryPath = "../data";
        CollectionFactory.createRepository(repositoryPath);
        NEW_COLLECTION_NAME.getDataCollection().put(changedDiagram);
        assertNotNull(changedDiagram);
    }

    public void diagramToAst() throws Exception
    {
        assertNotNull(astStart);
        AstStart changedAstStart = antimony.generateAstFromDiagram(diagram);
        StringBuffer dump = new StringBuffer();
        assertNotNull(changedAstStart);
        changedAstStart.dump(dump, "");
    }

    public void astToAntimony() throws Exception
    {
        assertNotNull(astStart);
        String changedText = antimony.generateText(astStart);
        assertNotNull(changedText);
    }

}
