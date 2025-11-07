package biouml.plugins.wdl;

import ru.biosoft.util.bean.BeanInfoEx2;

public class WorkflowSettingsBeanInfo extends BeanInfoEx2<WorkflowSettings>
{
    public WorkflowSettingsBeanInfo()
    {
        super( WorkflowSettings.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "executionType" );
        add( "outputPath" );
        property( "useJson" ).structureChanging().add();
        property( "json" ).hidden( "isNotJson" ).add();
        property( "parameters" ).hidden( "isUseJson" ).add();
    }
}