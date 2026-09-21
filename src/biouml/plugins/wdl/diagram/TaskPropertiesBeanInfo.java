package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class TaskPropertiesBeanInfo extends BeanInfoEx2<TaskProperties>
{
    public TaskPropertiesBeanInfo()
    {
        super( TaskProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("command");
    }
}