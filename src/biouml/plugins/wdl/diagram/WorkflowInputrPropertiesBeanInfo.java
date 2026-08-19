package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class WorkflowInputrPropertiesBeanInfo extends BeanInfoEx2<WorkflowInputProperties>
{
    public WorkflowInputrPropertiesBeanInfo()
    {
        super( WorkflowInputProperties.class );
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