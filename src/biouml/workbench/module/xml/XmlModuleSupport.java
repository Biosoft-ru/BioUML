package biouml.workbench.module.xml;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.List;
import java.util.ResourceBundle;

import java.util.logging.Logger;
import org.w3c.dom.Element;
import ru.biosoft.util.XmlStream;

public class XmlModuleSupport extends XmlModuleConstants
{
    protected static final Logger log = Logger.getLogger(XmlModuleSupport.class.getName());
    
    private static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());

    protected XmlModule xmlModule;

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
        String elementName = element.getAttribute(NAME_ATTR);
        if( elementName.isEmpty() )
            elementName = element.getTagName();

        try
        {
            List<Element> children = XmlStream.elements( element, childName ).toList();
            if( children.isEmpty() )
                return null;
            if( children.size() > 1 )
                warn( "WARN_MULTIPLE_DECLARATION", new String[] {children.get( 0 ).getTagName(), elementName, childName} );
            return children.get( 0 );
        }
        catch(Throwable t)
        {
            error("ERROR_ELEMENT_PROCESSING", new String[]{elementName, elementName, childName, t.getMessage()});
        }

        return null;
    }
}
