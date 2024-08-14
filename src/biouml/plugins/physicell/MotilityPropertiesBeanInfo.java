package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class MotilityPropertiesBeanInfo extends BeanInfoEx2<MotilityProperties>
{
    public MotilityPropertiesBeanInfo()
    {
        super( MotilityProperties.class );
    }

    @Override
    public void initProperties()
    {
        try
        {
            add( "motile" );
            property( "migrationSpeed" ).hidden( "isNotMotile" ).add();
            property( "persistenceTime" ).hidden( "isNotMotile" ).add();
            property( "migrationBias" ).hidden( "isNotMotile" ).add();
            property( "restrictTo2D" ).hidden( "isNotMotile" ).add();
            property( "chemotaxis" )
                    .childDisplayName( beanClass.getMethod( "getChemotaxisName", new Class[] {Integer.class, Object.class} ) )
                    .hidden( "isNotMotile" ).add();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}