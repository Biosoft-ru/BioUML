package ru.biosoft.bsa.finder;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class TrackSummaryBeanInfo extends BeanInfoEx2<TrackSummary>
{

    public TrackSummaryBeanInfo()
    {
        super( TrackSummary.class );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
        add( new PropertyDescriptorEx( "size", beanClass, "getSize", null ) );
        add( new PropertyDescriptorEx( "path", beanClass, "getPath", null ) );
    }
}
