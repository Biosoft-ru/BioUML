package ru.biosoft.galaxy;

import org.w3c.dom.Element;

import ru.biosoft.util.XmlUtil;

public class Requirement
{
    public static enum Type {PACKAGE, BINARY, GALAXY_LIB}
    private Type type;
    private String name;
    private String version;
    
    public Requirement(Type type, String name, String version)
    {
        this.type = type;
        this.name = name;
        this.version = version;
    }
    
    public Requirement(Element element)
    {
        String typeSpec = XmlUtil.getAttribute( element, "type", "binary" );
        try
        {
            this.type = Type.valueOf( typeSpec.toUpperCase() );
        }
        catch( IllegalArgumentException e )
        {
            throw new IllegalArgumentException("Unsupported requirement type '" + typeSpec + "'");
        }
        this.name = XmlUtil.getTextContent( element );
        this.version = XmlUtil.getAttribute( element, "version", "" );
    }

    public Type getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }
    
    @Override
    public String toString()
    {
        String result = type + " " + name;
        if(!version.isEmpty())
            result += " " + version;
        return result;
    }
}
