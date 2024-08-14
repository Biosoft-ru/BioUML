package ru.biosoft.bsa.view.colorscheme;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author lan
 *
 */
public class AutoTagColorSchemeBeanInfo extends BeanInfoEx
{
    public AutoTagColorSchemeBeanInfo()
    {
        super( AutoTagColorScheme.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("colors", beanClass));
    }
}
