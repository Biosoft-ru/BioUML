package ru.biosoft.graph;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class PathLayouterWrapperBeanInfo extends BeanInfoEx
{
    public PathLayouterWrapperBeanInfo()
    {
        super( PathLayouterWrapper.class, MessageBundle.class.getName() );
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx( "pathLayouterName", beanClass );
        //dirty setting to use property recreation on web, see BeanInfoEx2.structureChanging()
        pde.setValue( "structureChangingProperty", Boolean.TRUE );
        property( pde ).tags( PathLayouterWrapper.getTags() ).title( "PN_PATH_LAYOUTER_NAME" ).description( "PD_PATH_LAYOUTER_NAME" ).add();
        //        addWithTags( pde2, PathLayouterWrapper.getTags() );

        //        property( "pathLayouterName" ).tags( PathLayouterWrapper.getTags() ).title( "PN_PATH_LAYOUTER_NAME" )
        //                .description( "PD_PATH_LAYOUTER_NAME" ).add();

        pde = new PropertyDescriptorEx( "pathLayouter", beanClass );
        pde.setHidden( beanClass.getMethod( "hiddenOptions" ) );
        add( pde, getResourceString( "PN_PATH_LAYOUTER_OPTIONS" ), getResourceString( "PD_PATH_LAYOUTER_OPTIONS" ) );

    }
}
