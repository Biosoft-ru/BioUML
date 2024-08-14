package biouml.plugins.antimony._test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.UndirectedConnection;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.type.Stub;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class AnnotationAntimonyTest extends AntimonyTest
{
    public AnnotationAntimonyTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(AnnotationAntimonyTest.class.getName());
        suite.addTest(new AnnotationAntimonyTest("addBus"));
        suite.addTest(new AnnotationAntimonyTest("addConnectionBetweenBusAndPort"));
        suite.addTest(new AnnotationAntimonyTest("removeConnection"));
        suite.addTest(new AnnotationAntimonyTest("changeTitle"));
        suite.addTest(new AnnotationAntimonyTest("removeBus"));
        suite.addTest(new AnnotationAntimonyTest("addDirectedBus"));
        suite.addTest(new AnnotationAntimonyTest("addConnectionBetweenDirectedBusAndPort"));
        suite.addTest(new AnnotationAntimonyTest("addTable"));
        suite.addTest(new AnnotationAntimonyTest("editArgColumn"));
        suite.addTest(new AnnotationAntimonyTest("removeTable"));
        suite.addTest(new AnnotationAntimonyTest("editColumns"));
        suite.addTest(new AnnotationAntimonyTest("removeColumn"));

        return suite;
    }

    private final static String FILE_PATH_MODEL = "biouml/plugins/antimony/_test/example_8/Model_0.txt";
    private final static String FILE_PATH_INPUT_OUTPUT_PORTS = "biouml/plugins/antimony/_test/example_8/Model_1.txt";

    private final static String FILE_PATH_MODEL_CHANGE_1 = "biouml/plugins/antimony/_test/example_8/Model_0_re1.txt";
    private final static String FILE_PATH_MODEL_CHANGE_2 = "biouml/plugins/antimony/_test/example_8/Model_0_re2.txt";
    private final static String FILE_PATH_MODEL_CHANGE_3 = "biouml/plugins/antimony/_test/example_8/Model_0_re3.txt";

    private final static String FILE_PATH_INPUT_OUTPUT_PORTS_CHANGE_1 = "biouml/plugins/antimony/_test/example_8/Model_1_re1.txt";
    private final static String FILE_PATH_INPUT_OUTPUT_PORTS_CHANGE_2 = "biouml/plugins/antimony/_test/example_8/Model_1_re2.txt";

    private final static String TABLE_MODEL_BASELINE = "biouml/plugins/antimony/_test/example_8/Model_2.txt";
    private final static String TABLE_MODEL_CHANGE_1 = "biouml/plugins/antimony/_test/example_8/Model_2_re1.txt";
    private final static String TABLE_MODEL_CHANGE_2 = "biouml/plugins/antimony/_test/example_8/Model_2_re2.txt";
    private final static String TABLE_MODEL_CHANGE_3 = "biouml/plugins/antimony/_test/example_8/Model_2_re3.txt";
    private final static String TABLE_MODEL_CHANGE_4 = "biouml/plugins/antimony/_test/example_8/Model_2_re4.txt";
    private final static String TABLE_MODEL_CHANGE_5 = "biouml/plugins/antimony/_test/example_8/Model_2_re5.txt";
    private final static String TABLE_MODEL_CHANGE_6 = "biouml/plugins/antimony/_test/example_8/Model_2_re6.txt";

    private final static String FILE_PATH_TABLE = "tables/test_table";

    private static final DataElementPath PATH = DataElementPath.create("../data/test/biouml/plugins/antimony/tables");


    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        DataCollection<?> repository = CollectionFactory.createRepository(PATH.toString());

        assertNotNull(repository);

        CollectionFactory.registerRoot(repository);
    }

    public void addBus() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL));

        preprocess(antimonyText);

        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        generator.createBus(antimonyDiagram, "BUS_1", false);

        compareResult(FILE_PATH_MODEL_CHANGE_1);
    }

    public void addConnectionBetweenBusAndPort() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_CHANGE_1));

        preprocess(antimonyText);

        Node busNode = (Node)antimonyDiagram.findNode("BUS_1");
        assertNotNull(busNode);
        Node portFromSubdiagram = ( (Compartment)antimonyDiagram.findNode("Module_1") ).findNode("s1_port");
        assertNotNull(portFromSubdiagram);

        Edge newEdge = new Edge(antimonyDiagram, new Stub.UndirectedConnection(null, "con"), portFromSubdiagram, busNode);
        newEdge.setRole(new UndirectedConnection(newEdge));
        antimonyDiagram.put(newEdge);

        compareResult(FILE_PATH_MODEL_CHANGE_2);
    }

    public void removeConnection() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_CHANGE_2));

        preprocess(antimonyText);

        antimonyDiagram.remove("connection");

        compareResult(FILE_PATH_MODEL_CHANGE_1);
    }


    public void changeTitle() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_CHANGE_2));

        preprocess(antimonyText);

        Node busNode = (Node)antimonyDiagram.findNode("BUS_1");
        assertNotNull(busNode);
        busNode.setTitle("S1");

        compareResult(FILE_PATH_MODEL_CHANGE_3);
    }


    public void removeBus() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_CHANGE_1));

        preprocess(antimonyText);

        antimonyDiagram.remove("BUS_1");

        compareResult(FILE_PATH_MODEL);
    }


    public void addDirectedBus() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_INPUT_OUTPUT_PORTS));

        preprocess(antimonyText);

        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        generator.createBus(antimonyDiagram, "BUS_1", true);

        compareResult(FILE_PATH_INPUT_OUTPUT_PORTS_CHANGE_1);
    }


    public void addConnectionBetweenDirectedBusAndPort() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_INPUT_OUTPUT_PORTS_CHANGE_1));

        preprocess(antimonyText);

        Node busNode = (Node)antimonyDiagram.findNode("BUS_1");
        assertNotNull(busNode);

        Node portFromSubdiagram = ( (Compartment)antimonyDiagram.findNode("Module_1") ).findNode("s1_port");
        assertNotNull(portFromSubdiagram);

        Edge newEdge = new Edge(antimonyDiagram, new Stub.DirectedConnection(null, "con"), portFromSubdiagram, busNode);
        newEdge.setRole(new DirectedConnection(newEdge));
        antimonyDiagram.put(newEdge);


        portFromSubdiagram = ( (Compartment)antimonyDiagram.findNode("Module_2") ).findNode("s1_port");
        assertNotNull(portFromSubdiagram);

        newEdge = new Edge(antimonyDiagram, new Stub.DirectedConnection(null, "con"), busNode, portFromSubdiagram);
        newEdge.setRole(new DirectedConnection(newEdge));
        antimonyDiagram.put(newEdge);

        compareResult(FILE_PATH_INPUT_OUTPUT_PORTS_CHANGE_2);
    }

    public void addTable() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(TABLE_MODEL_BASELINE));

        preprocess(antimonyText);

        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        generator.createTable(antimonyDiagram, DataElementPath.create(FILE_PATH_TABLE), "table_1");

        compareResult(TABLE_MODEL_CHANGE_1);
    }

    public void editArgColumn() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(TABLE_MODEL_CHANGE_1));

        preprocess(antimonyText);

        Node tableNode = (Node)antimonyDiagram.findNode("table_1");
        assertNotNull(tableNode);
        VarColumn argColumn = ( (SimpleTableElement)tableNode.getRole() ).getArgColumn();

        //add variable
        argColumn.setVariable("X");
        compareResult(TABLE_MODEL_CHANGE_2);
        //add column
        argColumn.setColumn("X_coordinate");
        compareResult(TABLE_MODEL_CHANGE_3);
    }


    public void editColumns() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(TABLE_MODEL_CHANGE_1));

        preprocess(antimonyText);

        Node tableNode = (Node)antimonyDiagram.findNode("table_1");
        assertNotNull(tableNode);

        List<VarColumn> cols = new ArrayList<VarColumn>();
        VarColumn col1 = new VarColumn();
        VarColumn col2 = new VarColumn();
        cols.add(col1);
        cols.add(col2);

        ( (SimpleTableElement)tableNode.getRole() ).setColumns(cols.toArray(new VarColumn[0]));

        col1.setColumn("Y_coordinate");
        col1.setVariable("Y");
        col2.setColumn("Z_coordinate");
        col2.setVariable("Z");

        compareResult(TABLE_MODEL_CHANGE_4);
    }

    public void removeColumn() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(TABLE_MODEL_CHANGE_4));

        preprocess(antimonyText);

        Node tableNode = (Node)antimonyDiagram.findNode("table_1");
        assertNotNull(tableNode);

        VarColumn[] columns = ( (SimpleTableElement)tableNode.getRole() ).getColumns();
        ( (SimpleTableElement)tableNode.getRole() ).setColumns(Arrays.copyOfRange(columns, 0, 1));

        compareResult(TABLE_MODEL_CHANGE_5);
    }

    public void removeTable() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(TABLE_MODEL_CHANGE_1));

        preprocess(antimonyText);

        antimonyDiagram.remove("table_1");

        compareResult(TABLE_MODEL_BASELINE);
    }


}
