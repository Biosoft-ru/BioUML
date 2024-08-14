
package biouml.plugins.bindingregions.cisregmodule;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.standard.type.Species;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author yura
 *
 */
public class CisRegModuleParametersBeanInfo extends BeanInfoEx
{
    public CisRegModuleParametersBeanInfo()
    {
        super(CisRegModuleParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_CIS_REG_MODULE);
        beanDescriptor.setShortDescription(MessageBundle.CD_CIS_REG_MODULE);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("mode", beanClass), getResourceString("PN_MODE"), getResourceString("PD_MODE"));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("expParameters", beanClass);
        pde.setHidden(beanClass.getMethod("isExpParametersHidden"));
        add(pde, "Exp parameters", "Exp parameters");
        
        add(DataElementPathEditor.registerInputChild("chipSeqPeaksPath", beanClass, SqlTrack.class), getResourceString("PN_CHIP_SEQ_TRACK_PATH"), getResourceString("PD_CHIP_SEQ_TRACK_PATH"));
        add(DataElementPathEditor.registerInputChild("sequencePath", beanClass, AnnotatedSequence.class), getResourceString("PN_SEQUENCES_PATH"), getResourceString("PD_SEQUENCES_PATH"));
        add(new PropertyDescriptorEx("minimalNumberOfOverlaps", beanClass), getResourceString("PN_MINIMAL_NUMBER_OF_OVERLAPS"), getResourceString("PD_MINIMAL_NUMBER_OF_OVERLAPS"));
        add(DataElementComboBoxSelector.registerSelector("specie", beanClass, Species.SPECIES_PATH), getResourceString("PN_SPECIE"), getResourceString("PD_SPECIE"));
        add(DataElementPathEditor.registerOutput("cisRegModuleTable", beanClass, TableDataCollection.class), getResourceString("PN_CIS_REG_MODULE_TABLE"), getResourceString("PD_CIS_REG_MODULE_TABLE"));
        
        
 //       add(DataElementPathEditor.registerInput("sequencePath", beanClass, Map.class));   path to given sequence

    }
}