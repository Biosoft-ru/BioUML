package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;

public class BAMTrackBeanInfo extends BeanInfoEx
{
    public BAMTrackBeanInfo()
    {
        super(BAMTrack.class, true);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add("genomeSelector");
    }
}
