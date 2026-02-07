import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.SignalBehavior;

public class CargoCellRule extends UpdatePhenotype
{
    private double elasticCoefficient;
    private SignalBehavior signals;
    private boolean isInit;

    public void init(Model model)
    {
        signals = model.getSignals();
        elasticCoefficient = model.getParameterDouble( "elastic_coefficient" );
        isInit = true;
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt) throws Exception
    {
        if (!isInit)
            init(pCell.getModel());
        signals.setSingleBehavior( pCell, "cell-cell adhesion elastic constant", elasticCoefficient );
    }
}