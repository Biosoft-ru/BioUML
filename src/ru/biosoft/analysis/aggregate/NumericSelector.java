package ru.biosoft.analysis.aggregate;

import one.util.streamex.StreamEx;

/**
 * Subtype of {@link NumericAggregator} which has the property that returned value belongs to list of source values
 */
public abstract class NumericSelector extends NumericAggregator
{
    abstract public int select(double[] values);
    
    @Override
    public double aggregate(double[] values)
    {
        return values.length > 0 ? values[select( values )] : Double.NaN;
    }

    private static NumericSelector[] selectors;
    private static void init()
    {
        selectors = StreamEx.of( getAggregators() ).select( NumericSelector.class ).toArray( NumericSelector[]::new );
    }
    
    public static NumericSelector[] getSelectors()
    {
        if(selectors == null) init();
        return selectors.clone();
    }
}
