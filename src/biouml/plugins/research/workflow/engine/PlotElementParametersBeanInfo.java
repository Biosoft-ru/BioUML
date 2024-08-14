
package biouml.plugins.research.workflow.engine;

import ru.biosoft.access.repository.DataElementPathEditor;
import biouml.plugins.research.MessageBundle;
import biouml.standard.simulation.plot.Plot;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author anna
 *
 */
public class PlotElementParametersBeanInfo extends BeanInfoEx
{
    public PlotElementParametersBeanInfo()
    {
        super(PlotElementParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_PLOT_PROPERTIES"));
        beanDescriptor.setShortDescription(getResourceString("CD_PLOT_PROPERTIES"));
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = DataElementPathEditor.registerOutput("plotPath", beanClass, Plot.class);
        add(pde, getResourceString("PN_PLOT_PATH"), getResourceString("PD_PLOT_PATH"));
        
        pde = new PropertyDescriptorEx("autoOpen", beanClass, "isAutoOpen", "setAutoOpen");
        add(pde, getResourceString("PN_PLOT_AUTO_OPEN"), getResourceString("PD_PLOT_AUTO_OPEN"));
    }
}
