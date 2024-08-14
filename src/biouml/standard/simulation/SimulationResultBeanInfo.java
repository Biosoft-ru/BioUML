package biouml.standard.simulation;

import biouml.standard.simulation.resources.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

/**
 * Definition of common properties for simulation engine
 */
public class SimulationResultBeanInfo
    extends BeanInfoEx
{
    public SimulationResultBeanInfo()
    {
        super(SimulationResult.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_SIMULATION_RESULT"));
        beanDescriptor.setShortDescription(getResourceString("CD_SIMULATION_RESULT"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null);
        HtmlPropertyInspector.setDisplayName(pde, "ID");
        add(pde,
            getResourceString("PN_SIMULATION_RESULT_NAME"),
            getResourceString("PD_SIMULATION_RESULT_NAME"));

        pde = new PropertyDescriptorEx("title", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TT");
        add(pde,
            getResourceString("PN_TITLE"),
            getResourceString("PD_TITLE"));

        pde = new PropertyDescriptorEx("diagramName", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "DN");
        add(pde,
            getResourceString("PN_DIAGRAM_NAME"),
            getResourceString("PD_DIAGRAM_NAME"));

        pde = new PropertyDescriptorEx("description", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "DE");
        add(pde,
            getResourceString("PN_SIMULATION_RESULT_DESCRIPTION"),
            getResourceString("PD_SIMULATION_RESULT_DESCRIPTION"));

        pde = new PropertyDescriptorEx("simulatorName", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "SN");
        pde.setReadOnly(true);
        pde.setHidden(beanClass.getMethod("isNotFilled"));
        add(pde,
            getResourceString("PN_SIMULATOR_NAME"),
            getResourceString("PD_SIMULATOR_NAME"));

        pde = new PropertyDescriptorEx("initialTime", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "FROM");
        pde.setReadOnly(true);
        pde.setHidden(beanClass.getMethod("isNotFilled"));
        add(pde,
            getResourceString("PN_INITIAL_TIME"),
            getResourceString("PD_INITIAL_TIME"));

        pde = new PropertyDescriptorEx("completionTime", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "TO");
        pde.setReadOnly(true);
        pde.setHidden(beanClass.getMethod("isNotFilled"));
        add(pde,
            getResourceString("PN_COMPLETION_TIME"),
            getResourceString("PD_COMPLETION_TIME"));
    }
}
