package biouml.plugins.antimony._test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.astparser.AstSymbol;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.standard.type.Unit;
import junit.framework.TestSuite;

public class BasicAntimonyTest extends AntimonyTest
{
    final static String FILE_PATH_EX1 = "biouml/plugins/antimony/_test/example_1/antimony_ex1.txt";
    final static String FILE_PATH_EX1_1 = "biouml/plugins/antimony/_test/example_1/antimony_ex1_1.txt";
    final static String FILE_PATH_RE_1_1 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_1.txt";
    final static String FILE_PATH_RE_1_2 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_2.txt";
    final static String FILE_PATH_RE_1_3 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_3.txt";
    final static String FILE_PATH_RE_1_4 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_4.txt";
    final static String FILE_PATH_RE_1_5 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_5.txt";
    final static String FILE_PATH_RE_1_6 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_6.txt";
    final static String FILE_PATH_RE_1_7 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_7.txt";
    final static String FILE_PATH_RE_1_8 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_8.txt";
    final static String FILE_PATH_RE_1_9 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_9.txt";
    final static String FILE_PATH_RE_1_10 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_10.txt";
    final static String FILE_PATH_RE_1_11 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_11.txt";
    final static String FILE_PATH_RE_1_12 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_12.txt";
    final static String FILE_PATH_RE_1_13 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_13.txt";
    final static String FILE_PATH_RE_1_14 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_14.txt";
    final static String FILE_PATH_RE_1_15 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_15.txt";
    final static String FILE_PATH_RE_1_16 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_16.txt";
    final static String FILE_PATH_RE_1_17 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_17.txt";
    final static String FILE_PATH_RE_1_18 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_18.txt";
    final static String FILE_PATH_RE_1_19 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_19.txt";
    final static String FILE_PATH_RE_1_20 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_20.txt";
    final static String FILE_PATH_RE_1_21 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_21.txt";
    final static String FILE_PATH_RE_1_22 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_22.txt";
    final static String FILE_PATH_RE_1_23 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_23.txt";
    final static String FILE_PATH_RE_1_24 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_24.txt";
    final static String FILE_PATH_RE_1_25 = "biouml/plugins/antimony/_test/example_1/antimony_re_1_25.txt";

    final static String FILE_PATH_EX2 = "biouml/plugins/antimony/_test/example_2/antimony_ex2.txt";
    final static String FILE_PATH_RE_2_1 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_1.txt";
    final static String FILE_PATH_RE_2_2 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_2.txt";
    final static String FILE_PATH_RE_2_3 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_3.txt";
    final static String FILE_PATH_RE_2_4 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_4.txt";
    final static String FILE_PATH_RE_2_5 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_5.txt";
    final static String FILE_PATH_RE_2_6 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_6.txt";
    final static String FILE_PATH_RE_2_7 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_7.txt";
    final static String FILE_PATH_RE_2_8 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_8.txt";
    final static String FILE_PATH_RE_2_9 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_9.txt";
    final static String FILE_PATH_RE_2_10 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_10.txt";
    final static String FILE_PATH_RE_2_11 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_11.txt";
    final static String FILE_PATH_RE_2_12 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_12.txt";
    final static String FILE_PATH_RE_2_13 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_13.txt";
    final static String FILE_PATH_RE_2_14 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_14.txt";
    final static String FILE_PATH_RE_2_15 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_15.txt";
    final static String FILE_PATH_RE_2_16 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_16.txt";
    final static String FILE_PATH_RE_2_17 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_17.txt";
    final static String FILE_PATH_RE_2_18 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_18.txt";
    final static String FILE_PATH_RE_2_19 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_19.txt";
    final static String FILE_PATH_RE_2_20 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_20.txt";
    final static String FILE_PATH_RE_2_21 = "biouml/plugins/antimony/_test/example_2/antimony_re_2_21.txt";

    final static String FILE_PATH_EX3 = "biouml/plugins/antimony/_test/example_3/antimony_ex3.txt";
    final static String FILE_PATH_RE_3_1 = "biouml/plugins/antimony/_test/example_3/antimony_re_3_1.txt";

