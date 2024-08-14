package biouml.plugins.ensembl.homology;

import biouml.standard.type.Species;
import ru.biosoft.analysis.TableConverterParametersBeanInfo;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

/**
 * @author lan
 *
 */
public class HomologyMappingParametersBeanInfo extends TableConverterParametersBeanInfo
{
    public HomologyMappingParametersBeanInfo()
    {
        super(HomologyMappingParameters.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(findPropertyIndex("aggregator"),
                DataElementComboBoxSelector.registerSelector("targetSpecies", beanClass, Species.SPECIES_PATH),
                getResourceString("PN_OUTPUT_SPECIES"), getResourceString("PD_OUTPUT_SPECIES"));
    }
}
