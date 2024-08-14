package biouml.plugins.antimony._test;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.Type;
import biouml.plugins.sbml.SbmlConstants;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class AntimonyAnnotationImportTest extends AntimonyTest
{
    final static String FILE_PATH = "biouml/plugins/antimony/_test/example_7/antimony_ex7.txt";
    final static String FILE_PATH_INVALID_PROPERTY = "biouml/plugins/antimony/_test/example_7/antimony_re7_1.txt";
    final static String FILE_PATH_INVALID_VALUE = "biouml/plugins/antimony/_test/example_7/antimony_re7_2.txt";

    private static final DataElementPath PATH = DataElementPath.create("../data/test/biouml/plugins/antimony");

    public AntimonyAnnotationImportTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(AntimonyAnnotationImportTest.class.getName());
        suite.addTest(new AntimonyAnnotationImportTest("importAndUseAnnotation"));
        suite.addTest(new AntimonyAnnotationImportTest("applyInvalidValue"));
        suite.addTest(new AntimonyAnnotationImportTest("changeValueOfNotImportedProperty"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        DataCollection<?> repository = CollectionFactory.createRepository(PATH.toString());

        assertNotNull(repository);

        CollectionFactory.registerRoot(repository);
    }

    public void importAndUseAnnotation() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH));

        preprocess(antimonyText);

        Compartment node = (Compartment)antimonyDiagram.findNode("s1");
        assertNotNull("Failed to find entity", node);
        String value = ( (Specie)node.getKernel() ).getType();
        assertEquals(Type.TYPE_UNSPECIFIED, value);

        node = (Compartment)antimonyDiagram.findNode("s2");
        assertNotNull("Failed to find entity", node);
        Node[] nodes = node.getNodes();

        assertEquals(1, nodes.length);
        assertEquals("val", nodes[0].getName());

        node = (Compartment)antimonyDiagram.findNode("s3");
        assertNotNull("Failed to find entity", node);
        value = node.getAttributes().getProperty(SBGNPropertyConstants.SBGN_MULTIMER).getValue().toString();
        assertEquals("3", value);

        Node reactionNode = antimonyDiagram.findNode("r1");
        assertNotNull("Failed to find reaction node", reactionNode);
        value = reactionNode.getAttributes().getProperty(SBGNPropertyConstants.SBGN_REACTION_TYPE).getValue().toString();
        assertEquals(Type.TYPE_ASSOCIATION, value);

        Edge edge = (Edge)antimonyDiagram.findObject("r1__s3_as_modifier");
        assertNotNull("Failed to find edge", edge);
        SpecieReference sp = (SpecieReference)edge.getKernel();
        value = sp.getModifierAction();
        assertEquals(Type.TYPE_STIMULATION, value);
    }


    public void applyInvalidValue() throws Exception
    {
        String message = "message";
        try
        {
            String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_INVALID_VALUE));

            preprocess(antimonyText);
        }
        catch( Exception e )
        {
            message = e.getMessage();
        }

        assertEquals("Can't add @sbgn s1 in diagram: Property value \"something\" is invalid", message);
    }

    public void changeValueOfNotImportedProperty() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH));

        preprocess(antimonyText);

        Node node = antimonyDiagram.findNode("s1");
        assertNotNull("Failed to find entity", node);
        node.getAttributes().setValue(SbmlConstants.SBO_TERM_ATTR, "EMPTY");

        compareResult(FILE_PATH);
    }
}
