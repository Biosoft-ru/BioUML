package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class TransformationsPropertiesBeanInfo extends BeanInfoEx2<TransformationsProperties>
{
    public TransformationsPropertiesBeanInfo()
    {
        super( TransformationsProperties.class );
        setSubstituteByChild( true );
    }

    @Override
    public void initProperties()
    {
        try
        {
            property( "transformations" )
                    .childDisplayName( beanClass.getMethod( "getTransformationName", new Class[] {Integer.class, Object.class} ) ).add();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}