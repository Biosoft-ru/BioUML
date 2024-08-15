package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Volume;

@PropertyName ( "Volume" )
public class VolumeProperties
{
    private Volume v = new Volume();

    public VolumeProperties()
    {
    }

    public VolumeProperties(Volume v)
    {
        this.v = v;
    }

    public void createVolume(CellDefinition cd)
    {
        v.adjust();
        cd.phenotype.volume = v.clone();
        cd.phenotype.geometry.update( null, cd.phenotype, 0.0 );
    }

    @PropertyName ( "Total volume" )
    public double getTotal()
    {
        return v.total;
    }
    public void setTotal(double total)
    {
        v.total = total;
    }

    @PropertyName ( "Fluid fraction" )
    public double getFluid_fraction()
    {
        return v.fluid_fraction;
    }
    public void setFluid_fraction(double fluid_fraction)
    {
        v.fluid_fraction = fluid_fraction;
    }

    @PropertyName ( "Nuclear" )
    public double getNuclear()
    {
        return v.nuclear;
    }
    public void setNuclear(double nuclear)
    {
        v.nuclear = nuclear;
    }

    @PropertyName ( "Fluid change rate" )
    public double getFluid_change_rate()
    {
        return v.fluid_change_rate;
    }
    public void setFluid_change_rate(double fluid_change_rate)
    {
        v.fluid_change_rate = fluid_change_rate;
    }

    @PropertyName ( "Cytoplasmic biomass change rate" )
    public double getCytoplasmic_biomass_change_rate()
    {
        return v.cytoplasmic_biomass_change_rate;
    }
    public void setCytoplasmic_biomass_change_rate(double cytoplasmic_biomass_change_rate)
    {
        v.cytoplasmic_biomass_change_rate = cytoplasmic_biomass_change_rate;
    }

    @PropertyName ( "Nuclear biomass change rate" )
    public double getNuclear_biomass_change_rate()
    {
        return v.nuclear_biomass_change_rate;
    }
    public void setNuclear_biomass_change_rate(double nuclear_biomass_change_rate)
    {
        v.nuclear_biomass_change_rate = nuclear_biomass_change_rate;
    }

    @PropertyName ( "Calcified fraction" )
    public double getCalcified_fraction()
    {
        return v.calcified_fraction;
    }
    public void setCalcified_fraction(double calcified_fraction)
    {
        v.calcified_fraction = calcified_fraction;
    }

    @PropertyName ( "Calcification rate" )
    public double getCalcification_rate()
    {
        return v.calcification_rate;
    }
    public void setCalcification_rate(double calcification_rate)
    {
        v.calcification_rate = calcification_rate;
    }

    @PropertyName ( "Relative rapture volume" )
    public double getRelative_rupture_volume()
    {
        return v.relative_rupture_volume;
    }
    public void setRelative_rupture_volume(double relative_rupture_volume)
    {
        v.relative_rupture_volume = relative_rupture_volume;
    }

    public VolumeProperties clone()
    {
        return new VolumeProperties( v.clone() );
    }

    public Volume getVolume()
    {
        return v;
    }
}