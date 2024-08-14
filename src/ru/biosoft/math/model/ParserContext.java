
package ru.biosoft.math.model;

public interface ParserContext
{
    ///////////////////////////////////////////////////////////////////
    // Constant issues
    //

    public boolean containsConstant(String name);

    /**
     * Returns the constant value or null if constant not declared.
     */
    public Object getConstantValue(String name);

    /** Declares the constant. */
    public void declareConstant(String name, Object value);
    
    public void removeConstant(String name);

    ///////////////////////////////////////////////////////////////////
    // Variable issues
    //

    public boolean containsVariable(String name);

    /**
     * Returns value of variable with the specified name or null
     * if variable is not declared.
     */
    public Object getVariableValue(String name);

    /** Declares the variable. */
    public void declareVariable(String name, Object value);

    /** returns true if variable with given name can be declared in current context*/
    public boolean canDeclare(String value);

    ///////////////////////////////////////////////////////////////////
    // Function issues
    //

    /**
     * Returns function or operator with the specified name
     * or null if function is not declared.
     */
    public Function getFunction(String name);

    /** Declares the function. */
    public void declareFunction(Function function);
}


