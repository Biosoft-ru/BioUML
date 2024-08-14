package ru.biosoft.math.model;

public class VariableType implements Comparable
{
    public String name;
    public String codeName;

    /**
     * Index in vector 'Y'
     */
    public int index;

    /**
     * Index in resulting solution (including scalar variables)
     */
    public int resultIndex;

    /**
     * Index in array of "historical" variables
     * (or -1 if the variable is not historical)
     */
    public int historicalIndex;

    
    public String delayedExpression;

    
    public String type;

    public VariableType(String name, String type)
    {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString()
    {
        return "Variable " + name + "(" + type + ")";
    }

    @Override
    public int compareTo(Object obj)
    {
        if( obj instanceof VariableType )
            obj = ( (VariableType)obj ).name;

        return name.compareTo((String)obj);
    }
}