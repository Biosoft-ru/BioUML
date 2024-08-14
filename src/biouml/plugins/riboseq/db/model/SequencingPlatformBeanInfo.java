package biouml.plugins.riboseq.db.model;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SequencingPlatformBeanInfo extends BeanInfoEx
{
    public SequencingPlatformBeanInfo()
    {
        super( SequencingPlatform.class, true );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
        add( "title" );
    }
}
