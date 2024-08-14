package biouml.plugins.keynodes;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author anna
 *
 */
public class SaveNetworkAnalysisParametersBeanInfo extends BeanInfoEx2<SaveNetworkAnalysisParameters>
{
    public SaveNetworkAnalysisParametersBeanInfo()
    {
        super( SaveNetworkAnalysisParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "knResultPath" ).inputElement( TableDataCollection.class )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/keynodes.gif" ) ).add();

        property( "rankColumn" ).editor( ColumnNameSelector.class ).simple().canBeNull()
                .value( ColumnNameSelector.COLLECTION_PROPERTY, "knResultPath" ).add();

        add( "numTopRanking" );

        property( "separateResults" ).expert().titleRaw( "Separate results" )
                .descriptionRaw( "Create an individual table for each top ranking molecule" ).add();

        property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$knResultPath$ network (top $numTopRanking$)" )
                .titleRaw( "Output name" ).descriptionRaw( "Output name." ).add();
    }
}
