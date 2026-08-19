package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class InputPropertiesBeanInfo extends BeanInfoEx2<InputProperties>
{
    public InputPropertiesBeanInfo()
    {
        super( InputProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("type");
        add("variable");
        add("rhs");
    }
}