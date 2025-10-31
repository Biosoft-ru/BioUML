package biouml.plugins.wdl.parser;

public class ExpressionFormatter
{
    protected StringBuilder result;

    public String format(SimpleNode start)
    {
        result = new StringBuilder();

        if( start != null )
        {
            int n = start.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
                processNode( start.jjtGetChild( i ) );
        }
        return result.toString();
    }

    protected void processNode(Node node)
    {
        if( node instanceof AstArray )
            processArray( (AstArray)node );
        else if( node instanceof AstFunction )
            processFunction( (AstFunction)node );
        else if( node instanceof AstText )
            processText( (AstText)node );
        else if( node instanceof AstContainerElement )
            processContainer( (AstContainerElement)node );
        else if (node instanceof AstSubSymbol)
            processSubSymbol((AstSubSymbol)node);
        else if (node instanceof AstTernary)
            processTernary((AstTernary)node);
        else
        {
            result.append( node.toString() );
        }
    }

    private void processTernary(AstTernary ternary)
    {
        result.append( "if (" );
        result.append(ternary.getChildren()[0]);
        result.append( ")" );
        for(  int i=1; i<ternary.getChildren().length; i++ )
        {
            result.append( " " );
            processNode( ternary.jjtGetChild( i ) );
        }
    }
    
    private void processArray(AstArray array)
    {
        for( Node child : array.getChildren() )
            processNode( child );
    }

    protected void processSubSymbol(AstSubSymbol node)
    {
        for (int i=0; i< node.jjtGetNumChildren(); i++)
        {
            if (i > 0)
                result.append( "." );
            processNode(node.jjtGetChild( i ));
            
        }
    }
    
    protected void processText(AstText node)
    {
        result.append( "\"" );
        result.append( node.toString() );
        result.append( "\"" );
    }

    protected void processContainer(AstContainerElement node)
    {
        result.append( node.toString() );
        result.append( "[" );
        for( Node child : node.children )
            processNode( child );
        result.append( "]" );
    }

    protected void processFunction(AstFunction node)
    {
        result.append( node.toString() );
        for( Node child : node.children )
            processNode( child );
    }
}