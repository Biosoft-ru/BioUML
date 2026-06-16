package biouml.plugins.wdl.parser;

import java.io.StringReader;

/**
 * Parses expression in form of string into AST 
 */
public class ExpressionParser extends WDLParser
{
    public AstExpression parseExpression(String expression) throws ParseException
    {
        ReInit( new StringReader( expression ) );

        expression();

        jj_consume_token( 0 );

        Node node = jjtree.rootNode();

        if( node instanceof AstExpression )
            return (AstExpression)node;

        throw new ParseException( "Expected AstExpression, got: " + node.getClass().getName() );
    }

    public static void main(String ... args)
    {
        try
        {
            AstExpression expression = new ExpressionParser().parseExpression( "read_lines(stdout())" );
            
            String formatted = new ExpressionFormatter().format( expression );
            
            System.out.println( formatted );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}
