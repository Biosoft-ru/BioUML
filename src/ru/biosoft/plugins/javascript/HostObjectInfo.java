package ru.biosoft.plugins.javascript;

import java.util.Iterator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * Provides description of JavaScript host object.
 * Description also includes information about hostObject properties and functions.
 *
 * <p><code>HostObjectInfo</code> implements <code>DataCollection</code> interface,
 * so it can be shown in repository tree.
 *
 * @see JScriptContext#defineFunction
 */
public class HostObjectInfo extends VectorDataCollection
{
    private Class<?> objectClass;
    private String alias;
    
    /**
     * Creates info for the specified host object.
     *
     * @param name - host object name
     * @param parent - <code>DataCollection</code> to which this host object belongs.
     * @param objectClass - java class which corresponds to this host object
     * Generally it is <code>plugins/Javascript/host objects</code>.
     */
    public HostObjectInfo(String name, DataCollection parent, Class<?> objectClass)
    {
        super(name, parent, null);
        this.objectClass = objectClass;
    }

    public HostObjectInfo(String name, DataCollection parent)
    {
        this(name, parent, null);
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer("\n");

        buf.append("<h2 align=center>");
        buf.append("JavaScript host object");
        buf.append("</h2>");
        buf.append("\n");

        buf.append(type);
        buf.append(' ');
        buf.append("<b>");
        buf.append(getName());
        buf.append("</b>");
        buf.append("<br>");        
        buf.append("Alias: ");
        buf.append("<b>");
        buf.append(alias);
        buf.append("</b>");
        buf.append("\n");

        if( description != null && description.length() > 0 )
        {
            buf.append("<p>");
            buf.append(description);
            buf.append("</p>");
            buf.append("\n");
        }

        //print functions declaration
        buf.append("<h3 align=center>");
        buf.append("Functions:");
        buf.append("</h3><ul>");
        buf.append("\n");

        Iterator<?> iter = iterator();
        while( iter.hasNext() )
        {
            Object obj = iter.next();
            if( obj instanceof FunctionInfo )
            {
                FunctionInfo fInfo = (FunctionInfo)obj;
                buf.append("<li>");
                buf.append(fInfo.getFunctionDeclaration());
                buf.append("\n");
            }
        }

        buf.append("</ul>");
        buf.append("\n");

        return buf.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /** Property type */
    private String type;
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }

    /** The function description */
    protected String description;
    @Override
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }

    public Class<?> getObjectClass()
    {
        return objectClass;
    }
    
    public void setAlias(String alias)
    {
        this.alias = alias;
    }
}
