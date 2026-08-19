package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ConditionPropertiesBeanInfo extends BeanInfoEx2<ConditionProperties>
{
    public ConditionPropertiesBeanInfo()
    {
        super( ConditionProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("expression");
    }
}