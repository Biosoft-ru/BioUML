package biouml.plugins.simulation;

import com.developmentontheedge.beans.BeanInfoEx;

public class OdeSimulatorOptionsBeanInfo extends BeanInfoEx
{
    public OdeSimulatorOptionsBeanInfo(Class<?> beanClass)
    {
        super( beanClass, true );
    }

    public OdeSimulatorOptionsBeanInfo()
    {
        super( OdeSimulatorOptions.class, true );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("atol");
        add("rtol");
        addWithTags("statisticsMode", OdeSimulatorOptions.STATISTICS_MODS);
    }
}