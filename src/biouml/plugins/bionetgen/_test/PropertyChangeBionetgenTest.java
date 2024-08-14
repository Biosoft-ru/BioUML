package biouml.plugins.bionetgen._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import biouml.model.Compartment;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class PropertyChangeBionetgenTest extends AbstractBionetgenTest
{
    protected static final String FILE_PATH_EX_3 = dir + "test_examples_3/bionetgen_ex_3.bngl";

    protected static final String FILE_PATH_RES_3_1 = dir + "test_examples_3/bionetgen_res_3_1.bngl";
    protected static final String FILE_PATH_RES_3_2 = dir + "test_examples_3/bionetgen_res_3_2.bngl";
    protected static final String FILE_PATH_RES_3_3 = dir + "test_examples_3/bionetgen_res_3_3.bngl";
    protected static final String FILE_PATH_RES_3_4 = dir + "test_examples_3/bionetgen_res_3_4.bngl";
    protected static final String FILE_PATH_RES_3_5 = dir + "test_examples_3/bionetgen_res_3_5.bngl";
    protected static final String FILE_PATH_RES_3_6 = dir + "test_examples_3/bionetgen_res_3_6.bngl";
    protected static final String FILE_PATH_RES_3_7 = dir + "test_examples_3/bionetgen_res_3_7.bngl";
    protected static final String FILE_PATH_RES_3_8 = dir + "test_examples_3/bionetgen_res_3_8.bngl";
    protected static final String FILE_PATH_RES_3_9 = dir + "test_examples_3/bionetgen_res_3_9.bngl";
    protected static final String FILE_PATH_RES_3_10 = dir + "test_examples_3/bionetgen_res_3_10.bngl";
    protected static final String FILE_PATH_RES_3_11 = dir + "test_examples_3/bionetgen_res_3_11.bngl";
    protected static final String FILE_PATH_RES_3_12 = dir + "test_examples_3/bionetgen_res_3_12.bngl";
    protected static final String FILE_PATH_RES_3_13 = dir + "test_examples_3/bionetgen_res_3_13.bngl";
    protected static final String FILE_PATH_RES_3_14 = dir + "test_examples_3/bionetgen_res_3_14.bngl";
    protected static final String FILE_PATH_RES_3_15 = dir + "test_examples_3/bionetgen_res_3_15.bngl";
    protected static final String FILE_PATH_RES_3_16 = dir + "test_examples_3/bionetgen_res_3_16.bngl";
    protected static final String FILE_PATH_RES_3_17 = dir + "test_examples_3/bionetgen_res_3_17.bngl";
    protected static final String FILE_PATH_RES_3_18 = dir + "test_examples_3/bionetgen_res_3_18.bngl";
    protected static final String FILE_PATH_RES_3_19 = dir + "test_examples_3/bionetgen_res_3_19.bngl";
    protected static final String FILE_PATH_RES_3_20 = dir + "test_examples_3/bionetgen_res_3_20.bngl";
    protected static final String FILE_PATH_RES_3_21 = dir + "test_examples_3/bionetgen_res_3_21.bngl";
    protected static final String FILE_PATH_RES_3_22 = dir + "test_examples_3/bionetgen_res_3_22.bngl";
    protected static final String FILE_PATH_RES_3_23 = dir + "test_examples_3/bionetgen_res_3_23.bngl";
    protected static final String FILE_PATH_RES_3_24 = dir + "test_examples_3/bionetgen_res_3_24.bngl";
    protected static final String FILE_PATH_RES_3_25 = dir + "test_examples_3/bionetgen_res_3_25.bngl";
    protected static final String FILE_PATH_RES_3_26 = dir + "test_examples_3/bionetgen_res_3_26.bngl";
    protected static final String FILE_PATH_RES_3_27 = dir + "test_examples_3/bionetgen_res_3_27.bngl";
    protected static final String FILE_PATH_RES_3_28 = dir + "test_examples_3/bionetgen_res_3_28.bngl";
    protected static final String FILE_PATH_RES_3_29 = dir + "test_examples_3/bionetgen_res_3_29.bngl";
    protected static final String FILE_PATH_RES_3_30 = dir + "test_examples_3/bionetgen_res_3_30.bngl";

    public PropertyChangeBionetgenTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(PropertyChangeBionetgenTest.class.getName());

        suite.addTest(new PropertyChangeBionetgenTest("testChangeInitialValue"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeFormula"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeComment1"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeComment2"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeComment3"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeComment4"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeAttributeGraph"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeAttributeMatchOnce"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeContentAttribute1"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeContentAttribute2"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeAdditionAttribute1"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeAdditionAttribute2"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeReversible"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeForwardRate"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeBackwardRate"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeRateLawType"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeAttributeMolecule"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeMoleculeComponent"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeObservableName"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeTypeAttribute"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeEquationVariableName"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeSeedSpeciesConstancy"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeMoleculeType"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeLabel1"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeLabel2"));
        suite.addTest(new PropertyChangeBionetgenTest("testChangeVariableName"));

        return suite;
    }

    //----------------------------------------------------------------
    public void testChangeInitialValue() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Variable var = diagram.getRole(EModel.class).getVariable("k1");
        var.setInitialValue(18.3);

        compareResult(FILE_PATH_RES_3_1, 1);

        var = diagram.findNode("B(a)").getRole(VariableRole.class);
        var.setInitialValue(25.0);

        compareResult(FILE_PATH_RES_3_5, 2);
    }

    public void testChangeFormula() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node eqNode = diagram.findNode(BionetgenConstants.EQUATION_NAME);
        Equation eq = eqNode.getRole(Equation.class);
        eq.setFormula("5*k1/(7-4)");

        compareResult(FILE_PATH_RES_3_2, 1);

        eqNode = diagram.findNode(BionetgenConstants.EQUATION_NAME + "_2");
        eq = eqNode.getRole(Equation.class);
        eq.setFormula("arccos(cos(pi*floor(ln(abs(min(-10,-exp(5))))))) + -arctanh(tanh(25))/-(log(10000)+1)^2");

        compareResult(FILE_PATH_RES_3_29, 2);
    }

    public void testChangeTypeAttribute() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("B(a)");
        DynamicPropertySet dps = node.getAttributes();

        dps.setValue( BionetgenConstants.IS_SEED_SPECIES_ATTR, false );
        compareResult(FILE_PATH_RES_3_20, 1);

        dps.setValue( BionetgenConstants.IS_SEED_SPECIES_ATTR, true );
        compareResult(FILE_PATH_RES_3_21, 2);
    }

    //removes all comments
    public void testChangeComment1() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Variable var = diagram.getRole(EModel.class).getVariable("k2");
        var.setComment("");
        Node node = diagram.findNode("A(b)");
        node.setComment("");
        node = diagram.findNode("observable_1");
        node.setComment("");
        node = diagram.findNode("j01");
        node.setComment("");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME + "_1");
        node.setComment("");

        compareResult(FILE_PATH_RES_3_3);
    }

    //adds comments back
    public void testChangeComment2() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_3));

        Variable var = diagram.getRole(EModel.class).getVariable("k2");
        var.setComment("comment param");
        Node node = diagram.findNode("A(b)");
        node.setComment("# comment seed species");
        node = diagram.findNode("observable_1");
        node.setComment("# comment observable");
        node = diagram.findNode("j01");
        node.setComment("# comment reaction");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME + "_1");
        node.setComment("# comment molecule type");

        compareResult(FILE_PATH_EX_3);
    }

    //changes text for all comments
    public void testChangeComment3() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        diagram.setComment("diagram description comment\nand its extension");
        Variable var = diagram.getRole(EModel.class).getVariable("k2");
        var.setComment("changed param comment");
        Node node = diagram.findNode("A(b)");
        node.setComment("# changed seed species comment");
        node = diagram.findNode("observable_1");
        node.setComment("and observable comment");
        node = diagram.findNode("j01");
        node.setComment("# reaction comment changed to");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME + "_1");
        node.setComment("# and molecule type comment");

        compareResult(FILE_PATH_RES_3_4);
    }

    //changes comments and removes them
    public void testChangeComment4() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        diagram.setComment("diagram description comment\r\nand its extension");
        Variable var = diagram.getRole(EModel.class).getVariable("k2");
        var.setComment("changed param comment");
        Node node = diagram.findNode("A(b)");
        node.setComment("# changed seed species comment");
        node = diagram.findNode("observable_1");
        node.setComment("and observable comment");
        node = diagram.findNode("j01");
        node.setComment("# reaction comment changed to");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME + "_1");
        node.setComment("# and molecule type comment");

        compareResult(FILE_PATH_RES_3_4, 1);

        diagram.setComment("");
        var.setComment("");
        node = diagram.findNode("A(b)");
        node.setComment("");
        node = diagram.findNode("observable_1");
        node.setComment("");
        node = diagram.findNode("j01");
        node.setComment("");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME + "_1");
        node.setComment("");

        compareResult(FILE_PATH_RES_3_3, 2);
    }

    public void testChangeAttributeGraph() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("B(a)");
        DynamicPropertySet dps = node.getAttributes();
        dps.setValue(BionetgenConstants.GRAPH_ATTR, "B(a).D(d~0)");

        compareResult(FILE_PATH_RES_3_6);
    }

    public void testChangeAttributeMatchOnce() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("observable_1");
        DynamicPropertySet dps = node.getAttributes();
        dps.setValue(BionetgenConstants.MATCH_ONCE_ATTR, !Boolean.valueOf(dps.getValueAsString(BionetgenConstants.MATCH_ONCE_ATTR)));

        compareResult(FILE_PATH_RES_3_7);
    }

    public void testChangeContentAttribute1() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("observable_1");
        DynamicPropertySet dps = node.getAttributes();
        dps.setValue(BionetgenConstants.CONTENT_ATTR, content);

        compareResult(FILE_PATH_RES_3_8);
    }

    public void testChangeContentAttribute2() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("observable_1");
        DynamicPropertySet dps = node.getAttributes();
        dps.setValue(BionetgenConstants.CONTENT_ATTR, new String[0]);

        compareResult(FILE_PATH_RES_3_9, 1);

        dps.setValue(BionetgenConstants.CONTENT_ATTR, content);
        compareResult(FILE_PATH_RES_3_8, 2);
    }

    //remove addition components and add new components
    public void testChangeAdditionAttribute1() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("j01");
        DynamicPropertySet dps = node.getAttributes();
        dps.setValue(BionetgenConstants.ADDITION_ATTR, new String[0]);

        compareResult(FILE_PATH_RES_3_10, 1);

        dps.setValue(BionetgenConstants.ADDITION_ATTR, addition);
        compareResult(FILE_PATH_RES_3_11, 2);
    }

    //add addition to reaction
    public void testChangeAdditionAttribute2() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_10));

        Node node = diagram.findNode("j01");
        DynamicProperty dp = new DynamicProperty(BionetgenConstants.ADDITION_ATTR, String[].class, new String[0]);
        DynamicPropertySet dps = node.getAttributes();
        dps.add(dp);
        dps.setValue(BionetgenConstants.ADDITION_ATTR, addition);

        compareResult(FILE_PATH_RES_3_12);
    }

    public void testChangeReversible() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("j01");
        DynamicPropertySet dps = node.getAttributes();
        DynamicProperty dp = new DynamicProperty(BionetgenConstants.REVERSIBLE_ATTR, Boolean.class, false);
        dps.add(dp);
        dps.setValue(BionetgenConstants.REVERSIBLE_ATTR, true);

        compareResult(FILE_PATH_RES_3_13);
    }

    public void testChangeForwardRate() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_13));

        Node node = diagram.findNode("j01");
        DynamicPropertySet dps = node.getAttributes();

        dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, "k3+1");
        compareResult(FILE_PATH_RES_3_14, 1);

        dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, "MM(k3+1,k3)");
        compareResult(FILE_PATH_RES_3_16, 2);

        dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, "Sat(k3+1,k3)");
        compareResult(FILE_PATH_RES_3_17, 3);

        dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, "k3+1");
        compareResult(FILE_PATH_RES_3_14, 4);

        dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, "Sat(k3+1,k3)");
        compareResult(FILE_PATH_RES_3_17, 5);

        dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, "MM(k3+1,k3)");
        compareResult(FILE_PATH_RES_3_16, 6);

        dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, "k3+1");
        compareResult(FILE_PATH_RES_3_14, 7);
    }

    public void testChangeBackwardRate() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_13));

        Node node = diagram.findNode("j01");
        DynamicPropertySet dps = node.getAttributes();
        dps.setValue(BionetgenConstants.BACKWARD_RATE_ATTR, "k3-1");

        compareResult(FILE_PATH_RES_3_15);
    }

    public void testChangeRateLawType() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_14));

        Node node = diagram.findNode("j01");
        DynamicPropertySet dps = node.getAttributes();

        dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.MM);
        compareResult(FILE_PATH_RES_3_16, 1);

        dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.DEFAULT);
        compareResult(FILE_PATH_RES_3_14, 2);

        dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.MM);
        compareResult(FILE_PATH_RES_3_16, 3);

        dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.SATURATION);
        compareResult(FILE_PATH_RES_3_17, 4);

        dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.DEFAULT);
        compareResult(FILE_PATH_RES_3_14, 5);

        dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.SATURATION);
        compareResult(FILE_PATH_RES_3_17, 6);

        dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.MM);
        compareResult(FILE_PATH_RES_3_16, 7);
    }

    public void testChangeAttributeMolecule() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Compartment molecule = (Compartment) ( (Compartment)diagram.findNode("B(a)") ).findNode("B");
        DynamicPropertySet dps = molecule.getAttributes();
        dps.setValue(BionetgenConstants.MOLECULE_ATTR, "B1(a,b~P)");

        compareResult(FILE_PATH_RES_3_18);
    }

    public void testChangeMoleculeComponent() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_18));

        Node moleculeComponent = ( (Compartment) ( (Compartment)diagram.findNode("B1(a,b~P)") ).findNode("B1") ).findNode("b");
        moleculeComponent.setTitle("b~uP!+");

        compareResult(FILE_PATH_RES_3_19);
    }

    public void testChangeObservableName() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node observable = diagram.findNode("observable_1");
        observable.setTitle("");
        compareResult(FILE_PATH_EX_3, 1);

        observable.setTitle("B_total");
        compareResult(FILE_PATH_RES_3_30, 2);
    }

    public void testChangeEquationVariableName() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node eqNode = diagram.findNode(BionetgenConstants.EQUATION_NAME);
        Equation eq = eqNode.getRole(Equation.class);
        eq.setVariable("k8");

        compareResult(FILE_PATH_RES_3_23);
    }

    public void testChangeSeedSpeciesConstancy() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        VariableRole role = diagram.findNode("B(a)").getRole(VariableRole.class);
        role.setConstant(true);
        compareResult(FILE_PATH_RES_3_22, 1);

        role.setConstant(false);
        compareResult(FILE_PATH_EX_3, 2);

        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_22));

        role = diagram.findNode("B(a)").getRole(VariableRole.class);
        role.setConstant(false);
        compareResult(FILE_PATH_EX_3, 3);

        role.setConstant(true);
        compareResult(FILE_PATH_RES_3_22, 4);
    }

    public void testChangeMoleculeType() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME + "_3");
        node.getAttributes().setValue(BionetgenConstants.MOLECULE_TYPE_ATTR, "D(d)");

        compareResult(FILE_PATH_RES_3_24);
    }

    public void testChangeLabel1() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Node node = diagram.findNode("A(b)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("B(a)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("observable");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("observable_1");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("j01");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME);
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "MT1");

        compareResult(FILE_PATH_RES_3_25, 1);

        node = diagram.findNode("A(b)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "S1");
        node = diagram.findNode("B(a)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "S2");
        node = diagram.findNode("observable");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "O1");
        node = diagram.findNode("observable_1");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "O2");
        node = diagram.findNode("j01");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "R1");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME);
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");

        compareResult(FILE_PATH_EX_3, 2);
    }

    public void testChangeLabel2() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_RES_3_25));

        Node node = diagram.findNode("A(b)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "S1");
        node = diagram.findNode("B(a)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "S2");
        node = diagram.findNode("observable");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "O1");
        node = diagram.findNode("observable_1");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "O2");
        node = diagram.findNode("j01");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "R1");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME);
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "MoleculeType");

        compareResult(FILE_PATH_RES_3_26, 1);

        node = diagram.findNode("A(b)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "Species1");
        node = diagram.findNode("B(a)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "Species2");
        node = diagram.findNode("observable");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "Observable1");
        node = diagram.findNode("observable_1");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "Observable2");
        node = diagram.findNode("j01");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "Reaction1");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME);
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");

        compareResult(FILE_PATH_RES_3_27, 2);

        node = diagram.findNode("A(b)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("B(a)");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("observable");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("observable_1");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode("j01");
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "");
        node = diagram.findNode(BionetgenConstants.MOLECULE_TYPE_NAME);
        node.getAttributes().setValue(BionetgenConstants.LABEL_ATTR, "MT1");

        compareResult(FILE_PATH_RES_3_25, 3);
    }

    public void testChangeVariableName() throws Exception
    {
        preprocess(BionetgenTestUtility.readFile(FILE_PATH_EX_3));

        Variable var = diagram.getRole(EModel.class).getVariable("k2");
        var.setName("k02");

        compareResult(FILE_PATH_RES_3_28);
    }
}
