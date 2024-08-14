package biouml.plugins.cellml;

import java.beans.PropertyDescriptor;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Utility class that provides meta information for DomElementTransformer.
 */
public interface DomHelper
{
    /**
     * Returns {@link PropertyDescriptor} for the specified node (attribute or element).
     */
    public PropertyDescriptor getPropertyDescriptor(Node node, Element parent);

    /**
     * Indicates whether the element should be mapped into composite or simple property.
     */
//    public boolean isComposite(Element element);
}


