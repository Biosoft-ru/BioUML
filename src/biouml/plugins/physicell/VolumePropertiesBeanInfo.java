package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class VolumePropertiesBeanInfo extends BeanInfoEx2<VolumeProperties>
{
    public VolumePropertiesBeanInfo()
    {
        super( VolumeProperties.class );
    }

    @Override
    public void initProperties()
    {
        add( "total" );
        add( "fluid_fraction" );
        add( "nuclear" );
        add( "fluid_change_rate" );
        add( "cytoplasmic_biomass_change_rate" );
        add( "nuclear_biomass_change_rate" );
        add( "calcified_fraction" );
        add( "calcification_rate" );
        add( "relative_rupture_volume" );
    }
}