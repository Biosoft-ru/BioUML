package biouml.plugins.simulation.plot;

import ru.biosoft.access.repository.DataElementPathEditor;
import biouml.standard.simulation.plot.Plot;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * Definition of common properties for simulation engine
 */
public class PlotExBeanInfo
    extends BeanInfoEx
{
    public PlotExBeanInfo()
    {
        super(PlotEx.class, biouml.plugins.simulation.resources.MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_PLOT_EX"));
        beanDescriptor.setShortDescription(getResourceString("CD_PLOT_EX"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("currentPlotPath", beanClass, Plot.class),
                getResourceString("PN_PLOT_EX_PLOT_NAME"), getResourceString("PD_PLOT_EX_PLOT_NAME"));
        add(DataElementPathEditor.registerOutput("savePlotPath", beanClass, Plot.class),
                getResourceString("PN_PLOT_EX_SAVE_NAME"), getResourceString("PD_PLOT_EX_SAVE_NAME"));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("plot", beanClass);
        add(pde,
            getResourceString("PN_PLOT_EX_PLOT"),
            getResourceString("PD_PLOT_EX_PLOT"));
    }
    
}
