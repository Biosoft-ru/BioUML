package biouml.plugins.ensembl.analysis;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.standard.type.Species;

public class GeneOverlapStatisticsParametersBeanInfo extends BeanInfoEx2<GeneOverlapStatisticsParameters>
{
    public GeneOverlapStatisticsParametersBeanInfo()
    {
        super( GeneOverlapStatisticsParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputTrack" ).inputElement( Track.class ).add();
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
        add("fivePrimeFlankSize");
        add("threePrimeFlankSize");
        property( "outputFolder" ).outputElement( FolderCollection.class ).auto( "$inputTrack$ gene overlap" ).add();
    }
}
