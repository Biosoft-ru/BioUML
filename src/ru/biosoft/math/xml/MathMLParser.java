/*
 * Created on 22.03.2004 at 5:03:50
 * by ias
 *
 * Modification started on 2004.04.22
 * Last modification date is 2004.05.06
 *
 * if you want to enable debug console output simply find and replace all entries of "//System.out.println(" to "System.out.println("
 */
package ru.biosoft.math.xml;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import one.util.streamex.IntStreamEx;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import ru.biosoft.math.model.AbstractParser;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.AstPiece;
import ru.biosoft.math.model.AstPiecewise;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.ParserContext;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.UndeclaredFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.math.parser.ParserTreeConstants;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

/**
 * We assume xmlinput has the following format:
 * <math>...</math>
 *
 * @pending high csymbol parsing
 */

public class MathMLParser extends AbstractParser implements ParserTreeConstants
{
    protected org.w3c.dom.Document xmlDoc = null;

    protected Hashtable<String, String> mathSignsMap = new Hashtable<>(); //see ru.biosoft.math.model.ParserContext for full list
    protected Hashtable<String, CSymbol> csymbolMap = new Hashtable<>(); //name --> CSymbol
    protected Hashtable<String, CSymbol> csymbolURLMap = new Hashtable<>(); //definitionURL --> CSymbol

    private Map<String, String> replacements = new HashMap<>();
    
    public MathMLParser()
    {
        fillMathSignsMap();
    }
    
    public void setReplacements(Map<String, String> replacements)
    {
        this.replacements = replacements;
    }

    protected void fillMathSignsMap()
    {
        mathSignsMap.put("or", "||");
        mathSignsMap.put("and", "&&");
        mathSignsMap.put("not", "!");
        mathSignsMap.put("xor", "xor");

        mathSignsMap.put("gt", ">");
        mathSignsMap.put("lt", "<");
        mathSignsMap.put("eq", "==");
        mathSignsMap.put("leq", "<=");
        mathSignsMap.put("geq", ">=");
        mathSignsMap.put("neq", "!=");

        mathSignsMap.put("plus", "+");
        mathSignsMap.put("minus", "u-");
        mathSignsMap.put("minus", "-");
        mathSignsMap.put("times", "*");
        mathSignsMap.put("divide", "/");
        mathSignsMap.put("power", "^");
        mathSignsMap.put("root", "root");
        mathSignsMap.put("abs", "abs");
        mathSignsMap.put("exp", "exp");
        mathSignsMap.put("ln", "ln");
        mathSignsMap.put("log", "log");

        mathSignsMap.put("sin", "sin");
        mathSignsMap.put("cos", "cos");
        mathSignsMap.put("tan", "tan");
        mathSignsMap.put("cot", "cot");

        mathSignsMap.put("arcsin", "arcsin");
        mathSignsMap.put("arccos", "arccos");
        mathSignsMap.put("arctan", "arctan");
        mathSignsMap.put("arccosh", "arccosh");
        mathSignsMap.put("arcsinh", "arcsinh");
        mathSignsMap.put("arccot", "arccot");
        mathSignsMap.put("arccoth", "arccoth");
        mathSignsMap.put("arccsc", "arccsc");
        mathSignsMap.put("arccsch", "arccsch");
        mathSignsMap.put("arcsec", "arcsec");
        mathSignsMap.put("arcsech", "arcsech");
        mathSignsMap.put("arctanh", "arctanh");
        mathSignsMap.put("cosh", "cosh");
        mathSignsMap.put("coth", "coth");
        mathSignsMap.put("csc", "csc");
        mathSignsMap.put("csch", "csch");
        mathSignsMap.put("sec", "sec");
        mathSignsMap.put("sech", "sech");
        mathSignsMap.put("sinh", "sinh");
        mathSignsMap.put("tanh", "tanh");

        mathSignsMap.put("diff", "diff");
    }

    //Probably legacy of old sbml versions
    public void declareCSymbol(String name)
    {
        csymbolMap.put(name, new CSymbol(name, JJTVARNODE));
    }

    public void declareCSymbol(Function function)
    {
        String name = function.getName();
        csymbolMap.put(name, new CSymbol(name, JJTFUNNODE));
        context.declareFunction(function);
    }

