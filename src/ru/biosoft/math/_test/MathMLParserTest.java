
package ru.biosoft.math._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.xml.MathMLParser;
import ru.biosoft.util.XmlUtil;

public class MathMLParserTest extends TestCase
{
    protected static final MathMLParser mlp = new MathMLParser(); 
    protected double delta = 0.1;

    public MathMLParserTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MathMLParserTest.class.getName());
        suite.addTest(new MathMLParserTest("test_MathMLParser"));
        suite.addTest(new MathMLParserTest("test_CN"));
        //don't work
        //suite.addTest(new MathMLParserTest("test_Functions"));
        suite.addTest(new MathMLParserTest("test_Qualifiers"));
        suite.addTest(new MathMLParserTest("test_LAMBDA"));
        //don't work
        //suite.addTest(new MathMLParserTest("test_PIECEWISE"));
        return suite;
    }

    public void test_MathMLParser()  throws Exception
    {
//        System.out.println("****** MathMLParserTest ******");
        test_SkippingOfNonTagElements(
"<math>"
+"  <apply>"
+"      <plus/>"
+"      <ci>a</ci>"
+"      <ci>b</ci>"
+"  </apply>"
+"</math>"
);
    }
/*
    public void test_PIECEWISE()
    {
//    System.out.println("\n*** test_PIECEWISE ***");
    String goodExpr = "<eq/><ci>y</ci><piecewise><piece><ci>x</ci><apply><lt/><ci>x</ci><cn>Pi</cn></apply></piece><otherwise><cn>Pi</cn></otherwise></piecewise>";

    ru.biosoft.math.model.Node eq = test_ApplyTag(goodExpr);

    assertTrue(eq != null);
    assertTrue(eq.jjtGetChild(1) instanceof AstPiecewise);
    assertTrue(eq.jjtGetChild(1).jjtGetChild(0) instanceof AstPiece);
    assertTrue(eq.jjtGetChild(1).jjtGetChild(1) instanceof AstPiece);

    assertTrue(eq.jjtGetChild(1).jjtGetChild(0).jjtGetChild(1) instanceof AstVarNode);
    assertTrue(eq.jjtGetChild(1).jjtGetChild(0).jjtGetChild(0) instanceof AstFunNode);

    assertTrue(((AstFunNode)(eq.jjtGetChild(1).jjtGetChild(0).jjtGetChild(0))).getFunction().getName() == "<");
    }
    */
    public void test_LAMBDA()
    {
//        System.out.println("\n*** test_LAMBDA ***");

        String goodExpr = "<bvar><ci>F</ci></bvar><bvar><ci>G</ci></bvar><apply><plus/><ci>F</ci><ci>G</ci><cn>5</cn></apply>";
        ru.biosoft.math.model.Node func = test_LambdaTag(goodExpr);
        assertTrue(func != null);

        assertEquals(2, ( (AstFunctionDeclaration)(func) ).getNumberOfParameters());

        String badExpr = "<bvar><cn>F</cn></bvar><bvar><ci>G</ci></bvar><apply><plus/><ci>F</ci><ci>G</ci><cn>5</cn></apply>";
        func = test_LambdaTag(badExpr);
        assertTrue(func != null);

        assertEquals(1, ( (AstFunctionDeclaration)(func) ).getNumberOfParameters());
    }

    public void test_Qualifiers()
    {
        test_Degree_Root();
    }

    public void test_Degree_Root()
    {
//        System.out.println("\n*** test_Degree_Root ***");
        String goodExpr = "<root/><degree><ci type=\"integer\">n</ci></degree><ci>a</ci>";
        ru.biosoft.math.model.Node root = test_ApplyTag(goodExpr);

        assertTrue(root != null);

        assertEquals("a", ( (AstVarNode)(root.jjtGetChild(0)) ).getName());

        goodExpr = "<root/><degree><cn>n</cn></degree><ci>a</ci>";
        root = test_ApplyTag(goodExpr);

        assertTrue(root != null);

        assertEquals("n", ( (AstConstant)(root.jjtGetChild(1)) ).getName());

        String badExpr = "<root/><ci>a</ci>";
        root = test_ApplyTag(badExpr);

        assertTrue(root != null);
//    System.out.println("blya");
        Object expected = ( (AstConstant)( root.jjtGetChild(1) ) ).getValue();

        assertEquals("2 was expected", 2, ((Integer)(expected)).intValue() );

/*
        Object value = ((AstConstant)( plus.jjtGetChild(0) )).getValue();

        if( value instanceof Double && expected instanceof Double )
            assertEquals( ((Double)expected).doubleValue(),  ((Double)value).doubleValue(), delta );
        else
            assertEquals(xmlMathML, expected,  value);
*/
    }
