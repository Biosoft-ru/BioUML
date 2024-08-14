package biouml.plugins.antimony._test;

import java.io.File;
import java.util.Collection;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.AntimonyUtility;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.Type;
import biouml.standard.diagram.Util;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Specie;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import junit.framework.TestSuite;

public class CompositeAntimonyTest extends AntimonyTest
{
    final static String FILE_PATH_EX4 = "biouml/plugins/antimony/_test/example_4/antimony_ex4.txt";
    final static String FILE_PATH_RE_4_1 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_1.txt";
    final static String FILE_PATH_RE_4_2 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_2.txt";
    final static String FILE_PATH_RE_4_3 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_3.txt";
    final static String FILE_PATH_RE_4_4 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_4.txt";
    final static String FILE_PATH_RE_4_5 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_5.txt";
    final static String FILE_PATH_RE_4_6 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_6.txt";
    final static String FILE_PATH_RE_4_7 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_7.txt";
    final static String FILE_PATH_RE_4_8 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_8.txt";
    final static String FILE_PATH_RE_4_9 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_9.txt";
    final static String FILE_PATH_RE_4_10 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_10.txt";
    final static String FILE_PATH_RE_4_11 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_11.txt";
    final static String FILE_PATH_RE_4_12 = "biouml/plugins/antimony/_test/example_4/antimony_re_4_12.txt";

    public CompositeAntimonyTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(CompositeAntimonyTest.class.getName());
        suite.addTest(new CompositeAntimonyTest("addModelDefinition"));
        suite.addTest(new CompositeAntimonyTest("addSubDiagram"));
        suite.addTest(new CompositeAntimonyTest("addPublicPort"));
        suite.addTest(new CompositeAntimonyTest("changeModelDefinition"));
        suite.addTest(new CompositeAntimonyTest("addConnection"));
        suite.addTest(new CompositeAntimonyTest("addPortTitle"));
        suite.addTest(new CompositeAntimonyTest("changePortTitle"));
        suite.addTest(new CompositeAntimonyTest("addSpeciesInModelDefinition"));
        suite.addTest(new CompositeAntimonyTest("removeModelDefinition"));
        suite.addTest(new CompositeAntimonyTest("removeSubDiagram"));
        suite.addTest(new CompositeAntimonyTest("removePublicPort"));
        suite.addTest(new CompositeAntimonyTest("removeConnection"));
        suite.addTest(new CompositeAntimonyTest("removeSpeciesInSubDiagram"));
        suite.addTest(new CompositeAntimonyTest("removeSpeciesInModelDefinition"));
        suite.addTest(new CompositeAntimonyTest("changeFactorsOfSubDiagram"));
        suite.addTest(new CompositeAntimonyTest("removeFactorsOfSubDiagram"));