    public void declareCSymbol(String definitionURL, String varName)
    {
        declareCSymbol(definitionURL, varName, 0.0);
    }
    //TODO: proper implementation
    public void declareCSymbol(String definitionURL, String varName, double initialValue)
    {
        CSymbol csymbol = new CSymbol(varName, JJTVARNODE);
        csymbol.initialValue = initialValue;
        csymbolURLMap.put(definitionURL, csymbol);
    }
    
    public void declareCSymbol(String definitionURL, Function function)
    {
        String name = function.getName();
        csymbolURLMap.put(definitionURL, new CSymbol(name, JJTFUNNODE));
        context.declareFunction(function);
    }
    
    public int parse(Document doc)
    {
        reinit();
        astStart = new AstStart( JJTSTART );

        try
        {
            xmlDoc = doc;
            buildTree();
        }
        catch( Exception e )
        {
            fatalError( "Can NOT build DOM document" );
        }

        astStart.setStatus( status );
        astStart.setMessages( messages );

        return status;
    }

    @Override
    public int parse(String xmlString)
    {
        try
        {
            StringReader sr = new StringReader( xmlString );
            InputSource xmlSource = new InputSource( sr );
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse( xmlSource );
            parse( doc );
        }
        catch( Exception e )
        {
            fatalError( "Can NOT build DOM document" );
        }
        return status;
    }

    /**
     * Method returns all elements with specified tag inside the element (even recursively inside element's children
     */
    public Set<org.w3c.dom.Element> getRecursiveElements(org.w3c.dom.Element element, String tag)
    {
        Set<org.w3c.dom.Element> result = new HashSet<>();
        for (org.w3c.dom.Element innerElement: XmlUtil.elements( element ))
        {
            if( innerElement.getTagName().equals( tag ) )
                result.add( innerElement );
            result.addAll( getRecursiveElements( innerElement, tag) );
        }      
        return result;
    }
    
    /**
     * Method returns all function names used inside given element
     */
    public Set<String> getFunctions(org.w3c.dom.Element funcElement)
    {
        Set<String> result = new HashSet<>();
        Set<org.w3c.dom.Element> elements = getRecursiveElements(funcElement, "apply");
        
        for (Element innerFunction: elements)
        {
            org.w3c.dom.Node item = innerFunction.getChildNodes().item( 1 );
            if( item.getNodeName().equals( "ci" ) )
            {
                String innerFuncName = item.getFirstChild().getNodeValue();
                result.add( innerFuncName.trim() );
            }
        }
        return result;
    }

    public int parse(org.w3c.dom.Element math)
    {
        try
        {
            reinit();
            astStart = new AstStart(JJTSTART);
            ru.biosoft.math.model.Node astRoot = processMATH(math);
            if( astRoot != null )
            {
                int nChilds = astStart.jjtGetNumChildren();
                astStart.jjtAddChild(astRoot, nChilds);
            }
        }
        catch( Throwable t )
        {
            fatalError("Can not parse MathML element, error: " + t);
        }
        return status;
    }

    public void buildTree()
    {
        if( xmlDoc == null || astStart == null )
            return;

        if( context == null )
            context = new ru.biosoft.math.model.DefaultParserContext();

        org.w3c.dom.Node xmlRoot = xmlDoc;
        ru.biosoft.math.model.Node astRoot = buildAstTree(xmlRoot);

        if( astRoot != null )
        {
            int nChilds = astStart.jjtGetNumChildren();
            astStart.jjtAddChild(astRoot, nChilds);
        }
        else
        {

        }
    }

    protected ru.biosoft.math.model.Node buildAstTree(org.w3c.dom.Node xmlRoot)
    {
        if( xmlRoot == null )
            return null;

        ru.biosoft.math.model.Node astRoot = null;
        org.w3c.dom.Node domNode = xmlRoot.getFirstChild();

        while( domNode != null )
        {
            String domNodeName = domNode.getNodeName();
            //System.out.println("\tXML Root: " + domNodeName);

            if( "math".equals(domNodeName) )
            {
                astRoot = processMATH(domNode);
                break;
            }
            domNode = domNode.getNextSibling();
        }
        return astRoot;
    }

