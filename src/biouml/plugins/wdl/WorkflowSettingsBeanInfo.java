package biouml.plugins.wdl;

import ru.biosoft.util.bean.BeanInfoEx2;

public class WorkflowSettingsBeanInfo extends BeanInfoEx2<WorkflowSettings>
{
    public WorkflowSettingsBeanInfo(WorkflowSettings settings)
    {
        super( WorkflowSettings.class );
    }

    @Override
    public void initProperties()
    {
        add( "parameters" );
        add( "outputPath" );
    }
}