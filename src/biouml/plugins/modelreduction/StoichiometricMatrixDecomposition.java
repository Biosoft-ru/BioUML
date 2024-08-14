package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.analysis.Util;
import ru.biosoft.analysis.Util.GaussJordanElimination;
import ru.biosoft.table.TableDataCollection;

public class StoichiometricMatrixDecomposition
{
    public StoichiometricMatrixDecomposition(TableDataCollection stoichiometry)
    {
        this.sourceStoichiometry = stoichiometry;
        getDecomposition(stoichiometry);
    }

    private final TableDataCollection sourceStoichiometry;
    public TableDataCollection getSourceStoichiometry()
    {
        return this.sourceStoichiometry;
    }

    private double[][] reducedStoichiometry;
    public double[][] getReducedStoichiometry()
    {
        return this.reducedStoichiometry;
    }

    private double[][] linkMatrix;
    public double[][] getLinkMatrix()
    {
        return this.linkMatrix;
    }

    private List<String> independentSpecies;
    public List<String> getIndependentSpecies()
    {
        return this.independentSpecies;
    }

    private List<String> dependentSpecies;
    public List<String> getDependentSpecies()
    {
        return this.dependentSpecies;
    }

    /**
     * Decomposes the stoichiometric matrix N into the product of the reduced matrix N_R
     * consisting of linearly independent rows of the matrix N and the link matrix L
     * so that N = L * N_R
     */
    private void getDecomposition(TableDataCollection stoichiometry)
    {
        if( stoichiometry == null )
            return;

        double[][] matrix = AnalysisUtils.getMatrix(stoichiometry);
        GaussJordanElimination elimination = Util.getGaussJordanElimination(matrix);

        getSpeciesOrder(stoichiometry, elimination);
        reducedStoichiometry = getReducedStoichiometry(stoichiometry);
        linkMatrix = getLinkMatrix(stoichiometry, elimination);
    }

    /**
     * Generates a list of linearly independent and a list of linearly dependent species detected by the Gauss-Jordan elimination.
     * Order of the dependent species can be obtained by permuting the original species order
     * (represented in the rows of the stoichiometric matrix) with the permutation matrix P.
     */
    private void getSpeciesOrder(TableDataCollection stoichiometry, GaussJordanElimination elimination)
    {
        double[][] P = elimination.getP();
        double[][] R = elimination.getR();

        int n = P.length;
        int num = getDependentSpeciesNumber(R);

        dependentSpecies = new ArrayList<>();
        for( int i = n - num; i < n; ++i )
        {
            int j;
            for( j = 0; j < n; ++j )
            {
                if( P[i][j] == 1 )
                    break;
            }
            dependentSpecies.add(stoichiometry.getName(j));
        }

        independentSpecies = new ArrayList<>();
        for( int i = 0; i < stoichiometry.getSize(); ++i )
        {
            String species = stoichiometry.getName(i);
            if( !dependentSpecies.contains(species) )
                independentSpecies.add(species);
        }
    }

    /**
     * This method calculates the number of zero rows in the matrix R.
     * These are the last rows of the matrix because it has
     * the upper triangular form after the Gauss-Jordan elimination.
     */
    private int getDependentSpeciesNumber(double[][] R)
    {
        int n = R.length;
        int m = R[0].length;

        int num = 0;
        for( int i = n - 1; i >= 0; --i )
        {
            int zeros = 0;
            for( int j = 0; j < m; ++j )
            {
                if( R[i][j] == 0.0 )
                    zeros++;
            }

            if( zeros == m )
                num++;
            else
                break;
        }
        return num;
    }

    private double[][] getReducedStoichiometry(TableDataCollection stoichiometry)
    {
        int n = independentSpecies.size();
        int m = stoichiometry.getColumnModel().getColumnCount();

        double[][] result = new double[n][m];

        for( int i = 0; i < stoichiometry.getSize(); ++i )
        {
            String species = stoichiometry.getName(i);

            if( independentSpecies.contains(species) )
            {
                for( int j = 0; j < m; ++j )
                {
                    result[independentSpecies.indexOf(species)][j] = (Integer)stoichiometry.getValueAt(i, j);
                }
            }
        }

        return result;
    }

    private double[][] getLinkMatrix(TableDataCollection stoichiometry, GaussJordanElimination elimination)
    {
        int n = stoichiometry.getSize();
        int m = independentSpecies.size();

        double[][] result = new double[n][m];

        double[][] M = elimination.getM();

        for( int i = 0; i < n; ++i )
        {
            String species = stoichiometry.getName(i);
            int ind = independentSpecies.indexOf(species);

            if( ind > -1 )
            {
                result[i][ind] = 1.0;
            }
            else
            {
                double[] factors = M[m + dependentSpecies.indexOf(species)];

                int index = 0;
                double coeff = 0;

                for( int j = 0; j < n; ++j )
                {
                    if( stoichiometry.getName(j).equals(species) )
                        coeff = factors[j];

                    if( independentSpecies.contains(stoichiometry.getName(j)) )
                    {
                        result[i][index++] = factors[j];
                    }
                }

                if( coeff != -1 )
                {
                    for( int j = 0; j < result[0].length; ++j )
                    {
                        if( result[i][j] != 0 )
                            result[i][j] /= -1 * coeff;
                    }
                }
            }
        }

        return result;
    }
}
