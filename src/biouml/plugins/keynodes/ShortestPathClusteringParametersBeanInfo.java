package biouml.plugins.keynodes;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.UserHubCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.standard.type.Species;

/**
 * @author anna
 *
 */
public class ShortestPathClusteringParametersBeanInfo extends BeanInfoEx2<ShortestPathClusteringParameters>
{
    public ShortestPathClusteringParametersBeanInfo()
    {
        this( ShortestPathClusteringParameters.class );
    }
    protected ShortestPathClusteringParametersBeanInfo(Class<? extends ShortestPathClusteringParameters> beanClass)
    {
        super( beanClass );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "sourcePath" ).inputElement( TableDataCollection.class )
                .value( DataElementPathEditor.ICON_ID, ShortestPathClusteringParameters.class.getMethod( "getIcon" ) ).add();

        addExpert( "inputSizeLimit" );

        add( "direction", DirectionEditor.class );

        add( "maxRadius" );

        add( "useFullPath" );

        property( "bioHub" ).simple().editor( BioHubSelector.class ).structureChanging().add();
        property( "customHubCollection" ).inputElement( UserHubCollection.class ).hidden( "isCustomHubCollectionHidden" ).add();

        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) );

        property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$sourcePath$ shortest path $direction$ $maxRadius$" ).add();
    }
}
