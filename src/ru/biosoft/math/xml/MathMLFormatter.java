package ru.biosoft.math.xml;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.AstPiece;
import ru.biosoft.math.model.AstPiecewise;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Formatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.ParserContext;
import ru.biosoft.math.model.Utils;

public class MathMLFormatter implements Formatter
{
    protected Document doc = null;
    protected Hashtable<String, String> mathSignsMap = new Hashtable<>();
    protected ParserContext parserContext;
    
    public ParserContext getParserContext()
    {
        return parserContext;
    }
    public void setParserContext(ParserContext parserContext)
    {
        this.parserContext = parserContext;
    }

    public MathMLFormatter()
    {
        fillMathSignsMap();
        definitionUrlToName = new HashMap<>();
    }

    @Override
    public String[] format(AstStart start)
    {
        createDocument();
        if( doc == null )
            return null;

        processStartNode(start, doc);

        return new String[] {"", dom2String()};
    }


    public void format(AstStart start, org.w3c.dom.Node parent, org.w3c.dom.Document document)
    {
        if( parent != null )
        {
            doc = document;
            processStartNode(start, parent);
        }
    }

    protected void processStartNode(AstStart start, org.w3c.dom.Node parent)
    {
        Element math = doc.createElement("math");
        math.setAttribute("xmlns", "http://www.w3.org/1998/Math/MathML");
        parent.appendChild(math);

        for(Node node : Utils.children( start ))
            processNode(node, math);
    }

    protected void processNode(ru.biosoft.math.model.Node astNode, org.w3c.dom.Node parent)
    {
        if( astNode instanceof AstConstant )
            processConstant((AstConstant)astNode, parent);

        else if( astNode instanceof AstVarNode )
            processVariable((AstVarNode)astNode, parent);

        else if( astNode instanceof AstFunNode )
            processFunction((AstFunNode)astNode, parent);

        else if( astNode instanceof AstFunctionDeclaration )
            processFunctionDeclaration((AstFunctionDeclaration)astNode, parent);

        else if( astNode instanceof AstPiece )
            processPiece((AstPiece)astNode, parent);

        else if( astNode instanceof AstPiecewise )
            processPiecewise((AstPiecewise)astNode, parent);
    }

    protected void processPiecewise(AstPiecewise pw, org.w3c.dom.Node parent)
    {
        Element pwise = doc.createElement("piecewise");
        for(Node node : Utils.children( pw ))
            processNode(node, pwise);
        parent.appendChild(pwise);
    }

    protected void processPiece(AstPiece piece, org.w3c.dom.Node parent)
    {
        Element p = null;
        if( piece.jjtGetNumChildren() == 2 )
        {
            p = doc.createElement("piece");
            processNode(piece.jjtGetChild(1), p);
            processNode(piece.jjtGetChild(0), p);
        }
        else
        {
            p = doc.createElement("otherwise");
            processNode(piece.jjtGetChild(0), p);
        }

        parent.appendChild(p);
    }

    protected void processConstant(AstConstant constant, org.w3c.dom.Node parent)
    {
        String name = null;
        if( constant.getName() != null )
        {
            // symbolic constant
            name = constant.getName();
        }
        else
        {
            // number, string or boolean value
            name = constant.getValue().toString();
        }
        if( "true".equals( name ) || "false".equals( name ) )
        {
            parent.appendChild( doc.createElement( name ) );
        }
        else if ( "pi".equals(name) || "exp".equals(name) || "exponentiale".equals(name))
        {
            parent.appendChild(doc.createElement(name));
        }
        else
        {
            Element cn = doc.createElement("cn");
            Text cnValue = doc.createTextNode(name);
            cn.appendChild(cnValue);
            parent.appendChild(cn);
        }
    }

