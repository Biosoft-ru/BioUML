package biouml.plugins.brain.model.regional;

import ru.biosoft.util.bean.BeanInfoEx2;

public class RosslerRegionalModelPropertiesBeanInfo extends BeanInfoEx2<RosslerRegionalModelProperties>
{
    public RosslerRegionalModelPropertiesBeanInfo()
    {
        super(RosslerRegionalModelProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("omega");
        add("alpha");
        add("b");
        add("gamma");
        
        add("a1");
        add("b1");
        add("a2");
        add("b2");
        add("etaTh");
        
        add("portsFlag");
    }
}