package biouml.plugins.wdl.parser;

import biouml.plugins.wdl.nextflow.NextFlowPreprocessor;

public class GlobProcessor
{ 
    public String process(String expression) throws ParseException
    {
        AstExpression astExpression = new ExpressionParser().parseExpression( expression );
        process( astExpression );
        return new ExpressionFormatter().format( astExpression );
    }

    public void process(Node start)
    {
        if( start != null )
        {
            int n = start.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
                visit( start.jjtGetChild( i ) );
        }
    }

    protected void visit(Node node)
    {
        if( node instanceof AstFunction )
        {
            visit( (AstFunction)node );
        }
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            visit( node.jjtGetChild( i ) );
        }
    }

    private void visit(AstFunction node)
    {
        if( "glob".equals( node.toString() ) )
        {
            AstText arg = NextFlowPreprocessor.hasOneArgument( node );
//            if( arg != null )
//            {
                SimpleNode parent = (SimpleNode)node.jjtGetParent();
                ParserUtil.replaceChild( parent, node, arg );
//            }
        }
    }
}