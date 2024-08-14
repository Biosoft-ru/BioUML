

package biouml.plugins.simulation._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import junit.framework.TestSuite;
import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ae.NewtonSolver;

public class NewtonSolverTest extends AbstractBioUMLTest
{
    public NewtonSolverTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(ResultTransformerTest.class.getName());
        suite.addTest(new NewtonSolverTest("test"));
        return suite;
    }


    public void test() throws Exception
    {
        Model model = new Model();
        double[] initialGuess = new double[] {0, 0, 0};
        NewtonSolver.solve(initialGuess, model);

        for( double val : initialGuess )
            System.out.print(val + "\t");
    }

    static class Model implements AeModel
    {

        @Override
        public double[] getConstraints()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public double[] solveAlgebraic(double[] z)
        {
            double[] result = new double[3];
            result[0] = z[0] + 2 * z[1];
            result[1] = z[1] - 2 * z[2];
            result[2] = z[2] - 1;
            return result;
        }
    }
}