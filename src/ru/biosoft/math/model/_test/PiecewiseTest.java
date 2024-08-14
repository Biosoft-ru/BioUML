
package ru.biosoft.math.model._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.math.model.JavaFormatter;
import ru.biosoft.math.model.Parser;
import ru.biosoft.math.xml.MathMLParser;

/** Batch unit test for biouml.model package. */
public class PiecewiseTest extends TestCase
{
    /** Standart JUnit constructor */
    public PiecewiseTest(String name)
    {
        super(name);
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(PiecewiseTest.class.getName());
        suite.addTest(new PiecewiseTest("testBuildFormulas"));
        return suite;
    }

    public void testBuildFormulas()
    {
        String expr1 = "<math><piecewise>" +
            "<piece>" +
            "<cn>1</cn>" +
                    "<apply>" +
                        "<gt/><ci>a</ci><cn>0</cn>" +
                    "</apply>" +
                "</piece>" +
                "<piece>" +
                    "<ci>2</ci>" +
                    "<apply><lt/><ci>a</ci><cn>0</cn></apply>" +
                "</piece>" +
            "</piecewise></math>";

    String expr2 = "<math><piecewise>" +
                "<piece>" +
                    "<ci>x</ci>" +
                    "<apply><lt/><ci>x</ci><cn>Pi</cn></apply>" +
                "</piece>" +
                "<otherwise>" +
                    "<cn>1</cn>" +
                "</otherwise>" +
            "</piecewise></math>";

    String expr3 = "<math><piecewise>" +
                "<piece>" +
                    "<cn>1</cn>" +
                    "<apply>" +
                        "<eq/><ci>a</ci><cn>0</cn>" +
                    "</apply>" +
                "</piece>" +
                "<piece>" +
                    "<cn>2</cn>" +
                    "<apply>" +
                        "<lt/><ci>a</ci><cn>0</cn>" +
                    "</apply>" +
                "</piece>" +
                "<piece>" +
                    "<cn>3</cn>" +
                    "<apply>" +
                        "<gt/><ci>a</ci><cn>0</cn>" +
                    "</apply>" +
                "</piece>" +
            "</piecewise></math>";

    String expr4 = "<math><piecewise>" +
                "<piece>" +
                    "<cn>2</cn>" +
                    "<apply>" +
                        "<and/>" +
                        "<apply><gt/><ci>a</ci><cn>0</cn></apply>" +
                        "<piecewise>" +
                            "<piece>" +
                                "<cn>1</cn>" +
                                "<apply><lt/><ci>b</ci><cn>0</cn></apply>"  +
                            "</piece>" +
                            "<piece>" +
                                "<cn>0</cn>" +
                                "<apply><gt/><ci>b</ci><cn>0</cn></apply>" +
                            "</piece>" +
                        "</piecewise>" +
                    "</apply>" +
                "</piece>" +
                "<piece>" +
                    "<cn>1</cn>" +
                    "<apply><lt/><ci>a</ci><cn>0</cn></apply>" +
                "</piece>" +
            "</piecewise></math>";
/*
         <math><piecewise>
         <piece>
         <piecewise>
         <piece>
         <cn>1</cn>
         <apply><lt/><ci>b</ci><cn>0</cn></apply>
         </piece>
         <piece>
         <cn>2</cn>
         <apply><gt/><ci>b</ci><cn>0</cn></apply>
         </piece>
         </piecewise>
         <apply>
         <eq/><ci>a</ci><cn>0</cn>
         </apply>
         </piece>
         <piece>
         <cn>3</cn>
         <apply><lt/><ci>a</ci><cn>0</cn></apply>
         </piece>
 */
    String expr5 = "<math><piecewise>" +
                "<piece>" +
                    "<piecewise>" +
                        "<piece>" +
                            "<cn>1</cn>" +
                            "<apply><lt/><ci>b</ci><cn>0</cn></apply>" +
                        "</piece>" +
                        "<piece>" +
                            "<cn>2</cn>" +
                            "<apply><gt/><ci>b</ci><cn>0</cn></apply>" +
                        "</piece>" +
                    "</piecewise>" +
                    "<apply>" +
                        "<eq/><ci>a</ci><cn>0</cn>" +
                    "</apply>" +
                "</piece>" +
                "<piece>" +
                    "<cn>3</cn>" +
                    "<apply><lt/><ci>a</ci><cn>0</cn></apply>" +
                "</piece>" +
            "</piecewise></math>";


    String expr6 =  "<math><apply>" +
                "<plus/>" +
                "<piecewise>" +
                    "<piece>" +
                        "<cn>1</cn>" +
                        "<apply>" +
                            "<gt/><ci>a</ci><cn>0</cn>" +
                        "</apply>" +
                    "</piece>" +
                    "<piece>" +
                        "<cn>3</cn>" +
                        "<apply><lt/><ci>a</ci><cn>0</cn></apply>" +
                    "</piece>" +
                "</piecewise>" +
                "<piecewise>" +
                    "<piece>" +
                        "<cn>1</cn>" +
                        "<apply>" +
                            "<gt/><ci>b</ci><cn>0</cn>" +
                        "</apply>" +
                    "</piece>" +
                    "<piece>" +
                        "<cn>2</cn>" +
                        "<apply>" +
                            "<lt/><ci>b</ci><cn>0</cn>" +
                        "</apply>" +
                    "</piece>" +
                "</piecewise>" +
            "</apply></math>";


    MathMLParser mlp = new MathMLParser();
    assertEquals(Parser.STATUS_OK, mlp.parse(expr1));
    ru.biosoft.math.model.AstStart start = mlp.getStartNode();

//    System.err.print("start = " + start);

    JavaFormatter matfmt = new JavaFormatter(null);
    String[] result = null;

    result = matfmt.format(start);

    System.err.print("declaration = " + result[0]);
    System.err.print("expression = " + result[1]);

    System.err.print("\n");

    assertEquals(Parser.STATUS_OK, mlp.parse(expr2));
    start = mlp.getStartNode();

    matfmt = new JavaFormatter(null);
//    matfmt = new MatlabFormatter();
    result = matfmt.format(start);

    System.err.print("declaration = " + result[0]);
    System.err.print("expression = " + result[1]);

    System.err.print("\n");

    assertEquals(Parser.STATUS_OK, mlp.parse(expr3));
    start = mlp.getStartNode();

    matfmt = new JavaFormatter(null);
//    matfmt = new MatlabFormatter();
    result = matfmt.format(start);

    System.err.print("declaration = " + result[0]);
    System.err.print("expression = " + result[1]);

    System.err.print("\n");

    assertEquals(Parser.STATUS_OK, mlp.parse(expr4));
    start = mlp.getStartNode();

    matfmt = new JavaFormatter(null);
//    matfmt = new MatlabFormatter();
    result = matfmt.format(start);

    System.err.print("declaration = " + result[0]);
    System.err.print("expression = " + result[1]);

    System.err.print("\n");

    assertEquals(Parser.STATUS_OK, mlp.parse(expr5));
    start = mlp.getStartNode();

    matfmt = new JavaFormatter(null);
//    matfmt = new MatlabFormatter();
    result = matfmt.format(start);

    System.err.print("declaration = " + result[0]);
    System.err.print("expression = " + result[1]);

    System.err.print("\n");

    assertEquals(Parser.STATUS_OK, mlp.parse(expr6));
    start = mlp.getStartNode();

    matfmt = new JavaFormatter(null);
//    matfmt = new MatlabFormatter();
    result = matfmt.format(start);

    System.err.print("declaration = " + result[0]);
    System.err.print("expression = " + result[1]);

    System.err.print("\n");
    }
}