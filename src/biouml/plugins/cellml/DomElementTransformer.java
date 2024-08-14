package biouml.plugins.cellml;

import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

/**
 * This class is used to tranform DOM element to {@link DydnamicPropertySet}
 * and vice versa. This allows property inspector show and edit DOME element
 * content as a usual bean. Further this property set can be transdormed
 * into corresponding DOM element..
 */
public class DomElementTransformer
{
    protected static final Logger log = Logger.getLogger( DomElementTransformer.class.getName() );

    /**
     * Key name that is used to store XML node type corresponding
     * to DynamicProperty. Possible values are: ELEMENT_NODE_TYPE and ATTRIBURE_NODE_TYPE.
     */
    public static final String NODE_TYPE            = "xml-node-type";
    public static final String ELEMENT_NODE_TYPE    = "element";
    public static final String ATTRIBUTE_NODE_TYPE  = "attribute";

    public static DynamicPropertySet transform(Element element, DomHelper helper)
    {
        if( log.isLoggable( Level.FINE ) )
            log.log(Level.FINE, "Transfrorm RDF:");

        return doTransform(element, helper, "");
    }

    public static DynamicPropertySet doTransform(Element element, DomHelper helper, String ident)
    {
        DynamicPropertySet propertySet = new DynamicPropertySetSupport();
        ident += "  ";

        // transform DOM element attributes
        for(Node node : XmlStream.nodes( element.getAttributes() ))
        {
            if( log.isLoggable( Level.FINE ) )
                log.log(Level.FINE, ident + "Attr: " + node.getNodeName() + ", value=" + node.getNodeValue());

            PropertyDescriptor pd = helper.getPropertyDescriptor(node, element);
            pd.setValue(NODE_TYPE, ATTRIBUTE_NODE_TYPE);
            DynamicProperty property = new DynamicProperty(pd, String.class);
            property.setValue( node.getNodeValue() );
            propertySet.add(property);
        }

        // transform DOM element children
        NodeList nodeList = element.getChildNodes();
        for(Element node : XmlUtil.elements(nodeList))
        {
            if( node.getParentNode() == element )
            {

                if( log.isLoggable( Level.FINE ) )
                    log.log(Level.FINE, ident + "Element: " + node.getNodeName() + ", size=" + node.getChildNodes().getLength());

                Class type = DynamicPropertySet.class;
                Object value = null;

                if( node.getChildNodes().getLength() != 1 )
                    value = doTransform(node, helper, ident);
                else
                {
                    type = String.class;
                    value = node.getChildNodes().item(0).getNodeValue().trim();
                }

                PropertyDescriptor pd = helper.getPropertyDescriptor(node, element);
                pd.setValue(NODE_TYPE, ELEMENT_NODE_TYPE);
                DynamicProperty property = new DynamicProperty(pd, type, value);
                propertySet.add(property);
            }
        }

        return propertySet;
    }

    /**
     * @todo implement
     */
    public static Element transform(DynamicPropertySet propertySet)
    {
        return null;
    }
}
