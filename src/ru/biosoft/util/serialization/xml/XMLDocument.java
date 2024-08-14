package ru.biosoft.util.serialization.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 27.06.2006
 * Time: 14:03:32
 * <p/>
 * Simpliest implementation of serializable XML document
 */
public class XMLDocument
{
    String encoding;

    public XMLDocument( String encoding )
    {
        this.encoding = encoding;
    }

    public XMLDocument()
    {
        this.encoding = "ISO-8859-1";
    }

    public static class Element
    {
        public String name;

        public Map<String, String> attributes = new HashMap<>();
        public List<Element> children = new ArrayList<>();

        public Element( String name )
        {
            this.name = name;
        }

        public void setAttribute( String name, String value )
        {
            attributes.put( name, value );
        }

        public void appendChild( Element elem )
        {
            children.add( elem );
        }

        public boolean hasChildren()
        {
            return children.size() > 0;
        }
    }

    List<Element> children = new ArrayList<>();

    public void appendChild( Element elem )
    {
        children.add( elem );
    }

    public Element createElement( String name )
    {
        return new Element( name );
    }

    public void serialize( Writer writer ) throws IOException
    {
        writer.write( "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>" );
        for( Element element : children )
        {
            serializeElement( writer, element );
        }
    }

    private void serializeElement( Writer writer, Element e ) throws IOException
    {
        writer.write( "<" + e.name );
        for( Map.Entry<String, String> entry : e.attributes.entrySet() )
        {
            writer.write( " " + entry.getKey() + "=\"" + XMLUtil.escape( entry.getValue() ) + "\"" );
        }
        if( e.hasChildren() )
        {
            writer.write( ">" );
            for( Element c : e.children )
            {
                serializeElement( writer, c );
            }
            writer.write( "</" + e.name + ">" );
        }
        else
        {
            writer.write( "/>" );
        }
    }
}
