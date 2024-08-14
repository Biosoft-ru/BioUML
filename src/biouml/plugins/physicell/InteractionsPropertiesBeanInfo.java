package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class InteractionsPropertiesBeanInfo extends BeanInfoEx2<InteractionsProperties>
{
    public InteractionsPropertiesBeanInfo()
    {
        super( InteractionsProperties.class );
        setSubstituteByChild( true );
    }

    @Override
    public void initProperties()
    {
        try
        {
            add( "damageRate" );
            add( "deadPhagocytosisRate" );
            property( "interactions" )
                    .childDisplayName( beanClass.getMethod( "getInteractionName", new Class[] {Integer.class, Object.class} ) )
                    .add();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}