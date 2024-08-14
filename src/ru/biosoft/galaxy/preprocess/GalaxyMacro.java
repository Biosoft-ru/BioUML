package ru.biosoft.galaxy.preprocess;

import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.biosoft.util.XmlUtil;

public class GalaxyMacro
{
    private static final String YIELD_ELEMENT = "yield";
    private static final String EXPAND_ELEM = "expand";
    private static final String MACRO_NAME_ATTR = "macro";

    private Element source;
    private boolean isInit = false;

    public GalaxyMacro(Element source)
    {
        this.source = source;
    }

    public String getName()
    {
        return source.getAttribute( "name" );
    }
    
    public Element getSource()
    {
        return source;
    }

    public void init(Map<String, GalaxyMacro> scope)
    {
        if( isInit )
            return;
        applyMacros( scope, source );
        isInit = true;
    }

    public NodeList expand(NodeList param)
    {
        Element clone = (Element)source.cloneNode( true );
        if( param != null )
            replaceTag( clone, YIELD_ELEMENT, param );
        return clone.getChildNodes();
    }

    private static void replaceTag(Element elem, String tag, NodeList replacement)
    {
        NodeList targets = elem.getElementsByTagName( tag );
        for( Node target : XmlUtil.nodes(targets) )
        {
            Node parent = target.getParentNode();
            for( Node item : XmlUtil.nodes(replacement) )
            {
                item = parent.getOwnerDocument().importNode( item, true );
                parent.insertBefore( item, target );
            }
            parent.removeChild( target );
        }
    }

    public static void applyMacros(Map<String, GalaxyMacro> scope, Element elem)
    {
        Element expand;
        while( ( expand = XmlUtil.findElementByTagName(elem, EXPAND_ELEM) ) != null)
        {
            String macroName = expand.getAttribute( MACRO_NAME_ATTR );
            NodeList param = expand.getChildNodes();

            GalaxyMacro macro = scope.get( macroName );
            NodeList result = macro.expand( param );

            Node parent = expand.getParentNode();
            for(Node item : XmlUtil.nodes(result))
            {
                item = parent.getOwnerDocument().importNode( item, true );
                parent.insertBefore( item, expand );
            }
            parent.removeChild( expand );
        }
    }
}
