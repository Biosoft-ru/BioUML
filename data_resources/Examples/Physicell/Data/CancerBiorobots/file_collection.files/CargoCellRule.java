import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.CustomCellRule;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.SignalBehavior;

public class CargoCellRule extends CustomCellRule
{
    private SignalBehavior signals;

    public CargoCellRule(Model model)
    {
        signals = model.getSignals();
    }

    public void execute(Cell pCell, Phenotype phenotype, double dt) throws Exception
    {
        if( signals.getSingleSignal( pCell, "dead" ) > 0.5 )
        {
            // the cell death functions don't automatically turn off custom functions, since those are part of mechanics.
            // Let's just fully disable now.
            pCell.functions.customCellRule = null;
            return;
        }

        // if I'm docked
        if( pCell.state.numberAttachedCells() > 0 )
        {
            signals.setSingleBehavior( pCell, "migration speed", 0.0 );
            return;
        }
    }
}