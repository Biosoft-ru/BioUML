package biouml.standard.type;

import java.beans.IntrospectionException;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElementSupport;
import ru.biosoft.exception.InternalException;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Base class for all data element types.
 *
 * @pending we are using lazy initialization for attributes;
 */
public class BaseSupport extends MutableDataElementSupport implements Base, CloneableDataElement
{
    private static final long serialVersionUID = -7951348639656468291L;
    
    protected String type;
    protected String title;
    protected DynamicPropertySet attributes;
    
    protected BaseSupport(DataCollection parent, String name)
    {
        this(parent, name, TYPE_UNKNOWN);
    }

    public BaseSupport(DataCollection parent, String name, String type)
    {
        super(parent, name);

        this.type = type;
        title = name;
    }

    @Override
    @PropertyName("Type")
    @PropertyDescription("Type.")
    public String getType()
    {
        return type;
    }

    @Override
    @PropertyName("Title")
    @PropertyDescription("The object title (generally it is object brief name).")
    public String getTitle()
    {
        return title;
    }
    public void setTitle (String title)
    {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }

    @Override
    @PropertyName("Attributes")
    @PropertyDescription("Dynamic set of attributes. <br>" + "This attributes can be added:<br>" + "<ul>"
            + "<li>during mapping of information from a database into Java objects"
            + "<li>by plug-in for some specific usage" + "<li>by customer to store some specific information formally"
            + "<li>during import of experimental data" + "</ul>")
    public DynamicPropertySet getAttributes()
    {
        if( attributes == null )
            attributes = new DynamicPropertySetAsMap();

        return attributes;
    }
    
    public boolean hasNoAttributes()
    {
        return getAttributes().isEmpty();
    }

    @Override
    public BaseSupport clone(DataCollection<?> newOrigin, String newName)
    {
        BaseSupport clone = (BaseSupport)internalClone(newOrigin, newName);
        if(attributes != null)
        {
            clone.attributes = new DynamicPropertySetAsMap();
            for(DynamicProperty dp : attributes)
            {
                try
                {
                    clone.attributes.add(DynamicPropertySetSupport.cloneProperty(dp));
                }
                catch( Exception e )
                {
                    throw new InternalException(e, "While cloning property "+dp.getName());
                }
            }
        }
        return clone;
    }
}