    protected void processVariable(AstVarNode var, org.w3c.dom.Node parent)
    {
        String name = var.getName();
        Text ciValue = doc.createTextNode(name);
        if( var.isCSymbol() )
        {
            Element csymbol = doc.createElement("csymbol");
            parent.appendChild(csymbol);
        }
        else if ( definitionUrlToName.containsKey(name) )
        {
            Element csymbol = doc.createElement("csymbol");
            csymbol.appendChild(ciValue);
            csymbol.setAttribute("definitionURL", definitionUrlToName.get(name));
            csymbol.setAttribute("encoding", "text");
            parent.appendChild(csymbol);
        }
        else
        {
            Element ci = doc.createElement("ci");
            ci.appendChild(ciValue);
            parent.appendChild(ci);
        }
    }

    protected void processFunction(AstFunNode astFunc, org.w3c.dom.Node parent)
    {
        Element apply = doc.createElement("apply");
        parent.appendChild(apply);

        String sign = astFunc.getFunction().getName();
        String name = mathSignsMap.get(sign);
        if( name == null )
        {
            name = sign;
            if (this.definitionUrlToName.containsKey(name))
            {
                Element csymbolTag = doc.createElement("csymbol");
                apply.appendChild(csymbolTag);
                Text nameText = doc.createTextNode(name);
                csymbolTag.appendChild(nameText);
                csymbolTag.setAttribute("definitionURL", definitionUrlToName.get(name));
                csymbolTag.setAttribute("encoding", "text");
            }
            else if( parserContext == null || parserContext.getFunction( name ) != null )
            {
                Text funcName = doc.createTextNode(name);
                Element ciTag = doc.createElement("ci");
                ciTag.appendChild(funcName);
                apply.appendChild(ciTag);
            }
            else
            {
                Element csymbolTag = doc.createElement("csymbol");
                apply.appendChild(csymbolTag);
                Text nameText = doc.createTextNode(name);
                csymbolTag.appendChild(nameText);
            }
        }
        else
        {
            // special definition for function called by <ci>
            if( astFunc.getFunction() instanceof AstFunctionDeclaration )
            {
                Element ciTag = doc.createElement("ci");
                apply.appendChild(ciTag);
                Text funcName = doc.createTextNode(name);
                ciTag.appendChild(funcName);
            }
            else
            {
                Element nameTag = doc.createElement(name);
                apply.appendChild(nameTag);
            }
        }

        if( name.equals( "log" ) )
        {
            if( astFunc.jjtGetNumChildren() == 2 )
            {
                Element logbase = doc.createElement( "logbase" );
                apply.appendChild( logbase );
                processNode( astFunc.jjtGetChild( 1 ), logbase );
                processNode( astFunc.jjtGetChild( 0 ), apply );
            }
            else if( astFunc.jjtGetNumChildren() == 1 )
            {
                processNode( astFunc.jjtGetChild( 0 ), apply );
            }
            return;
        }

        if( name.equals("root") )
        {
            if( astFunc.jjtGetNumChildren() == 2 )
            {
                Element degree = doc.createElement( "degree" );
                apply.appendChild( degree );
                processNode( astFunc.jjtGetChild( 1 ), degree );
            }
            processNode( astFunc.jjtGetChild( 0 ), apply );

            return;
        }

        if( name.equals("diff") )
        {
            int n = astFunc.jjtGetNumChildren();
            for( int i = 0; i < n - 1; i++ )
            {
                Element bvar = doc.createElement("bvar");
                apply.appendChild(bvar);
                processNode(astFunc.jjtGetChild(i), bvar);
            }
            processNode(astFunc.jjtGetChild(n - 1), apply);
            return;
        }

        for( Node node : Utils.children( astFunc ) )
            processNode(node, apply);
    }

    protected void processFunctionDeclaration(AstFunctionDeclaration astFuncDecl, org.w3c.dom.Node parent)
    {
        Element lambda = doc.createElement("lambda");
        parent.appendChild(lambda);

        int n = astFuncDecl.jjtGetNumChildren();
        for( int i = 0; i < n - 1; i++ )
        {
            Element bvar = doc.createElement("bvar");
            lambda.appendChild(bvar);
            processNode(astFuncDecl.jjtGetChild(i), bvar);
        }
        processNode(astFuncDecl.jjtGetChild(n - 1), lambda);
    }

