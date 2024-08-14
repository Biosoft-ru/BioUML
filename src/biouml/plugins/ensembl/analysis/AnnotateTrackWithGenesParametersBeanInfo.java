package biouml.plugins.ensembl.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.OptionEx;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

import biouml.standard.type.Species;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author lan
 *
 */
public class AnnotateTrackWithGenesParametersBeanInfo extends BeanInfoEx
{
    public AnnotateTrackWithGenesParametersBeanInfo()
    {
        super(AnnotateTrackWithGenesParameters.class, MessageBundle.class.getName());
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("inputTrack", beanClass, Track.class), getResourceString("PN_INPUT_TRACK"), getResourceString("PD_INPUT_TRACK"));
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH), getResourceString("PN_SPECIES"), getResourceString("PD_SPECIES"));
        add(new PropertyDescriptorEx("from", beanClass), getResourceString("PN_GENESET_FROM"),
                getResourceString("PD_GENESET_FROM"));
        add(new PropertyDescriptorEx("to", beanClass), getResourceString("PN_GENESET_TO"),
                getResourceString("PD_GENESET_TO"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputTrack", beanClass, SqlTrack.class), "$inputTrack$ annotated"), getResourceString("PN_OUTPUT_TRACK"),
                getResourceString("PD_OUTPUT_TRACK"));
    }
}