    protected ru.biosoft.math.model.Node processMATH(org.w3c.dom.Node domNodeRoot)
    {
        if( domNodeRoot == null )
            return null;

        ru.biosoft.math.model.Node astRoot = null;
        org.w3c.dom.Node domNode = domNodeRoot.getFirstChild();

        while( domNode != null )
        {
            Node node = processNode(domNode);
            if( node != null )
            {
                astRoot = node;
                break;
            }
            domNode = domNode.getNextSibling();
        }
        return astRoot;
    }

    protected Node processNode(org.w3c.dom.Node domNode)
    {
        String nodeName = domNode.getNodeName();

        if( nodeName.equals("apply") )
            return processAPPLY(domNode);
        else if( nodeName.equals("lambda") )
            return processLAMBDA(domNode);
        if( nodeName.equals("ci") )
            return processCI(domNode);
        if( nodeName.equals("cn") )
            return processCN((Element)domNode);
        if( nodeName.equals("csymbol") )
            return processCSYMBOL(domNode);
        if( nodeName.equals("piecewise") )
            return processPIECEWISE(domNode);
        // process constants
        if( nodeName.equals("pi") )
            return processPI(domNode);
        if( nodeName.equals("exponentiale") )
            return processEXP(domNode);
        if( nodeName.equals("true") )
            return processTRUE();
        if( nodeName.equals("false") )
            return processFALSE();
        if( nodeName.equals("infinity") )
            return processINFINITY();
        if( nodeName.equals("notanumber") )
            return processNAN();
        // process semantics
        if( nodeName.equals("semantics") )
            return processMATH(domNode);

        return null;
    }

