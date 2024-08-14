package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Util;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GaussJordanEliminationTest extends TestCase
{
    public GaussJordanEliminationTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(GaussJordanEliminationTest.class);
        return suite;
    }

    public void testGaussJordanElimination1() throws Exception
    {
        double[][] A = new double[][] { {0, 2, 1, 4}, {1, 1, 2, 6}, {2, 1, 1, 7}};

        //Result elimination M * A = R
        double[][] R = new double[][] { {1, 1, 2, 6}, {0, 1, 0, 1.4}, {0, 0, 1, 1.2}};
        double[][] P = new double[][] { {0, 1, 0}, {1, 0, 0}, {0, 0, 1}};
        double[][] M = new double[][] { {0, 1, 0}, {0.6, -0.4, 0.2}, { -0.2, 0.8, -0.4}};

        Util.GaussJordanElimination elimination = Util.getGaussJordanElimination(A);

        assertEquals(true, isSolutionOk(R, elimination.getR()) && isSolutionOk(P, elimination.getP())
                && isSolutionOk(M, elimination.getM()));
    }

    public void testGaussJordanElimination2() throws Exception
    {
        double[][] A = new double[][] { {2, -5, 4}, { -1, 2.5, -2}, {4, -10, 8}};

        //Result elimination M * A = R
        double[][] R = new double[][] { {1, -2.5, 2}, {0, 0, 0}, {0, 0, 0}};
        double[][] P = new double[][] { {1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        double[][] M = new double[][] { {0.5, 0, 0}, {0.5, 1, 0}, { -2, 0, 1}};

        Util.GaussJordanElimination elimination = Util.getGaussJordanElimination(A);

        assertEquals(true, isSolutionOk(R, elimination.getR()) && isSolutionOk(P, elimination.getP())
                && isSolutionOk(M, elimination.getM()));
    }

    private boolean isSolutionOk(double[][] solution, double[][] calculatedResult)
    {
        for( int i = 0; i < calculatedResult.length; ++i )
        {
            for( int j = 0; j < calculatedResult[0].length; ++j )
            {
                if( solution[i][j] != calculatedResult[i][j] )
                {
                    return false;
                }
            }
        }
        return true;
    }
}
