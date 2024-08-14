package biouml.model.dynamics._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;

public class EModelTest extends TestCase
{
    public EModelTest(String name)
    {
        super(name);
        AccessCoreInit.init();
    }

    public void testParseConstants() throws Exception
    {
        Base kernel = new Stub(null, "kernel");
        Diagram diagram = new Diagram(null, new Stub(null, "test"), null);
        EModel model = new EModel(diagram);

        String formula = "k1+1 *$blood.A / $B - k1*k1-k1/k2 - k2 *233.0 + sqrt(k1,2)";
        /*        Set constants = model.extractConstants(formula);

         assertEquals("Wrong count of constants", 2, constants.size() );

         String k1 = null;
         String k2 = null;
         Iterator i = constants.iterator();
         while( i.hasNext() )
         {
         String str = (String)i.next();

         if( str.equals("k1") )
         k1 = str;

         if( str.equals("k2") )
         k2 = str;
         }

         assertNotNull("'k1' constant not found", k1);
         assertNotNull("'k2' constant not found", k2);
         */
    }

    public void testPharmoSimpleConstants() throws Exception
    {
        Base kernel = new Stub(null, "kernel");
        Diagram diagram = new Diagram(null, new Stub(null, "test"), null);
        EModel model = new EModel(diagram);

        Edge edge = new Edge(new Stub(null, "edgeKernel"), new Node(null, kernel), new Node(null, kernel));
        edge.setRole(new Equation(null, Equation.TYPE_RATE, null, "-k_44*k_E0*$A/(k_Km+$A/v_liver)"));
        diagram.put(edge);

        /*
         assertNotNull( "No one constants",model.getConstants());
         assertEquals( "Wrong count of constants",4,model.getConstants().getSize() );

         Constant constant = (Constant)model.getConstants().get("k_44");
         assertNotNull("'k_44' constant not found",constant);

         constant = (Constant)model.getConstants().get("k_E0");
         assertNotNull("'k_E0' constant not found",constant);

         constant = (Constant)model.getConstants().get("k_Km");
         assertNotNull("'k_Km' constant not found",constant);

         constant = (Constant)model.getConstants().get("v_liver");
         assertNotNull("'v_liver' constant not found", constant);
         */
    }

    public void testParseConstantsAndBrackets() throws Exception
    {
        Base kernel = new Stub(null, "kernel");
        Diagram diagram = new Diagram(null, new Stub(null, "test"), null);
        EModel model = new EModel(diagram);

        Edge edge = new Edge(new Stub(null, "edgeKernel"), new Node(null, kernel), new Node(null, kernel));
        edge.setRole(new Equation(null, Equation.TYPE_RATE, null, "k1+1 *$blood.A / $B - f(k1)*g(k1-k1/k2,32) - k2 *233.0"));
        diagram.put(edge);
        /*

         assertNotNull("No one constants", model.getConstants());
         assertEquals("Wrong count of constants", 2, model.getConstants().getSize() );

         Constant constant = (Constant)model.getConstants().get("k1");
         assertNotNull("'k1' constant not found",constant );

         constant = (Constant)model.getConstants().get("k2");
         assertNotNull("'k2' constant not found", constant);
         */
    }

    public void testUpdateConstants() throws Exception
    {
        Base kernel = new Stub(null, "kernel");
        Diagram diagram = new Diagram(null, new Stub(null, "test"), null);
        EModel model = new EModel(diagram);
        Edge edge = new Edge(new Stub(null, "edgeKernel"), new Node(null, kernel), new Node(null, kernel));
        edge.setRole(new Equation(null, Equation.TYPE_RATE, null, "k1+1 *$blood.A / $B - k1*k1-k1/k2 - k2 *233.0"));
        diagram.put(edge);
        /*
         model.addConstant( "OLD",1 );
         model.addConstant( "OLD",1 );
         assertNotNull( "No one constants",model.getConstants());
         assertEquals( "Wrong count of constants",3,model.getConstants().getSize() );
         Constant constant = (Constant)model.getConstants().get("k1");
         assertNotNull( "'k1' constant not found",constant );
         constant = (Constant)model.getConstants().get("k2");
         assertNotNull( "'k2' constant not found",constant );
         constant = (Constant)model.getConstants().get("OLD");
         assertNotNull( "'OLD' constant not found",constant );
         */
    }

