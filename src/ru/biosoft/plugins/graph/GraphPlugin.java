package ru.biosoft.plugins.graph;

import java.lang.reflect.Field;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.graph.Layouter;
import ru.biosoft.util.ExtensionRegistrySupport;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class GraphPlugin extends ExtensionRegistrySupport<LayouterDescriptor>
{
    private static final GraphPlugin instance = new GraphPlugin();
    
    private GraphPlugin()
    {
        super("ru.biosoft.plugins.graph.layout", LayouterDescriptor.TITLE_ATTR);
    }

    public static List<LayouterDescriptor> loadLayouters()
    {
        return instance.stream().toList();
    }
    
    public static LayouterDescriptor getLayouter(String name)
    {
        return instance.getExtension(name);
    }

    @Override
    protected LayouterDescriptor loadElement(IConfigurationElement element, String title) throws Exception
    {
        Class<? extends Layouter> type = getClassAttribute(element, LayouterDescriptor.CLASS_ATTR, Layouter.class);
        String descr = element.getAttribute(LayouterDescriptor.DESCRIPTION_ATTR);
        boolean isPublic = getBooleanAttribute(element, LayouterDescriptor.PUBLIC_ATTR, true);
        LayouterDescriptor ld = new LayouterDescriptor(type, title, descr, isPublic);
        DynamicPropertySet properties = ld.getDefaultParameters();
        for( IConfigurationElement child : element.getChildren(LayouterDescriptor.PROPERTY_ELEMENT) )
        {
            String name = getStringAttribute(child, LayouterDescriptor.PROPERTY_NAME_ATTR);
            String value = getStringAttribute(child, LayouterDescriptor.PROPERTY_VALUE_ATTR);
            Field field = type.getDeclaredField(name);
            try
            {
                Class propertyType = field.getType();
                Object valueObj = null;
                if( propertyType == Boolean.TYPE )
                {
                    valueObj = Boolean.parseBoolean(value);
                }
                else if( propertyType == Integer.TYPE )
                {
                    valueObj = Integer.parseInt(value);
                }
                else if( propertyType == Double.TYPE )
                {
                    valueObj = Double.parseDouble(value);
                }
                else
                {
                    valueObj = value;
                }
                DynamicProperty dp = new DynamicProperty(name, propertyType, valueObj);
                properties.add(dp);
            }
            catch( NumberFormatException e )
            {
            }
        }
        return ld;
    }
}
