package ru.biosoft.analysis.aggregate;

import java.util.Arrays;

public class Sum extends NumericAggregator
{
    @Override
    public double aggregate(double[] values)
    {
        if( isIgnoreNaNs() )
            return Arrays.stream( values ).filter( d -> !Double.isNaN( d ) ).sum();
        return Arrays.stream( values ).sum();
    }

    @Override
    public String toString()
    {
        return "sum";
    }
}
