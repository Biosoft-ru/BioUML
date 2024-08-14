package ru.biosoft.analysis.aggregate;

/**
 * Class containing method to aggregate list of double values into single value
 */
public abstract class NumericAggregator
{
    public abstract double aggregate(double[] values);
    @Override
    public abstract String toString();

    private static final NumericAggregator[] aggregators = new NumericAggregator[] {
        new Extreme(),
        new Minimum(),
        new Maximum(),
        new Average(),
        new AverageNoOutliers(),
        new Sum()
    };
    
    protected boolean ignoreNaNs = true;
    public boolean isIgnoreNaNs()
    {
        return ignoreNaNs;
    }
    public void setIgnoreNaNs(boolean ignoreNaNs)
    {
        this.ignoreNaNs = ignoreNaNs;
    }

    public static NumericAggregator[] getAggregators()
    {
        return aggregators.clone();
    }
    
    public static NumericAggregator createInstance(String name)
    {
        for(NumericAggregator aggregator: aggregators)
        {
            if(aggregator.toString().equals(name))
                return aggregator;
        }
        return null;
    }
}
