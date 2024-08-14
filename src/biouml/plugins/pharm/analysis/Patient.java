package biouml.plugins.pharm.analysis;

import javax.annotation.Nonnull;

import one.util.streamex.DoubleStreamEx;

public class Patient implements Cloneable
{
    double[] input;
    double[] observed;

    String[] varPaths;
    double[] allValues;

    double likelihood;
    boolean invalid = false;
    
    public Patient(double[] input, double[] observed, boolean invalid)
    {
        this.input = input;
        this.observed = observed;
        this.invalid = invalid;
    }
    
    public Patient(double[] input, double[] observed)
    {
        this(input, observed, false);
    }
    
    public Patient(double[] input, double[] observed, double[] allValues)
    {
        this(input, observed);
        this.allValues = allValues;
    }

    public double[] getAllValues()
    {
        return allValues;
    }
    
    public @Nonnull Object[] getValues()
    {
        Object[] result = new Object[input.length + observed.length];
        System.arraycopy(input, 0, result, 0, input.length);
        System.arraycopy(observed, 0, result, input.length, observed.length);
        return result;
    }

    public double[] getObserved()
    {
        return observed;
    }
    
    public double[] getInput()
    {
        return input;
    }

    @Override
    public String toString()
    {
        return new StringBuilder("Input:\t").append(DoubleStreamEx.of(input).joining("\t")).append("\tOutput:\t")
                .append(DoubleStreamEx.of(observed).joining("\t")).toString();
    }
    
    public void invalidate()
    {
        this.invalid = true;
    }
}