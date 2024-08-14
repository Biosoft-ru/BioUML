package ru.biosoft.bsa.track.big;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.bsa.ChrNameMapping.ChrMappingSelector;
import ru.biosoft.bsa.gui.MessageBundle;

public class BigTrackBeanInfo extends BeanInfoEx
{
    public BigTrackBeanInfo(Class<?> beanClass)
    {
        super( beanClass, MessageBundle.class.getName() );
    }

    public BigTrackBeanInfo()
    {
        super( BigTrack.class, MessageBundle.class.getName() );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "genomeSelector" );
        property( "chrMapping" ).canBeNull().simple().editor( ChrMappingSelector.class ).add();
    }
}
