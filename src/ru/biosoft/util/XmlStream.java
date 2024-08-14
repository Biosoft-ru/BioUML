package ru.biosoft.util;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlStream
{
    public static StreamEx<Node> nodes(Element parent)
    {
        return nodes(parent.getChildNodes());
    }
    
    public static StreamEx<Node> nodes(NodeList nodeList)
    {
        return IntStreamEx.range( nodeList.getLength() ).mapToObj( nodeList::item );
    }
    
    public static StreamEx<Node> nodes(NamedNodeMap nodeList)
    {
        return IntStreamEx.range( nodeList.getLength() ).mapToObj( nodeList::item );
    }
    
    public static StreamEx<Element> elements(Element parent)
    {
        return nodes(parent).select( Element.class );
    }
    
    public static StreamEx<Element> elements(Element parent, String tagName)
    {
        return nodes(parent).select( Element.class ).filter( e -> e.getTagName().equalsIgnoreCase( tagName ) );
    }
    
    public static StreamEx<Element> elements(NodeList nodeList)
    {
        return nodes(nodeList).select( Element.class );
    }
    
    public static EntryStream<String, String> attributes(Element element)
    {
        return nodes(element.getAttributes()).mapToEntry( Node::getNodeName, Node::getNodeValue );
    }
}
