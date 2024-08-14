package biouml.plugins.fbc.analysis;

import biouml.model.Diagram;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ReconTransformerParametersBeanInfo extends BeanInfoEx2<ReconTransformerParameters>
{
    public ReconTransformerParametersBeanInfo()
    {
        super( ReconTransformerParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "diagramPath" ).inputElement( Diagram.class ).add();
        property( "resultPath" ).outputElement( Diagram.class ).auto( "$diagramPath$_fbc" ).add();
    }
}
