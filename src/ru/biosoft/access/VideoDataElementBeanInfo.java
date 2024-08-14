package ru.biosoft.access;

import java.beans.PropertyDescriptor;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VideoDataElementBeanInfo extends BeanInfoEx2<VideoDataElement>
{
    public VideoDataElementBeanInfo()
    {
        super(VideoDataElement.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptor("name", beanClass, "getName", null));
        property("description").readOnly().add();
        property("format").readOnly().add();

    }

}
