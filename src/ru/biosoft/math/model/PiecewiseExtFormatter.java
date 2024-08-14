
package ru.biosoft.math.model;

abstract public class PiecewiseExtFormatter extends LinearFormatter
{
    private static int auxVariableCounter = 0;
    protected String auxVariableName = null;
    protected void resetVariableCounter()
    {
        auxVariableCounter = 0;
    }

    StringBuilder declarations = null;

    @Override
    public String[] format(AstStart start)
    {
        result = new StringBuilder();
        declarations = new StringBuilder();

        int n = start.jjtGetNumChildren();

        for( int i = 0; i < n; i++ )
        {
            Node node = start.jjtGetChild(i);
            if( node instanceof AstPiecewise )
                processPiecewise((AstPiecewise)node);
            else
                processNode(node);
        }
        return new String[] {declarations.toString(), result.toString()};
    }

    @Override
    protected void processPiecewise(AstPiecewise node)
    {
        int n = node.jjtGetNumChildren();

        // create new execution context
        StringBuilder oldResult = result;
        result = new StringBuilder();

        String local_auxVariableName = auxVariableName;
        auxVariableName = "piecewise_" + ( auxVariableCounter++ );

        if( n > 0 )
        {
            processPiecewiseBegin(node);
            processIf((AstPiece)node.jjtGetChild(0));
            for( int i = 1; i < n; i++ )
            {
                result.append(endl);
                AstPiece piece = (AstPiece)node.jjtGetChild(i);
                if( "Otherwise".equals(piece.toString()) )
                    processOtherwise(piece);
                else
                    processElseIf(piece);
            }
            processPiecewiseEnd(node);
        }

        // replace the node with "piecewise_XXX" variable
        AstVarNode varNode = new AstVarNode(ru.biosoft.math.parser.ParserTreeConstants.JJTVARNODE);
        varNode.setName(auxVariableName);
        node.jjtGetParent().jjtReplaceChild(node, varNode);

        declarations.append(result.toString() + endl);

        // restore context
        result = oldResult;

        result.append(auxVariableName);

        auxVariableName = local_auxVariableName;
    }

    abstract protected void processIf(AstPiece node);
    abstract protected void processElseIf(AstPiece node);
    abstract protected void processOtherwise(AstPiece node);

    /**
     * Auxiliary variables initialization
     */
    abstract protected void processPiecewiseBegin(Node node);

    /**
     * Prints some final keyword if necessary
     */
    abstract protected void processPiecewiseEnd(Node node);

}