package biouml.plugins.wdl.parser;

import ru.biosoft.math.model.AstVarNode;

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
        
        if (node instanceof AstArray)
        {
            for( Node child : ((AstArray)node).getChildren() )
                processNode( child );
        }
        else if( node instanceof AstFunction )
            processFunction((AstFunction)node);
        else if( node instanceof AstText )
            processText((AstText)node);
        
        else 
            result.append( node.toString() );
    }
    
    protected void processText(AstText node)
    {
        result.append( "\"");
        result.append( node.toString());
        result.append( "\"");
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
