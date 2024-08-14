package ru.biosoft.math.model;

/**
 * Utility class to process variable's name.
 *
 * This class allows parser to replace some short variable name
 * by its fully qualified name.
 *
 * Additionally VariableResolver can declare necessary variables
 * in ParserContext if necessary.
 */
public interface VariableResolver
{
    /**
     * Get variable name(ID) by title
     */
    public String getVariableName(String variableTitle);
    /**
     * Get variable specific title by name
     */
    public String resolveVariable(String variableName);
}


