
package ru.biosoft.bsa.view.colorscheme;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.IndexedPropertyDescriptorEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;


public class CompositeColorSchemeBeanInfo extends BeanInfoEx
{
    public CompositeColorSchemeBeanInfo()
    {
        super(CompositeColorScheme.class, "ru.biosoft.bsa.view.colorscheme.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_COMPOSITE_COLOR_SCHEME"));
        beanDescriptor.setShortDescription(getResourceString("CD_COMPOSITE_COLOR_SCHEME"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("name");
        add(new PropertyDescriptorEx("defaultBrush", beanClass),
            getResourceString("PN_DEFAULT_BRUSH"),
            getResourceString("PD_DEFAULT_BRUSH"));

        add(new IndexedPropertyDescriptorEx("colorSchemes", beanClass),
            getResourceString("PN_SCHEMES"),
            getResourceString("PD_SCHEMES"));
    }
}