    public BasicAntimonyTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(BasicAntimonyTest.class.getName());
        suite.addTest(new BasicAntimonyTest("addCompartment"));
        suite.addTest(new BasicAntimonyTest("addSpecie"));
        suite.addTest(new BasicAntimonyTest("addSpecieInOldLine"));
        suite.addTest(new BasicAntimonyTest("addSpecieWithParent"));
        suite.addTest(new BasicAntimonyTest("addEquation"));
        suite.addTest(new BasicAntimonyTest("addEvent"));
        suite.addTest(new BasicAntimonyTest("addAssignment"));
        suite.addTest(new BasicAntimonyTest("addConstraint"));
        suite.addTest(new BasicAntimonyTest("addConstraintMessage"));
        suite.addTest(new BasicAntimonyTest("addFunction"));
        suite.addTest(new BasicAntimonyTest("addReaction"));
        suite.addTest(new BasicAntimonyTest("addReactionWithModifier"));
        suite.addTest(new BasicAntimonyTest("addSubstanceOnlySpecie"));
        suite.addTest(new BasicAntimonyTest("removeCompartment"));
        suite.addTest(new BasicAntimonyTest("removeSpecieFromLine"));
        suite.addTest(new BasicAntimonyTest("removeSpecieWithLine"));
        suite.addTest(new BasicAntimonyTest("removeSpecieWithParent"));
        suite.addTest(new BasicAntimonyTest("removeEvent"));
        suite.addTest(new BasicAntimonyTest("removeEventAssignment"));
        suite.addTest(new BasicAntimonyTest("removeConstraint"));
        suite.addTest(new BasicAntimonyTest("removeConstraintMessage"));
        suite.addTest(new BasicAntimonyTest("removeFunction"));
        suite.addTest(new BasicAntimonyTest("removeReaction"));
        suite.addTest(new BasicAntimonyTest("removeEquation"));
        suite.addTest(new BasicAntimonyTest("addInitialValue"));
        suite.addTest(new BasicAntimonyTest("changeQuantityType"));
        suite.addTest(new BasicAntimonyTest("changeInitialValue"));
        suite.addTest(new BasicAntimonyTest("addConst"));
        suite.addTest(new BasicAntimonyTest("addConstInLine"));
        suite.addTest(new BasicAntimonyTest("changeConst"));
        suite.addTest(new BasicAntimonyTest("addTitle"));
        suite.addTest(new BasicAntimonyTest("changeTitle"));
        suite.addTest(new BasicAntimonyTest("changeFormula"));
        suite.addTest(new BasicAntimonyTest("changeModifierType"));
        suite.addTest(new BasicAntimonyTest("changeStoichiometry"));
        suite.addTest(new BasicAntimonyTest("changeEventTrigger"));
        suite.addTest(new BasicAntimonyTest("addEventDelay"));
        suite.addTest(new BasicAntimonyTest("changeEventAssignment"));
        suite.addTest(new BasicAntimonyTest("changeConstraintCondition"));
        suite.addTest(new BasicAntimonyTest("changeConstraintMessage"));
        suite.addTest(new BasicAntimonyTest("changeEquation"));
        //suite.addTest(new BasicAntimonyTest("reservedNameTest"));
        suite.addTest(new BasicAntimonyTest("addUnit"));
        suite.addTest(new BasicAntimonyTest("changeUnitName"));
        suite.addTest(new BasicAntimonyTest("assignUnit"));
        suite.addTest(new BasicAntimonyTest("changeAssignedUnit"));
        suite.addTest(new BasicAntimonyTest("removeUnit"));
        suite.addTest(new BasicAntimonyTest("addInitialValueWithUnit"));
        suite.addTest(new BasicAntimonyTest("removeUnitFromInitialValue"));

