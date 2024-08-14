package biouml.plugins.cellml;

import java.beans.PropertyDescriptor;
import java.util.Enumeration;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.XmlStream;

/**
 * Spike solution for DomHelper.
 */
public class DomHelperSupport implements DomHelper
{
    @Override
    public PropertyDescriptor getPropertyDescriptor(Node node, Element parent)
    {
        if( node instanceof Element )
            return getElementDescriptor( (Element)node, parent);
        else
            return getAttributeDescriptor(node, parent);
    }

    ///////////////////////////////////////////////////////////////////
    // Attribute issues
    //

    protected HashMap<String, PropertyDescriptor> attributes = new HashMap<>();

    public void addAttribute(String name, PropertyDescriptor pd)
    {
        attributes.put(name, pd);
    }

    public boolean hasAttribute(String name)
    {
        return attributes.containsKey(name);
    }

    public PropertyDescriptor getAttributeDescriptor(Node node, Element parent)
    {
        String name = node.getNodeName();
        if( attributes.containsKey(name) )
            return attributes.get(name);

        // try to create attribute PropertyDescriptor
        PropertyDescriptor pd = BeanUtil.createDescriptor(name);

        pd.setExpert( isExpert(name) );
        pd.setHidden( isHidden(name) );
        pd.setDisplayName( getDisplayName(name) );

        return pd;
    }

    /**
     * Indicates whether this property should be marked as expert.
     *
     * This method marks xmlns properties as expert.
     */
    protected boolean isExpert(String name)
    {
        return name.startsWith("xmlns:");
    }

    /**
     * Indicates whether this property should be hidden.
     *
     * This method does nod hide any properties.
     */
    protected boolean isHidden(String name)
    {
        return false;
    }

    /**
     * Returns property display name that can differ from property programmatic name.
     *
     * This method returns property programmatic name that is the same as
     * corresponding DOM node name.
     */
    protected String getDisplayName(String name)
    {
        return name;
    }

    ///////////////////////////////////////////////////////////////////
    // Element issues
    //

    protected HashMap<String, PropertyDescriptor> elements   = new HashMap<>();
    public void addElement(String name, PropertyDescriptor pd)
    {
        elements.put(name, pd);
    }

    public boolean hasElement(String name)
    {
        return elements.containsKey(name);
    }

    public PropertyDescriptor getElementDescriptor(Element child, Element parent)
    {
        String name = child.getNodeName();

        // if parent contains several childs, we need to add index
        // helper method is used to know whether the child element
        // should be unique
        String index = getElementIndex(child, parent);
        String programmaticName = name + index;

        PropertyDescriptor pd = null;
        if( elements.containsKey(name) )
        {
            pd = attributes.get(name);
            if( index != null && index.length() > 0 )
                pd = clone(pd, programmaticName);

            return pd;
        }

        // try to create element PropertyDescriptor
        pd = BeanUtil.createDescriptor(name);

        pd.setExpert( isExpert(name) );
        pd.setHidden( isHidden(name) );
        pd.setDisplayName( getDisplayName(name) );

        return pd;
    }

    /**
     * If parent contains several childs, we need to add index to property programmatic name.
     */
    protected String getElementIndex(Element child, Element parent)
    {
        NodeList list = parent.getElementsByTagName(child.getNodeName());
        return "[" + XmlStream.nodes( list ).indexOf( child ).getAsLong() + "]";
    }

    protected PropertyDescriptor clone(PropertyDescriptor pd, String name)
    {
        PropertyDescriptor result = BeanUtil.createDescriptor(name);

        result.setBound         ( pd.isBound() );
        result.setConstrained   ( pd.isConstrained() );
        result.setExpert        ( pd.isExpert() );
        result.setHidden        ( pd.isHidden() );
        result.setPreferred     ( pd.isPreferred() );

        result.setDisplayName       ( pd.getDisplayName() );
        result.setShortDescription  ( pd.getShortDescription() );
        result.setPropertyEditorClass( pd.getPropertyEditorClass() );

        Enumeration<String> e = pd.attributeNames();
        while ( e.hasMoreElements() )
        {
            String atr = e.nextElement();
            result.setValue( atr, pd.getValue( atr ) );
        }

        return result;
    }
}


