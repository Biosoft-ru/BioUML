package biouml.plugins.keynodes;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.standard.type.Species;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.UserHubCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

public class LongestChainFinderParametersBeanInfo extends BeanInfoEx2<LongestChainFinderParameters>
{
    public LongestChainFinderParametersBeanInfo()
    {
        super( LongestChainFinderParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "sourcePath" ).inputElement( TableDataCollection.class )
                .value( DataElementPathEditor.ICON_ID, LongestChainFinderParameters.class.getMethod( "getIcon" ) ).add();

        property( new PropertyDescriptorEx( "isInputSizeLimited", beanClass, "isInputSizeLimited", "setInputSizeLimited" ) ).expert().add();
        addExpert( "inputSizeLimit" );

        add( "direction", DirectionEditor.class );
        add( "maxRadius" );
        add( "maxDijkstraDepth" );
        add( "scoreCutoff" );

        property( "bioHub" ).simple().editor( BioHubSelector.class ).structureChanging().add();
        property( "customHubCollection" ).inputElement( UserHubCollection.class ).hidden( "isCustomHubCollectionHidden" ).add();
        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) );

        add( "decorators" );

        property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$sourcePath$ Longest chains $direction$ $maxRadius$" )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/keynodes.gif" ) ).add();

        addExpert( "scoreCoeff" );
    }
}