        suite.addTest(new BasicAntimonyTest("addDatabaseReferences"));
        suite.addTest(new BasicAntimonyTest("changeRelationshipTypeOfDatabaseReference"));
        suite.addTest(new BasicAntimonyTest("changeUriOfDatabaseReference"));
        suite.addTest(new BasicAntimonyTest("removeDatabaseReferences"));
        suite.addTest(new BasicAntimonyTest("addInitialValueToSpecieWithParent"));
        suite.addTest(new BasicAntimonyTest("addUnitToSpecieWithParent"));
        suite.addTest(new BasicAntimonyTest("changeInitialQuantityType"));

        return suite;
    }

    public void addCompartment() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        //add compartment in diagram
        Compartment newComp = new Compartment(antimonyDiagram, "newComp", new biouml.standard.type.Compartment(antimonyDiagram, "newComp"));
        antimonyDiagram.put(newComp);

        compareResult(FILE_PATH_RE_1_1);
    }



    public void addSpecie() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        //add specie in diagram
        Compartment newSpecie = new Compartment(antimonyDiagram,
                new Specie(antimonyDiagram, "s1", biouml.plugins.sbgn.Type.TYPE_MACROMOLECULE));
        antimonyDiagram.put(newSpecie);

        compareResult(FILE_PATH_RE_1_2);
    }

    public void addSpecieInOldLine() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_2));

        preprocess(antimonyText);

        //add specie in diagram
        Compartment newSpecie = new Compartment(antimonyDiagram,
                new Specie(antimonyDiagram, "s2", biouml.plugins.sbgn.Type.TYPE_MACROMOLECULE));
        antimonyDiagram.put(newSpecie);

        compareResult(FILE_PATH_RE_1_5);
    }

    public void addSpecieWithParent() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_2));

        preprocess(antimonyText);

        //add specie in diagram
        Compartment parent = (Compartment)antimonyDiagram.get("default");
        Compartment newSpecie = new Compartment(parent, new Specie(parent, "s2", biouml.plugins.sbgn.Type.TYPE_MACROMOLECULE));
        parent.put(newSpecie);

        compareResult(FILE_PATH_RE_1_6);
    }


    public void addEquation() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        EModel emodel = antimonyDiagram.getRole(EModel.class);
        Variable k = new Variable("k", emodel, emodel.getVariables());
        emodel.put(k);

        //add equation
        Node node = new Node(antimonyDiagram, "math_equation_1", new Stub(antimonyDiagram, "math_equation_1", Type.MATH_EQUATION));
        Role variable = new Equation(node, Equation.TYPE_RATE, "k", "9");
        node.setRole(variable);
        antimonyDiagram.put(node);

        //add equation
        node = new Node(antimonyDiagram, "math_equation_2", new Stub(antimonyDiagram, "math_equation_2", Type.MATH_EQUATION));
        variable = new Equation(node, Equation.TYPE_SCALAR, "k", "9");
        node.setRole(variable);
        antimonyDiagram.put(node);

        //add equation
        node = new Node(antimonyDiagram, "math_equation_3", new Stub(antimonyDiagram, "math_equation_3", Type.MATH_EQUATION));
        variable = new Equation(node, Equation.TYPE_INITIAL_ASSIGNMENT, "k", "9");
        node.setRole(variable);
        antimonyDiagram.put(node);

        compareResult(FILE_PATH_RE_1_4);
    }

    public void addEvent() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        //add event
        Node node = new Node(antimonyDiagram, "math_event", new Stub(antimonyDiagram, "math_event", Type.MATH_EVENT));
        Assignment[] actions = new Assignment[1];
        actions[0] = new Assignment("k", "k/7");
        Event variable = new Event(node, "k<5", null, actions);
        variable.setTriggerInitialValue(false);
        variable.setTriggerPersistent(false);
        node.setRole(variable);
        antimonyDiagram.put(node);

        compareResult(FILE_PATH_RE_1_7);
    }

    public void addAssignment() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_7));

        preprocess(antimonyText);

        //add assignment
        DiagramElement de = antimonyDiagram.get("math_event");
        Event event = de.getRole(Event.class);
        event.addEventAssignment(new Assignment("k", "0"), true);


        compareResult(FILE_PATH_RE_1_8);
    }

    public void addConstraint() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        //add constraint
        Node node = new Node(antimonyDiagram, "math_constraint", new Stub(antimonyDiagram, "math_constraint", Type.MATH_CONSTRAINT));
        Constraint con = new Constraint(node, "a>100", "");

        node.setRole(con);
        antimonyDiagram.put(node);

        compareResult(FILE_PATH_RE_1_20);
    }

    public void addConstraintMessage() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_20));

        preprocess(antimonyText);

        //add message
        DiagramElement de = antimonyDiagram.get("math_constraint");
        assertNotNull("Failed to find constraint", de);

        Constraint constraint = de.getRole(Constraint.class);
        constraint.setMessage("a must be > 100");

        compareResult(FILE_PATH_RE_1_21);
    }

    public void addFunction() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        //add function
        Node node = new Node(antimonyDiagram, "math_function", new Stub(antimonyDiagram, "math_function"));
        Function var = new Function(node, "function f()=0");
        node.setRole(var);
        antimonyDiagram.put(node);

        compareResult(FILE_PATH_RE_1_9);
    }

    public void addReaction() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_5));

        preprocess(antimonyText);

        //add reaction in diagram
        Node nodeS1 = antimonyDiagram.findNode("s1");
        Node nodeS3 = antimonyDiagram.findNode("s2");
        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        List<SpecieReference> specieRef = new ArrayList<>();
        specieRef.add(generator.createSpeciesReference(nodeS1, SpecieReference.REACTANT));
        specieRef.add(generator.createSpeciesReference(nodeS3, SpecieReference.PRODUCT));
        generator.createReaction(antimonyDiagram, "r1", "1", specieRef);
        compareResult(FILE_PATH_RE_1_10);
    }

    public void addReactionWithModifier() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_5));

        preprocess(antimonyText);

        //add reaction in diagram
        Node nodeS1 = antimonyDiagram.findNode("s1");
        Node nodeS3 = antimonyDiagram.findNode("s2");
        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        List<SpecieReference> specieRef = new ArrayList<>();
        specieRef.add(generator.createSpeciesReference(nodeS1, SpecieReference.REACTANT));
        specieRef.add(generator.createSpeciesReference(nodeS3, SpecieReference.MODIFIER));
        generator.createReaction(antimonyDiagram, "r1", "1", specieRef);
        compareResult(FILE_PATH_RE_1_13);
    }


    public void removeCompartment() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        //remove compartment
        antimonyDiagram.remove("default");

        compareResult(FILE_PATH_RE_1_3);
    }

    public void removeSpecieFromLine() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_5));

        preprocess(antimonyText);

        //remove specie
        antimonyDiagram.remove("s2");

        compareResult(FILE_PATH_RE_1_2);
    }

    public void removeSpecieWithLine() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_2));

        preprocess(antimonyText);

        //remove specie
        antimonyDiagram.remove("s1");

        compareResult(FILE_PATH_EX1);
    }

    public void removeSpecieWithParent() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_6));

        preprocess(antimonyText);

        //remove specie
        Compartment parent = (Compartment)antimonyDiagram.get("default");
        parent.remove("s2");

        compareResult(FILE_PATH_RE_1_2);
    }

    public void removeEvent() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_7));

        preprocess(antimonyText);

        //remove event
        antimonyDiagram.remove("math_event");
        antimonyDiagram.getRole(EModel.class).getVariables().remove("k");

        compareResult(FILE_PATH_EX1);
    }

    public void removeEventAssignment() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_8));

        preprocess(antimonyText);

        //remove assignment
        Node nodeEvent = antimonyDiagram.findNode("math_event");
        Event event = nodeEvent.getRole(Event.class);
        Assignment[] actions = new Assignment[1];
        actions[0] = event.getEventAssignment(0);
        event.setEventAssignment(actions);

        compareResult(FILE_PATH_RE_1_7);
    }

    public void removeConstraint() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_20));

        preprocess(antimonyText);

        //remove event
        antimonyDiagram.remove("math_constraint");
        antimonyDiagram.getRole(EModel.class).getVariables().remove("a");

        compareResult(FILE_PATH_EX1);
    }

    public void removeConstraintMessage() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_21));

        preprocess(antimonyText);

        //remove constraint message
        Node nodeConstraint = antimonyDiagram.findNode("math_constraint");
        assertNotNull("Failed to find constraint", nodeConstraint);
        Constraint constraint = nodeConstraint.getRole(Constraint.class);
        constraint.setMessage("");

        compareResult(FILE_PATH_RE_1_20);
    }

    public void removeFunction() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_9));

        preprocess(antimonyText);

        //remove function
        antimonyDiagram.remove("f");

        compareResult(FILE_PATH_EX1_1);
    }

    public void removeReaction() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_10));

        preprocess(antimonyText);

        //remove reaction
        antimonyDiagram.remove("r1");

        compareResult(FILE_PATH_RE_1_5);
    }

    public void removeEquation() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_4));

        preprocess(antimonyText);

        //remove reaction
        antimonyDiagram.remove("k" + "_" + AstSymbol.RATE);

        compareResult(FILE_PATH_RE_1_11);
    }

    public void addInitialValue() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //add initial value
        VariableRole role = antimonyDiagram.findNode("s2").getRole(VariableRole.class);
        role.setInitialValue(5.0);

        compareResult(FILE_PATH_RE_2_1);
    }

    public void changeInitialValue() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change initial value
        antimonyDiagram.getRole(EModel.class).getVariable("k").setInitialValue(5);


        compareResult(FILE_PATH_RE_2_2);
    }

    public void addConst() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change property
        VariableRole role = antimonyDiagram.findNode("s1").getRole(VariableRole.class);
        role.setConstant(true);

        compareResult(FILE_PATH_RE_2_3);
    }

    public void addConstInLine() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_2_3));

        preprocess(antimonyText);

        //change property
        VariableRole role = antimonyDiagram.findNode("s2").getRole(VariableRole.class);
        role.setConstant(true);

        compareResult(FILE_PATH_RE_2_6);
    }


    public void changeConst() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_2_3));

        preprocess(antimonyText);

        //change property
        VariableRole role = antimonyDiagram.findNode("s1").getRole(VariableRole.class);
        role.setConstant(false);

        compareResult(FILE_PATH_RE_2_4);
    }

    public void addTitle() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change property
        Node node = antimonyDiagram.findNode("s1");
        node.setTitle("element");

        compareResult(FILE_PATH_RE_2_11);
    }

    public void changeTitle() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_2_11));

        preprocess(antimonyText);

        //change property
        Node node = antimonyDiagram.findNode("s1");
        node.setTitle("element S1");

        compareResult(FILE_PATH_RE_2_12);
    }


    public void changeFormula() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change formula
        Node reactionNode = antimonyDiagram.findNode("r1");
        ( (Reaction)reactionNode.getKernel() ).setFormula("2");

        compareResult(FILE_PATH_RE_2_5);
    }

    public void changeStoichiometry() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change formula
        Node reactionNode = antimonyDiagram.findNode("r1");
        SpecieReference sp = ( (Reaction)reactionNode.getKernel() ).getSpecieReferences()[0];
        sp.setStoichiometry("2");
        sp = ( (Reaction)reactionNode.getKernel() ).getSpecieReferences()[1];
        sp.setStoichiometry("3");
        sp.setStoichiometry("4");

        compareResult(FILE_PATH_RE_2_13);
    }

    public void changeModifierType() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_13));

        preprocess(antimonyText);

        //change formula
        Node reactionNode = antimonyDiagram.findNode("r1");
        SpecieReference[] sps = ( (Reaction)reactionNode.getKernel() ).getSpecieReferences();
        for( SpecieReference sp : sps )
        {
            if( !sp.isReactantOrProduct() )
                sp.setModifierAction(SpecieReference.ACTION_INHIBITION);
        }

        compareResult(FILE_PATH_RE_1_14);
    }


    public void changeEventTrigger() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change trigger
        Node nodeEvent = antimonyDiagram.findNode("math_event");
        Event event = nodeEvent.getRole(Event.class);
        event.setTrigger("s1>0");

        compareResult(FILE_PATH_RE_2_7);
    }

    public void addEventDelay() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change trigger
        Node nodeEvent = antimonyDiagram.findNode("math_event");
        Event event = nodeEvent.getRole(Event.class);
        event.setDelay("10");

        compareResult(FILE_PATH_RE_2_10);
    }

    public void changeEventAssignment() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change assignment
        Node nodeEvent = antimonyDiagram.findNode("math_event");
        Event event = nodeEvent.getRole(Event.class);
        event.getEventAssignment(0).setVariable("s1");
        event.getEventAssignment(0).setMath("7");

        compareResult(FILE_PATH_RE_2_8);
    }

    public void changeConstraintCondition() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change constraint condition
        Node nodeConstraint = antimonyDiagram.findNode("math_constraint");
        Constraint constraint = nodeConstraint.getRole(Constraint.class);
        constraint.setFormula("300>a>100");

        compareResult(FILE_PATH_RE_2_16);
    }

    public void changeConstraintMessage() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change constraint message
        Node nodeConstraint = antimonyDiagram.findNode("math_constraint");
        Constraint constraint = nodeConstraint.getRole(Constraint.class);
        constraint.setMessage("Variable a should be greater than hundred");

        compareResult(FILE_PATH_RE_2_17);
    }
    public void changeEquation() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        //change equation
        Node nodeEquation = antimonyDiagram.findNode("k" + "_" + AstSymbol.RULE);
        Equation eq = nodeEquation.getRole(Equation.class);
        eq.setVariable("s1");
        eq.setFormula("5.0");

        nodeEquation = antimonyDiagram.findNode("k" + "_" + AstSymbol.RATE);
        eq = nodeEquation.getRole(Equation.class);
        eq.setVariable("s1");
        eq.setFormula("piecewise(x>sin(4.0) && w<5.0 && x<8.0=>y; z)");

        compareResult(FILE_PATH_RE_2_9);
    }

    public void addUnit() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX1));

        preprocess(antimonyText);

        Unit voltage = new Unit(null, "voltage");
        BaseUnit[] voltBU = new BaseUnit[] {new BaseUnit("gram", 1000, 0, 1), new BaseUnit("metre", 1, 0, 2),
                new BaseUnit("second", 1, 0, -3), new BaseUnit("ampere", 1, 0, -1)};
        voltage.setBaseUnits(voltBU);

        Unit foo = new Unit(null, "foo");
        BaseUnit[] fooBU = new BaseUnit[] {new BaseUnit("mole", 100, 0, 1), new BaseUnit("litre", 5, 0, -1)};
        foo.setBaseUnits(fooBU);

        EModel emodel = antimonyDiagram.getRole(EModel.class);
        emodel.addUnit(voltage);
        emodel.addUnit(foo);

        compareResult(FILE_PATH_RE_1_15);
    }


    public void changeUnitName() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_15));

        preprocess(antimonyText);

        EModel emodel = antimonyDiagram.getRole(EModel.class);
        Unit foo = emodel.getUnits().get("foo");
        foo.setName("bar");

        compareResult(FILE_PATH_RE_1_16);
    }

    public void assignUnit() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_15));

        preprocess(antimonyText);

        EModel emodel = antimonyDiagram.getRole(EModel.class);
        Variable z = new Variable("z", emodel, emodel.getVariables());
        emodel.put(z);

        Unit foo = emodel.getUnits().get("foo");
        z.setUnits(foo.getName());

        compareResult(FILE_PATH_RE_1_17);
    }

    public void changeAssignedUnit() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_17));

        preprocess(antimonyText);

        EModel emodel = antimonyDiagram.getRole(EModel.class);
        Variable z = emodel.getVariable("z");
        Unit voltage = emodel.getUnits().get("voltage");

        z.setUnits(voltage.getName());

        compareResult(FILE_PATH_RE_1_18);
    }



    public void removeUnit() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_18));

        preprocess(antimonyText);

        EModel emodel = antimonyDiagram.getRole(EModel.class);
        emodel.removeUnit("voltage");

        compareResult(FILE_PATH_RE_1_19);
    }

    public void addInitialValueWithUnit() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX2));

        preprocess(antimonyText);

        VariableRole role = antimonyDiagram.findNode("s1").getRole(VariableRole.class);
        role.setInitialValue(3.3);
        role.setUnits("foo");

        compareResult(FILE_PATH_RE_2_14);
    }

    public void removeUnitFromInitialValue() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_2_14));

        preprocess(antimonyText);

        VariableRole role = antimonyDiagram.findNode("s1").getRole(VariableRole.class);
        role.setUnits(Unit.UNDEFINED);

        compareResult(FILE_PATH_RE_2_15);
    }

    public void addDatabaseReferences() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_10));

        preprocess(antimonyText);

        Node nodeS1 = antimonyDiagram.findNode("s1");
        Node comp = antimonyDiagram.findNode("default");
        Node reactionNode = antimonyDiagram.findNode("r1");

        List<DatabaseReference> drs = new ArrayList<DatabaseReference>();

        DatabaseReference dr = new DatabaseReference("go", "GO%3A0031594");
        drs.add(dr);
        ( (Referrer)comp.getKernel() ).setDatabaseReferences(drs.toArray(new DatabaseReference[0]));
        dr.setRelationshipType("identity");

        drs = new ArrayList<DatabaseReference>();
        dr = new DatabaseReference("interpro", "IPR002394");
        drs.add(dr);
        dr = new DatabaseReference("go", "GO%3A0005892");
        drs.add(dr);
        ( (Referrer)nodeS1.getKernel() ).setDatabaseReferences(drs.toArray(new DatabaseReference[0]));
        for( DatabaseReference d : ( (Referrer)nodeS1.getKernel() ).getDatabaseReferences() )
            d.setRelationshipType("isVersionOf");

        drs = new ArrayList<DatabaseReference>();
        dr = new DatabaseReference("go", "GO%3A0042166");
        drs.add(dr);
        ( (Referrer)reactionNode.getKernel() ).setDatabaseReferences(drs.toArray(new DatabaseReference[0]));
        dr.setRelationshipType("isVersionOf");

        compareResult(FILE_PATH_RE_1_22);
    }

    public void changeRelationshipTypeOfDatabaseReference() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_22));

        preprocess(antimonyText);

        Node nodeS1 = antimonyDiagram.findNode("s1");

        DatabaseReference[] drs = ( (Referrer)nodeS1.getKernel() ).getDatabaseReferences();

        DatabaseReference dr = null;
        for( DatabaseReference d : drs )
            if( d.getDatabaseName().equals("interpro") && d.getId().equals("IPR002394") && d.getRelationshipType().equals("isVersionOf") )
                dr = d;

        dr.setRelationshipType("part");

        compareResult(FILE_PATH_RE_1_23);
    }

    public void changeUriOfDatabaseReference() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_22));

        preprocess(antimonyText);

        Node reaction = antimonyDiagram.findNode("r1");

        DatabaseReference[] drs = ( (Referrer)reaction.getKernel() ).getDatabaseReferences();

        DatabaseReference dr = null;
        for( DatabaseReference d : drs )
            if( d.getDatabaseName().equals("go") && d.getId().equals("GO%3A0042166") && d.getRelationshipType().equals("isVersionOf") )
                dr = d;

        dr.setId("GO%3A0042235");

        compareResult(FILE_PATH_RE_1_24);
    }

    public void removeDatabaseReferences() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_22));

        preprocess(antimonyText);

        Node comp = antimonyDiagram.findNode("default");
        Node nodeS1 = antimonyDiagram.findNode("s1");

        ( (Referrer)comp.getKernel() ).setDatabaseReferences(new DatabaseReference[0]);

        DatabaseReference dr = new DatabaseReference("go", "GO%3A0005892");
        dr.setRelationshipType("isVersionOf");
        ( (Referrer)nodeS1.getKernel() ).setDatabaseReferences(new DatabaseReference[] {dr});

        compareResult(FILE_PATH_RE_1_25);
    }

    public void addSubstanceOnlySpecie() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_2));

        preprocess(antimonyText);

        Node s2 = (Node) ( (CreatorElementWithName)antimonyDiagram.getType().getSemanticController() )
                .createInstance(antimonyDiagram, Specie.class, "s2", null, null).getElement();
        s2.getRole(VariableRole.class).setQuantityType(VariableRole.AMOUNT_TYPE);
        antimonyDiagram.put(s2);

        compareResult(FILE_PATH_RE_2_18);
    }

    public void changeQuantityType() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_5));

        preprocess(antimonyText);

        VariableRole s2 = antimonyDiagram.findNode("s2").getRole(VariableRole.class);
        s2.setQuantityType(VariableRole.AMOUNT_TYPE);

        compareResult(FILE_PATH_RE_2_18);

        s2.setQuantityType(VariableRole.CONCENTRATION_TYPE);

        compareResult(FILE_PATH_RE_1_5);
    }

    public void addInitialValueToSpecieWithParent() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_1_6));

        preprocess(antimonyText);

        Compartment parent = (Compartment)antimonyDiagram.get("default");
        assertNotNull(parent);
        Node node = parent.findNode("s2");
        assertNotNull(node);

        assertEquals(node.getRole(VariableRole.class).getInitialQuantityType(), VariableRole.AMOUNT_TYPE);
        node.getRole(VariableRole.class).setInitialValue(5.0);

        compareResult(FILE_PATH_RE_2_19);
    }

    public void addUnitToSpecieWithParent() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_2_19));

        preprocess(antimonyText);

        Compartment parent = (Compartment)antimonyDiagram.get("default");
        assertNotNull(parent);
        Node node = parent.findNode("s2");
        assertNotNull(node);

        node.getRole(VariableRole.class).setUnits("foo");
        ;

        compareResult(FILE_PATH_RE_2_20);
    }

    public void changeInitialQuantityType() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_RE_2_20));

        preprocess(antimonyText);

        VariableRole s2 = antimonyDiagram.findNode("s2").getRole(VariableRole.class);
        s2.setInitialQuantityType(VariableRole.CONCENTRATION_TYPE);

        compareResult(FILE_PATH_RE_2_21);

        s2.setInitialQuantityType(VariableRole.AMOUNT_TYPE);

        compareResult(FILE_PATH_RE_2_20);
    }


    //------------------------------------------------------------


    public void reservedNameTest() throws Exception
    {
        String antimonyText = ApplicationUtils.readAsString(new File(FILE_PATH_EX3));

        preprocess(antimonyText);

        //change
        Compartment newComp = new Compartment(antimonyDiagram, "compartment",
                new biouml.standard.type.Compartment(antimonyDiagram, "compartment"));
        antimonyDiagram.put(newComp);

        Node newSpecie = new Node(antimonyDiagram, "species", new Specie(antimonyDiagram, "species"));
        newSpecie.setRole(new VariableRole(newSpecie));
        antimonyDiagram.put(newSpecie);

        Node nodeS1 = antimonyDiagram.findNode("species");
        DiagramGenerator generator = new DiagramGenerator(antimonyDiagram.getName());
        List<SpecieReference> specieRef = new ArrayList<>();
        specieRef.add(generator.createSpeciesReference(nodeS1, SpecieReference.REACTANT));
        generator.createReaction(antimonyDiagram, "r1", "species*2", specieRef);
        Node node = new Node(antimonyDiagram, "math_equation_1", new Stub(antimonyDiagram, "math_equation_1", Type.MATH_EQUATION));
        Role variable = new Equation(node, Equation.TYPE_RATE, "$species", "species*3");
        node.setRole(variable);
        antimonyDiagram.put(node);

        compareResult(FILE_PATH_RE_3_1);
    }

}
