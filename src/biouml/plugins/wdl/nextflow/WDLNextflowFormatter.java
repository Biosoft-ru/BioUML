package biouml.plugins.wdl.nextflow;

import java.util.Map;

import biouml.plugins.wdl.parser2.AstArguments;
import biouml.plugins.wdl.parser2.AstArray;
import biouml.plugins.wdl.parser2.AstClosure;
import biouml.plugins.wdl.parser2.AstClosureParameters;
import biouml.plugins.wdl.parser2.AstColon;
import biouml.plugins.wdl.parser2.AstComma;
import biouml.plugins.wdl.parser2.AstConstructor;
import biouml.plugins.wdl.parser2.AstContainerElement;
import biouml.plugins.wdl.parser2.AstElse;
import biouml.plugins.wdl.parser2.AstExpression;
import biouml.plugins.wdl.parser2.AstFunction;
import biouml.plugins.wdl.parser2.AstKeyValue;
import biouml.plugins.wdl.parser2.AstMap;
import biouml.plugins.wdl.parser2.AstNone;
import biouml.plugins.wdl.parser2.AstNull;
import biouml.plugins.wdl.parser2.AstPair;
import biouml.plugins.wdl.parser2.AstRegularFormulaElement;
import biouml.plugins.wdl.parser2.AstSubSymbol;
import biouml.plugins.wdl.parser2.AstSymbol;
import biouml.plugins.wdl.parser2.AstTernary;
import biouml.plugins.wdl.parser2.AstText;
import biouml.plugins.wdl.parser2.AstThen;
import biouml.plugins.wdl.parser2.ExpressionFormatter;
import biouml.plugins.wdl.parser2.ExpressionParser;
import biouml.plugins.wdl.parser2.Node;
import biouml.plugins.wdl.parser2.ParseException;
import biouml.plugins.wdl.parser2.SimpleNode;
import one.util.streamex.StreamEx;

