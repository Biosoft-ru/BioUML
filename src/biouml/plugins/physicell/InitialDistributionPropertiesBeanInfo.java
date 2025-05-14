package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class InitialDistributionPropertiesBeanInfo extends BeanInfoEx2<InitialDistributionProperties>
{
    public InitialDistributionPropertiesBeanInfo()
    {
        super( InitialDistributionProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "parameterDistributions" );
    }
}
