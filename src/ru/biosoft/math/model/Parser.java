
package ru.biosoft.math.model;

import java.util.List;

/**
 * Specifies general parser definition.
 *
 * @pending other methods can be formalised and added.
 * @todo comments
 */
public interface Parser
{
    public ParserContext getContext();
    public void setContext(ParserContext context);

    boolean isDeclareUndefinedVariables();
    public void setDeclareUndefinedVariables(boolean declareUndefinedVariables);

    ///////////////////////////////////////////////////////////////////
    // Parsing conventions
    //

    /** The expression was parsed successfully without any errors or wornings. */
    public static final int STATUS_OK           = 0;

    /** There were some warnings during expression parser. */
    public static final int STATUS_WARNING      = 1;

    /** There were some errors during expression parser. */
    public static final int STATUS_ERROR        = 2;

    /**
     * Some fatal errors have occured during the parser.
     *
     * For MathML such status indicates that XML document is not well formed,
     * for linear syntax parser this indicates that some rror in grammar was found.
     */
    public static final int STATUS_FATAL_ERROR  = 4;

    public int parse(String expression);

    /** Returs root of the AST tree. */
    public AstStart getStartNode();

    /** Returns list of warning and error messages. */
    public List<String> getMessages();
}


