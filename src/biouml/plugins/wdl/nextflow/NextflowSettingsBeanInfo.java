package biouml.plugins.wdl.nextflow;

import ru.biosoft.util.bean.BeanInfoEx2;

public class NextflowSettingsBeanInfo extends BeanInfoEx2<NextflowSettings>
{
    public NextflowSettingsBeanInfo()
    {
        super( NextflowSettings.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("root");
        property( "stageInput" ).tags( NextflowSettings.STAGE_IN_OPTIONS ).add();
        property( "publishOutput" ).tags( NextflowSettings.PUBLISH_OUT_OPTIONS ).add();
    }
}