    /**
     * <apply>-tag is like serialized function call.
     * The first child element of apply is function name (see mathSignsMap, also may be <csymbol>).
     * Then, various qualifier tags can be met. (we concern only <bvar>, <degree>, <logbase>)
     * Finally, arguments of the function can also be presented. (<apply>, <ci>, <cn>, <csymbol>)
     * p.s. <csymbol> is like on-the-fly declaration (not definition)
     */
    protected AstFunNode processAPPLY(org.w3c.dom.Node domNodeRoot)
    {
        //        System.err.println("processAPPLY");

        if( domNodeRoot == null )
            return null;

        org.w3c.dom.Node domNode = domNodeRoot.getFirstChild();

        /* First we should check for type of function we deal with.*/
        /* Then read qualifiers of function, if they are presented.*/
        /* And finally extract arguments.*/

        ru.biosoft.math.model.AstFunNode astNode = null;
        while( domNode != null )
        {
            if( domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE )
            {
                // skip non-element data
                domNode = domNode.getNextSibling();
                continue;
            }

            // here we meet first element-tag, assume this is function name
            // or functional csymbol
            String nodeName = domNode.getNodeName();
            if( nodeName.equals("csymbol") )
                astNode = (AstFunNode)processCSYMBOL(domNode);
            else
                astNode = processFunction(domNode);
            //System.out.println("\t\tFunction: " + astNode.getFunction().getName());
            domNode = domNode.getNextSibling();
            break;
        }
        if( astNode == null )
            return null;

        int argc = 0;

        // TODO: read qualifiers.
        // logBase and rootDegree must be after main argument of log and root correspondingly
        ru.biosoft.math.model.Node logBase = null;
        ru.biosoft.math.model.Node rootDegree = null;

        if( domNode != null )
        {
            // process qualifiers
            while( true )
            {
                if( domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE )
                {
                    domNode = domNode.getNextSibling();
                    if( domNode == null )
                        break;
                    continue;
                }
                String qualName = domNode.getNodeName();
                ru.biosoft.math.model.Node qual = null;

                if( qualName.equals("bvar") )
                {
                    qual = processBVAR(domNode);
                    if( qual != null )
                    {
                        argc++;
                        int nChilds = astNode.jjtGetNumChildren();
                        astNode.jjtAddChild(qual, nChilds);
                    }
                }
                else if( qualName.equals("degree") )
                    rootDegree = qual = processDEGREE(domNode);
                else if( qualName.equals("logbase") )
                    logBase = qual = processLOGBASE(domNode);

                if( qual != null )
                {
                    // System.out.println("\t\tQual: " + domNode.getNodeName() + "\tValue: " + domNode.getNodeValue() + "\tType: " + domNode.getNodeType());
                    domNode = domNode.getNextSibling();
                }
                else
                    break;
            }
        }

        // process arguments
        while( domNode != null )
        {
            if( domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE )
            {
                domNode = domNode.getNextSibling();
                continue;
            }
            ru.biosoft.math.model.Node child = null;

            child = processNode(domNode);

            if( child != null )
            {
                argc++;
                int nChilds = astNode.jjtGetNumChildren();
                astNode.jjtAddChild(child, nChilds);
                // System.out.println("\t\tChild: " + domNode.getNodeName() + "\tValue: " + domNode.getNodeValue() + "\tType: " + domNode.getNodeType());
            }

            domNode = domNode.getNextSibling();
        }

        // special dealing with Unary Minus
        Function suspicious = astNode.getFunction();
        if( ( suspicious.getName().equals( "-" ) ) && ( argc == 1 ) )
        {
            setOperator(astNode, "u-");
        }
        // special dealing with Log and Root
        else if( suspicious.getName().equals( "log" ) )
        {
            if( logBase != null )
            {
                int nChilds = astNode.jjtGetNumChildren();
                astNode.jjtAddChild( logBase, nChilds );
            }
        }
        else if( suspicious.getName().equals( "root" ) )
        {
            if( rootDegree == null )
            {
                //add root-degree arg (="10")
                rootDegree = new AstConstant(JJTVARNODE);
                Object two = 2;
                ( (AstConstant)rootDegree ).setValue(two);
            }
            int nChilds = astNode.jjtGetNumChildren();
            astNode.jjtAddChild(rootDegree, nChilds);
        }
        else if( potentiallyNAryFunctions.contains(suspicious.getName()) && argc > 2 )
        {
           astNode = processNAryFunction(astNode, argc);
        }
        //special dealing with Log and Root
//        else if( suspicious.getName() == "==" )
//        {
//            boolean need2transform = false;
//            int n = astNode.jjtGetNumChildren();
//            for( int i = 0; i < n; i++ )
//            {
//                if( ( astNode.jjtGetChild(i) instanceof AstFunNode ) || ( astNode.jjtGetChild(i) instanceof AstPiecewise ) )
//                {
//                    need2transform = true;
//                    break;
//                }
//            }
//            if( need2transform )
//                setOperator(astNode, "=");
//        }

        if (argc == 0)
            processNullAryFunctions(astNode);
        
        if (argc == 1)
            processUnaryFunctions(astNode);
            
        // here we must check whether the number of arguments supplied to function is correct
        suspicious = astNode.getFunction();
        //System.out.println("\t\tArgc expected: " + suspicious.getNumberOfParameters() + ",  was" + Integer.toString(argc));
        if( ( argc != suspicious.getNumberOfParameters() ) && ( suspicious.getNumberOfParameters() != -1 ) )
            astNode = null;

        //        System.err.println(":::processAPPLY");
        //        System.err.println("processAPPLY::out = " + astNode);
        return astNode;
    }

    private static final Set<String> potentiallyNAryFunctions = new HashSet<>( Arrays.asList( "==", ">=", "<=", "<", ">" ) );

    protected AstFunNode processNAryFunction(AstFunNode node, int argc)
    {
        Function f = node.getFunction();
        String name = f.getName();
        int priority = f.getPriority();

        Node[] arguments = IntStreamEx.range( argc ).mapToObj( node::jjtGetChild )
            .pairMap( (a, b) -> Utils.applyFunction(a, b, new PredefinedFunction(name, priority, 2)) ).toArray( Node[]::new );

        return (AstFunNode)Utils.applyFunction(arguments, new PredefinedFunction("&&", PredefinedFunction.LOGICAL_PRIORITY, -1));
    }

    protected void processNullAryFunctions(AstFunNode node)
    {
        if( node.getFunction().getName().equals("+") )
            node.jjtAddChild(Utils.createConstant(0), 0);
        else if( node.getFunction().getName().equals("*") )
            node.jjtAddChild(Utils.createConstant(1), 0);     
        else if( node.getFunction().getName().equals("&&") )
            node.jjtAddChild(Utils.createConstant(true), 0);
        else if( node.getFunction().getName().equals("||") || node.getFunction().getName().equals("xor"))
            node.jjtAddChild(Utils.createConstant(false), 0);
    }
    
