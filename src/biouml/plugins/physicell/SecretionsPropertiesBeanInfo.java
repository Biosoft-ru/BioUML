package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SecretionsPropertiesBeanInfo extends BeanInfoEx2<SecretionsProperties>
{
    public SecretionsPropertiesBeanInfo()
    {
        super( SecretionsProperties.class );
        setSubstituteByChild( true );
    }

    @Override
    public void initProperties()
    {
        try
        {
            property( "secretion" ).childDisplayName( beanClass.getMethod( "getSecretionName", new Class[] {Integer.class, Object.class} ) )
                    .add();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}