package biouml.plugins.sbml.validation;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * SBMLValidator is simply a container for the static method
 * validateSBML(filename, parameters).
 */
public class SBMLValidator
{
    public static final String validatorURL = "http://sbml-validator.caltech.edu:8888/validator_servlet/ValidatorServlet";

    /**
     * Validates the given SBML filename (or http:// URL) by calling the
     * SBML.org online validator. The results are returned as an
     * InputStream whose format may be controlled by setting
     * parameters.put("output", ...) to one of: "xml", "xhtml", "json",
     * "text" (default: xml).
     */
    public static String validateSBML(File f) throws Exception
    {
        List<String> errors = new ArrayList<>();
        MultipartPost post = new MultipartPost( validatorURL );
        post.writeParameter( "file", f );
        post.writeParameter( "offcheck", "u,p" );
        post.writeParameter( "output", "xml" );
        try (InputStream stream = post.done())
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(stream);
            NodeList list = document.getDocumentElement().getChildNodes();
            for( int i = 0; i < list.getLength(); i++ )
            {
                org.w3c.dom.Node node = list.item(i);
                if( node instanceof Element )
                {
                    if( node.getNodeName().equals("problem") )
                    {
                        NodeList nodes = ( (Element)node ).getElementsByTagName("location");

                        String line = ( (Element)nodes.item(0) ).getAttribute("line");
                        String column = ( ( (Element)nodes.item(0) ).getAttribute("column") );

                        nodes = ( (Element)node ).getElementsByTagName("message");
                        String message = nodes.item(0).getChildNodes().item(0).getNodeValue();

                        errors.add("At line=" + line + ", column=" + column + ":" + message);
                    }
                }
            }

            if( errors.isEmpty() )
                return null;

            return "Not a valid SBML file. Next problems occured: \n" + StringUtils.join(errors, "\n");
        }
        catch( NoSuchElementException e )
        {
            e.printStackTrace();
        }
        return null;
    }
}
