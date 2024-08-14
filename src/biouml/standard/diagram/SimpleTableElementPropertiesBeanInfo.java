package biouml.standard.diagram;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SimpleTableElementPropertiesBeanInfo extends BeanInfoEx2<SimpleTableElementProperties>
{
    public SimpleTableElementPropertiesBeanInfo()
    {
        super(SimpleTableElementProperties.class);
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add("name");
        add("element");
    }  
}