package biouml.plugins.sedml;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.stream.DoubleStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jmathml.ASTNode;
import org.jmathml.ASTOtherwise;
import org.jmathml.ASTPiece;
import org.jmathml.ASTPiecewise;
import org.jmathml.ASTToXMLElementVisitor;
import org.jmathml.MathMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.parser.Parser;
import ru.biosoft.math.parser.ParserTreeConstants;
import ru.biosoft.math.xml.MathMLFormatter;
import ru.biosoft.math.xml.MathMLParser;

public class MathMLUtils
{
    public static AstStart convertMathML(ASTNode node)
    {
        node = fixPieceWise( node );
        ASTToXMLElementVisitor xmlWriter = new ASTToXMLElementVisitor();
        node.accept( xmlWriter );
        Element xml = xmlWriter.getElement();
        XMLOutputter xmlOut = new XMLOutputter( Format.getPrettyFormat() );
        String xmlString = xmlOut.outputString( xml );

        InputSource xmlSource = new InputSource( new StringReader( xmlString ) );
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware( true );
        Document xmlDoc;
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            xmlDoc = builder.parse( xmlSource );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        removeNameSpace( xmlDoc );

        MathMLParser mathmlParser = new MathMLParser();
        mathmlParser.setContext( new DefaultParserContext() );
        mathmlParser.declareCSymbol( new PredefinedFunction( "max", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1 ) );
        mathmlParser.declareCSymbol( new PredefinedFunction( "min", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1 ) );
        mathmlParser.declareCSymbol( new PredefinedFunction( "sum", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1 ) );
        mathmlParser.declareCSymbol( new PredefinedFunction( "product", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1 ) );
        mathmlParser.parse( xmlDoc );
        return mathmlParser.getStartNode();
    }

    public static String mathMLToExpression(AstStart math)
    {
        LinearFormatter formatter = new LinearFormatter();
        return formatter.format( math )[1];
    }

    public static ASTNode convertExpressionToMathML(String expression)
    {
        Parser expressionParser = new Parser( );
        expressionParser.setContext( new DefaultParserContext() );
        expressionParser.parse( expression );
        AstStart ast = expressionParser.getStartNode();
        MathMLFormatter mathMLFormatter = new MathMLFormatter();
        mathMLFormatter.setParserContext( new DefaultParserContext() );
        String xmlString = mathMLFormatter.format( ast )[1];
        MathMLReader mathMLReader = new MathMLReader();
        try
        {
            return mathMLReader.parseMathMLFromString( xmlString );
        }
        catch( IOException impossible )
        {
            throw ExceptionRegistry.translateException( impossible );
        }
    }

    /* org.jmathml incorrectly parse piecewise */
    public static ASTNode fixPieceWise(ASTNode math)
    {
        math = fixPieceWiseInNode( math );
        for(int i = 0; i < math.getNumChildren(); i++)
        {
            ASTNode oldChild = math.getChildAtIndex( i );
            ASTNode newChild = fixPieceWise( oldChild );
            if(oldChild != newChild)
                math.replaceChild( oldChild, newChild );
        }
        return math;
    }

    private static ASTNode fixPieceWiseInNode(ASTNode math)
    {
        try
        {
            if( math.getNumChildren() > 1 && math.getNumChildren() % 2 == 1 )
            {
                Constructor<ASTPiecewise> piecewiseConstructor = ASTPiecewise.class.getDeclaredConstructor();
                piecewiseConstructor.setAccessible( true );
                ASTPiecewise piecewise = piecewiseConstructor.newInstance();
                for( int i = 0; i + 1 < math.getNumChildren(); i += 2 )
                {
                    ASTNode value = math.getChildAtIndex( i );
                    ASTNode condition = math.getChildAtIndex( i + 1 );
                    if(!condition.isRelational() && !condition.isLogical())
                        return math;
                    Constructor<ASTPiece> pieceConstructor = ASTPiece.class.getDeclaredConstructor();
                    pieceConstructor.setAccessible( true );
                    ASTPiece piece = pieceConstructor.newInstance();
                    piece.addChildNode( value );
                    piece.addChildNode( condition );
                    piecewise.addChildNode( piece );
                }
                ASTOtherwise otherwise = new ASTOtherwise();
                otherwise.addChildNode( math.getChildAtIndex( math.getNumChildren() - 1 ) );
                piecewise.addChildNode( otherwise );
                return piecewise;
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        return math;
    }

    private static void removeNameSpace(Node node) {
        Document document = node.getOwnerDocument();
        if (node.getNodeType() == Node.ELEMENT_NODE)
            node = document.renameNode(node, null, node.getLocalName());
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); ++i)
            removeNameSpace(list.item(i));
    }

    public static void evaluateVectorFunctions(ru.biosoft.math.model.Node node, Map<String, double[]> variableValues)
    {
        if(node instanceof AstFunNode && node.jjtGetNumChildren() == 1)
        {
            ru.biosoft.math.model.Node child = node.jjtGetChild( 0 );
            if(child instanceof AstVarNode)
            {
                String varName = ((AstVarNode)child).getName();
                double[] values = variableValues.get( varName );
                if(values == null)
                    throw new IllegalArgumentException("Unresolved variable: " + varName);
                String functionName = ( (AstFunNode)node ).getFunction().getName();
                Double value = null;
                if(functionName.equals( "min" ))
                    value = DoubleStream.of(values).min().getAsDouble();
                else if(functionName.equals( "max" ))
                    value = DoubleStream.of(values).max().getAsDouble();
                else if(functionName.equals("sum"))
                    value = DoubleStream.of( values ).sum();
                else if(functionName.equals( "product" ))
                    value = DoubleStream.of( values ).reduce(1, (a,b)->a*b );
                if(value != null)
                {
                    ru.biosoft.math.model.AstConstant newChild = new AstConstant( ParserTreeConstants.JJTCONSTANT );
                    newChild.setValue( value );
                    node.jjtGetParent().jjtReplaceChild( node, newChild  );
                }
            }
        }
        for(int i = 0; i < node.jjtGetNumChildren(); i++)
            evaluateVectorFunctions( node.jjtGetChild( i ), variableValues );
    }
}
