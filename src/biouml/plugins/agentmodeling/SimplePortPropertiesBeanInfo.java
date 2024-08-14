package biouml.plugins.agentmodeling;


import one.util.streamex.StreamEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SimplePortPropertiesBeanInfo extends BeanInfoEx2<SimplePortProperties>
{
    public SimplePortPropertiesBeanInfo()
    {
        super(SimplePortProperties.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("portType").tags( bean -> StreamEx.of(bean.getAvailablePortTypes()) ).add();
        property("varName").add();
    }
}
