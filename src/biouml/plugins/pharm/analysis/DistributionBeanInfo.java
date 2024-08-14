package biouml.plugins.pharm.analysis;

import one.util.streamex.StreamEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DistributionBeanInfo extends BeanInfoEx2<Distribution>
{
    public DistributionBeanInfo()
    {
        super(Distribution.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        property("name").tags(StreamEx.ofKeys(Distribution.nameToParameters).toArray(String[]::new)).add();
        add("parameters");
    }
}