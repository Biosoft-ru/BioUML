package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.bsa.ChrNameMapping.ChrMappingSelector;

public class TrackOptionsBeanInfo extends BeanInfoEx
{
    public TrackOptionsBeanInfo(Class<?> beanClass)
    {
        super( beanClass, MessageBundle.class.getName() );
    }

    public TrackOptionsBeanInfo()
    {
        super(TrackOptions.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "genomeSelector" );
        property( "chrMapping" ).canBeNull().simple().editor( ChrMappingSelector.class ).add();
    }
}
