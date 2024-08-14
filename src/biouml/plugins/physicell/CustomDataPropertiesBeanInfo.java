package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CustomDataPropertiesBeanInfo extends BeanInfoEx2<CustomDataProperties>
{
    public CustomDataPropertiesBeanInfo()
    {
        super( CustomDataProperties.class );
        this.setSubstituteByChild( true );
    }

    @Override
    public void initProperties()
    {
        add( "variables" );
    }
}