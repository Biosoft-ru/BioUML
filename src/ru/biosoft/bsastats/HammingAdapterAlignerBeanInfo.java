package ru.biosoft.bsastats;

import ru.biosoft.util.bean.BeanInfoEx2;

public class HammingAdapterAlignerBeanInfo extends BeanInfoEx2<HammingAdapterAligner>
{
    public HammingAdapterAlignerBeanInfo()
    {
        super( HammingAdapterAligner.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( "minMatchLength" );
        add( "errorRate" );
        add( "leftMost" );
    }
}
