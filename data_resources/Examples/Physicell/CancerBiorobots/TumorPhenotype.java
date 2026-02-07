import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.RandomGenerator;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.core.standard.O2based;

public class TumorPhenotype extends UpdatePhenotype
{
    private SignalBehavior signals;
    private RandomGenerator rng;

    public TumorPhenotype(Model model)
    {
        signals = model.getSignals();
        rng = model.getRNG();
    }

    public void execute(Cell pCell, Phenotype phenotype, double dt) throws Exception
    {
        double damage = signals.getSingleSignal( pCell, "damage" );

        double damageRate = signals.getSingleSignal( pCell, "custom:damage_rate" );
        double repairRate = signals.getSingleSignal( pCell, "custom:repair_rate" );
        double drugDeathRate = signals.getSingleSignal( pCell, "custom:drug_death_rate" );

        double drug = signals.getSingleSignal( pCell, "therapeutic" );

        double maxDamage = 1.0 * damageRate / ( 1e-16 + repairRate );

        // if I'm dead, don't bother. disable my phenotype rule
        if( signals.getSingleSignal( pCell, "dead" ) > 0.5 )
        {
            pCell.functions.updatePhenotype = null;
            return;
        }

        // first, vary the cell birth and death rates with oxygenation

        // std::cout << get_single_behavior( pCell , "cycle entry") << " vs ";
        new O2based().execute( pCell, phenotype, dt );
        // std::cout << get_single_behavior( pCell , "cycle entry") << std::endl;

        // the update the cell damage

        // dD/dt = alpha*c - beta-D by implicit scheme

        //        double temp = drug;

        // reuse temp as much as possible to reduce memory allocations etc.
        //        temp *= dt;
        //        temp *= damageRate;

        damage = ( damage + drug * dt * damageRate ) / ( 1 + repairRate * dt );//temp; // d_prev + dt*chemo*damage_rate

        //        temp = repairRate;
        //        temp *= dt;
        //        temp += 1.0;
        //damage /= ( repairRate * dt + 1 );//temp; // (d_prev + dt*chemo*damage_rate)/(1 + dt*repair_rate)

        // then, see if the cell undergoes death from the therapy
        double temp = dt * damage * drugDeathRate / maxDamage;
        //        temp = dt;
        //        temp *= damage;
        //        temp *= drugDeathRate;
        //        temp /= maxDamage; // dt*(damage/max_damage)*death_rate

        // make sure we write the damage (not current a behavior)
        pCell.phenotype.cellIntegrity.damage = damage;
        if( rng.checkRandom( temp ) )
        {
            // pCell.start_death( apoptosis_model_index );
            signals.setSingleBehavior( pCell, "apoptosis", 9e99 );
            pCell.functions.updatePhenotype = null;
            pCell.functions.customCellRule = null;
        }
    }
}