package ru.biosoft.bsastats;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.StubMessageBundle;

public class NWAdapterAlignerBeanInfo extends BeanInfoEx
{

    public NWAdapterAlignerBeanInfo()
    {
        super( NWAdapterAligner.class, StubMessageBundle.class.getName() );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( "minMatchLength" );
        add( "errorRate" );
    }

}
