package ru.biosoft.math.model;

/**
 * Predefined functions and operators.
 */
public class PredefinedFunction implements Function
{
    public PredefinedFunction(String name, int priority, int numberOfParameters)
    {
        if( name.charAt(0) == '"' )
            name = name.substring(1, name.length()-1);

        this.name = name;
        this.priority = priority;
        this.numberOfParameters = numberOfParameters;
    }

    /////////////////////////////////////////////////////////////////
    // Properties
    //

    private String name;
    private int priority;
    private int numberOfParameters;

    /** Returns the name of the node (operator symbol or function name). */
    @Override
    public String getName()
    {
        return name;
    }

    /** Returns the function or operator priority. */
    @Override
    public int getPriority()
    {
        return priority;
    }

    /**
     * Returns the number of required parameters, or -1 if any number of
     * parameters is allowed.
     */
    @Override
    public int getNumberOfParameters()
    {
        return numberOfParameters;
    }
}