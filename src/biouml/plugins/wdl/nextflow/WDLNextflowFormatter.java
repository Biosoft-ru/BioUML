package biouml.plugins.wdl.nextflow;

import java.util.Map;

import biouml.plugins.wdl.parser.AstArray;
import biouml.plugins.wdl.parser.AstConstructor;
import biouml.plugins.wdl.parser.AstContainerElement;
import biouml.plugins.wdl.parser.AstExpression;
import biouml.plugins.wdl.parser.AstFunction;
import biouml.plugins.wdl.parser.AstKeyValue;
import biouml.plugins.wdl.parser.AstMap;
import biouml.plugins.wdl.parser.AstPair;
import biouml.plugins.wdl.parser.AstRegularFormulaElement;
import biouml.plugins.wdl.parser.AstSubSymbol;
import biouml.plugins.wdl.parser.AstTernary;
import biouml.plugins.wdl.parser.AstText;
import biouml.plugins.wdl.parser.ExpressionFormatter;
import biouml.plugins.wdl.parser.Node;
import biouml.plugins.wdl.parser.SimpleNode;
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
        if( node instanceof AstArray )
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
            processMap( (AstMap)node );
        else if( node instanceof AstConstructor )
            processConstructor( (AstConstructor)node );
        else if( node instanceof AstPair )
            processPair( (AstPair)node );
        else if (node instanceof AstRegularFormulaElement)
            processFormulaElement((AstRegularFormulaElement)node);
        else if (node instanceof AstKeyValue)
            processKey((AstKeyValue)node);
        else if( node instanceof AstExpression )
        {
            for( Node child : ((AstExpression)node).getChildren() )
            {
                processNode( child );
            }
        }
        else if (node instanceof SimpleNode)
        {
            result.append( node.toString() );
        }
        else
        {
            result.append("ERROR");
        }
    }

    private void processTernary(AstTernary ternary)
    {
        result.append( "if (" );
        result.append( ternary.getChildren()[0] );
        result.append( ")" );
        for( int i = 1; i < ternary.getChildren().length; i++ )
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
        boolean hasQuote = content.contains( "\"" );
        result.append( hasQuote? "\'": "\"" );
        result.append( content );
        result.append( hasQuote? "\'": "\"" );
    }

    protected void processContainer(AstContainerElement node)
    {
        result.append( node.toString() );
        result.append( "[" );
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            Node child = node.jjtGetChild( i );
            processNode( child );
        }
        result.append( "]" );
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

    private void processMap(AstMap astMap)
    {
        result.append( "{ " );
        Map<String, Object> map = astMap.toMap();
        result.append( StreamEx.of( map.entrySet() ).map( e -> e.getKey() + ": " + e.getValue() ).joining( ", " ) );
        result.append( " }" );
    }

    private void processPair(AstPair astPair)
    {
        result.append( "[ " );
        AstExpression[] expressions = astPair.toPair();
        processNode( expressions[0] );
        result.append( ", " );
        processNode( expressions[1] );
        result.append( " ]" );
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
//        result.append( "[ " );
//        result.append( ( (AstConstructor)astConstructor ).jjtGetFirstToken() );
        for( Node node : astConstructor.getChildren() )
        {
            processNode( node );
        }
    }
}