package biouml.plugins.sbml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.util.DiagramXmlReader;
import biouml.standard.type.Specie;
import one.util.streamex.StreamEx;
import ru.biosoft.util.DPSUtils;

public class SbmlSupport extends SbmlConstants
{
    protected Logger log;
    protected String modelName;
    protected Diagram diagram;
    protected SbmlEModel emodel;
    protected Map<String, String> newPaths;
        
    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }  
    
    public Map<String, String> getNewPaths()
    {
        return newPaths;
    }  
    
    protected void warn(String key, String[] params)
    {
        MessageBundle.warn(log, key, params);
    }

    protected void error(String key, String[] params)
    {
        MessageBundle.error(log, key, params);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Common utility methods
    //

    public Element getElement(Element element, String childName)
    {
        Element child = null;
        String elementName = element.getAttribute(NAME_ATTR);
        if( elementName.isEmpty() )
        {
            elementName = element.getTagName();
        }
        try
        {
            NodeList list = element.getChildNodes();
            Element result = null;
            for( int i = 0; i < list.getLength(); i++ )
            {
                org.w3c.dom.Node node = list.item(i);
                if( node instanceof Element && node.getNodeName().equals(childName) )
                {
                    if( result == null )
                    {
                        result = (Element)node;
                    }
                    else
                    {
                        warn("WARN_MULTIPLE_DECLARATION", new String[] {modelName, elementName, childName});
                    }
                }
            }

            return result;
        }
        catch( Throwable t )
        {
            error("ERROR_ELEMENT_PROCESSING", new String[] {modelName, elementName, childName, t.getMessage()});
        }

        return child;
    }
  
    public static String getBriefName(String fullName)
    {
        int index = fullName.lastIndexOf( "." );
        return ( index == -1 )? fullName: fullName.substring( index + 1 );
    }


    /**
     * Reads html<body> section and returns its content as a single string.
     *
     * For apache.crimson we can get the content as:
     * <pre> String html = element.toString(); </pre>
     * but it does not works for apache.xerces.
     * Thus we implement an approach that has not any assumptions
     * and should work for all XML DOM parsers.
     */
    public String readXhtml(Element element)
    {
        try
        {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);

            DOMSource source = new DOMSource(element);

            //Do the transformation and output
            transformer.transform(source, result);

            String html = sw.toString();

            String body = ru.biosoft.util.TextUtil.getSection("body", html);
            if( body == null )
            {
                body = ru.biosoft.util.TextUtil.getSection("notes", html);
            }
            if( body == null )
            {
                body = html;
            }
            body = removeLongTables(body);
            return body;
        }
        catch( Throwable t )
        {
            error("ERROR_HTML_PROCESSING", new String[] {modelName, t.getMessage(), element.toString()});
            return null;
        }
    }

    protected final static int MAX_SIZE = 30000;
    /**
     * Remove very big tables from description (useful for Biomodels)
     */
    protected String removeLongTables(String html)
    {
        if( html.length() > MAX_SIZE )
        {
            String result = html;
            int offset = 0;

            while( true )
            {
                int start = result.indexOf("<table", offset);
                if( start == -1 )
                    break;
                int end = result.indexOf("</table>", start);
                if( end == -1 )
                    break;
                if( ( end - start ) > MAX_SIZE )
                {
                    result = result.substring(0, start) + result.substring(end + 8, result.length());
                    offset = start;
                }
                else
                {
                    offset = end;
                }
            }
            return result;
        }
        return html;
    }
    public void writeXhtml(Document document, Element element, String notes)
    {
        try
        {
            String html = notes;
            String body = ru.biosoft.util.TextUtil.getSection("body", html);
            if( body != null )
            {
                html = body;

            }
            html = "<body xmlns=\"http://www.w3.org/1999/xhtml\">" + html + "</body>";

            html = html.replace( "&nbsp;", " " );
            html = html.replace( "&ndash;", "-" );
            html = html.replace("&rdquo;", "\""); 
            html = html.replace("&ldquo;", "\""); 
            
            InputSource inputSource = new InputSource(new StringReader(html));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputSource);

            org.w3c.dom.Node node = document.importNode(doc.getFirstChild(), true);
            element.appendChild(node);
        }
        catch( Throwable t )
        {
            error("ERROR_XTML_WRITING", new String[] {diagram.getName(), t.getMessage(), notes});
        }
    }

    /**
     * General template to cinvert SBML formula into BioUML formula.
     *
     * Convertion rules are specified by corresponding procedures that are redefiened in subclasses:
     *
     * 1) Specie name:             specieName       $specieName
     * 2) Compartment name:        compartmentName  $compartmentName
     * 3) Reaction parameter name: parameterName    reactionName_parameterName
     */
    public String parseFormula(String formula, Node reaction)
    {
        if( formula == null )
            return null;

        String delimiters = "\n\t ,;()[]+-/%*^|&";
        StringTokenizer tokens = new StringTokenizer(formula.trim(), delimiters, true);
        StringBuffer result = new StringBuffer();

        while( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();

            // process delimiters
            if( delimiters.indexOf(token) >= 0 )
            {
                if( token.charAt(0) != '\n' && token.charAt(0) != '\t' )
                {
                    result.append(token);
                }
                continue;
            }

            if( parseAsSpecie(token, result) || parseAsCompartment(token, result) || parseAsParameter(token, result, reaction) )
            {
                continue;
            }

            result.append(token);
        }

        return result.toString();
    }

    protected boolean parseAsSpecie(String token, StringBuffer result)
    {
        return false;
    }

    protected boolean parseAsCompartment(String token, StringBuffer result)
    {
        return false;
    }

    protected boolean parseAsParameter(String token, StringBuffer result, Node reaction)
    {
        return false;
    }

    public static String castStringToSId(String input)
    {
        String result = input.replaceAll("\\W", "_");
        if( result.matches("\\d\\w*") )
        {
            result = "_" + result;
        }
        return result;
    }
    
    public static String castFullName(String fullName)
    {
        return StreamEx.of(fullName.split("\\.")).map(s -> castStringToSId(s)).joining(".");
    }

    protected boolean shouldLayout = true;
    protected void readBioUMLAnnotation(Element annotationElement, DiagramElement de, String expectedInfo)
    {
        Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );

        Element nodeInfoElement = bioumlElement != null ? getElement( bioumlElement, expectedInfo )
                : getElement( annotationElement, expectedInfo );

        if( nodeInfoElement != null )
        {
            shouldLayout = false;
            if( de instanceof Compartment )
                DiagramXmlReader.readCompartmentInfo( nodeInfoElement, (Compartment)de, diagram.getName() );
            if( de instanceof Node )
                DiagramXmlReader.readNodeInfo( nodeInfoElement, (Node)de, diagram.getName() );
            else if( de instanceof Edge )
                DiagramXmlReader.readEdgeInfo( nodeInfoElement, (Edge)de, diagram.getName() );
        }
    }

    protected String readEdgeName(Element annotationElement, String expectedInfo)
    {
        Element bioumlElement = getElement( annotationElement, BIOUML_ELEMENT );

        Element infoElement = bioumlElement != null ? getElement( bioumlElement, expectedInfo )
                : getElement( annotationElement, expectedInfo );
        if( infoElement != null )
        {
            String id = DiagramXmlReader.readEdgeID(infoElement);
            return ( castStringToSId(id) );
        }
        return "";
    }

    /**
     * Read sboTerm attribute
     */
    protected void readSBOTerm(Element element, DynamicPropertySet dps)
    {
        DynamicProperty dp = new DynamicProperty(SBO_TERM_ATTR, String.class, element.getAttribute(SBO_TERM_ATTR));
        DPSUtils.makeTransient(dp);
        dps.add(dp);
    }
    
    public static void writeSBOTerm(Element element, DynamicPropertySet dps)
    {
        Object sboTerm = dps.getValue( SBO_TERM_ATTR );
        if( sboTerm != null && ( sboTerm instanceof String ) && ( (String)sboTerm ).length() > 0 )
            element.setAttribute( SBO_TERM_ATTR, (String)sboTerm );
    }
    

    protected List<Node> fillSpecieList(Diagram diagram)
    {
        return diagram.recursiveStream().select(Node.class).filter(n->n.getKernel() instanceof Specie).toList();
    }
    

    protected static List<Compartment> fillCompartmentList(Diagram diagram)
    {
        return diagram.recursiveStream().select(Compartment.class).filter(c->c.getKernel() instanceof biouml.standard.type.Compartment).toList();
    }
    
    protected double readDouble(Element element, String attr, String error, double defaultValue)
    {
        double result = defaultValue;
        if( element.hasAttribute(attr) )
        {
            try
            {
                result = Double.parseDouble(element.getAttribute(attr));
            }
            catch( NumberFormatException ex )
            {
                log.log(Level.SEVERE, error);
            }
        }
        return result;
    }
    
    public static double parseSBMLDoubleValue(String value) throws NumberFormatException
    {
        if( value.equals(SbmlConstants.POSITIVE_INFINITY) )
            return Double.POSITIVE_INFINITY;
        else if( value.equals(SbmlConstants.NEGATIVE_INFINITY) )
            return Double.NEGATIVE_INFINITY;
        else if( value.equals(SbmlConstants.NAN) )
            return Double.NaN;
        return Double.parseDouble(value);
    }
    
    protected double readDouble(Element element, String attr, double defaultValue, String errorCode, String deID)
    {
        if( element.hasAttribute(attr) )
        {
            String attrValue = element.getAttribute(attr);
            try
            {
                return parseSBMLDoubleValue(attrValue);
            }
            catch( NumberFormatException ex )
            {
                error(errorCode, new String[] {modelName, deID, attrValue, ex.toString()});
            }
        }
        return defaultValue;
    }
    
    protected int readInt(Element element, String attr, int defaultValue, String errorCode, String deID)
    {
        if( element.hasAttribute(attr) )
        {
            String attrValue = element.getAttribute(attr);
            try
            {
                return Integer.parseInt(attrValue);
            }
            catch( NumberFormatException ex )
            {
                error(errorCode, new String[] {modelName, deID, attrValue, ex.toString()});
            }
        }
        return defaultValue;
    }
    
    protected boolean readObligatoryBoolean(Element element, String attr, boolean defaultValue, String error)
    {
        if( element.hasAttribute( attr ) )
            return Boolean.parseBoolean( element.getAttribute( attr ) );
        log.log(Level.SEVERE, error);
        return defaultValue;
    }
    
    protected boolean readOptionalBoolean(Element element, String attr, boolean defaultValue)
    {
        return ( element.hasAttribute( attr ) ) ? Boolean.parseBoolean( element.getAttribute( attr ) ) : defaultValue;
    }
}
