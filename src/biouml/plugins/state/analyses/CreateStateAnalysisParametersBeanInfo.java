package biouml.plugins.state.analyses;

import ru.biosoft.access.repository.DataElementPathEditor;
import biouml.standard.state.State;

public class CreateStateAnalysisParametersBeanInfo extends DiagramAndChangesBeanInfo
{
    public CreateStateAnalysisParametersBeanInfo()
    {
        super( CreateStateAnalysisParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( DataElementPathEditor.registerOutput( "statePath", beanClass, State.class ) );
    }
}