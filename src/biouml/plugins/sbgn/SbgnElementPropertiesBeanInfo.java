package biouml.plugins.sbgn;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SbgnElementPropertiesBeanInfo extends BeanInfoEx2<SbgnElementProperties>
{
    public SbgnElementPropertiesBeanInfo(Class<? extends SbgnElementProperties> clazz)
    {
        super(clazz);
    }

    public SbgnElementPropertiesBeanInfo()
    {
        super(SbgnElementProperties.class);
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add("name");
        add("title");
        add("properties");
    }
}