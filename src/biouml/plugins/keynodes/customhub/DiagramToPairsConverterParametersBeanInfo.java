package biouml.plugins.keynodes.customhub;

import biouml.model.Diagram;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramToPairsConverterParametersBeanInfo extends BeanInfoEx2<DiagramToPairsConverterParameters>
{
    public DiagramToPairsConverterParametersBeanInfo()
    {
        super( DiagramToPairsConverterParameters.class );
    }
    @Override
    public void initProperties() throws Exception
    {
        property( "diagramPath" ).inputElement( Diagram.class ).add();
        property( "weight" ).add();
        property( "tablePath" ).outputElement( TableDataCollection.class ).auto( "$diagramPath$ interactions table" ).add();
    }
}