/*
    public void test_Functions()
    {
    test_numberOfFuncArgs_correct("<plus/><ci>a</ci><ci>b</ci>", 2);
    test_numberOfFuncArgs_correct("<cos/><ci>a</ci>", 1);
    test_numberOfFuncArgs_correct("<power/><ci>a</ci><cn>a</cn>", 2);
    test_numberOfFuncArgs_correct("<minus/><ci>a</ci><cn>a</cn>", 2);
    test_numberOfFuncArgs_correct("<minus/><ci>a</ci>", 1);
    test_numberOfFuncArgs_correct("<diff/><bvar><ci>x</ci></bvar><bvar><ci>y</ci></bvar><bvar><ci>z</ci></bvar><apply><times/><ci>G</ci><cn>5</cn></apply>", 4);


    test_functionsSyntax_correct("<plus/><ci>a</ci><ci>b</ci>", "+");
    test_functionsSyntax_correct("<arccos/><ci>a</ci>", "arccos");
    test_functionsSyntax_correct("<cos/><cn>pi</cn>", "cos");
    test_functionsSyntax_correct("<arccos/><cn>jklashd</cn>", "arccos");
    test_functionsSyntax_correct("<power/><ci>x</ci><cn>pi</cn>", "^");


    //special test for minus with ONE argument (it must be interpreted as uminus)
    test_functionsSyntax_correct("<minus/><ci>x</ci>", "u-");

    //special test for EQ with function as argument
    test_functionsSyntax_correct("<eq/><ci>x</ci><apply><plus/><cn>1</cn><ci>z</ci></apply>", "=");
    test_functionsSyntax_correct("<eq/><ci>x</ci><cn>1</cn>", "==");

    //special test for function-context of <csymbol>
    test_functionsSyntax_correct("<csymbol>abba</csymbol><ci>a</ci><ci>b</ci>", "abba");
    test_numberOfFuncArgs_correct("<csymbol>abba</csymbol><csymbol>b</csymbol><ci>a</ci><csymbol>b</csymbol>", 3);
    }
*/
    public void test_numberOfFuncArgs_correct(String xmlInApply, int argc)
    {
//        System.out.println("\n*** test_numberOfFuncArgs_correct ***");

        String expr = xmlInApply;
        ru.biosoft.math.model.Node func = test_ApplyTag(expr);

        assertTrue(func != null);

        int n = func.jjtGetNumChildren();

        assertEquals(xmlInApply, argc, n);
    }

    public void test_numberOfFuncArgs_incorrect(String xmlInApply)
    {
        //        System.out.println("\n*** test_numberOfFuncArgs_incorrect ***");

        String expr = xmlInApply;
        ru.biosoft.math.model.Node func = test_ApplyTag(expr);

        assertTrue(func == null);
    }

    public void test_functionsSyntax_correct(String xmlInApply, String funcName)
    {
//        System.out.println("\n*** test_functionsSyntax_correct ***");

        String expr = xmlInApply;
        ru.biosoft.math.model.Node func = test_ApplyTag(expr);
        String name = ((AstFunNode)( func )).getFunction().getName();

        assertEquals(xmlInApply, funcName, name);
    }

    public void test_CN()  throws Exception
    {
    //testing correct expressions parsing
        test_processCN_correct("<cn base=\"10\">10</cn>", Double.valueOf(10) );
        test_processCN_correct("<cn type=\"integer\">10</cn>", 10);
        test_processCN_correct("<cn type=\"real\">10</cn>", Double.valueOf(10));
        test_processCN_correct("<cn type=\"e-notation\">2<sep/>3</cn>", Double.valueOf(2000));

    //testing _IN_correct expressions parsing
        test_processCN_incorrect("<cn type=\"integer\">10.0</cn>");
        test_processCN_incorrect("<cn type=\"real\">10e</cn>");
        test_processCN_incorrect("<cn type=\"e-notation\">23</cn>");
        test_processCN_incorrect("<cn type=\"real\">2<sep/>3</cn>");
    }

    void test_SkippingOfNonTagElements(String xmlMathML) throws Exception
    {
//        System.out.println("\n*** test_SkippingOfNonTagElements ***");

        mlp.parse(xmlMathML);

        AstStart astStart = mlp.getStartNode();
        assertEquals(((AstFunNode)( astStart.jjtGetChild(0) )).getFunction().getName(), "+");
    }

    ru.biosoft.math.model.Node test_ApplyTag(String tag)
    {
        String mathmlStr = "<math><apply>" + tag + "</apply></math>";
        mlp.parse(mathmlStr);

        assertNotNull("astStart == null", mlp.getStartNode());

        return mlp.getStartNode().jjtGetChild(0);
    }

    ru.biosoft.math.model.Node test_LambdaTag(String tag)
    {
        String mathmlStr = "<math><lambda>" + tag + "</lambda></math>";
        mlp.parse(mathmlStr);

        assertNotNull("astStart == null", mlp.getStartNode());

        return mlp.getStartNode().jjtGetChild(0);
    }

    void test_processCN_correct(String xmlMathML, Object expected) throws Exception
    {
//        System.out.println("\n*** test_processCN_correct ***");

        String expr = "<plus/>" + xmlMathML + "<ci>b</ci>";
        ru.biosoft.math.model.Node plus = test_ApplyTag(expr);
        Object value = ((AstConstant)( plus.jjtGetChild(0) )).getValue();

        if( value instanceof Double && expected instanceof Double )
            assertEquals( ((Double)expected).doubleValue(),  ((Double)value).doubleValue(), delta );
        else
            assertEquals(xmlMathML, expected,  value);
    }

    void test_processCN_incorrect(String xmlMathML) throws Exception
    {
//        System.out.println("\n*** test_processCN_incorrect ***");

        String expr = "<plus/>" + xmlMathML + "<ci>b</ci>";
        ru.biosoft.math.model.Node plus = test_ApplyTag(expr);
        Object value = ((AstConstant)( plus.jjtGetChild(0) )).getValue();
        assertEquals( 0.0,  ((Double)value).doubleValue(), delta); // the default value for various ambigiuos situations is 0.0
    }

    protected void dumpChildNodes(String prefix, org.w3c.dom.Node root)
    {
        for(org.w3c.dom.Node curr : XmlUtil.nodes(root))
        {
            System.out.println(prefix + curr.getNodeName());
            dumpChildNodes(prefix + " ", curr);
        }
    }
}
