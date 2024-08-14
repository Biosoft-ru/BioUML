package ru.biosoft.math.model._test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.Parser;
import ru.biosoft.math.parser.ParserTreeConstants;

public class UtilsTest extends TestCase
{
    LinearFormatter linearFormatter = new LinearFormatter();
    
    public UtilsTest(String s)
    {
        super(s);
    }

    private void testSubstitute(String expression, Map<String, String> mapping) throws Exception
    {
        Parser parser = new Parser();
        int status = parser.parse(expression);

        if( status > ru.biosoft.math.model.Parser.STATUS_WARNING )
            throw new Exception("There were errors during parsing: " + parser.getMessages());

        AstStart startNode = parser.getStartNode();
        AstStart substNode = Utils.substituteVars(startNode, mapping);
        
        String start = linearFormatter.format(startNode)[1];
        String subst = linearFormatter.format(substNode)[1];
        
        String s = start;
        for (Map.Entry<String, String> e : mapping.entrySet())
        {
            s = s.replaceAll(e.getKey(), e.getValue());
        }
        assertEquals(subst, s);
    }
    
    private void testCopy(String expression) throws Exception
    {
        Parser parser = new Parser();
        int status = parser.parse(expression);

        if( status > ru.biosoft.math.model.Parser.STATUS_WARNING )
            throw new Exception("There were errors during parsing: " + parser.getMessages());

        AstStart startNode = parser.getStartNode();
        Node copy = Utils.cloneAST(startNode);
        assertTrue(Utils.equalsAST(copy, startNode));
    }

    private void testApply(String expression1, String expression2) throws Exception
    {
        Parser parser = new Parser();
        int status1 = parser.parse(expression1);
        if( status1 > ru.biosoft.math.model.Parser.STATUS_WARNING )
            throw new Exception("There were errors during parsing: " + parser.getMessages());

        AstStart start1 = parser.getStartNode();

        int status2 = parser.parse(expression2);
        if( status2 > ru.biosoft.math.model.Parser.STATUS_WARNING )
            throw new Exception("There were errors during parsing: " + parser.getMessages());

        AstStart start2 = parser.getStartNode();

        Node result = Utils.applyFunction(start1, start2,
                new PredefinedFunction(DefaultParserContext.PLUS, Function.PLUS_PRIORITY, -1));

        AstStart start = new AstStart(ParserTreeConstants.JJTSTART);
        start.jjtAddChild(result, 0);
        
        String[] format1 = linearFormatter.format(start1);
        String[] format2 = linearFormatter.format(start2);
        String[] formatResult = linearFormatter.format(start);
        
        assertEquals(format1[1] + " + " + format2[1], formatResult[1]);
    }

    public void testApplyFunction() throws Exception
    {
        testApply("x*y", "z*10");
    }

    public void testCloneAST() throws Exception
    {
        
         testCopy("x*y");
         testCopy("x*y + sin(x)");
         testCopy("x*y + sin(x*x^2)");
         testCopy("x*y*z*t + sin(x*x^2) - x^3");
         
         testCopy("1 + 2*3");
         testCopy("(1 + 2)*3");
         testCopy("(1 + 2)*3 + (4 - 5)^6");
         
         testCopy("-1 + 2");
         testCopy("1 + (-2)");
         
         testCopy("-1 + sin(-2) - 3/(-4) + (-5)");
         
         testCopy("a + (-b)");
         testCopy("a + (-b)*c");
         testCopy("-b*c");
         testCopy("-b*c + a");
         
         testCopy("a/(-b)*c");
         
         testCopy("a/b*c");
         testCopy("a/(b*c)");
         
         testCopy("(a + b)/(a + 1)*(1 + b/c)");
         testCopy("(a + b)/((a + 1)*(1 + b/c))");

        testCopy("piecewise( a > 0 => 1 )");
        testCopy("piecewise( a > 0 => 1; 2 )");

        testCopy("piecewise( a > 0 => 1; a > 1 => 2 )");
        testCopy("piecewise( a > 0 => 1; a > 1 => 2; 3 )");

        testCopy("piecewise( a > 0 => 1; a > 1 => 2; a > 2 => 3 )");
        testCopy("piecewise( a > 0 => 1; a > 1 => 2; a > 2 => 3; 4 )");

        testCopy("piecewise( piecewise( a > 0 => true ) => 1 )" );
        testCopy("piecewise( piecewise( a > 0 => true; false ) => 1 )" );
        testCopy("piecewise( piecewise( a > 0 => true; false ) => 1; 2 )" );

        testCopy("piecewise( a > 0 => piecewise( b > 0 => true; false ) )") ;
        testCopy("piecewise( a > 0 => piecewise( b > 0 => true; false ); false )");
        testCopy("piecewise( a > 0 => true; piecewise( b > 0 => true; false ) )");
        
        testCopy("function f() = 1");
        testCopy("function f(x) = x + 1");
        testCopy("function f(x, y) = x + y");

        testCopy("a = b");
        testCopy("a = sin(b)");
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(Utils.class.getName());
        suite.addTest(new UtilsTest("testCloneAST"));
        suite.addTest(new UtilsTest("testApplyFunction"));
        return suite;
    }

