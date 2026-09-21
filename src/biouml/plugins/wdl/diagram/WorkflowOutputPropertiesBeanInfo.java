package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class WorkflowOutputPropertiesBeanInfo extends BeanInfoEx2<WorkflowOutputProperties>
{
    public WorkflowOutputPropertiesBeanInfo()
    {
        super( WorkflowOutputProperties.class );
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