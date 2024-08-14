package ru.biosoft.analysis.optimization.methods;

public class OptimizationMethodUtils
{
    public static double relativeError(double[] relativeValues, double[] exactValues)
    {
        if( relativeValues.length != exactValues.length )
            return Double.POSITIVE_INFINITY;

        double norm = 0;
        double diffNorm = 0;

        for( int i = 0; i < relativeValues.length; ++i )
        {
            diffNorm += Math.pow(exactValues[i] - relativeValues[i], 2);
            norm += Math.pow(relativeValues[i], 2);
        }

        diffNorm = Math.sqrt(diffNorm);
        norm = Math.sqrt(norm);

        return diffNorm / norm;
    }

    public static double absoluteError(double[] relativeValues, double[] exactValues)
    {
        if( relativeValues.length != exactValues.length )
            return Double.POSITIVE_INFINITY;

        double diffNorm = 0;

        for( int i = 0; i < relativeValues.length; ++i )
        {
            double diff = exactValues[i] - relativeValues[i];
            diffNorm += diff*diff;
        }

        return Math.sqrt(diffNorm);
    }
}
