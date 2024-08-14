package ru.biosoft.access;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.MessageBundle;

public class FileDataElementBeanInfo extends BeanInfoEx
{
    public FileDataElementBeanInfo()
    {
        super( FileDataElement.class, MessageBundle.class.getName() );
        initResources( "ru.biosoft.access.core.MessageBundle" );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();

        super.initProperties();
        add( new PropertyDescriptorEx( "class", beanClass, "getClass", null ) );
        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
        property( new PropertyDescriptorEx( "readableContentLength", beanClass, "getReadableContentLength", null ) )
                .titleRaw( "File size" )
                .add();
    }
}
