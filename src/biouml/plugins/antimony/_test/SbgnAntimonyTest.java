package biouml.plugins.antimony._test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.AntimonyConstants;
import biouml.plugins.antimony.AntimonyUtility;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbgn.Type;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class SbgnAntimonyTest extends AntimonyTest
{
    public SbgnAntimonyTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(SbgnAntimonyTest.class.getName());
        suite.addTest(new SbgnAntimonyTest("changeByAddingElements"));
        suite.addTest(new SbgnAntimonyTest("changeByAttributes"));
        suite.addTest(new SbgnAntimonyTest("changeByRemovingElements"));
        suite.addTest(new SbgnAntimonyTest("changeByAttributesType"));
        suite.addTest(new SbgnAntimonyTest("complexesCreation"));
        suite.addTest(new SbgnAntimonyTest("changeComplexesByAddingNodes"));
        suite.addTest(new SbgnAntimonyTest("changeTitlesInComplexes"));
        suite.addTest(new SbgnAntimonyTest("changeComplexesByAttributes"));
        suite.addTest(new SbgnAntimonyTest("changeComplexesByRemoving"));
        suite.addTest(new SbgnAntimonyTest("changeByEdgeType"));
        suite.addTest(new SbgnAntimonyTest("changeByReactionType"));
        suite.addTest(new SbgnAntimonyTest("addLogicalOperator"));
        suite.addTest(new SbgnAntimonyTest("changeLogicalType"));
        suite.addTest(new SbgnAntimonyTest("removeLogicalOperator"));
        suite.addTest(new SbgnAntimonyTest("addPhenotype"));
        suite.addTest(new SbgnAntimonyTest("removePhenotype"));
        suite.addTest(new SbgnAntimonyTest("addEquivalence"));
        suite.addTest(new SbgnAntimonyTest("deleteSubtypeSpecie"));
        suite.addTest(new SbgnAntimonyTest("deleteSupertypeSpecie"));
        suite.addTest(new SbgnAntimonyTest("removeEquivalenceOperator"));
        suite.addTest(new SbgnAntimonyTest("addNote"));
        suite.addTest(new SbgnAntimonyTest("changeNoteText"));
        suite.addTest(new SbgnAntimonyTest("changeNoteEdges"));

        return suite;
    }

    final static String FILE_PATH_MODEL = "biouml/plugins/antimony/_test/example_6/Model_0.txt";

    private final static String FILE_PATH_MODEL_SBGN = "biouml/plugins/antimony/_test/example_6/Model_1.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_1 = "biouml/plugins/antimony/_test/example_6/Model_1_re.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_2 = "biouml/plugins/antimony/_test/example_6/Model_1_re2.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_3 = "biouml/plugins/antimony/_test/example_6/Model_1_re3.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_4 = "biouml/plugins/antimony/_test/example_6/Model_1_re4.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_5 = "biouml/plugins/antimony/_test/example_6/Model_1_re5.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_6 = "biouml/plugins/antimony/_test/example_6/Model_1_re6.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_7 = "biouml/plugins/antimony/_test/example_6/Model_1_re7.txt";
    private final static String FILE_PATH_MODEL_SBGN_CHANGE_8 = "biouml/plugins/antimony/_test/example_6/Model_1_re8.txt";
    private static final String FILE_PATH_MODEL_SBGN_CHANGE_9 = "biouml/plugins/antimony/_test/example_6/Model_1_re9.txt";
    private static final String FILE_PATH_MODEL_SBGN_CHANGE_10 = "biouml/plugins/antimony/_test/example_6/Model_1_re10.txt";
    private static final String FILE_PATH_MODEL_SBGN_CHANGE_11 = "biouml/plugins/antimony/_test/example_6/Model_1_re11.txt";
    private static final String FILE_PATH_MODEL_SBGN_CHANGE_12 = "biouml/plugins/antimony/_test/example_6/Model_1_re12.txt";
    private static final String FILE_PATH_MODEL_PHENOTYPE = "biouml/plugins/antimony/_test/example_6/Model_1_re13.txt";

    private final static String FILE_PATH_MODEL_COMPLEXES = "biouml/plugins/antimony/_test/example_6/Model_2.txt";
    private final static String FILE_PATH_MODEL_COMPLEXES_CHANGE_1 = "biouml/plugins/antimony/_test/example_6/Model_2_re.txt";
    private static final String FILE_PATH_MODEL_COMPLEXES_CHANGE_2 = "biouml/plugins/antimony/_test/example_6/Model_2_re2.txt";
    private static final String FILE_PATH_MODEL_COMPLEXES_CHANGE_3 = "biouml/plugins/antimony/_test/example_6/Model_2_re3.txt";
    private static final String FILE_PATH_MODEL_COMPLEXES_CHANGE_4 = "biouml/plugins/antimony/_test/example_6/Model_2_re4.txt";
    private static final String FILE_PATH_MODEL_COMPLEXES_CHANGE_5 = "biouml/plugins/antimony/_test/example_6/Model_2_re5.txt";

    private static final String FILE_PATH_MODEL_REACTION = "biouml/plugins/antimony/_test/example_6/Model_3.txt";
    private static final String FILE_PATH_MODEL_REACTION_1 = "biouml/plugins/antimony/_test/example_6/Model_3_re.txt";
    private static final String FILE_PATH_MODEL_REACTION_2 = "biouml/plugins/antimony/_test/example_6/Model_3_re2.txt";
    private static final String FILE_PATH_MODEL_REACTION_3 = "biouml/plugins/antimony/_test/example_6/Model_3_re3.txt";
    private static final String FILE_PATH_MODEL_REACTION_4 = "biouml/plugins/antimony/_test/example_6/Model_3_re4.txt";
    private static final String FILE_PATH_MODEL_REACTION_5 = "biouml/plugins/antimony/_test/example_6/Model_3_re5.txt";
    private static final String FILE_PATH_MODEL_REACTION_6 = "biouml/plugins/antimony/_test/example_6/Model_3_re6.txt";
    private static final String FILE_PATH_MODEL_REACTION_7 = "biouml/plugins/antimony/_test/example_6/Model_3_re7.txt";

    private static final DataElementPath PATH = DataElementPath.create("../data/test/biouml/plugins/antimony");
    /**
     * Test sbgn view title creating and changing when new diagram elements are added
     * @throws Exception
     */

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        DataCollection<?> repository = CollectionFactory.createRepository(PATH.toString());

        assertNotNull(repository);

        CollectionFactory.registerRoot(repository);
    }

    public void changeByAddingElements() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Compartment entity = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "c", Type.TYPE_MACROMOLECULE));
        entity.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, 5));
        antimonyDiagram.put(entity);
        Node uoi = new Node(entity, new Stub(null, "uoi", Type.TYPE_UNIT_OF_INFORMATION));
        entity.put(uoi);

        entity = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "a1", Type.TYPE_MACROMOLECULE));
        entity.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, 3));
        antimonyDiagram.put(entity);

        Node var = new Node(entity, new Stub(null, "v_1", Type.TYPE_VARIABLE));
        entity.put(var);

        entity = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "d", Type.TYPE_UNSPECIFIED));
        antimonyDiagram.put(entity);

        entity = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "g", Type.TYPE_NUCLEIC_ACID_FEATURE));
        antimonyDiagram.put(entity);

        compareResult(FILE_PATH_MODEL_SBGN);
    }

    public void addPhenotype() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_SBGN));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("c");
        assertNotNull("Failed to find entity", node);

        List<String> nodeList = new ArrayList<>();
        nodeList.add(node.getName());

        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        generator.createPhenotype(antimonyDiagram, "p_1", nodeList);

        compareResult(FILE_PATH_MODEL_PHENOTYPE);
    }

    public void removePhenotype() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_PHENOTYPE));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("p_1");
        assertNotNull("Failed to find entity", node);

        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_SBGN);
    }

    /**
     * Test sbgn view title changing when diagram elements' attributes are changed
     * @throws Exception
     */
    public void changeByAttributes() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_SBGN));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("c");
        assertNotNull("Failed to find entity", node);
        node.getAttributes().setValue(SBGNPropertyConstants.SBGN_MULTIMER, 0);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_1);

        node = antimonyDiagram.findNode("uoi");
        assertNotNull("Failed to find unit of info", node);
        node.setTitle("changed_uoi");

        node = antimonyDiagram.findNode("v_1");
        assertNotNull("Failed to find variable", node);
        node.setTitle("changed_var");

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_2);

        node = antimonyDiagram.findNode("c");
        assertNotNull("Failed to find entity", node);
        ( (Specie)node.getKernel() ).setType(Type.TYPE_UNSPECIFIED);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_3);

        node = antimonyDiagram.findNode("d");
        assertNotNull("Failed to find entity", node);
        ( (Specie)node.getKernel() ).setType(Type.TYPE_PERTURBING_AGENT);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_5);
    }

    /**
     * Test sbgn view title creating and changing when diagram elements are removed
     * @throws Exception
     */
    public void changeByRemovingElements() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_SBGN_CHANGE_2));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("changed_var");
        assertNotNull("Failed to find variable", node);
        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_4);

        node = antimonyDiagram.findNode("g");
        assertNotNull("Failed to find entity", node);
        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_6);

    }

    /**
     * Tests sbgn type changing (convert from gene to specie and back check)
     * @throws Exception
     */
    public void changeByAttributesType() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_SBGN_CHANGE_9));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("a1");
        assertNotNull("Failed to find entity", node);
        ( (Specie)node.getKernel() ).setType(Type.TYPE_NUCLEIC_ACID_FEATURE);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_7);

        node = antimonyDiagram.findNode("d");
        assertNotNull("Failed to find entity", node);
        ( (Specie)node.getKernel() ).setType(Type.TYPE_NUCLEIC_ACID_FEATURE);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_8);

        node = antimonyDiagram.findNode("a1");
        assertNotNull("Failed to find entity", node);
        ( (Specie)node.getKernel() ).setType(Type.TYPE_MACROMOLECULE);
        node = antimonyDiagram.findNode("d");
        assertNotNull("Failed to find entity", node);
        ( (Specie)node.getKernel() ).setType(Type.TYPE_UNSPECIFIED);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_9);
    }

    public void complexesCreation() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        // unnamed empty complex
        Compartment complex = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "A", Type.TYPE_COMPLEX));
        complex.setRole(new VariableRole(complex));
        antimonyDiagram.put(complex);

        //unnamed complex with one element
        complex = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "B", Type.TYPE_COMPLEX));
        complex.setShowTitle(false);
        complex.setRole(new VariableRole(complex));
        antimonyDiagram.put(complex);

        Compartment entity = new Compartment(complex, new Specie(null, "lonely_elem", Type.TYPE_MACROMOLECULE));
        complex.put(entity);
        entity.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, 3));

        // unnamed complex containing an element and other complex 
        complex = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "C", Type.TYPE_COMPLEX));
        complex.setShowTitle(false);
        complex.setRole(new VariableRole(complex));
        antimonyDiagram.put(complex);

        Compartment subcomplex = new Compartment(complex, new Specie(complex, "C_1", Type.TYPE_COMPLEX));
        subcomplex.setShowTitle(false);
        subcomplex.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, 10));
        entity = new Compartment(subcomplex, new Specie(null, "a", Type.TYPE_MACROMOLECULE));
        subcomplex.put(entity);
        entity = new Compartment(subcomplex, new Specie(null, "b", Type.TYPE_MACROMOLECULE));
        subcomplex.put(entity);

        complex.put(subcomplex);

        entity = new Compartment(complex, new Specie(null, "c", Type.TYPE_MACROMOLECULE));
        complex.put(entity);

        // named complex containing two elements 
        complex = new Compartment(antimonyDiagram, new Specie(antimonyDiagram, "D", Type.TYPE_COMPLEX));
        complex.setRole(new VariableRole(complex));
        antimonyDiagram.put(complex);

        entity = new Compartment(complex, new Specie(null, "d", Type.TYPE_MACROMOLECULE));
        complex.put(entity);
        entity = new Compartment(complex, new Specie(null, "e", Type.TYPE_MACROMOLECULE));
        complex.put(entity);

        compareResult(FILE_PATH_MODEL_COMPLEXES);
    }

    public void changeComplexesByAddingNodes() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_COMPLEXES));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Compartment complex = (Compartment)antimonyDiagram.findNode("A");
        assertNotNull("Failed to find complex", complex);
        Compartment subcomplex = new Compartment(complex, new Specie(complex, "A_1", Type.TYPE_COMPLEX));
        subcomplex.setShowTitle(false);
        subcomplex.setRole(new VariableRole(subcomplex));
        complex.put(subcomplex);
        Node var = new Node(subcomplex, new Stub(null, "inactive", Type.TYPE_VARIABLE));
        subcomplex.put(var);
        Compartment entity = new Compartment(subcomplex, new Specie(null, "first", Type.TYPE_SIMPLE_CHEMICAL));
        subcomplex.put(entity);

        compareResult(FILE_PATH_MODEL_COMPLEXES_CHANGE_1);

        entity = (Compartment)antimonyDiagram.findNode("lonely_elem");
        assertNotNull("Failed to find entity", entity);
        var = new Node(entity, new Stub(null, "mt:prot", Type.TYPE_UNIT_OF_INFORMATION));
        entity.put(var);

        complex = (Compartment)antimonyDiagram.findNode("C");
        entity = (Compartment)antimonyDiagram.findNode("b");
        assertNotNull("Failed to find entity", entity);
        var = new Node(entity, new Stub(null, "active", Type.TYPE_VARIABLE));
        entity.put(var);
        entity = new Compartment(complex, new Specie(null, "subunit", Type.TYPE_UNSPECIFIED));
        complex.put(entity);

        complex = (Compartment)antimonyDiagram.findNode("D");
        assertNotNull("Failed to find complex", complex);
        var = new Node(complex, new Stub(null, "ct:gene", Type.TYPE_UNIT_OF_INFORMATION));
        complex.put(var);

        compareResult(FILE_PATH_MODEL_COMPLEXES_CHANGE_2);
    }

    public void changeTitlesInComplexes() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_COMPLEXES_CHANGE_2));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = null;
        Compartment complex = (Compartment)antimonyDiagram.findNode("A");
        assertNotNull("Failed to find complex", complex);
        for( Node n : complex.getNodes() )
            if( SbgnUtil.isComplex(n) )
                node = n;
        assertNotNull("Failed to find complex", node);

        node.setShowTitle(true);
        node.setTitle("A_1");
        node = antimonyDiagram.findNode("inactive");
        assertNotNull("Failed to find variable", node);
        node.setTitle("changed variable");

        complex = (Compartment)antimonyDiagram.findNode("C");
        assertNotNull("Failed to find complex", complex);
        complex.setShowTitle(true);
        complex.setTitle("C_comp");

        complex = (Compartment)antimonyDiagram.findNode("D");
        assertNotNull("Failed to find complex", complex);
        complex.setShowTitle(false);

        compareResult(FILE_PATH_MODEL_COMPLEXES_CHANGE_3);
    }



    public void changeComplexesByAttributes() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_COMPLEXES_CHANGE_3));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Compartment entity = (Compartment)antimonyDiagram.findNode("first");
        assertNotNull("Failed to find entity", entity);
        ( (Specie)entity.getKernel() ).setType(Type.TYPE_NUCLEIC_ACID_FEATURE);

        Node node = antimonyDiagram.findNode("lonely_elem");
        assertNotNull("Failed to find entity", node);
        node.getAttributes().setValue(SBGNPropertyConstants.SBGN_MULTIMER, 0);

        node = null;
        Compartment complex = (Compartment)antimonyDiagram.findNode("C");
        assertNotNull("Failed to find complex", complex);
        for( Node n : complex.getNodes() )
            if( SbgnUtil.isComplex(n) )
                node = n;
        assertNotNull("Failed to find complex", node);

        node.getAttributes().setValue(SBGNPropertyConstants.SBGN_MULTIMER, 1);

        entity = (Compartment)antimonyDiagram.findNode("subunit");
        assertNotNull("Failed to find entity", node);
        ( (Specie)entity.getKernel() ).setType(Type.TYPE_MACROMOLECULE);

        node = antimonyDiagram.findNode("D");
        assertNotNull("Failed to find complex", node);
        node.getAttributes().setValue(SBGNPropertyConstants.SBGN_MULTIMER, 2);

        compareResult(FILE_PATH_MODEL_COMPLEXES_CHANGE_4);
    }

    public void changeComplexesByRemoving() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_COMPLEXES_CHANGE_4));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("first");
        assertNotNull("Failed to find entity", node);
        ( (Compartment)node.getParent() ).remove(node.getName());

        node = antimonyDiagram.findNode("changed variable");
        assertNotNull("Failed to find variable", node);
        ( (Compartment)node.getParent() ).remove(node.getName());

        node = antimonyDiagram.findNode("mt:prot");
        assertNotNull("Failed to find unit of info", node);
        ( (Compartment)node.getParent() ).remove(node.getName());

        node = antimonyDiagram.findNode("C");
        assertNotNull("Failed to find complex", node);
        ( (Compartment)node.getParent() ).remove(node.getName());

        node = antimonyDiagram.findNode("d");
        assertNotNull("Failed to find entity", node);
        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_COMPLEXES_CHANGE_5);

    }

    public void changeByEdgeType() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Edge edge = (Edge)antimonyDiagram.findDiagramElement("Test_reaction__C_as_modifier");
        assertNotNull("Failed to modifier edge", edge);
        SpecieReference sp = (SpecieReference)edge.getKernel();
        sp.setModifierAction(Type.TYPE_STIMULATION);

        compareResult(FILE_PATH_MODEL_REACTION_1);
    }

    public void changeByReactionType() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("Test_reaction");
        assertNotNull("Failed to reaction node", node);
        node.getAttributes().setValue(SBGNPropertyConstants.SBGN_REACTION_TYPE, Type.TYPE_ASSOCIATION);

        compareResult(FILE_PATH_MODEL_REACTION_2);
    }

    public void addLogicalOperator() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        List<String> nodeList = new ArrayList<>();

        Node node = antimonyDiagram.findNode("D");
        assertNotNull("Failed to find node", node);
        nodeList.add(node.getName());

        node = antimonyDiagram.findNode("E");
        assertNotNull("Failed to find node", node);
        nodeList.add(node.getName());

        generator.createLogicalOperator(antimonyDiagram, "Test_reaction_mod_4", "And", "Test_reaction", nodeList);

        compareResult(FILE_PATH_MODEL_REACTION_3);
    }

    public void changeLogicalType() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION_3));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("Test_reaction_mod_4_And");
        assertNotNull("Failed to find node", node);

        node.getAttributes().setValue(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR, "Or");

        compareResult(FILE_PATH_MODEL_REACTION_4);
    }

    public void removeLogicalOperator() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION_3));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("Test_reaction_mod_4_And");
        assertNotNull("Failed to find logical operator node", node);

        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_REACTION);
    }

    public void addEquivalence() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        List<String> nodeList = new ArrayList<>();

        Node node = antimonyDiagram.findNode("D");
        assertNotNull("Failed to find node", node);
        nodeList.add(node.getName());

        node = antimonyDiagram.findNode("E");
        assertNotNull("Failed to find node", node);
        nodeList.add(node.getName());

        node = antimonyDiagram.findNode("F");
        assertNotNull("Failed to find node", node);

        generator.createEquivalence(antimonyDiagram, "equivalence_operator_1", "F", nodeList);

        compareResult(FILE_PATH_MODEL_REACTION_5);
    }

    public void deleteSubtypeSpecie() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION_5));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("D");
        assertNotNull("Failed to find node", node);

        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_REACTION_6);
    }

    public void deleteSupertypeSpecie() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION_5));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("F");
        assertNotNull("Failed to find node", node);

        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_REACTION_7);
    }

    public void removeEquivalenceOperator() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_REACTION_5));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("F_equivalent");
        assertNotNull("Failed to find node", node);

        ( (Compartment)node.getParent() ).remove(node.getName());

        compareResult(FILE_PATH_MODEL_REACTION);
    }

    public void addNote() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        List<String> nodeList = new ArrayList<>();

        Node node = antimonyDiagram.findNode("cell");
        assertNotNull("Failed to find node", node);
        nodeList.add(node.getName());

        generator.createNote(antimonyDiagram, "note_1", nodeList);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_10);
    }

    public void changeNoteText() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_SBGN_CHANGE_10));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("note_1");
        assertNotNull("Failed to find node", node);
        node.setTitle("New Note Text!");

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_11);
    }

    public void changeNoteEdges() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_MODEL_SBGN_CHANGE_10));

        preprocess(antimonyText);
        AntimonyUtility.setAntimonyAttribute(antimonyDiagram, antimonyText, AntimonyConstants.ANTIMONY_TEXT_ATTR);

        Node node = antimonyDiagram.findNode("note_1");
        assertNotNull("Failed to find node", node);

        Edge edge = null;
        for( Edge e : node.getEdges() )
        {
            edge = e;
            antimonyDiagram.remove(e.getName());
        }

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_12);

        assertNotNull("Failed to find edge", edge);
        antimonyDiagram.put(edge);

        compareResult(FILE_PATH_MODEL_SBGN_CHANGE_10);
    }
}
