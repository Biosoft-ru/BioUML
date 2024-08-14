package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SecretionPropertiesBeanInfo extends BeanInfoEx2<SecretionProperties>
{
    public SecretionPropertiesBeanInfo()
    {
        super( SecretionProperties.class );
    }

    @Override
    public void initProperties()
    {
        addReadOnly( "title" );
        add( "secretionRate" );
        add( "secretionTarget" );
        add( "uptakeRate" );
        add( "netExportRate" );
    }
}