    protected void processUnaryFunctions(AstFunNode node)
    {
        if( node.getFunction().getName().equals("&&") )
            node.jjtAddChild(Utils.createConstant(true), 1);
        else if( node.getFunction().getName().equals("||"))
            node.jjtAddChild(Utils.createConstant(false), 1);
        else if( node.getFunction().getName().equals("xor"))
            node.jjtAddChild(Utils.createConstant(false), 1);
    }

    /**
     * Atrribute "type" is ignored.
     */
    protected AstVarNode processCI(org.w3c.dom.Node domNode)
    {
        AstVarNode var = new AstVarNode(JJTVARNODE);
        String varName = domNode.getFirstChild().getNodeValue().trim();
            
        if (varName.equals( "time" ) && this.context.containsVariable( "_CONFLICTS_WITH_TIME_" ))        
            varName = "_CONFLICTS_WITH_TIME_";        
        
        varName = processVariable(varName);

        var.setName(varName);
        if( domNode.getAttributes() != null )
        {
            XmlStream.nodes( domNode.getAttributes() )
                    .findFirst( n -> n.getNodeName().equals( "definitionURL" ) )
                    .ifPresent( n -> var.setDefinitionUrl( n.getNodeValue() ) );
        }
        return var;
    }

    protected AstVarNode processPI(org.w3c.dom.Node piNode)
    {
        AstVarNode astVar = new AstVarNode(JJTVARNODE);
        astVar.setName("pi");
        return astVar;
    }

    protected AstVarNode processEXP(org.w3c.dom.Node expNode)
    {
        AstVarNode astVar = new AstVarNode(JJTVARNODE);
        astVar.setName("exponentiale");
        return astVar;
    }

    protected AstConstant processTRUE()
    {
        AstConstant astCN = new AstConstant(JJTCONSTANT);
        astCN.setValue(true);
        return astCN;
    }

    protected AstConstant processFALSE()
    {
        AstConstant astCN = new AstConstant(JJTCONSTANT);
        astCN.setValue(false);
        return astCN;
    }

    protected AstConstant processINFINITY()
    {
        AstConstant astCN = new AstConstant(JJTCONSTANT);
        astCN.setValue(Double.POSITIVE_INFINITY);
        return astCN;
    }

    protected AstConstant processNAN()
    {
        AstConstant astCN = new AstConstant(JJTCONSTANT);
        astCN.setValue(Double.NaN);
        return astCN;
    }

    protected AstPiecewise processPIECEWISE(org.w3c.dom.Node domNodePW)
    {
        //      System.err.println("processPIECEWISE");
        //    (new Exception()).printStackTrace();
        AstPiecewise astPW = new AstPiecewise(JJTPIECEWISE);
        org.w3c.dom.Node domNode = domNodePW.getFirstChild();

        while( domNode != null )
        {
            if( domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE )
            {
                //skip non-element data
                domNode = domNode.getNextSibling();
                continue;
            }
            String tagName = domNode.getNodeName();
            AstPiece piece = null;

            if( tagName.equals("piece") || tagName.equals("otherwise") )
            {
                piece = processPIECE(domNode);

                if( piece != null )
                    astPW.jjtAddChild(piece, astPW.jjtGetNumChildren());

                domNode = domNode.getNextSibling();
            }
            else
            {
                break;
            }
        }
        //        System.err.println(":::processPIECEWISE");
        return astPW;
    }

