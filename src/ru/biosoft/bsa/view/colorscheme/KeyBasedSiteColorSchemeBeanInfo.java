
package ru.biosoft.bsa.view.colorscheme;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class KeyBasedSiteColorSchemeBeanInfo extends BeanInfoEx
{
    public KeyBasedSiteColorSchemeBeanInfo()
    {
        super(KeyBasedSiteColorScheme.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_KEY_BASED_COLOR_SCHEME"));
        beanDescriptor.setShortDescription(getResourceString("CD_KEY_BASED_COLOR_SCHEME"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("name");
        add(new PropertyDescriptorEx("colorGroup", beanClass),
            getResourceString("PN_KEY_COLOR_GROUP"),
            getResourceString("PD_KEY_COLOR_GROUP"));

        addHidden(new PropertyDescriptorEx("keyGenerator", beanClass));
        addHidden(new PropertyDescriptorEx("keyDelimiters", beanClass));
    }
}




