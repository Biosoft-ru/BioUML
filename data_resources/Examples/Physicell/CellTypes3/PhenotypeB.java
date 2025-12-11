import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.standard.UpDownSignal;

public class PhenotypeB extends UpdatePhenotype
{
    private Model model;
    private boolean isInit = false;
    
    public void init(Model model)
    {
        this.model = model;
        this.isInit = true;
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        if ( !isInit )
            init( pCell.getModel() );

        // housekeeping 
        Microenvironment m = pCell.getMicroenvironment();
        CellDefinition pCD = model.getCellDefinition( "B" );
        int nApoptosis = pCD.phenotype.death.findDeathModelIndex( "Apoptosis" );
        int nNecrosis = pCD.phenotype.death.findDeathModelIndex( "Necrosis" );

        if( phenotype.death.dead )
        {
            phenotype.secretion.setSecretionToZero();
            phenotype.secretion.setUptakeToZero();
            phenotype.motility.isMotile = false;
            pCell.functions.updatePhenotype = null;
            return;
        }

        // sample A, B, C, resource, and pressure 
        int nA = m.findDensityIndex( "signal A" );
        int nB = m.findDensityIndex( "signal B" );
        int nC = m.findDensityIndex( "signal C" );
        int nR = m.findDensityIndex( "resource" );

        double A = pCell.nearest_density_vector()[nA];
        double B = pCell.nearest_density_vector()[nB];
        double C = pCell.nearest_density_vector()[nC];
        double R = pCell.nearest_density_vector()[nR];
        double p = pCell.state.simplePressure;

        // necrotic death rate 
        double base_necrosis_rate = pCD.phenotype.death.rates.get( nNecrosis );
        double necrosis_threshold = model.getParameterDouble( "B_necrosis_threshold" );
        phenotype.death.rates.set( nNecrosis, 0.0 );

        if( R < necrosis_threshold )
        {
            phenotype.death.rates.set( nNecrosis, base_necrosis_rate * ( 1.0 - R / necrosis_threshold ) );
            //                phenotype.death.rates[nNecrosis] = base_necrosis_rate;
            //                phenotype.death.rates[nNecrosis] *= ( 1.0 - R / necrosis_threshold );
        }

        // cycle rate 
        double param0 = model.getParameterDouble( "B_base_cycle" ) * R;

        UpDownSignal sig = new UpDownSignal( model );
        sig.baseParameter = param0;
        sig.maxParameter = model.getParameterDouble( "B_max_cycle" );
        sig.addEffect( A, model.getParameterString( "B_cycle_A" ) );// A
        sig.addEffect( B, model.getParameterString( "B_cycle_B" ) ); // 
        sig.addEffect( C, model.getParameterString( "B_cycle_C" ) );// C 
        phenotype.cycle.data.setTransitionRate( 0, 0, sig.computeEffect() );
        if( p > model.getParameterDouble( "B_cycle_pressure_threshold" ) )
        {
            phenotype.cycle.data.setTransitionRate( 0, 0, 0 );
        }

        // apoptotic rate 
        double base_death_rate = model.getParameterDouble( "B_base_death" );
        double max_death_rate = model.getParameterDouble( "B_max_death" );
        sig.reset();
        sig.baseParameter = base_death_rate;
        sig.maxParameter = max_death_rate;
        sig.addEffect( A, model.getParameterString( "B_death_A" ) );// A
        sig.addEffect( B, model.getParameterString( "B_death_B" ) ); // B 
        sig.addEffect( C, model.getParameterString( "B_death_C" ) );// C       
        sig.addEffect( C, model.getParameterString( "B_death_R" ) );// R 

        phenotype.death.rates.set( nApoptosis, sig.computeEffect() );
        if( p > model.getParameterDouble( "A_apoptosis_pressure_threshold" ) )
        {
            phenotype.death.rates.set( nApoptosis, 10.0 );
        }

        // speed 
        double base_speed = model.getParameterDouble( "B_base_speed" );
        double max_speed = model.getParameterDouble( "B_max_speed" );
        sig.reset();
        sig.baseParameter = base_speed;
        sig.maxParameter = max_speed;
        sig.addEffect( A, model.getParameterString( "B_speed_A" ) ); // A 
        sig.addEffect( B, model.getParameterString( "B_speed_B" ) ); // B
        sig.addEffect( C, model.getParameterString( "B_speed_C" ) );// C 
        sig.addEffect( C, model.getParameterString( "B_speed_R" ) ); // R 
        phenotype.motility.migrationSpeed = sig.computeEffect();

        // secretion 
        double base_secretion = model.getParameterDouble( "B_base_secretion" );
        double max_secretion = model.getParameterDouble( "B_max_secretion" );
        sig.reset();
        sig.baseParameter = base_secretion;
        sig.maxParameter = max_secretion;
        sig.addEffect( A, model.getParameterString( "B_signal_A" ) );// A        
        sig.addEffect( B, model.getParameterString( "B_signal_B" ) ); // B         
        sig.addEffect( C, model.getParameterString( "B_signal_C" ) );// C           
        sig.addEffect( R, model.getParameterString( "B_signal_R" ) );// R 
        phenotype.secretion.secretionRates[nB] = sig.computeEffect();
    }

    @Override
    public PhenotypeB clone()
    {
        return new PhenotypeB( );
    }
}