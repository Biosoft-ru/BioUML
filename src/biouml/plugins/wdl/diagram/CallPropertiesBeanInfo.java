package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CallPropertiesBeanInfo extends BeanInfoEx2<CallProperties>
{
    public CallPropertiesBeanInfo()
    {
        super( CallProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        property("taskRef").tags( bean->bean.getAvailableTasks() ).add();
    }
}