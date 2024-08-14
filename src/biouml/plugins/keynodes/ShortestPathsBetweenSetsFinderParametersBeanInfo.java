package biouml.plugins.keynodes;

import biouml.standard.type.Species;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.UserHubCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

public class ShortestPathsBetweenSetsFinderParametersBeanInfo extends BeanInfoEx2<ShortestPathsBetweenSetsFinderParameters>
{
    public ShortestPathsBetweenSetsFinderParametersBeanInfo()
    {
        super( ShortestPathsBetweenSetsFinderParameters.class );
    }
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "sourcePath" ).inputElement( TableDataCollection.class ).add();
        property( "endSet" ).inputElement( TableDataCollection.class ).add();

        property( "direction" ).editor( DirectionEditor.class ).add();

        add( "maxRadius" );

        property( "bioHub" ).simple().editor( BioHubSelector.class ).structureChanging().add();
        property( "customHubCollection" ).inputElement( UserHubCollection.class ).hidden( "isCustomHubCollectionHidden" ).add();
        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) );

        property( "outputPath" ).outputElement( TableDataCollection.class )
                .auto( "$sourcePath$ to $endSet/name$ paths $direction$ $maxRadius$" )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/keynodes.gif" ) ).add();
    }
}
