package biouml.plugins.modelreduction._test;

import ru.biosoft.analysis.Util;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.plugins.modelreduction.AnalysisUtils;
import biouml.plugins.modelreduction.StoichiometricAnalysis;
import biouml.plugins.modelreduction.StoichiometricMatrixDecomposition;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestStoichiometricMatrixDecomposition extends TestCase
{
    public TestStoichiometricMatrixDecomposition(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestStoichiometricMatrixDecomposition.class);
        return suite;
    }

    /**
     * Checks the equality N = L * N_R,
     * where N is a stoichiometric matrix of the diagram,
     * N_R is a reduced stoichiometry,
     * L is a linking matrix between N and N_R.
     */
    public void testMassConservationAnalysis() throws Exception
    {
        Diagram diagram = TestUtils.createTestDiagram_1();

        TableDataCollection stoichiometry = StoichiometricAnalysis.getStoichiometricMatrix(diagram);
        StoichiometricMatrixDecomposition smd = new StoichiometricMatrixDecomposition(stoichiometry);

        double[][] N = AnalysisUtils.getMatrix(stoichiometry);
        double[][] N_R = smd.getReducedStoichiometry();
        double[][] L = smd.getLinkMatrix();

        double[][] multiplication = Util.matrixMultiply(L, N_R);

        assertEquals(true, TestUtils.compare(N, multiplication, 1E-9));
    }
}
