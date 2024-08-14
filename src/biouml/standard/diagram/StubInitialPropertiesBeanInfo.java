package biouml.standard.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class StubInitialPropertiesBeanInfo extends BeanInfoEx2<StubInitialProperties>
{
    public StubInitialPropertiesBeanInfo()
    {
        super( StubInitialProperties.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("title");
    }
}
