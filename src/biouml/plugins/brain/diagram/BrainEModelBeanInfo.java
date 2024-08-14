package biouml.plugins.brain.diagram;

import com.developmentontheedge.beans.BeanInfoEx;

public class BrainEModelBeanInfo extends BeanInfoEx
{
    public BrainEModelBeanInfo()
    {
        super(BrainEModel.class, true);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("comment");
    }
}
