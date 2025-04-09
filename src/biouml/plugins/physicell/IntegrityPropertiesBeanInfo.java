package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class IntegrityPropertiesBeanInfo extends BeanInfoEx2<IntegrityProperties>
{
    public IntegrityPropertiesBeanInfo()
    {
        super( IntegrityProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "damageRate" );
        add( "damageRepairRate" );
    }
}