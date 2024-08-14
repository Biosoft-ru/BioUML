package biouml.plugins.jupyter.access;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class IPythonElementBeanInfo extends BeanInfoEx
{
    public IPythonElementBeanInfo()
    {
        super( IPythonElement.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();

        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
    }
}
