
package ru.biosoft.math._test;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.parser.Parser;

public class LinearFormatterTest extends TestCase
{
    static String endl = System.getProperty("line.separator");

    public LinearFormatterTest(String name)
    {
        super(name);
        File configFile = new File( "./ru/biosoft/math/_test/test.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public void testConstant() throws Exception
    {
        test("1");
        test("1.0");
        test("1.0E10");
        test("pi");
        test("abc");
        test("true");
    }

    public void testIdentifier() throws Exception
    {
        test("a");
        test("$\"a\"");
        test("$\"4\"");
        test("$\"pkc inactive\"");
        test("$\"a(*,1,2).*\"");
    }

    public void testFunction() throws Exception
    {
        // Relational operators
        test("a > 1");
        test("a < 1");
        test("a == 1");
        test("a <= 1");
        test("a >= 1");
        test("a != 1");

        // Logical operators
        test("(a == 1) || (b != 2) || (c <= 3)");
        test("(a == 1) && (b != 2) && (c <= 3)");

        // Arithmetic operators
        test("1 + 2");
        test("1 + 2 + 3");
        test("1*2");
        test("1/2");
        test("1^2");

        // Unary operators
        test("!a");

        // functions
        test("f()",         new Function[] { new PredefinedFunction("f", Function.FUNCTION_PRIORITY, 0)} );
        test("f(1)",        new Function[] { new PredefinedFunction("f", Function.FUNCTION_PRIORITY, 1)} );
        test("f(a + 1)",    new Function[] { new PredefinedFunction("f", Function.FUNCTION_PRIORITY, 1)} );
        test("f(1, 2, 3)",  new Function[] { new PredefinedFunction("f", Function.FUNCTION_PRIORITY, 3)} );
        test("f(1, z(y))",  new Function[] { new PredefinedFunction("f", Function.FUNCTION_PRIORITY, 2),
                                             new PredefinedFunction("z", Function.FUNCTION_PRIORITY, 1)} );
    }

    public void testParenthis() throws Exception
    {
        test("1 + 2*3");
        test("(1 + 2)*3");
        test("(1 + 2)*3 + (4 - 5)^6");

        test("-1 + 2");
        test("1 + (-2)");

        test("-1 + sin(-2) - 3/(-4) + (-5)");

        test("a + (-b)");
        test("a + (-b)*c");
        test("-b*c");
        test("-b*c + a");

        test("a/(-b)*c"); // PENDING

        test("a/b*c");
        test("a/(b*c)"); // PENDING

        test("(a + b)/(a + 1)*(1 + b/c)");
        test("(a + b)/((a + 1)*(1 + b/c))");
    }

    public void testDiff() throws Exception
    {
        test("diff(x, t) = sin(x) + t");
        test("diff(x, t) = (a + b)*c");
        test("diff(x, t) = -1");
        test("diff(x, t) = -a^5");
    }

    public void testPiecewise() throws Exception
    {
        test("piecewise( a > 0 => 1 )");
        test("piecewise( a > 0 => 1; 2 )");

        test("piecewise( a > 0 => 1; a > 1 => 2 )");
        test("piecewise( a > 0 => 1; a > 1 => 2; 3 )");

        test("piecewise( a > 0 => 1; a > 1 => 2; a > 2 => 3 )");
        test("piecewise( a > 0 => 1; a > 1 => 2; a > 2 => 3; 4 )");
    }

    public void testNestedPiecewise() throws Exception
    {
        test("piecewise( piecewise( a > 0 => true ) => 1 )" );
        test("piecewise( piecewise( a > 0 => true; false ) => 1 )" );
        test("piecewise( piecewise( a > 0 => true; false ) => 1; 2 )" );

        test("piecewise( a > 0 => piecewise( b > 0 => true; false ) )") ;
        test("piecewise( a > 0 => piecewise( b > 0 => true; false ); false )");
        test("piecewise( a > 0 => true; piecewise( b > 0 => true; false ) )");
    }

    public void testPiecewiseError() throws Exception
    {
        catchError("piecewise( )" );
        catchError("piecewise( 1 )" );
        catchError("piecewise( 1; a > 0 => 1 )" );
        catchError("piecewise( a > 0 => 1; 2; a > 2 => 3; )" );
    }

    public void testFunctionDeclaration() throws Exception
    {
        test("function f() = 1");
        test("function f(x) = x + 1");
        test("function f(x, y) = x + y");
    }

    public void testAssignment() throws Exception
    {
        test("a = b");
        test("a = sin(b)");

        test("x = piecewise( a < 0 => -a; a == 0 => 0; a )");
    }

    void test(String expression) throws Exception
    {
        test(expression, null);
    }

    void test(String expression, Function[] predefinedFunctions) throws Exception
    {
        Parser parser = new Parser();

        if( predefinedFunctions != null )
        {
            for(int i=0; i<predefinedFunctions.length; i++)
                parser.getContext().declareFunction(predefinedFunctions[i]);
        }

        int status = parser.parse(expression);
        if( status > Parser.STATUS_WARNING )
            throw new Exception("There were errors during parsing: " + parser.getMessages());

        AstStart start = parser.getStartNode();
        String[] result =  (new LinearFormatter()).format(start);

        if( !expression.equals(result[1]) )
        {
            System.out.println("Expression: " + expression);
            System.out.println("Result    : " + result[1]);
            assertEquals(expression, result[1]);
        }

//System.out.println(result[1]);
    }

    void catchError(String expression) throws Exception
    {
        Parser parser = new Parser();

        int status = parser.parse(expression);
        assertTrue("Error is missed, expression: " + expression, status > Parser.STATUS_OK);
    }
}


