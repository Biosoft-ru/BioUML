package ru.biosoft.bsa.view.colorscheme;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditor;

/**
 * @author lan
 *
 */
public class SitePropertyColorSchemeBeanInfo extends BeanInfoEx
{
    public SitePropertyColorSchemeBeanInfo()
    {
        super( SitePropertyColorScheme.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "colorProperty", beanClass ), SitePropertySelector.class );
        add(new PropertyDescriptorEx("colors", beanClass));
    }

    public static class SitePropertySelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ( (SitePropertyColorScheme)getBean() ).getSiteProperties();
        }
    }
}
