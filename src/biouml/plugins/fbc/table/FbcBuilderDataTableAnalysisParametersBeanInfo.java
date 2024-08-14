package biouml.plugins.fbc.table;

import biouml.model.Diagram;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FbcBuilderDataTableAnalysisParametersBeanInfo extends BeanInfoEx2<FbcBuilderDataTableAnalysisParameters>
{
    public FbcBuilderDataTableAnalysisParametersBeanInfo()
    {
        super( FbcBuilderDataTableAnalysisParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "diagramPath" ).inputElement( Diagram.class ).add();
        property( "fbcResultPath" ).outputElement( TableDataCollection.class ).add();

        add( "lowerBoundDefault" );
        add( "equalsDefault" );
        add( "upperBoundDefault" );
        add( "fbcObjective" );
    }
}