    public void testSubstituteVars() throws Exception
    {
        Map<String, String> m = new HashMap<>();
        m.put("x", "Q");
        testSubstitute("x*y*z*t + sin(x*x^2) - x^3", m);
        
        m.clear();
        m.put("x", "Q");
        m.put("z", "P");
        m.put("t", "W");
        testSubstitute("x*y*z*t + sin(x*x^2) - x^3", m);
    }
    
    public void testPruneFunction() throws Exception
    {
        checkPrune("1.0/2", "0.5");
        checkPrune("(1.0+1)/2", "1.0");
        checkPrune("(1.0*2)/2", "1.0");
        checkPrune("exp(0)", "1.0");
        checkPrune("sin(0)", "0.0");
        checkPrune("cos(0)", "1.0");
        checkPrune("arcsin(1)", String.valueOf(Math.PI/2));
        checkPrune("arccos(0)", String.valueOf(Math.PI/2));
        checkPrune("exp(3-4+1)+1", "2.0");
        checkPrune("sin(sin(0*1+(4^6)*0))", "0.0");
        checkPrune( "piecewise( 1 > 2 => 1; 1 < 2 => 1 + 1; 3 )", "2.0" );
        checkPrune( "piecewise( a > 2 => 1; 3 )", "piecewise( a > 2 => 1; 3 )" );
    }

    private void checkPrune(String expression, String expectedResult) throws Exception
    {
        Parser parser = new Parser();
        
        int status = parser.parse(expression);

        if( status > ru.biosoft.math.model.Parser.STATUS_WARNING )
            throw new Exception("There were errors during parsing: " + parser.getMessages());

        AstStart startNode = parser.getStartNode();

        Utils.pruneFunctions(startNode);
        String[] formatResult = linearFormatter.format(startNode);
        assertEquals(expectedResult, formatResult[1]);
    }

    public void testOptimizeDummyExpressions() throws Exception
    {
       checkOptimize("x+0", "x");
       checkOptimize("0+x", "x");
       checkOptimize("1*x", "x");
       checkOptimize("x*1", "x");
       checkOptimize("x*0.", "0.0");
       checkOptimize("0.*x", "0.0");
       checkOptimize("0./x", "0.0");
       checkOptimize("x/1.", "x");
       checkOptimize("1.^x", "1.0");
       checkOptimize("x^1.", "x");
       
       checkOptimize("x/x", "1.0");
       checkOptimize("3/3", "1.0");
       checkOptimize("(x*x+sin(1))/(x*x+sin(1))", "1.0");
       
       checkOptimize("1*(x+0)/1", "x");
       checkOptimize("0.*(x+0)/1", "0.0");
    }

    private void checkOptimize(String expression, String expectedResult) throws Exception
    {
        Parser parser = new Parser();
        
        int status = parser.parse(expression);

        if( status > ru.biosoft.math.model.Parser.STATUS_WARNING )
            throw new Exception("There were errors during parsing: " + parser.getMessages());

        AstStart startNode = parser.getStartNode();

        Utils.optimizeDummyExpressions(startNode);
        String[] formatResult = linearFormatter.format(startNode);
        assertEquals(expectedResult, formatResult[1]);
    }
    
    
}
