package ru.biosoft.analysis.aggregate;

import one.util.streamex.IntStreamEx;

public class Minimum extends NumericSelector
{
    @Override
    public String toString()
    {
        return "minimum";
    }

    @Override
    public int select(double[] values)
    {
        return IntStreamEx.ofIndices( values )
                .filter( idx -> !Double.isNaN( values[idx] ) )
                .minByDouble( idx -> values[idx] )
                .orElse( 0 );
    }
}
