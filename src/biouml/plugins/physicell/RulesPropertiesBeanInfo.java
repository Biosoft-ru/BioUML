package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class RulesPropertiesBeanInfo extends BeanInfoEx2<RulesProperties>
{
    public RulesPropertiesBeanInfo()
    {
        super( RulesProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "rules" );
    }
}