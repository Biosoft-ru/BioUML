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
//        if( node instanceof AstConstant )
//            processConstant((AstConstant)node);
//
//        else if( node instanceof AstVarNode )
//            processVariable((AstVarNode)node);

        if( node instanceof AstFunction )
            processFunction((AstFunction)node);
        
        else 
            result.append( node.toString() );
    }
    
    protected void processFunction(AstFunction node)
    {
        result.append( node.toString());//.firstToken.image);
//        result.append( "(" );
        for (Node child: node.children)
            processNode(child);
//        result.append( ")" );
    }
}
