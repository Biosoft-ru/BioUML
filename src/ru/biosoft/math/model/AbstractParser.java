package ru.biosoft.math.model;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractParser implements Parser
{
    protected ParserContext context = new DefaultParserContext();
    @Override
    public ParserContext getContext()
    {
        return context;
    }
    @Override
    public void setContext(ParserContext context)
    {
        this.context = context;
    }

    protected boolean declareUndefinedVariables = true;
    @Override
    public boolean isDeclareUndefinedVariables()
    {
        return declareUndefinedVariables;
    }
    @Override
    public void setDeclareUndefinedVariables(boolean declareUndefinedVariables)
    {
        this.declareUndefinedVariables = declareUndefinedVariables;
    }

    protected VariableResolver variableResolver;
    public VariableResolver getVariableResolver()
    {
        return variableResolver;
    }
    public void setVariableResolver(VariableResolver variableResolver)
    {
        this.variableResolver = variableResolver;
    }

    ///////////////////////////////////////////////////////////////////
    // Error processing issues
    //

    protected int status;
    protected ArrayList<String> messages = new ArrayList<>();

    public void warning(String warningStr)
    {
        messages.add("Warning: " + warningStr + ".");
        status |= STATUS_WARNING;
    }

    public void error(String errorStr)
    {
        messages.add("Error: " + errorStr + ".");
        status |= STATUS_ERROR;
    }


    public void fatalError(String errorStr)
    {
        messages.add("Syntax error: " + errorStr + ".");
        status |= STATUS_FATAL_ERROR;
    }

    @Override
    public List<String> getMessages()
    {
        return messages;
    }

    ///////////////////////////////////////////////////////////////////
    // Utility functions
    //

    protected AstStart astStart;
    @Override
    public AstStart getStartNode()
    {
        return astStart;
    }

    protected void reinit()
    {
        status = 0;
        messages.clear();
        astStart = null;
    }

    /**
     * Set ups build in operators.
     *
     * @pending validation.
     */
    protected void setOperator(Node node, String name)
    {
        Function operator = context.getFunction(name.trim());
        if( operator == null )
        {
            error("Unknown operator '" + name + "'");
            operator = new UndeclaredFunction(name, -1);
        }

        AstFunNode funNode = (AstFunNode)node;
        funNode.setFunction(operator);
    }

    /**
     * Register variable, if necessary.
     *
     * @pending validation.
     */
    protected String processVariable(String name)
    {
        String varName = name;
        String result = name;
        if( variableResolver != null )
        {
            try
            {
                varName = variableResolver.getVariableName(varName);
                result = variableResolver.resolveVariable(varName);
            }
            catch( Throwable t )
            {
                error(t.getMessage());
                return name;
            }
        }
        if( varName == null || result == null ) //variable was not found in the context
        {
            varName = name;
            
            if (context.containsConstant(varName))
            {
                return varName + "_CONFLICTS_WITH_CONSTANT_";
            }
            if (context.canDeclare(varName)) //check if we can potentially add variable
            {
                if( isDeclareUndefinedVariables() ) //if we actually should declare variable
                    context.declareVariable(varName, 0.0);
                else //else issue warning
                    warning("Variable " + varName + " will be added to context");
                return varName;
            }
            else
            {
                error("Undefined variable " + varName); //if we can not declare this variable at all (e.g. it starts with "$") then issue error
                return null;
            }
        }
        return result;
    }

    /**
     * Register constant, if neccessary.
     *
     * @pending validation.
     * @should we use the same flag "isDeclareUndefinedVariables" for const-case.
     */
    protected void processConstant(Node node, String constName)
    {
        AstConstant constant = (AstConstant)node;
        constant.setName(constName);

        if( context.containsConstant(constName) )
            constant.setValue(context.getConstantValue(constName));
        else
        {
            if( isDeclareUndefinedVariables() )
                context.declareConstant(constName, 0.0);
            else
            {
                error("undefined constant '" + constName);
                constant.setValue(Double.valueOf(0));
            }
        }
    }
}