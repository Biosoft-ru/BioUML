package biouml.plugins.riboseq.db.model;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SpeciesBeanInfo extends BeanInfoEx
{
    public SpeciesBeanInfo()
    {
        super( Species.class, true );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
        add( "latinName" );
        add( "commonName" );
    }
}
