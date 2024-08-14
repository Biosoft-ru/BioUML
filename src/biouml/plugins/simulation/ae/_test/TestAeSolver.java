package biouml.plugins.simulation.ae._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.plugins.simulation.ae.AeConjugateGradientSolver;
import biouml.plugins.simulation.ae.AeLevenbergMarquardSolver;
import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ae.AeNelderMeadSolver;
import biouml.plugins.simulation.ae.AeSolver;
import biouml.plugins.simulation.ae.KinSolverWrapper;
import biouml.plugins.simulation.ae.NewtonSolverWrapper;
import biouml.plugins.simulation.ae.NewtonSolverWrapperEx;
import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;

public class TestAeSolver extends AbstractBioUMLTest
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAeSolver.class.getName());
        suite.addTest(new TestAeSolver("testNewtonSolver"));
        suite.addTest(new TestAeSolver("testNewtonSolverEx"));
        suite.addTest(new TestAeSolver("testKinSolver"));
        suite.addTest(new TestAeSolver("testAeConjugateGradientSolver"));
        suite.addTest(new TestAeSolver("testAeLevenbergMarquardSolver"));
        suite.addTest(new TestAeSolver("testAeNelderMeadSolver"));
        return suite;
    }
    
    public TestAeSolver(String test)
    {
        super(test);
    }
    
    private AeModel model;
    private double[] initialGuess;
    private double TOLERANCE = 1E-6;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        model = new Model();
        initialGuess = new double[] {0, 1};
    }

    private void testSolver(AeSolver solver) throws Exception
    {
        double[] result = solver.solve( initialGuess, model );
        assertTrue("solver "+solver.toString()+" failed to produce any solution: "+solver.getMessage(), solver.isSuccess());
        double max = DoubleStreamEx.of(model.solveAlgebraic( result )).max().getAsDouble();
        assertEquals( 0, max, TOLERANCE );
    }

    public void testNewtonSolver() throws Exception
    {
        testSolver( new NewtonSolverWrapper() );
    }

    public void testNewtonSolverEx() throws Exception
    {
        testSolver( new NewtonSolverWrapperEx() );
    }

    public void testKinSolver() throws Exception
    {
        testSolver( new KinSolverWrapper() );
    }

    public void testAeConjugateGradientSolver() throws Exception
    {
        testSolver( new AeConjugateGradientSolver() );
    }

    public void testAeLevenbergMarquardSolver() throws Exception
    {
        testSolver( new AeLevenbergMarquardSolver() );
    }

    public void testAeNelderMeadSolver() throws Exception
    {
        testSolver( new AeNelderMeadSolver() );
    }

    private static class Model implements AeModel
    {

        public double compartment;
        public double compartment_S1;
        public double compartment_S2;
        public double compartment_X0;
        public double compartment_X1;
        public double compartment_X2;
        public double J0_v0;
        public double J1_k3;
        public double J2_c;
        public double J2_k1;
        public double J2_k_1;
        public double J2_q;
        public double J3_k2;

        {
            compartment = 1.0; // initial value of $compartment
            compartment_S1 = 0.0; // initial value of $compartment.S1
            compartment_S2 = 1.0; // initial value of $compartment.S2
            compartment_X0 = 1.0; // initial value of $compartment.X0
            compartment_X1 = 0.0; // initial value of $compartment.X1
            compartment_X2 = 0.0; // initial value of $compartment.X2
            J0_v0 = 8.0; // initial value of J0_v0
            J1_k3 = 0.0; // initial value of J1_k3
            J2_c = 1.0; // initial value of J2_c
            J2_k1 = 1.0; // initial value of J2_k1
            J2_k_1 = 0.0; // initial value of J2_k_1
            J2_q = 3.0; // initial value of J2_q
            J3_k2 = 5.0; // initial value of J3_k2
        }


        @Override
        public double[] solveAlgebraic(double[] z)
        {
            /*double S1 = z[0];
            double S2 = z[1];

            double J0_v0 = 8.0;
            double J1_k3 = 0.0;
            double J2_k1 = 1;
            double J2_k_1 = 0;
            double J2_c = 1;
            double J2_q = 3;
            double J3_k2 = 5;

            double eq1 = J0_v0 - J1_k3*S1 - (J2_k1*S1 - J2_k_1*S2)*(1.0 + J2_c*Math.pow( S2, J2_q));
            double eq2 = (J2_k1*S1 - J2_k_1*S2)*(1.0 + J2_c*Math.pow(S2,J2_q)) -J3_k2*S2;

            return new double[] {eq1, eq2};*/
            compartment_S1 = z[0];
            compartment_S2 = z[1];
            final double [] result = new double [2];
            result[0] = J0_v0 + (-(J1_k3*(compartment_S1/compartment))) + (-((J2_k1*(compartment_S1/compartment) - J2_k_1*(compartment_S2/compartment))*(1.0 + J2_c*Math.pow((compartment_S2/compartment), J2_q))));
            result[1] = (J2_k1*(compartment_S1/compartment) - J2_k_1*(compartment_S2/compartment))*(1.0 + J2_c*Math.pow((compartment_S2/compartment), J2_q)) + (-(J3_k2*(compartment_S2/compartment)));
            return result;
        }


        @Override
        public double[] getConstraints()
        {
            return null;
        }

    }
}
