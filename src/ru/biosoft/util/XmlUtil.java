package ru.biosoft.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlUtil
{
    public static Element getChildElement(Element root, String name)
    {
        for( Node child = root.getFirstChild(); child != null; child = child.getNextSibling() )
            if( child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName()) )
                return (Element)child;
        return null;
    }
    
    public static Element getChildElement(Element root, String attributeName, String attributeValue)
    {
        for( Node child = root.getFirstChild(); child != null; child = child.getNextSibling() )
            if( child.getNodeType() == Node.ELEMENT_NODE && attributeValue.equals(((Element)child).getAttribute(attributeName)) )
                return (Element)child;
        return null;
    }

    public static String getTextContent(Element root)
    {
        NodeList nodeList = root.getChildNodes();
        if( nodeList.getLength() > 0 )
        {
            Node k = nodeList.item(0);
            return k.getNodeValue();
        }
        return "";
    }
    
    /**
     * Obtains string from value of attribute with given name if present,
     * else from text content of child element with given name if present,
     * else return empty string.
     */
    public static String getAttributeOrText(Element element, String name)
    {
        if(element.hasAttribute(name))
            return element.getAttribute(name);
        Element childElement = getChildElement(element, name);
        if( childElement != null )
            return getTextContent(childElement);
        return "";
    }
    
    public static String getAttribute(Element element, String name, String defaultValue)
    {
        return element.hasAttribute(name) ? element.getAttribute(name) : defaultValue;
    }
    
    public static Element findElementByTagName(Element root, String name)
    {
        NodeList nodes = root.getElementsByTagName(name);
        if(nodes.getLength() == 0)
            return null;
        return (Element)nodes.item(0);
    }
    
    /**
     * Returns an iterable which iterates over all children Elements of the given (skipping any non-Element Nodes)
     * @param element parent Element
     * @return Iterable object
     */
    public static Iterable<Element> elements(Element element)
    {
        return elements(element.getChildNodes());
    }
    
    /**
     * Returns an iterable which iterates over all children Elements of the given (skipping any non-Element Nodes) with the specified tag name
     * @param element parent Element
     * @return Iterable object
     */
    public static Iterable<Element> elements(Element element, String tagName)
    {
        return elements(element.getChildNodes(), tagName);
    }
    
    /**
     * Returns an iterable which iterates over all Elements from the NodeList (skipping any non-Element Nodes)
     * @param nodes NodeList to iterate
     * @return Iterable object
     */
    public static Iterable<Element> elements(final NodeList nodes)
    {
        return () -> new ReadAheadIterator<Element>()
        {
            int i=0;

            @Override
            protected Element advance()
            {
                while(true)
                {
                    Node node = nodes.item(i++);
                    if(node == null)
                        return null;
                    if(node instanceof Element)
                        return (Element)node;
                }
            }
        };
    }
    
    /**
     * Returns an iterable which iterates over all Elements from the NodeList (skipping any non-Element Nodes) with the specified tag name
     * @param nodes NodeList to iterate
     * @param tagName tag name to compare with
     * @return Iterable object
     */
    public static Iterable<Element> elements(final NodeList nodes, final String tagName)
    {
        return () -> new ReadAheadIterator<Element>()
        {
            int i=0;

            @Override
            protected Element advance()
            {
                while(true)
                {
                    Node node = nodes.item(i++);
                    if(node == null)
                        return null;
                    if(node instanceof Element && ((Element)node).getTagName().equalsIgnoreCase(tagName))
                        return (Element)node;
                }
            }
        };
    }
    
    /**
     * Returns an iterable which iterates over all child nodes from the Element
     * @param element parent Element
     * @return Iterable object
     */
    public static Iterable<Node> nodes(Node element)
    {
        return nodes(element.getChildNodes());
    }
    
    /**
     * Returns an iterable which iterates over all nodes from the NodeList
     * @param nodes NodeList to iterate
     * @return Iterable object
     */
    public static Iterable<Node> nodes(final NodeList nodes)
    {
        return () -> new ReadAheadIterator<Node>()
        {
            int i=0;

            @Override
            protected Node advance()
            {
                return nodes.item(i++);
            }
        };
    }

    public static int readDoubleAsInt(Element element, String attr)
    {
        return (int)Math.round( Double.parseDouble( element.getAttribute( attr ) ));
    }
}
