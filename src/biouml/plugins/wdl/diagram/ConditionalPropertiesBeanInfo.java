package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ConditionalPropertiesBeanInfo extends BeanInfoEx2<ConditionalProperties>
{
    public ConditionalPropertiesBeanInfo()
    {
        super( ConditionalProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
    }
}