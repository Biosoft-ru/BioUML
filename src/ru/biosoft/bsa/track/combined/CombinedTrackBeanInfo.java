package ru.biosoft.bsa.track.combined;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.bsa.MessageBundle;


public class CombinedTrackBeanInfo extends BeanInfoEx
{
    public CombinedTrackBeanInfo()
    {
        super( CombinedTrack.class, MessageBundle.class.getName() );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( "trackColorItems" );
        add( "genomeSelector" );
        add( "condition" );
    }

}
