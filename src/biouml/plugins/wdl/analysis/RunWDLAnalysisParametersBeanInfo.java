package biouml.plugins.wdl.analysis;

import biouml.model.Diagram;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class RunWDLAnalysisParametersBeanInfo extends BeanInfoEx2<RunWDLAnalysisParameters>
{
    public RunWDLAnalysisParametersBeanInfo()
    {
        super( RunWDLAnalysisParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "outputPath" ).outputElement( GenericDataCollection.class ).add();
        property( "wdlPath" ).inputElement( Diagram.class ).add();
        add( "useJson");
        addHidden( "jsonPath", "isNotUseJson");
        addHidden( "parameters", "isUseJson");
    }
}