    protected void createDocument()
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        }
        catch( Throwable t )
        {
            System.out.println("Can NOT create DOM document");
            t.printStackTrace();
        }
    }

    protected String dom2String()
    {
        try
        {
            DOMSource source = new DOMSource(doc);

            StringWriter string = new StringWriter();
            StreamResult result = new StreamResult(string);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(source, result);

            String rawOut = string.toString();
            int n = rawOut.indexOf('<');
            n = rawOut.indexOf('<', n + 1);
            String out = rawOut.substring(n);

            return out;
        }
        catch( Throwable t )
        {
            System.out.println("Can NOT create create string representation of DOM documnet");
            t.printStackTrace();
            return null;
        }
    }

    protected void fillMathSignsMap()
    {
        mathSignsMap.put("||", "or");
        mathSignsMap.put("&&", "and");
        mathSignsMap.put("!", "not");
        mathSignsMap.put("xor", "xor");

        mathSignsMap.put(">", "gt");
        mathSignsMap.put("<", "lt");
        mathSignsMap.put("==", "eq");
        mathSignsMap.put("<=", "leq");
        mathSignsMap.put(">=", "geq");
        mathSignsMap.put("!=", "neq");

        mathSignsMap.put("+", "plus");
        mathSignsMap.put("-", "minus");
        mathSignsMap.put("u-", "minus");
        mathSignsMap.put("*", "times");
        mathSignsMap.put("/", "divide");
        mathSignsMap.put("^", "power");
        mathSignsMap.put("root", "root");
        mathSignsMap.put("abs", "abs");
        mathSignsMap.put("exp", "exp");
        mathSignsMap.put("ln", "ln");
        mathSignsMap.put("log", "log");

        mathSignsMap.put("sin", "sin");
        mathSignsMap.put("cos", "cos");
        mathSignsMap.put("tan", "tan");
        mathSignsMap.put("cot", "cot");
        mathSignsMap.put("sec", "sec");
        mathSignsMap.put("csc", "csc");
        mathSignsMap.put("sinh", "sinh");
        mathSignsMap.put("cosh", "cosh");
        mathSignsMap.put("tanh", "tanh");
        mathSignsMap.put("coth", "coth");
        mathSignsMap.put("sech", "sech");
        mathSignsMap.put("csch", "csch");

        mathSignsMap.put("arcsin", "arcsin");
        mathSignsMap.put("arccos", "arccos");
        mathSignsMap.put("arctan", "arctan");
        mathSignsMap.put("arcsec", "arcsec");
        mathSignsMap.put("arccsc", "arccsc");
        mathSignsMap.put("arccot", "arccot");
        mathSignsMap.put("arcsinh", "arcsinh");
        mathSignsMap.put("arccosh", "arccosh");
        mathSignsMap.put("arctanh", "arctanh");
        mathSignsMap.put("arcsech", "arcsech");
        mathSignsMap.put("arccsch", "arccsch");
        mathSignsMap.put("arccoth", "arccoth");

        mathSignsMap.put("diff", "diff");
        mathSignsMap.put("=", "eq");

        mathSignsMap.put("ceiling", "ceiling");
        mathSignsMap.put("floor", "floor");
        mathSignsMap.put("factorial", "factorial");
        
        mathSignsMap.put("quotient", "quotient");
        mathSignsMap.put("min", "min");
        mathSignsMap.put("max", "max");
        
        mathSignsMap.put("sqrt", "root");
    }
    
    private final Map<String, String> definitionUrlToName;
    
    public void declareCSymbol(String varName, String definitionURL)
    {
        definitionUrlToName.put(varName, definitionURL);
    }
}
