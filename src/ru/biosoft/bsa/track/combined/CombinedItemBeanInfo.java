package ru.biosoft.bsa.track.combined;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.MessageBundle;
import ru.biosoft.bsa.Track;

public class CombinedItemBeanInfo extends BeanInfoEx
{
    public CombinedItemBeanInfo()
    {
        super( CombinedItem.class, MessageBundle.class.getName() );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( DataElementPathEditor.registerInput( "path", beanClass, Track.class ) );
        add( "color" );
    }

}
