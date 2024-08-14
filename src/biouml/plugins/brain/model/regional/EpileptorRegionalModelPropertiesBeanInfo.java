package biouml.plugins.brain.model.regional;

import ru.biosoft.util.bean.BeanInfoEx2;

public class EpileptorRegionalModelPropertiesBeanInfo extends BeanInfoEx2<EpileptorRegionalModelProperties>
{
    public EpileptorRegionalModelPropertiesBeanInfo()
    {
        super(EpileptorRegionalModelProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("modification");
        add("w");
        add("x0");
        add("y0");
        add("tau0");
        add("tau1");
        add("tau2");
        add("I1");
        add("I2");
        add("gamma");
        
        add("portsFlag");
    }
}