import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.standard.UpDownSignal;

public class PhenotypeA extends UpdatePhenotype
{
    private Model model;
    private boolean isInit = false;
    
    public void init(Model model)
    {
        this.model = model;
        this.isInit = true;
    }

    public void execute(Cell cell, Phenotype phenotype, double dt)
    {
        if ( !isInit )
            init( cell.getModel() );
        
        Microenvironment m = cell.getMicroenvironment();
        CellDefinition cd = model.getCellDefinition( "A" );
        int nApoptosis = cd.phenotype.death.findDeathModelIndex( "Apoptosis" );
        int nNecrosis = cd.phenotype.death.findDeathModelIndex( "Necrosis" );

        if( phenotype.death.dead )
        {
            phenotype.secretion.setSecretionToZero();
            phenotype.secretion.setUptakeToZero();
            phenotype.motility.isMotile = false;
            cell.functions.updatePhenotype = null;
            return;
        }

        // sample A, B, C, resource, and pressure 
        int nA = m.findDensityIndex( "signal A" );
        int nB = m.findDensityIndex( "signal B" );
        int nC = m.findDensityIndex( "signal C" );
        int nR = m.findDensityIndex( "resource" );

        double A = cell.nearest_density_vector()[nA];
        double B = cell.nearest_density_vector()[nB];
        double C = cell.nearest_density_vector()[nC];
        double R = cell.nearest_density_vector()[nR];
        double p = cell.state.simplePressure;

        // necrotic death rate 
        double base_necrosis_rate = cd.phenotype.death.rates.get( nNecrosis );
        double necrosis_threshold = model.getParameterDouble( "A_necrosis_threshold" );
        phenotype.death.rates.set( nNecrosis, 0.0 );

        if( R < necrosis_threshold )
            phenotype.death.rates.set( nNecrosis, base_necrosis_rate * ( 1.0 - R / necrosis_threshold ) );

        // cycle rate 
        double param0 = model.getParameterDouble( "A_base_cycle" ) * R;
        UpDownSignal sig = new UpDownSignal( model );
        sig.baseParameter = param0;
        sig.maxParameter = model.getParameterDouble( "A_max_cycle" );
        sig.addEffect( A, model.getParameterString( "A_cycle_A" ) );// A 
        sig.addEffect( B, model.getParameterString( "A_cycle_B" ) );// B
        sig.addEffect( C, model.getParameterString( "A_cycle_C" ) );// C 
        phenotype.cycle.data.setTransitionRate( 0, 0, sig.computeEffect() );
        if( p > model.getParameterDouble( "A_cycle_pressure_threshold" ) )
        {
            phenotype.cycle.data.setTransitionRate( 0, 0, 0 );
        }

        // apoptotic rate 
        double base_death_rate = model.getParameterDouble( "A_base_death" );
        double max_death_rate = model.getParameterDouble( "A_max_death" );
        sig.reset();
        sig.baseParameter = base_death_rate;
        sig.maxParameter = max_death_rate;
        sig.addEffect( A, model.getParameterString( "A_death_A" ) ); // A       
        sig.addEffect( B, model.getParameterString( "A_death_B" ) ); // B         
        sig.addEffect( C, model.getParameterString( "A_death_C" ) ); // C 
        sig.addEffect( C, model.getParameterString( "A_death_R" ) ); // R 
        phenotype.death.rates.set( nApoptosis, sig.computeEffect() );
        if( p > model.getParameterDouble( "A_apoptosis_pressure_threshold" ) )
        {
            phenotype.death.rates.set( nApoptosis, 10.0 );
        }

        // speed 
        double base_speed = model.getParameterDouble( "A_base_speed" );
        double max_speed = model.getParameterDouble( "A_max_speed" );
        sig.reset();
        sig.baseParameter = base_speed;
        sig.maxParameter = max_speed;
        sig.addEffect( A, model.getParameterString( "A_speed_A" ) );// A
        sig.addEffect( B, model.getParameterString( "A_speed_B" ) );// B
        sig.addEffect( C, model.getParameterString( "A_speed_C" ) );// C 
        sig.addEffect( C, model.getParameterString( "A_speed_R" ) ); // R 
        phenotype.motility.migrationSpeed = sig.computeEffect();

        // secretion 
        double base_secretion = model.getParameterDouble( "A_base_secretion" );
        double max_secretion = model.getParameterDouble( "A_max_secretion" );
        sig.reset();
        sig.baseParameter = base_secretion;
        sig.maxParameter = max_secretion;
        sig.addEffect( A, model.getParameterString( "A_signal_A" ) );// A            
        sig.addEffect( B, model.getParameterString( "A_signal_B" ) );// B            
        sig.addEffect( C, model.getParameterString( "A_signal_C" ) );// C            
        sig.addEffect( R, model.getParameterString( "A_signal_R" ) ); // R
        phenotype.secretion.secretionRates[nA] = sig.computeEffect();
    }

    @Override
    public PhenotypeA clone()
    {
        return new PhenotypeA( );
    }
}