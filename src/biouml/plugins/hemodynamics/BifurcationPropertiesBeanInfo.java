package biouml.plugins.hemodynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

public class BifurcationPropertiesBeanInfo extends BeanInfoEx2<BifurcationProperties>
{
    public BifurcationPropertiesBeanInfo()
    {
        super(BifurcationProperties.class);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        addWithTags("parentVesselName", bean -> bean.getAvailableParents());
        add("vessel");
    }
}