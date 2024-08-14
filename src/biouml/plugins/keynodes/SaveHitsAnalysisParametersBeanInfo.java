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
public class SaveHitsAnalysisParametersBeanInfo extends BeanInfoEx2<SaveHitsAnalysisParameters>
{
    public SaveHitsAnalysisParametersBeanInfo()
    {
        super( SaveHitsAnalysisParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "knResultPath" ).inputElement( TableDataCollection.class )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/keynodes.gif" ) ).add();

        //add("score");

        add( ColumnNameSelector.registerSelector( "rankColumn", beanClass, "knResultPath" ) );

        add( "numTopRanking" );

        property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$knResultPath$ hits (top $numTopRanking$)" )
                .titleRaw( "Output name" ).descriptionRaw( "Output name." ).add();
    }
}