public class WDLNextflowFormatter extends ExpressionFormatter
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
        if (node instanceof AstSymbol)
            result.append(node.toString());
        else if( node instanceof AstArray )
            processArray( (AstArray)node );
        else if( node instanceof AstFunction )
            processFunction( (AstFunction)node );
        else if( node instanceof AstText )
            processText( (AstText)node );
        else if( node instanceof AstContainerElement )
            processContainer( (AstContainerElement)node );
        else if( node instanceof AstSubSymbol )
            processSubSymbol( (AstSubSymbol)node );
        else if( node instanceof AstTernary )
            processTernary( (AstTernary)node );
        else if( node instanceof AstMap )
            processMap( (AstMap)node , false);
        else if( node instanceof AstConstructor )
            processConstructor( (AstConstructor)node );
        else if( node instanceof AstPair )
            processPair( (AstPair)node );
        else if (node instanceof AstRegularFormulaElement)
            processFormulaElement((AstRegularFormulaElement)node);
        else if (node instanceof AstKeyValue)
            processKey((AstKeyValue)node);
        else if (node instanceof AstNone)
            processNone((AstNone) node);
        else if( node instanceof AstExpression )
        {
            for( Node child : ((AstExpression)node).getChildren() )
            {
                processNode( child );
            }
        }
        else if (node instanceof AstThen)
        {
            result.append( "? " );
        }
        else if (node instanceof AstElse)
        {
            result.append( ": " );
        }
        else if (node instanceof AstArguments)
        {
            for( Node child : ((AstArguments)node).getChildren() )
            {
                processNode( child );
            }
        }
        else if (node instanceof AstColon)
        {
            result.append( " : " );
        }
        else if (node instanceof AstComma)
        {
            result.append( ", " );
        }
        else if (node instanceof AstClosure)
        {
            processClosure((AstClosure)node);
        }
        else if (node instanceof AstNull)
        {
            result.append( "null" );
        }
        else if (node instanceof SimpleNode)
        {
            result.append(node.toString());
        }
        else
        {
            result.append("ERROR");
        }
    }

    private void processTernary(AstTernary ternary)
    {
        for( int i = 0; i < ternary.getChildren().length; i++ )
        {
            processNode( ternary.jjtGetChild( i ) );
        }
    }

    private void processArray(AstArray array)
    {
//        boolean alreadyHas = array.jjtGetParent() != null && array.jjtGetParent().jjtGetParent() instanceof AstFunction
//                && ( (AstFunction)array.jjtGetParent().jjtGetParent() ).toString().equals( "toArray" );
//        boolean needToArray = false && !alreadyHas && containsChannel( array );
//        if( needToArray )
//            result.append( "toArray(" );
        for( Node child : array.getChildren() )
            processNode( child );
//        if( needToArray )
//            result.append( ")" );
    }
    
    private boolean containsChannel(SimpleNode node)
    {
        for (Node child: node.getChildren())
        {
            if (child instanceof AstSubSymbol || child instanceof AstSymbol || child instanceof AstExpression)
                return true;
        }
        return false;
    }

    protected void processSubSymbol(AstSubSymbol node)
    {
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            if( i > 0 )
                result.append( "." );
            processNode( node.jjtGetChild( i ) );

        }
    }

    protected void processText(AstText node)
    {
        String content = node.toString();
//        content = ensureDoubledBackslashes(content);// .content.replace( "\\", "\\\\");
//        boolean hasQuote = content.contains( "\"" );
//        result.append( hasQuote? "\'": "\"" );
        result.append( content );
//        result.append( hasQuote? "\'": "\"" );
    }

    protected void processContainer(AstContainerElement node)
    {
        int indexes = node.jjtGetNumChildren() - 1;
        for( int i = 0; i < indexes; i++ )
            result.append( "get(" );
        processNode( node.jjtGetChild( 0 ) );
        for( int i = 1; i < node.jjtGetNumChildren(); i++ )
        {
            result.append( "," );
            processNode( node.jjtGetChild( i ) );
            result.append( ")" );
        }
    }

    protected void processFunction(AstFunction node)
    {
        result.append( node.toString() );
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            Node child = node.jjtGetChild( i );
            processNode( child );
        }
    }
    
    private void processKey(AstKeyValue astKey)
    {
        result.append( astKey.getKey() );
        for( Node child : astKey.getChildren() )
        {
            processNode(child);
        }
    }
    
    private void processNone(AstNone astNone)
    {
        result.append( "\"NO_VALUE\"" );
    }

    private void processMap(AstMap astMap, boolean isObject)
    {
        Map<String, SimpleNode> map = astMap.toMap();

        if( map.isEmpty() )
        {
            result.append( "[:]" );
            return;
        }

        result.append( "[ " );

        result.append( StreamEx.of( map.entrySet() ).map( e -> {
            String key = e.getKey().trim();

            if( isObject )
                key = "\"" + key + "\"";

            if( !isParenthesized( key ) )
                key = "(" + key + ")";

            return key + " : " + new WDLNextflowFormatter().format( e.getValue() );
        } ).joining( ", " ) );

        result.append( " ]" );
    }
    
    private boolean isParenthesized(String s)
    {
        s = s.trim();
        return s.startsWith("(") && s.endsWith(")");
    }
    
    private void processMap2(AstMap astMap)
    {
        result.append( "[ " );
        Map<String, SimpleNode> map = astMap.toMap();
        result.append( StreamEx.of( map.entrySet() ).map( e -> e.getKey() + " : " + new WDLNextflowFormatter().format( e.getValue()) ).joining( ", " ) );
        result.append( " ]" );
    }

    private void processPair(AstPair astPair)
    {
        result.append( "pair( " );
        AstExpression[] expressions = astPair.toPair();
        processNode( expressions[0] );
        result.append( ", " );
        processNode( expressions[1] );
        result.append( " )" );
    }

    private void processFormulaElement(AstRegularFormulaElement formulaElement)
    {
        Node parent = formulaElement.jjtGetParent();
        if( parent instanceof AstConstructor )
        {
            if( formulaElement.toString().equals( "{" ) )
                result.append( "[" );
            else if( formulaElement.toString().equals( "}" ) )
                result.append( "]" );
            else
                result.append( formulaElement.toString() );
        }
        else
            result.append( formulaElement.toString() );
    }
    
    private void processConstructor(AstConstructor astConstructor)
    {
        result.append( "[ " );

        boolean first = true;

        for( Node node : astConstructor.getChildren() )
        {
            if( !( node instanceof AstKeyValue ) )
                continue;

            if( !first )
                result.append( ", " );

            first = false;

            AstKeyValue keyValue = (AstKeyValue)node;
            String key = keyValue.jjtGetFirstToken().image;

            result.append( "\"" ).append( key ).append( "\" : " );

            Node[] children = keyValue.getChildren();
            processNode( children[children.length - 1] );
        }

        if( first )
            result.append( ":" );

        result.append( " ]" );
    }
    
    public static String ensureDoubledBackslashes(String input)
    {
        if (input == null || input.isEmpty())
            return input;

        StringBuilder result = new StringBuilder(input.length() * 2);

        int index = 0;

        while (index < input.length())
        {
            char current = input.charAt(index);

            if (current != '\\')
            {
                result.append(current);
                index++;
                continue;
            }

            if (index + 1 < input.length()
                && input.charAt(index + 1) == '\\')
            {
                result.append("\\\\");
                index += 2;
            }
            else
            {
                result.append("\\\\");
                index++;
            }
        }

        return result.toString();
    }
    
    public static void main(String... args)
    {
        AstExpression expr;
        try
        {
            expr = new ExpressionParser().parseExpression("[0,1,2]");

            String formatted = new WDLNextflowFormatter().format( expr );
            System.out.println( formatted );
//            expr.dump("");
        }
        catch( ParseException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    
    private void processClosure(AstClosure closure)
    {
        Node[] children = closure.getChildren();
        int index = 0;

        if( children.length > 0 )
        {
            result.append("{ ");

            if( children[0] instanceof AstClosureParameters )
            {
                AstClosureParameters params = (AstClosureParameters)children[0];

                for( int i = 0; i < params.jjtGetNumChildren(); i++ )
                {
                    if( i > 0 )
                        result.append(", ");

                    processNode(params.jjtGetChild(i));
                }

                result.append(" -> ");
                index = 1;
            }

            for( int i = index; i < children.length; i++ )
            {
                if( i > index )
                    result.append(" ");

                processNode(children[i]);
            }

            result.append(" ");
        }

        result.append("}");
    }
}