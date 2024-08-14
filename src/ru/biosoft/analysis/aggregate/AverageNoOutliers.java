package ru.biosoft.analysis.aggregate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

public class AverageNoOutliers extends NumericAggregator
{
    private static final double OUTLIERS_FRACTION = 0.2;

    @Override
    public double aggregate(double[] values)
    {
        DoubleStream valuesStream;
        int size;
        if( isIgnoreNaNs() )
        {
            List<Double> valuesList = new ArrayList<>();
            for( int i = 0; i < values.length; i++ )
            {
                double val = values[i];
                if( !Double.isNaN( val ) )
                    valuesList.add( val );
            }
            size = valuesList.size();
            valuesStream = valuesList.stream().mapToDouble( Double::doubleValue );
        }
        else
        {
            size = values.length;
            valuesStream = Arrays.stream( values );
        }
        int nOutliers = size <= 1 / OUTLIERS_FRACTION ? 0 : (int)Math.ceil( size * OUTLIERS_FRACTION / 2 );
        return valuesStream.sorted().skip( nOutliers ).limit( values.length - 2 * nOutliers ).average().orElse( Double.NaN );
    }

    @Override
    public String toString()
    {
        return "average w/o 20% outliers";
    }
}
