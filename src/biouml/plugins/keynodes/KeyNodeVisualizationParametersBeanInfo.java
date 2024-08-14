package biouml.plugins.keynodes;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import biouml.model.Diagram;

/**
 * @author anna
 *
 */
public class KeyNodeVisualizationParametersBeanInfo extends BeanInfoEx2<KeyNodeVisualizationParameters>
{
    public KeyNodeVisualizationParametersBeanInfo()
    {
        super( KeyNodeVisualizationParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property( "knResultPath" ).inputElement( TableDataCollection.class )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/keynodes.gif" ) ).add();

        add( ColumnNameSelector.registerNumericSelector( "rankColumn", beanClass, "knResultPath" ) );

        add( "numTopRanking" );
        add( "lowestRanking" );

        property( "separateResults" ).expert().titleRaw( "Separate diagrams" )
                .descriptionRaw( "Create an individual diagram for each top ranking molecule" ).add();

        property( "visualizeAllPaths" ).expert().hidden( "hideVisualizeAllPaths" ).add();
        property( new PropertyDescriptorEx( "suffix", beanClass, "getSuffix", null ) ).hidden().readOnly().add();

        property( "outputPath" ).outputElement( Diagram.class ).auto( "$knResultPath$ viz$suffix" )
                .titleRaw( "Diagram path" ).descriptionRaw( "Path to result diagram" ).add();
        
        add("layoutDiagram");
        add("addParticipants");

        addHidden( "layoutXDist" );
        addHidden( "layoutYDist" );
    }
}