        return suite;
    }

    public void addModelDefinition() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX4));

        preprocess(antimonyText);

        //add model definition
        Diagram diagramModel = new Diagram(null, new DiagramInfo("example"), new SbgnDiagramType());
        diagramModel.setRole(new EModel(diagramModel));

        Compartment newSpecie = new Compartment(diagramModel, new Specie(diagramModel, "s1", Type.TYPE_MACROMOLECULE));
        newSpecie.setRole(new VariableRole(newSpecie));
        diagramModel.put(newSpecie);

        ModelDefinition modelDefinition = new ModelDefinition(antimonyDiagram, diagramModel, diagramModel.getName());
        modelDefinition.setParent(antimonyDiagram);
        antimonyDiagram.put(modelDefinition);

        compareResult(FILE_PATH_RE_4_1);
    }

    public void addSubDiagram() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_1));

        preprocess(antimonyText);

        //add subDiagram
        Node modelDefinition = antimonyDiagram.findNode("example");
        Node subDiagram = new SubDiagram(antimonyDiagram, ( (ModelDefinition)modelDefinition ).getDiagram(), "Sub");

        antimonyDiagram.put(subDiagram);

        compareResult(FILE_PATH_RE_4_2);
    }

    public void addPublicPort() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_1));

        preprocess(antimonyText);

        //add port in diagram
        Node modelDefinition = antimonyDiagram.findNode("example");
        Diagram diagramFromModelDefinition = ( (ModelDefinition)modelDefinition ).getDiagram();
        Node node = new Node(diagramFromModelDefinition, new Stub.InputConnectionPort(diagramFromModelDefinition, "default_port"));
        node.getAttributes().add(new DynamicProperty(ConnectionPort.VARIABLE_NAME_ATTR, String.class, "s1"));
        diagramFromModelDefinition.put(node);

        compareResult(FILE_PATH_RE_4_3);
    }

    public void addSpeciesInModelDefinition() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_1));

        preprocess(antimonyText);

        //add port in diagram
        Node modelDefinition = antimonyDiagram.findNode("example");
        Diagram diagramFromModelDefinition = ( (ModelDefinition)modelDefinition ).getDiagram();
        Compartment newSpecie = new Compartment(diagramFromModelDefinition,
                new Specie(diagramFromModelDefinition, "s2", Type.TYPE_MACROMOLECULE));
        diagramFromModelDefinition.put(newSpecie);

        compareResult(FILE_PATH_RE_4_4);
    }

    public void changeModelDefinition() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_1));

        preprocess(antimonyText);

        //add specie in diagram
        Node modelDefinition = antimonyDiagram.findNode("example");
        Diagram diagramFromModelDefinition = ( (ModelDefinition)modelDefinition ).getDiagram();

        Compartment newSpecie = new Compartment(diagramFromModelDefinition,
                new Specie(diagramFromModelDefinition, "s2", Type.TYPE_MACROMOLECULE));
        diagramFromModelDefinition.put(newSpecie);

        compareResult(FILE_PATH_RE_4_4);
    }

    public void addConnection() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_5));

        preprocess(antimonyText);

        //add private port in diagram
        Node node = new Node(antimonyDiagram, new Stub.ContactConnectionPort(antimonyDiagram, "s_port"));
        node.getAttributes().add(new DynamicProperty(ConnectionPort.VARIABLE_NAME_ATTR, String.class, "s"));
        node.getAttributes().add(new DynamicProperty(ConnectionPort.ACCESS_TYPE, String.class, ConnectionPort.PRIVATE));
        antimonyDiagram.put(node);

        //add connection
        Node portFromSubdiagram = ( (Compartment)antimonyDiagram.findNode("Sub") ).findNode("s1_port");
        assertNotNull(portFromSubdiagram);
        Edge newEdge = new Edge(antimonyDiagram, new Stub.UndirectedConnection(null, "con"), portFromSubdiagram, node);
        newEdge.setRole(new UndirectedConnection(newEdge));
        antimonyDiagram.put(newEdge);

        compareResult(FILE_PATH_RE_4_6);
    }

    public void addPortTitle() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_6));

        preprocess(antimonyText);

        Node portFromTopDiagram = Util.findPort(antimonyDiagram, "$s");
        assertNotNull(portFromTopDiagram);
        portFromTopDiagram.setTitle("title of s");
        Compartment subDiagram = (Compartment)antimonyDiagram.findNode("Sub");
        assertNotNull(subDiagram);
        for( Node portFromSubdiagram : AntimonyUtility.getPortNodes(subDiagram, false) )
            if( "$s1".equals(Util.getPortVariable(portFromSubdiagram)) )
                portFromSubdiagram.setTitle("title of Sub.s1");

        compareResult(FILE_PATH_RE_4_11);

    }

    public void changePortTitle() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_11));

        preprocess(antimonyText);
        Node portFromTopDiagram = Util.findPort(antimonyDiagram, "$s");
        assertNotNull(portFromTopDiagram);
        portFromTopDiagram.setTitle("new title of s");
        Compartment subDiagram = (Compartment)antimonyDiagram.findNode("Sub");
        assertNotNull(subDiagram);
        for( Node portFromSubdiagram : AntimonyUtility.getPortNodes(subDiagram, false) )
            if( "$s1".equals(Util.getPortVariable(portFromSubdiagram)) )
                portFromSubdiagram.setTitle("new title of Sub.s1");

        compareResult(FILE_PATH_RE_4_12);

    }

    public void removeModelDefinition() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_1));

        preprocess(antimonyText);
        antimonyDiagram.remove("example");

        compareResult(FILE_PATH_EX4);
    }

    public void removeSubDiagram() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_2));

        preprocess(antimonyText);
        antimonyDiagram.remove("Sub");

        compareResult(FILE_PATH_RE_4_1);
    }

    public void removePublicPort() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_3));

        preprocess(antimonyText);

        //remove port
        Node modelDefinition = antimonyDiagram.findNode("example");
        Diagram diagramFromModelDefinition = ( (ModelDefinition)modelDefinition ).getDiagram();
        diagramFromModelDefinition.remove("s1_port");

        compareResult(FILE_PATH_RE_4_1);
    }

    public void removeConnection() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_8));

        preprocess(antimonyText);

        //remove connections
        antimonyDiagram.remove("connection");
        antimonyDiagram.remove("connection_1");

        compareResult(FILE_PATH_RE_4_9);
    }

    public void removeSpeciesInModelDefinition() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_4));

        preprocess(antimonyText);

        //remove species
        Node modelDefinition = antimonyDiagram.findNode("example");
        Diagram diagramFromModelDefinition = ( (ModelDefinition)modelDefinition ).getDiagram();

        diagramFromModelDefinition.remove("s2");

        compareResult(FILE_PATH_RE_4_1);
    }

    public void removeSpeciesInSubDiagram() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_5));

        preprocess(antimonyText);

        //remove species
        SubDiagram subDiagram = (SubDiagram)antimonyDiagram.findNode("Sub");
        Diagram diagramFromSubDiagram = subDiagram.getDiagram();

        diagramFromSubDiagram.remove("s1");

        compareResult(FILE_PATH_RE_4_10);
    }

    public void changeFactorsOfSubDiagram() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_2));

        preprocess(antimonyText);
        Node subDiagram = antimonyDiagram.findNode("Sub");

        subDiagram.getAttributes().add(new DynamicProperty(Util.EXTENT_FACTOR, String.class, "k"));
        subDiagram.getAttributes().add(new DynamicProperty(Util.TIME_SCALE, String.class, "t"));

        compareResult(FILE_PATH_RE_4_7);
    }

    public void removeFactorsOfSubDiagram() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_4_7));

        preprocess(antimonyText);
        Node subDiagram = antimonyDiagram.findNode("Sub");

        subDiagram.getAttributes().setValue(Util.EXTENT_FACTOR, "");
        subDiagram.getAttributes().setValue(Util.TIME_SCALE, "");

        compareResult(FILE_PATH_RE_4_2);
    }

    private void addCompositeElementListeners(Diagram insideDiagram)
    {
        Collection<ModelDefinition> modelsList = AntimonyUtility.getModelDefinitions(insideDiagram);
        for( ModelDefinition model : modelsList )
        {
            Diagram diagram = model.getDiagram();
            diagram.addDataCollectionListener(editor);
            diagram.addPropertyChangeListener(editor);
            addCompositeElementListeners(diagram);
        }
        Collection<SubDiagram> subDiagramList = AntimonyUtility.getSubdiagrams(insideDiagram);
        for( SubDiagram subDiagram : subDiagramList )
        {
            Diagram diagram = subDiagram.getDiagram();
            diagram.addDataCollectionListener(editor);
            diagram.addPropertyChangeListener(editor);
            addCompositeElementListeners(diagram);
        }
    }

    @Override
    protected void preprocess(String antimonyText) throws Exception
    {
        super.preprocess(antimonyText);
        addCompositeElementListeners(antimonyDiagram);
    }
}
