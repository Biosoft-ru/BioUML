package ru.biosoft.bsastats;

import one.util.streamex.StreamEx;


public class CutAdaptersBeanInfo extends TaskProcessorBeanInfo
{
    public CutAdaptersBeanInfo()
    {
        super( CutAdapters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( "adapters" );
        property("adapterAlignerName").tags( bean -> StreamEx.ofKeys( ((CutAdapters)bean).ADAPTER_ALIGNERS ) ).add();
        add( "adapterAligner" );
    }
}
