package biouml.plugins.brain.model.regional;

import com.developmentontheedge.beans.BeanInfoEx;

public class BrainMatrixPropertiesBeanInfo extends BeanInfoEx
{
    public BrainMatrixPropertiesBeanInfo()
    {
        super(BrainMatrixProperties.class, true);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("name");
        add("tablePath");
        add("table");
        add("formula");
    }
}