    public void testRemoveConstants() throws Exception
    {

        Base kernel = new Stub(null, "kernel");
        Diagram diagram = new Diagram(null, new Stub(null, "test"), null);
        EModel model = new EModel(diagram);

        Edge edge = new Edge(new Stub(null, "edgeKernel"), new Node(null, kernel), new Node(null, kernel));
        edge.setRole(new Equation(null, Equation.TYPE_RATE, null, "a+1"));
        diagram.put(edge);

        /*
         assertNotNull( "No one constants",model.getConstants());
         assertEquals( "Wrong count of constants",1,model.getConstants().getSize() );
         Constant constant = (Constant)model.getConstants().get("a");
         assertNotNull( "'a' constant not found",constant );

         edge.setRole( new Equation(null,Equation.TYPE_RATE, null,"b+1") );

         constant = (Constant)model.getConstants().get("b");
         assertNotNull( "'b' constant not found",constant );
         assertEquals( "Wrong count of constants after changing equation",1,model.getConstants().getSize() );
         */
    }

    public void testAddVariable() throws Exception
    {
        Base kernel = new Stub(null, "kernel");
        Diagram diagram = new Diagram(null, new Stub(null, "test"), null);
        assertNotNull("Can't create diagram", diagram);
        EModel model = new EModel(diagram);
        assertNotNull("Can't create model", model);
        DataCollection variables = model.getVariables();
        assertNotNull("Variables collection can't be null", variables);

        // test add
        Node node = new Node(diagram, kernel);
        VariableRole var = new VariableRole(node, 0);
        node.setRole(var);
        diagram.put(node);
        assertTrue("Can't find variable '$kernel'", variables.contains("$kernel"));

        VariableRole varKernel = (VariableRole)variables.get("$kernel");
        assertNotNull("Can't find variable '$kernel'", varKernel);

        // test remove
        diagram.remove(node.getName());
        assertTrue("Variable '$kernel' found.", !variables.contains("$kernel"));
    }

    public void testAddVariableIntoCompartment() throws Exception
    {
        Base kernel = new Stub(null, "kernel");
        Diagram diagram = new Diagram(null, new Stub(null, "test"), null);
        assertNotNull("Can't create diagram", diagram);
        EModel model = new EModel(diagram);
        assertNotNull("Can't create model", model);
        DataCollection variables = model.getVariables();
        assertNotNull("Variables collection can't be null", variables);
        Base compartmentKernel = new Stub(null, "compartmentKernel");
        assertNotNull("Can't create compartment kernel", compartmentKernel);
        Compartment compartment = new Compartment(diagram, compartmentKernel);
        diagram.put(compartment);

        // test add
        Node node = new Node(compartment, kernel);
        VariableRole var = new VariableRole(node, 0);
        node.setRole(var);
        compartment.put(node);

        //   for (Iterator iter = variables.getNameList().iterator(); iter.hasNext();)
        //       System.out.println(iter.next().toString());

        assertTrue("Can't find variable '$compartmentKernel.kernel'", variables.contains("$compartmentKernel.kernel"));

        // test remove
        compartment.remove(node.getName());
        assertTrue("Variable '$compartmentKernel.kernel' found.", !variables.contains("$compartmentKernel.kernel"));
    }


    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(EModelTest.class);
        return suite;
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("swing") )
        {
            junit.swingui.TestRunner.run(EModelTest.class);
        }
        else
        {
            junit.textui.TestRunner.run(suite());
        }
    }
}
