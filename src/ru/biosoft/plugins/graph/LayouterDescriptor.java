package ru.biosoft.plugins.graph;

import ru.biosoft.graph.Layouter;
import ru.biosoft.util.BeanUtil;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.model.Property;

/**
 * Descriptor of used layouter
 */
public class LayouterDescriptor
{

    public static final String TITLE_ATTR = "title";

    public static final String CLASS_ATTR = "class";

    public static final String DESCRIPTION_ATTR = "description";

    public static final String PUBLIC_ATTR = "public";

    public static final String PROPERTY_ELEMENT = "property";
    public static final String PROPERTY_NAME_ATTR = "name";
    public static final String PROPERTY_VALUE_ATTR = "value";

    private Class<? extends Layouter> type;
    private String title;
    private String description;
    private boolean visible;
    private DynamicPropertySet defaultParameters;

    /**
     * Descriptor of layouter (class - derived from
     * <code>ru.biosoft.graph.Layouter</code>) interface
     */
    public LayouterDescriptor(Class<? extends Layouter> type, String title, String description)
    {
        this(type, title, description, true);
    }

    /**
     * Descriptor of layouter (class - derived from
     * <code>ru.biosoft.graph.Layouter</code>) interface
     */
    public LayouterDescriptor(Class<? extends Layouter> type, String title, String description, boolean visible)
    {
        this.type = type;
        this.title = title;
        this.description = description;
        this.visible = visible;
    }

    public Class<? extends Layouter> getType()
    {
        return type;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isPublic()
    {
        return visible;
    }

    public DynamicPropertySet getDefaultParameters()
    {
        if( defaultParameters == null )
        {
            defaultParameters = new DynamicPropertySetAsMap();
        }
        return defaultParameters;
    }

    /**
     * Create layouter with default properties
     */
    public Layouter createLayouter() throws Exception
    {
        Layouter layouter = type.newInstance();
        if( defaultParameters != null )
        {
            for( Property property : BeanUtil.properties( layouter ) )
            {
                Object value = defaultParameters.getValue(property.getName());
                if( value != null )
                {
                    property.setValue(value);
                }
            }
        }
        return layouter;
    }
}
