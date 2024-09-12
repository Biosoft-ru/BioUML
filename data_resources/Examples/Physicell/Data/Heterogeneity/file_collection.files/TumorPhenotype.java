import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.core.standard.O2based;

public class TumorPhenotype extends O2based
{
    private SignalBehavior signals;
    private boolean isInit = false;

    public void init(Model model)
    {
        signals = model.getSignals();
        isInit = true;
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt) throws Exception
    {
        super.execute( pCell, phenotype, dt );
 
        if ( !isInit )
            init(pCell.getModel());
        
        if( signals.getSingleSignal( pCell, "dead" ) > 0.5 )
        {
            pCell.functions.updatePhenotype = null;// if cell is dead, don't bother with future phenotype changes. 
        }
        else
        {
            // multiply proliferation rate by the oncoprotein 
            double rate = signals.getSinglBehavior( pCell, "cycle entry" );
            double factor = signals.getSingleSignal( pCell, "custom:oncoprotein" );
            signals.setSingleBehavior( pCell, "cycle entry", rate * factor );
        }
    }
}