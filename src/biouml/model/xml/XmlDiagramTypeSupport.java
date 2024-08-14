package biouml.model.xml;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.ResourceBundle;

import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.util.XmlUtil;

public class XmlDiagramTypeSupport extends XmlDiagramTypeConstants
{
    protected static final Logger log = Logger.getLogger(XmlDiagramTypeSupport.class.getName());
    
    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected XmlDiagramType diagramType;

    protected static void error(String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.log(Level.SEVERE, message);
    }

    protected static void error(String messageBundleKey, Object[] params, Throwable t)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.log(Level.SEVERE, message, t);
    }

    protected static void warn(String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.warning(message);
    }

    public static Element getElement(Element element, String childName)
    {
        Element child = null;
        String elementName = element.getAttribute(NAME_ATTR);
        if( elementName.isEmpty() )
            elementName = element.getTagName();

        try
        {
            NodeList list = element.getChildNodes();
            Element result = null;
            for(Element node : XmlUtil.elements(list))
            {
                if( node.getNodeName().equals(childName) )
                {
                    if( result == null )
                        result = node;
                    else
                        warn("WARN_MULTIPLE_DECLARATION", new String[]{result.getTagName ( ), elementName, childName});
                }
            }

            return result;
        }
        catch(Throwable t)
        {
            error("ERROR_ELEMENT_PROCESSING", new String[]{elementName, elementName, childName, t.getMessage()});
        }

        return child;
    }
}
