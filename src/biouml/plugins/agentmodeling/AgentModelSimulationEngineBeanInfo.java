package biouml.plugins.agentmodeling;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class AgentModelSimulationEngineBeanInfo extends BeanInfoEx2<AgentModelSimulationEngine>
{
    public AgentModelSimulationEngineBeanInfo()
    {
        super(AgentModelSimulationEngine.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("initialTime");
        add("completionTime");
        add("timeIncrement");
        add("flatSubmodels");
        add("mainEngine");
        PropertyDescriptorEx pde = new PropertyDescriptorEx("engines", beanClass);
        pde.setChildDisplayName(beanClass.getMethod("calcEngineName", new Class[] {Integer.class, Object.class}));
        add(pde);
    }

}
