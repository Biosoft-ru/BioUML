package biouml.plugins.pharm;

import com.developmentontheedge.beans.BeanInfoEx;

public class PopulationEModelBeanInfo extends BeanInfoEx
{
    public PopulationEModelBeanInfo()
    {
        super( PopulationEModel.class, true );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("comment");
    }
}
