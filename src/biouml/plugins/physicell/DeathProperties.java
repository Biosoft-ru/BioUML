package biouml.plugins.physicell;

import java.util.List;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CycleModel;
import ru.biosoft.physicell.core.Death;
import ru.biosoft.physicell.core.DeathParameters;
import ru.biosoft.physicell.core.standard.StandardModels;

@PropertyName ( "Death model" )
public class DeathProperties extends Option
{
    private DeathModelProperties[] deathModels = new DeathModelProperties[0];

    public DeathProperties(Death death)
    {
        List<CycleModel> models = death.models;
        List<Double> rates = death.rates;
        List<DeathParameters> parameters = death.parameters;
        deathModels = new DeathModelProperties[models.size()];
        for( int i = 0; i < models.size(); i++ )
        {
            deathModels[i] = new DeathModelProperties( models.get( i ), parameters.get( i ), rates.get( i ) );
        }
    }

    public DeathProperties()
    {
        try
        {
            CycleModel[] models = StandardModels.getBasicDeathModels();
            deathModels = new DeathModelProperties[models.length];
            for( int i = 0; i < models.length; i++ )
            {
                deathModels[i] = new DeathModelProperties( models[i], StandardModels.getBasicParameters( models[i].name ),
                        StandardModels.getBasicRate( models[i].name ) );
            }
        }
        catch( Exception ex )
        {

        }
    }

    public DeathProperties clone()
    {
        DeathProperties result = new DeathProperties();
        result.deathModels = new DeathModelProperties[deathModels.length];
        for( int i = 0; i < deathModels.length; i++ )
            result.deathModels[i] = deathModels[i].clone();
        return result;
    }

    public void createDeath(CellDefinition cd) throws Exception
    {
        Death result = cd.phenotype.death;
        result.models.clear();
        result.parameters.clear();
        result.rates.clear();
        for( DeathModelProperties deathModel : deathModels )
        {
            double rate = deathModel.getRate();
            CycleProperties cycleProperties = deathModel.getCycleProperties();
            DeathParameters parameters = deathModel.getParameters();
            result.addDeathModel( rate, cycleProperties.createCycle(), parameters );
        }
    }
    
    public Death createDeath() throws Exception
    {
        Death result = new Death();
        result.models.clear();
        result.parameters.clear();
        result.rates.clear();
        for( DeathModelProperties deathModel : deathModels )
        {
            double rate = deathModel.getRate();
            CycleProperties cycleProperties = deathModel.getCycleProperties();
            DeathParameters parameters = deathModel.getParameters();
            result.addDeathModel( rate, cycleProperties.createCycle(), parameters );
        }
        return result;
    }

    @PropertyName ( "Death models" )
    public DeathModelProperties[] getDeathModels()
    {
        return deathModels;
    }

    public void setDeathModels(DeathModelProperties[] deathModels)
    {
        Object oldValue = this.deathModels;
        this.deathModels = deathModels;
        firePropertyChange( "deathModels", oldValue, deathModels );
    }

    public String getDeathModelName(Integer i, Object obj)
    {
        try
        {
            return ( (DeathModelProperties)obj ).getCycle().getCycleName();
        }
        catch( Exception e )
        {
            return "UNDEFINED";
        }
    }
}