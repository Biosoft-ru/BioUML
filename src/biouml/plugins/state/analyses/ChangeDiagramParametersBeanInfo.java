package biouml.plugins.state.analyses;

import ru.biosoft.access.repository.DataElementPathEditor;
import biouml.model.Diagram;

public class ChangeDiagramParametersBeanInfo extends DiagramAndChangesBeanInfo
{
    public ChangeDiagramParametersBeanInfo()
    {
        super( ChangeDiagramParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( DataElementPathEditor.registerOutput( "outputDiagram", beanClass, Diagram.class ) );
    }
}
