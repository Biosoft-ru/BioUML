
package biouml.plugins.simulation;

import biouml.model.Diagram;
import biouml.plugins.simulation.resources.MessageBundle;
import biouml.standard.simulation.SimulationResult;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author anna
 *
 */
public class SimulationAnalysisParametersBeanInfo extends BeanInfoEx
{
    public SimulationAnalysisParametersBeanInfo()
    {
        super(SimulationAnalysisParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_SIMULATION_ANALYSIS"));
        beanDescriptor.setShortDescription(getResourceString("CD_SIMULATION_ANALYSIS"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = DataElementPathEditor.registerInput("modelPath", beanClass, Diagram.class);
        add(pde, getResourceString("PN_MODEL"), getResourceString("PD_MODEL"));

        pde = new PropertyDescriptorEx("simulationEngineName", beanClass);
        pde.setPropertyEditorClass(SimulationEngineEditor.class);
        add(pde, getResourceString("PN_SIMULATION_ENGINE_NAME"), getResourceString("PD_SIMULATION_ENGINE_NAME"));

        pde = new PropertyDescriptorEx("simulationEngine", beanClass);
        add(pde, getResourceString("PN_SIMULATION_ENGINE"), getResourceString("PD_SIMULATION_ENGINE"));

        //TODO: Non-unifom output fields are expert, not totally supported yet
        pde = new PropertyDescriptorEx("skipPoints", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_SKIP_POINTS"), getResourceString("PD_SKIP_POINTS"));

        pde = new PropertyDescriptorEx("outputStartTime", beanClass);
        pde.setExpert(true);
        pde.setNumberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE);
        add(pde, getResourceString("PN_OUTPUT_START_TIME"), getResourceString("PD_IOUTPUT_START_TIME"));

        pde = DataElementPathEditor.registerOutput("simulationResultPath", beanClass, SimulationResult.class);
        add(pde, getResourceString("PN_SIMULATION_RESULT"), getResourceString("PD_SIMULATION_RESULT"));

    }

    public static class SimulationEngineEditor extends GenericComboBoxEditor
    {
        @Override
        public Object[] getAvailableValues()
        {
            return ((SimulationAnalysisParameters)this.bean).getAvailableEngines();
        }
    }
}
