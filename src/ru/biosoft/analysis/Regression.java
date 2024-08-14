package ru.biosoft.analysis;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;

import Jama.Matrix;

public class Regression implements Comparable<Regression>
{
    private double[] coefficients = new double[] {0, 0};

    private double[] pvalue = new double[] {1, 1};

    private double sse = 10000;

    private double quality = 0;

    private double error = 0;

    private double[] modelValues = new double[0];

    private double[] y = new double[0];

    public Regression()
    {

    }

    public Regression(double[] y, double[] ... x)
    {
        if( x == null )
            throw new IllegalArgumentException("At least one argument required");
        for( double[] sample : x )
            if( sample.length != y.length )
                throw new IllegalArgumentException("samples lengths must be equal");
        calculateRegression(y, x);

    }

    public Regression(double[] y, double[] x, int power) throws IllegalArgumentException
    {
        if( power <= 0 )
            throw new IllegalArgumentException("Not positive power");
        if( y.length != x.length )
            throw new IllegalArgumentException("Y and X dimensions must agree");

        double[][] poweredX = IntStreamEx.range( power )
                .mapToObj( i -> DoubleStreamEx.of( x ).map( xj -> Util.pow( xj, i + 1 ) ).toArray() ).toArray( double[][]::new );
        calculateRegression(y, poweredX);
    }

    private void calculateRegression(double[] y, double[][] x)
    {
        this.y = y;

        int n = y.length;
        int p = x.length;

        Matrix matrix = new Matrix(p + 1, n);

        matrix.setMatrix(0, 0, 0, n - 1, new Matrix(1, n, 1));
        matrix.setMatrix(1, p, 0, n - 1, new Matrix(x));

        Matrix matrixT = matrix.transpose();
        Matrix covariationMatrix = matrix.times(matrixT).inverse();
        Matrix valuesY = new Matrix(y, n);
        Matrix parameters = covariationMatrix.times(matrix.times(valuesY));

        coefficients = parameters.getColumnPackedCopy();

        Matrix modelValuesY = matrixT.times(parameters);

        modelValues = modelValuesY.getColumnPackedCopy();

        // calculating p-value
        Matrix m = valuesY.minus(modelValuesY);
        sse = m.transpose().times(m).get(0, 0);

        Matrix mean = new Matrix(n, 1, Stat.mean(y));
        m = valuesY.minus(mean);
        error = m.transpose().times(m).get(0, 0);

        quality = error / sse;

        pvalue = new double[p + 1];
        for( int i = 0; i < p + 1; i++ )
        {
            if( sse == 0 )
                pvalue[i] = 0;
            else
            {
                pvalue[i] = coefficients[i] / Math.sqrt(sse * covariationMatrix.get(i, i) / ( n - p - 1 ));
                pvalue[i] = Stat.studentDistribution(Math.abs(pvalue[i]), n - p - 1, 100)[1];
            }
        }
    }

    public double[] getPvalue()
    {
        return pvalue;
    }

    public double[] getCoefficients()
    {
        return coefficients;
    }

    public double getSSE()
    {
        return sse;
    }

    public double getError()
    {
        return error;
    }

    public double getQuality()
    {
        return quality;
    }

    public double[] getModelValues()
    {
        return modelValues;
    }

    public double[] getY()
    {
        return y;
    }

    @Override
    public Regression clone()
    {
        Regression result = new Regression();
        result.coefficients = this.coefficients;
        result.error = this.error;
        result.modelValues = this.modelValues;
        result.pvalue = this.pvalue;
        result.sse = this.sse;
        result.quality = this.quality;
        result.y = this.y;

        return result;
    }

    /**
     * Checks if parabola given by y = b*x + c*x^2 has it's peack in x[]
     */
    public static boolean checkParaboloidForm(double[] x, double b, double c)
    {
        return ( x[0] < b / ( 2 * c ) && b / ( 2 * c ) < x[x.length - 1] );
    }

    @Override
    public int compareTo(Regression r)
    {
        return Double.compare(getQuality(), r.getQuality());
    }

}