    protected AstPiece processPIECE(org.w3c.dom.Node domNodePiece)
    {
        AstPiece astPiece = new AstPiece(JJTPIECE);
        org.w3c.dom.Node domNode = domNodePiece.getFirstChild();

        ru.biosoft.math.model.Node expr1 = null;
        ru.biosoft.math.model.Node expr2 = null;

        while( domNode != null )
        {
            if( domNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE )
            {
                // here we meet first element-tag, assume this is function value
                expr1 = processNode(domNode);
                domNode = domNode.getNextSibling();
                break;
            }
            domNode = domNode.getNextSibling();
        }

        while( domNode != null )
        {
            if( domNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE )
            {
                // here we meet first element-tag, assume this is condition
                expr2 = processNode(domNode);
                domNode = domNode.getNextSibling();
                break;
            }
            domNode = domNode.getNextSibling();
        }
        
        //if we have nested piecewise we translate it to boolean expression
        if( expr2 instanceof AstPiecewise )
            expr2 = translatePiecewise((AstPiecewise)expr2);

        //in AstPiece we assume that first child is condition and second is function value
        if( domNodePiece.getNodeName().equals("piece") )
        {
            if( expr2 != null )
                astPiece.jjtAddChild(expr2, astPiece.jjtGetNumChildren());
            if( expr1 != null )
                astPiece.jjtAddChild(expr1, astPiece.jjtGetNumChildren());
        }
        else
        {
            if( expr1 != null )
                astPiece.jjtAddChild(expr1, astPiece.jjtGetNumChildren());
        }
        return astPiece;
    }
    
    /**
     * We assume that attribute "type" can only be "integer", "real" or "e-notation". Attribute "base" is ignored.
     */
    protected AstConstant processCN(Element cnElement)
    {
        AstConstant constant = new AstConstant(JJTCONSTANT);
        String constName = cnElement.getFirstChild().getNodeValue().trim();

        // if failed, check the first char
        if( Character.isLetter(constName.charAt(0)) || constName.charAt(0) == '_' )
        {
            processConstant(constant, constName);
            return constant;
        }

        String base = "10";
        if( cnElement.hasAttribute("base") )
            base = cnElement.getAttribute("base");

        // currently we are supporting only base equal to 10
        if( !base.equals("10") )
            error("Unsupported base value = " + base);

        String type = "real";
        if( cnElement.hasAttribute("type") )
            type = cnElement.getAttribute("type");

        Object value = Double.valueOf(0);

        try
        {
            if( type.equals("real") )
            {
                if( cnElement.getChildNodes().getLength() == 1 )
                    value = Double.valueOf(constName);
            }
            else if( type.equals("double") )
            {
                if( cnElement.getChildNodes().getLength() == 1 )
                    value = Double.valueOf(constName);
            }
            else if( type.equals("integer") )
            {
                if( cnElement.getChildNodes().getLength() == 1 )
                    value = Integer.valueOf(constName);
            }
            else if( type.equals("e-notation") )
            {
                if( cnElement.getChildNodes().getLength() == 3 )
                {
                    String factor = constName;
                    String power = cnElement.getLastChild().getNodeValue();
                    String e_string = factor.trim() + "e" + power.trim();
                    value = Double.valueOf(e_string);
                }
            }
            else if( type.equals("rational") )
            {
                if( cnElement.getChildNodes().getLength() == 3 )
                {
                    Object ch1 = cnElement.getFirstChild().getNodeValue();
                    Object ch2 = cnElement.getLastChild().getNodeValue();
                    int numerator = Integer.parseInt(ch1.toString().trim());
                    int denominator = Integer.parseInt(ch2.toString().trim());
                    //                    int numerator = Integer.parseInt(cnElement.getFirstChild().getNodeValue());
                    //                    int denominator = Integer.parseInt(cnElement.getLastChild().getNodeValue());
                    value = Double.valueOf((double) numerator / denominator);
                }
            }
            else
            {
                error("Unsupported or unknown type value = " + type);
            }
        }
        catch( Throwable t )
        {
            error("Can NOT parse the const value = " + constName + ", error=" + t);
        }
        constant.setValue(value);
        return constant;
    }

