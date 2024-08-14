
package ru.biosoft.bsa.view.colorscheme;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SiteWeightColorSchemeBeanInfo extends BeanInfoEx
{
    public SiteWeightColorSchemeBeanInfo()
    {
        super(SiteWeightColorScheme.class, "ru.biosoft.bsa.view.colorscheme.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_WEIGHT_COLOR_SCHEME"));
        beanDescriptor.setShortDescription(getResourceString("CD_WEIGHT_COLOR_SCHEME"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        
        add(new PropertyDescriptorEx("firstColor", beanClass),
            getResourceString("PN_COLOR1"),
            getResourceString("PD_COLOR1"));

        add(new PropertyDescriptorEx("secondColor", beanClass),
            getResourceString("PN_COLOR2"),
            getResourceString("PD_COLOR2"));

        add(new PropertyDescriptorEx( "weightProperty", beanClass ),
                getResourceString( "PN_WEIGHT_PROPERTY" ),
                getResourceString( "PD_WEIGHT_PROPERTY"));
        
        add(new PropertyDescriptorEx("minValue", beanClass),
            getResourceString( "PN_MIN_VALUE" ),
            getResourceString( "PD_MIN_VALUE" ));
        
        add(new PropertyDescriptorEx("maxValue", beanClass),
            getResourceString( "PN_MAX_VALUE" ),
            getResourceString( "PD_MAX_VALUE" ));
        
        add(new PropertyDescriptorEx("defaultBrush", beanClass),
            getResourceString("PN_DEFAULT_BRUSH"),
            getResourceString("PD_DEFAULT_BRUSH"));
    }
}




