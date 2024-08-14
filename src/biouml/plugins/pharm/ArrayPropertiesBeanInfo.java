package biouml.plugins.pharm;

import com.developmentontheedge.beans.BeanInfoEx;

public class ArrayPropertiesBeanInfo extends BeanInfoEx
{
    public ArrayPropertiesBeanInfo()
    {
        super( ArrayProperties.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("name");
        add("index");
        add("from");
        add("upTo");
    }
}