package biouml.plugins.mirprom;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class MiRNAPromoterBeanInfo extends BeanInfoEx2<MiRNAPromoter>
{
    public MiRNAPromoterBeanInfo()
    {
        super( MiRNAPromoter.class );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
        add( new PropertyDescriptorEx( "miRName", beanClass, "getMiRName", null ) );
        add( new PropertyDescriptorEx( "location", beanClass, "getLocation", null ) );

        PropertyDescriptorEx pd = new PropertyDescriptorEx( "cells", beanClass, "getCells", null );
        pd.setReadOnly( true );
        add( pd );
    }
}