    protected Node processCSYMBOL(org.w3c.dom.Node domNode)
    {
        CSymbol csymbol = null;
        String definitionURL = domNode.getAttributes().getNamedItem("definitionURL").getNodeValue();
        if( definitionURL != null )
        {
            csymbol = csymbolURLMap.get(definitionURL);
            if( csymbol != null && csymbol.type == JJTVARNODE )
            {
                AstVarNode var = new AstVarNode(JJTVARNODE);
                if( !context.containsVariable(csymbol.name) && !context.containsConstant(csymbol.name))
                    context.declareVariable(csymbol.name, csymbol.initialValue);
                var.setName(csymbol.name);
                var.setIsCSymbol(true);
                return var;
            }
            //if csymbol denotes function and have definitionURL than we need to use this URL instead of csymbol name which may be incorrect
            else if( csymbol != null && csymbol.type == JJTFUNNODE )
            {
                AstFunNode astFunc = new AstFunNode(JJTFUNNODE);
                setOperator(astFunc, csymbol.name);
                return astFunc;
            }
        }
        //TODO: according to SBML specification, using csymbol name is incorrect since it can contain almost anything, even be empty!
        String name = domNode.getFirstChild().getNodeValue().trim();
        csymbol = csymbolMap.get(name);
        if( csymbol == null || csymbol.type == JJTVARNODE )
        {
            AstVarNode var = new AstVarNode(JJTVARNODE);
            var.setName(processVariable(name));
            var.setIsCSymbol(true);
            return var;
        }
        else if( csymbol.type == JJTFUNNODE )
        {
            AstFunNode astFunc = new AstFunNode(JJTFUNNODE);
            setOperator(astFunc, name);
            return astFunc;
        }

        return null;
    }
    
    protected AstFunNode processFunction(org.w3c.dom.Node domNode)
    {
        //      System.err.println("processFunction");
        AstFunNode astNode = new AstFunNode(JJTFUNNODE);

        // get mathsign from the map
        String funcName = domNode.getNodeName();
        if( funcName.equals( "csymbol" ) || funcName.equals( "ci" ) )
        {
            funcName = domNode.getFirstChild().getNodeValue().trim();
        }
        String sign = mathSignsMap.get(funcName);
        if( sign == null )
        {
            if (this.replacements.containsKey( funcName ))
                funcName = replacements.get( funcName );
            Function func = context.getFunction( funcName );
            if( func == null )
            {
                warning( "Unknown function '" + funcName + "'" );
                func = new UndeclaredFunction( funcName, Function.FUNCTION_PRIORITY );
            }
            astNode.setFunction( func );
        }
        else
            setOperator( astNode, sign );

        return astNode;
    }

    protected String lambdaFunctionName = null;
    public void setLambdaFunctionName(String lambdaFunctionName)
    {
        this.lambdaFunctionName = lambdaFunctionName;
    }

    protected AstFunctionDeclaration processLAMBDA(org.w3c.dom.Node domNodeRoot)
    {
        //            System.err.println("processLAMBDA");
        if( domNodeRoot == null )
            return null;

        AstFunctionDeclaration astFD = new AstFunctionDeclaration(JJTFUNCTIONDECLARATION);
        astFD.setName(lambdaFunctionName);

        //escape current context to the local var
        ParserContext oldContext = getContext();
        astFD.init(oldContext);
        setContext(astFD);

        VariableResolver oldResolver = getVariableResolver();
        setVariableResolver(null);

        // First we scan for Bound Vars and then for Apply expression
        org.w3c.dom.Node domNode = domNodeRoot.getFirstChild();

        while( domNode != null )
        {
            if( domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE )
            {
                //skip non-element data
                domNode = domNode.getNextSibling();
                continue;
            }

            // here we meet first element-tag, assume this is function name
            String tagName = domNode.getNodeName();
            ru.biosoft.math.model.AstVarNode bvar = null;

            if( tagName.equals("bvar") )
            {
                boolean declareVars = isDeclareUndefinedVariables();
                setDeclareUndefinedVariables(true);
                bvar = processBVAR(domNode);
                setDeclareUndefinedVariables(declareVars);

                if( bvar != null )
                {
                    int nChilds = astFD.jjtGetNumChildren();
                    astFD.jjtAddChild(bvar, nChilds);
                    //System.out.println("\t\tBVAR: " + bvar.getName());
                }
                domNode = domNode.getNextSibling();
            }
            else
                break;
        }
        while( domNode != null )
        {
            if( domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE )
            {
                domNode = domNode.getNextSibling(); //skip non-element data
                continue;
            }
            //here we meet first element-tag, assume this is a function definition
            String tagName = domNode.getNodeName();
            ru.biosoft.math.model.Node func = null;

            if( tagName.equals( "apply" ) )
                func = processAPPLY( domNode );
            else if( tagName.equals( "piecewise" ) )
                func = processPIECEWISE( domNode );
            else if( tagName.equals( "ci" ) )
                func = processCI( domNode );
            else if( tagName.equals( "cn" ) )
                func = processCN( (Element)domNode );
            else if( tagName.equals( "pi" ) )
                func = processPI( domNode );
            else if( tagName.equals( "exponentiale" ) )
                func = processEXP( domNode );
            else if( tagName.equals( "true" ) )
                func = processTRUE();
            else if( tagName.equals( "false" ) )
                func = processFALSE();
            else if( tagName.equals( "infinity" ) )
                func = processINFINITY();
            else if( tagName.equals( "notanumber" ) )
                func = processNAN();

            if( func != null )
            {
                int nChilds = astFD.jjtGetNumChildren();
                astFD.jjtAddChild( func, nChilds );
                //System.out.println("\t\tFunction: " + ((AstFunNode)func).getFunction().getName());
            }
            domNode = domNode.getNextSibling();
        }
        setContext( oldContext );
        setVariableResolver( oldResolver );
        return astFD;
    }

