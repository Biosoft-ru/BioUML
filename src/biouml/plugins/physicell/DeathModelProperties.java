package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.CycleModel;
import ru.biosoft.physicell.core.DeathParameters;

public class DeathModelProperties extends Option
{
    private DeathParameters parameters;
    private CycleProperties deathModel;
    private double rate;

    public DeathModelProperties()
    {
        deathModel = new CycleProperties( true );
        deathModel.setDeathModel( true );
        parameters = new DeathParameters();
    }

    public DeathModelProperties(CycleModel model, DeathParameters params, double rate)
    {
        deathModel = new CycleProperties( model, true );
        deathModel.setDeathModel( true );
        this.setRate( rate );
        this.parameters = params;
    }

    public DeathModelProperties clone()
    {
        DeathModelProperties result = new DeathModelProperties();
        result.rate = rate;
        result.parameters = parameters.clone();
        result.deathModel = deathModel.clone();
        return result;
    }

    public void setParameters(DeathParameters parameters)
    {
        this.parameters = parameters;
    }

    public void setCycleProperties(CycleProperties cycle)
    {
        this.deathModel = cycle;
    }

    public DeathParameters getParameters()
    {
        return parameters;
    }

    public CycleProperties getCycleProperties()
    {
        return deathModel;
    }

    @PropertyName ( "Time Units" )
    public String getTime_units()
    {
        return parameters.time_units;
    }
    public void setTime_units(String time_units)
    {
        parameters.time_units = time_units;
    }

    @PropertyName ( "Unlysed fluid change rate" )
    public double getUnlysed_fluid_change_rate()
    {
        return parameters.unlysed_fluid_change_rate;
    }
    public void setUnlysed_fluid_change_rate(double unlysed_fluid_change_rate)
    {
        parameters.unlysed_fluid_change_rate = unlysed_fluid_change_rate;
    }

    @PropertyName ( "Lysed fluid change rate" )
    public double getLysed_fluid_change_rate()
    {
        return parameters.lysed_fluid_change_rate;
    }
    public void setLysed_fluid_change_rate(double lysed_fluid_change_rate)
    {
        parameters.lysed_fluid_change_rate = lysed_fluid_change_rate;
    }

    @PropertyName ( "Cytoplasmic biomass change rate" )
    public double getCytoplasmic_biomass_change_rate()
    {
        return parameters.cytoplasmic_biomass_change_rate;
    }
    public void setCytoplasmic_biomass_change_rate(double cytoplasmic_biomass_change_rate)
    {
        parameters.cytoplasmic_biomass_change_rate = cytoplasmic_biomass_change_rate;
    }

    @PropertyName ( "Nuclear biomass change rate" )
    public double getNuclear_biomass_change_rate()
    {
        return parameters.nuclear_biomass_change_rate;
    }
    public void setNuclear_biomass_change_rate(double nuclear_biomass_change_rate)
    {
        parameters.nuclear_biomass_change_rate = nuclear_biomass_change_rate;
    }

    @PropertyName ( "Calcification rate" )
    public double getCalcification_rate()
    {
        return parameters.calcification_rate;
    }
    public void setCalcification_rate(double calcification_rate)
    {
        parameters.calcification_rate = calcification_rate;
    }

    @PropertyName ( "Relative rupture volume" )
    public double getRelative_rupture_volume()
    {
        return parameters.relative_rupture_volume;
    }
    public void setRelative_rupture_volume(double relative_rupture_volume)
    {
        parameters.relative_rupture_volume = relative_rupture_volume;
    }

    @PropertyName ( "Rate" )
    public double getRate()
    {
        return rate;
    }
    public void setRate(double rate)
    {
        this.rate = rate;
    }

    @PropertyName ( "Cycle" )
    public CycleProperties getCycle()
    {
        return deathModel;
    }
    public void setCycle(CycleProperties cycle)
    {
        Object oldValue = this.getCycle();
        this.deathModel = cycle;
        firePropertyChange( "cycle", oldValue, cycle );
        firePropertyChange( "*", null, null );
    }

    //    @PropertyName ( "Transitions" )
    //    public TransitionProperties[] getTransitions()
    //    {
    //        return deathModel.getTransitions();
    //    }
    //    public void setTransitions(TransitionProperties[] transitions)
    //    {
    //        this.deathModel.setTransitions( transitions );
    //    }
    //
    //    public String getTransitionName(Integer i, Object obj)
    //    {
    //        return ( (TransitionProperties)obj ).getTitle();
    //    }
    //
    //    @PropertyName ( "Phases" )
    //    public PhaseProperties[] getPhases()
    //    {
    //        return deathModel.getPhases();
    //    }
    //    public void setPhases(PhaseProperties[] phases)
    //    {
    //        this.deathModel.setPhases( phases );
    //    }
    //    public String getPhaseName(Integer i, Object obj)
    //    {
    //        return ( (PhaseProperties)obj ).getName();
    //    }

    //    @PropertyName ( "Name" )
    //    public String getName()
    //    {
    //        return deathModel.getCycleName();
    //    }
    //    public void setName(String name)
    //    {
    //        this.deathModel.setCycleName( name );
    //    }
}
