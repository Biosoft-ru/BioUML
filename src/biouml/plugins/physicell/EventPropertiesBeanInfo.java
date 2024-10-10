package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

import java.beans.IntrospectionException;

public class EventPropertiesBeanInfo extends BeanInfoEx2<EventProperties>
{
    public EventPropertiesBeanInfo()
    {
        super( EventProperties.class );
    }

    @Override
    public void initProperties() throws IntrospectionException, NoSuchMethodException
    {
        add("name");
        add("executionTime");
        add("useCustomExecutionCode");
        add("executionCodePath");
        add("comment");
        add("showCode");
        add("formatCode");
    }
}