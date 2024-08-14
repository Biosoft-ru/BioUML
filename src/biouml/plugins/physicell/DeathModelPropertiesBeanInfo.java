package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class DeathModelPropertiesBeanInfo extends BeanInfoEx2<DeathModelProperties>
{
    public DeathModelPropertiesBeanInfo()
    {
        super( DeathModelProperties.class );
    }

    @Override
    public void initProperties()
    {
        try
        {
            //            property( "name" ).hidden().add();
            add( "rate" );
            add( "cycle" );
            //            property("phases").childDisplayName(beanClass.getMethod("getPhaseName", new Class[] { Integer.class, Object.class })).fixedLength().add();
            //            property( "transitions" )
            //                    .childDisplayName(beanClass.getMethod("getTransitionName", new Class[] { Integer.class, Object.class })).fixedLength().add();
            add( "unlysed_fluid_change_rate" );
            add( "lysed_fluid_change_rate" );
            add( "cytoplasmic_biomass_change_rate" );
            add( "nuclear_biomass_change_rate" );
            add( "calcification_rate" );
            add( "relative_rupture_volume" );
            add( "time_units" );
        }
        catch( SecurityException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}