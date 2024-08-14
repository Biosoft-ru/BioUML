package ru.biosoft.analysis.aggregate;

import one.util.streamex.IntStreamEx;

public class Maximum extends NumericSelector
{
    @Override
    public int select(double[] values)
    {
        return IntStreamEx.ofIndices( values )
                .filter( idx -> !Double.isNaN( values[idx] ) )
                .maxByDouble( idx -> values[idx] )
                .orElse( 0 );
    }

    @Override
    public String toString()
    {
        return "maximum";
    }
}
