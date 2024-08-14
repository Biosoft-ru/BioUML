package biouml.model.dynamics._test;

import java.util.ArrayList;
import java.util.List;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.MathCalculator;
import biouml.model.dynamics.MathContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MathCalculatorTest extends TestCase
{
    private MathCalculator calculator = new MathCalculator();

    public MathCalculatorTest(String name)
    {
        super(name);
    }

    public void testFormula() throws Exception
    {
        String formula = "3 * a + 5 * b";

        MathContext values = new MathContext();
        values.put("a", 100.0);
        values.put("b", 50.0);

        double result = calculator.calculateMath(formula, values)[0];
        calculator.clearRepositories();

        assertEquals(result, 550.0);
    }

    public void testFormulaWithEquations() throws Exception
    {
        String formula = "3 * a + 5 * b";

        List<Equation> equations = new ArrayList<>();
        equations.add(new Equation(null, Equation.TYPE_SCALAR, "a", "20 + b"));
        equations.add(new Equation(null, Equation.TYPE_SCALAR, "b", "5"));

        calculator.addEquations(equations);
        double result = calculator.calculateMath(formula, new MathContext())[0];
        calculator.clearRepositories();

        assertEquals(result, 100.0);
    }

    public void testFormulaWithFunctionDeclarations() throws Exception
    {
        String formula = "3 * f(a, g(a)) + 5 * f(a, b)";

        Function f = new Function(null);
        f.setFormula("function f(x, y) = x * y");

        Function g = new Function(null);
        g.setFormula("function g(x) = x + c");

        MathContext values = new MathContext();
        values.put("a", 100.0);
        values.put("b", 50.0);
        values.put("c", 10.0);

        calculator.addFunctionDeclarations(new Function[] {f, g});
        double result = calculator.calculateMath(formula, values)[0];
        calculator.clearRepositories();

        assertEquals(result, 58000.0);
    }

    public void testComplexFormula() throws Exception
    {
        String formula = "(3 * f(a, b) + 5 * f(b, c)) / 100";

        Function f = new Function(null);
        f.setFormula("function f(x, y) = x * y + g(x)");

        Function g = new Function(null);
        g.setFormula("function g(x) = x + c");

        List<Equation> equations = new ArrayList<>();
        equations.add(new Equation(null, Equation.TYPE_SCALAR, "a", "20 + b"));
        equations.add(new Equation(null, Equation.TYPE_SCALAR, "b", "5"));

        MathContext values = new MathContext();
        values.put("c", 10.0);

        calculator.addFunctionDeclarations(new Function[] {f, g});
        calculator.addEquations(equations);
        double result = calculator.calculateMath(formula, values)[0];
        calculator.clearRepositories();

        assertEquals(result, 8.05);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MathCalculatorTest.class);
        return suite;
    }
}
