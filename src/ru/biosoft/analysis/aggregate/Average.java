package ru.biosoft.analysis.aggregate;

import java.util.Arrays;

public class Average extends NumericAggregator
{
    @Override
    public double aggregate(double[] values)
    {
        if( isIgnoreNaNs() )
        {
            double sum = 0;
            int n = 0;
            for( double v : values )
                if( Double.isFinite( v ) )
                {
                    sum += v;
                    n++;
                }
            return n > 0 ? sum / n : Double.NaN;
        }
        return Arrays.stream( values ).average().orElse( Double.NaN );
    }

    @Override
    public String toString()
    {
        return "average";
    }
}
