package ru.biosoft.math.model;

/**
 * Predefined functions and operators.
 */
public class UndeclaredFunction implements Function
{
    public UndeclaredFunction(String name, int priority)
    {
        if( name.charAt(0) == '"' )
            name = name.substring(1, name.length()-1);

        this.name = name;
        this.priority = priority;
    }

    /////////////////////////////////////////////////////////////////
    // Properties
    //

    private String name;
    private int priority;

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

    @Override
    public int getNumberOfParameters()
    {
        return -1;
    }
}