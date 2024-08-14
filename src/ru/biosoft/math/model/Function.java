package ru.biosoft.math.model;

/**
 * Any function or operator.
 *
 * @pending - refine priorities.
 */
public interface Function
{
    /////////////////////////////////////////////////////////////////
    // Function and operators priorities
    //

    public static final int LOGICAL_PRIORITY    = 1;  // and, or, xor
    public static final int RELATIONAL_PRIORITY = 2 ; // eq, neq, gt, lt, geq, leq
    public static final int PLUS_PRIORITY       = 3 ; // plus, minus
    public static final int TIMES_PRIORITY      = 4 ; // times, divide
    public static final int UNARY_PRIORITY      = 5 ; // not, unary minus
    public static final int POWER_PRIORITY      = 6 ; // power
    public static final int FUNCTION_PRIORITY   = 7 ; // misc: sqrt, sin, cos, ...
    public static final int ASSIGNMENT_PRIORITY = 8 ; // =

    /////////////////////////////////////////////////////////////////
    // Properties
    //

    /** Returns the name of the node (operator symbol or function name). */
    public String getName();

    /** Returns the function or operator priority. */
    public int getPriority();

    /**
     * Returns the number of required parameters, or -1 if any number of
     * parameters is allowed.
     */
    public int getNumberOfParameters();
}