    protected ru.biosoft.math.model.Node processDEGREE(org.w3c.dom.Node domNode)
    {
        if( getFirstChildElement(domNode) == null )
            return null;

        String type = getFirstChildElement(domNode).getNodeName();
        ru.biosoft.math.model.Node astNode = null;

        if( type.equals("cn") )
            astNode = processCN((Element)getFirstChildElement(domNode));
        else if( type.equals("ci") )
            astNode = processCI(getFirstChildElement(domNode));

        return astNode;
    }

    protected ru.biosoft.math.model.Node processLOGBASE(org.w3c.dom.Node domNode)
    {
        //      System.err.println("processLOGBASE");
        if( getFirstChildElement(domNode) == null )
            return null;

        String type = getFirstChildElement(domNode).getNodeName();
        ru.biosoft.math.model.Node astNode = null;

        if( type.equals("cn") )
            astNode = processCN((Element)getFirstChildElement(domNode));
        else if( type.equals("ci") )
            astNode = processCI(getFirstChildElement(domNode));

        return astNode;
    }

    // we don't support <degree> in <bvar> at the moment
    protected ru.biosoft.math.model.AstVarNode processBVAR(org.w3c.dom.Node domNode)
    {
        //      System.err.println("processBVAR");
        if( getFirstChildElement(domNode) == null )
            return null;

        String type = getFirstChildElement(domNode).getNodeName();
        //System.out.println("processBVAR: " + type);
        ru.biosoft.math.model.AstVarNode astNode = null;

        if( type.equals("ci") )
            astNode = processCI(getFirstChildElement(domNode));

        return astNode;
    }

    // util function: skip all non-element tags and return first child element
    protected org.w3c.dom.Node getFirstChildElement(org.w3c.dom.Node domNode)
    {
        if( domNode == null )
            return null;
        domNode = domNode.getFirstChild();

        while( domNode != null )
        {
            if( domNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE )
            {
                //skip non-element data
                domNode = domNode.getNextSibling();
                continue;
            }
            break;
        }
        return domNode;
    }

    protected static class CSymbol
    {
        public String name;
        public int type;
        public double initialValue;
        public CSymbol(String name, int type)
        {
            this.name = name;
            this.type = type;
        }
    }
    
    /**
     * Translation from x =  piecewise( b => a; c =>d; e) to x = ( a && b ) || ( c && d ) || ( e )
     * @return functional node
     */
    public static Node translatePiecewise(AstPiecewise piecewise)
    {
        Node[] processedPieces = new Node[piecewise.jjtGetNumChildren()];
        for (int i = 0; i< piecewise.jjtGetNumChildren(); i++)
        {
            AstPiece child = (AstPiece)piecewise.jjtGetChild(i);
            Node condition = child.getCondition();
            Node value = child.getValue();
            processedPieces[i] = condition == null? value: Utils.applyFunction(condition, value, new PredefinedFunction("&&", PredefinedFunction.LOGICAL_PRIORITY, -1));
        }
       return Utils.applyFunction(processedPieces, new PredefinedFunction("||", PredefinedFunction.LOGICAL_PRIORITY, -1) );
    }
    
    public static Set<String> parserConstants = new HashSet<String>()
    {
        {
            add( "function" );
            add( "diff" );
            add( "piecewise" );
            add( "xor" );
        }
    };
}