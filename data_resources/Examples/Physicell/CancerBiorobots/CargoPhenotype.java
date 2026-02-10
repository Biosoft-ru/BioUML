import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.SignalBehavior;

public class CargoPhenotype extends UpdatePhenotype
{
    private SignalBehavior signals;

    public CargoPhenotype(Model model)
    {
        signals = model.getSignals();
    }

    public void execute(Cell pCell, Phenotype phenotype, double dt) throws Exception
    {
        // if dettached and receptor on, secrete signal
        // if dettached and receptor off, secrete chemo
        double receptor = signals.getSingleSignal( pCell, "custom:receptor" );

        if( pCell.state.numberAttachedCells() == 0 )
        {
            if( receptor > 0.1 )
            {
                signals.setSingleBehavior( pCell, "chemoattractant secretion", 10 );
                signals.setSingleBehavior( pCell, "therapeutic secretion", 0 );
            }
            else
            {
                signals.setSingleBehavior( pCell, "chemoattractant secretion", 0 );
                signals.setSingleBehavior( pCell, "therapeutic secretion", 10 );
            }
            return;
        }

        // if you reach this point of the code, the cell is attached
        // if attached and oxygen high, secrete nothing, receptor off
        // if attached and oxygen low, dettach, start secreting chemo, receptor off
        double o2 = signals.getSingleSignal( pCell, "oxygen" );
        double o2Drop = signals.getSingleSignal( pCell, "custom:cargo_release_o2_threshold" );

        if( o2 > o2Drop )
        {
            signals.setSingleBehavior( pCell, "chemoattractant secretion", 0 );
            signals.setSingleBehavior( pCell, "therapeutic secretion", 0 );
            signals.setSingleBehavior( pCell, "custom:receptor", 0 );
        }
        else
        {
            signals.setSingleBehavior( pCell, "chemoattractant secretion", 0 );
            signals.setSingleBehavior( pCell, "therapeutic secretion", 10 );
            signals.setSingleBehavior( pCell, "custom:receptor", 0 );
            pCell.removeAllAttachedCells();
        }
    }
}