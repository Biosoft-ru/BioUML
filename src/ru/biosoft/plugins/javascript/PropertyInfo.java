package ru.biosoft.plugins.javascript;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

/**
 * Provides description for property of JavaScript class or host object.
 *
 * <p><code>PropertyInfo</code> implements <code>ru.biosoft.access.core.DataElement</code> interface,
 * so it can be shown in repository tree.
 *
 * @see JScriptContext#defineFunction
 */
public class PropertyInfo extends DataElementSupport
{
    /**
     * Creates info for the specified property.
     *
     * @param name - property name
     * @param parent - info for host object or class to which this property belongs.
     * Generally it is <code>plugins/Javascript/host objects/object</code>
     * or <code>plugins/Javascript/classes/class</code>.
     */
    public PropertyInfo(String name, DataCollection parent)
    {
        super(name, parent);
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer("\n");

        buf.append("<h2 align=center>");
        buf.append(getOrigin().getName() + "." + getName() + " property");
        buf.append("</h2>");
        buf.append("\n\n");

        buf.append(type);
        buf.append(' ');
        buf.append("<b>");
        buf.append(getName());
        buf.append("</b>");
        if( readOnly )
            buf.append("(read only)");
        buf.append("\n\n");

        if( description != null && description.length() > 0 )
        {
            buf.append("<p>");
            buf.append(description);
            buf.append("\n\n");
        }

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

    /** Indicates whether this property is read only. */
    private boolean readOnly;
    public boolean isReadOnly()
    {
        return readOnly;
    }
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    /** The function description */
    protected String description;
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }
}
