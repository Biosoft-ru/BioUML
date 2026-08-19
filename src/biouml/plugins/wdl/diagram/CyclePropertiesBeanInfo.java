package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CyclePropertiesBeanInfo extends BeanInfoEx2<CycleProperties>
{
    public CyclePropertiesBeanInfo()
    {
        super( CycleProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("variable");
    }
}