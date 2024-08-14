package biouml.model.dynamics;

import one.util.streamex.StreamEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class UndirectedConnectionBeanInfo extends BeanInfoEx2<UndirectedConnection>
{
    public UndirectedConnectionBeanInfo()
    {
        super(UndirectedConnection.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("outputPort");
        add("inputPort");
        property("variableNamePath").tags(bean->StreamEx.of(bean.getAvailableNames())).add();
        add("conversionFactor");
    }
}
