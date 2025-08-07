package biouml.plugins.physicell;

import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ReportPropertiesBeanInfo extends BeanInfoEx2<ReportProperties>
{
    public ReportPropertiesBeanInfo()
    {
        super(  ReportProperties.class );
    }

    @Override
    public void initProperties()
    {
        add("customReport");
        property( "reportPath" ).hidden( "isDefaultReport" ).inputElement( ScriptDataElement.class ).add();
        add("customGlobalReport");
        property( "globalReportPath" ).hidden( "isDefaultGlobalReport" ).inputElement( ScriptDataElement.class ).add();
        add("customVisualizer");
        property( "visualizerPath" ).hidden( "isDefaultVisualizer" ).inputElement( ScriptDataElement.class ).add();
    }
}