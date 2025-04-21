package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.physicell.core.AsymmetricDivision;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CycleModel;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.util.bean.BeanInfoEx2;

@PropertyName ( "Division model" )
public class DivisionProperties extends Option
{
    private boolean asymmetric = false;
    private DivisionProbability[] probabilities = new DivisionProbability[0];

    public DivisionProperties()
    {

    }

    public DivisionProperties(CycleModel cycle)
    {
        this.setAsymmetric( cycle.getAsymmetricDivision().isEnabled() );
        double[] probs = cycle.getAsymmetricDivision().getProbabilities();
        String[] names = cycle.getAsymmetricDivision().getNames();
        probabilities = new DivisionProbability[names.length];
        for( int i = 0; i < probabilities.length; i++ )
        {
            probabilities[i] = new DivisionProbability();
            probabilities[i].setName( names[i] );
            probabilities[i].setProbability( probs[i] );
        }
    }

    public void createDivision(CellDefinition cd, Model model) throws Exception
    {
        if( isDefault() )
            return;
        AsymmetricDivision aDivision = new AsymmetricDivision();
        aDivision.initialize( model );
        for( DivisionProbability dProbability : this.probabilities )
            aDivision.setProbability( dProbability.name, dProbability.probability );
        cd.phenotype.cycle.setAsymmetricDivision( aDivision );
    }

    @PropertyName ( "Asymmetric division" )
    public boolean isAsymmetric()
    {
        return asymmetric;
    }
    public void setAsymmetric(boolean asymmetric)
    {
        boolean oldValue = isAsymmetric();
        this.asymmetric = asymmetric;
        firePropertyChange( "asymmetric", oldValue, asymmetric );
        firePropertyChange( "*", null, null );
    }

    public boolean isDefault()
    {
        return !isAsymmetric();
    }

    @PropertyName ( "Probabilities" )
    public DivisionProbability[] getProbabilities()
    {
        return probabilities;
    }
    public void setProbabilities(DivisionProbability[] probabilities)
    {
        DivisionProbability[] oldValue = getProbabilities();
        this.probabilities = probabilities;
        firePropertyChange( "probabilities", oldValue, probabilities );
    }

    public DivisionProperties clone()
    {
        DivisionProperties result = new DivisionProperties();
        result.setAsymmetric( asymmetric );
        result.setProbabilities( StreamEx.of( probabilities ).map( p -> p.clone() ).toArray( DivisionProbability[]::new ) );
        return result;
    }

    public static class DivisionProbability extends Option
    {
        private String name;
        private double probability;

        @PropertyName ( "Cell type" )
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }

        @PropertyName ( "Probability" )
        public double getProbability()
        {
            return probability;
        }
        public void setProbability(double probability)
        {
            this.probability = probability;
        }

        public DivisionProbability clone()
        {
            DivisionProbability result = new DivisionProbability();
            result.setName( name );
            result.setProbability( probability );
            return result;
        }
    }

    public static class DivisionProbabilityBeanInfo extends BeanInfoEx2<DivisionProbability>
    {
        public DivisionProbabilityBeanInfo()
        {
            super( DivisionProbability.class );
        }

        @Override
        public void initProperties()
        {
            add( "name" );
            add( "probability" );
        }
    }

}