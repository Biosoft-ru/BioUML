package ru.biosoft.bsa.track.combined;

import com.developmentontheedge.beans.BeanInfoEx;

public class CombinedColorSchemeBeanInfo extends BeanInfoEx
{
    public CombinedColorSchemeBeanInfo()
    {
        super( CombinedColorScheme.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( "trackColorItems" );
    }
}
