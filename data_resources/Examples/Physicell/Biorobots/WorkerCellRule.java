import java.util.Set;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.SignalBehavior;

public class WorkerCellRule extends UpdatePhenotype
{
    private double attachedMigrationBias;
    private double unattachedMigrationBias;
    private double elasticCoefficient;
    private double threshold;
    private SignalBehavior signals;
    private boolean isInit = false;

    public void init(Model m)
    {
        this.signals = m.getSignals();
        threshold = m.getParameterDouble( "drop_threshold" ); // 0.4; 
        attachedMigrationBias = m.getParameterDouble( "attached_worker_migration_bias" );
        unattachedMigrationBias = m.getParameterDouble( "unattached_worker_migration_bias" );
        elasticCoefficient = m.getParameterDouble( "elastic_coefficient" );
        isInit = true;
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt) throws Exception
    {
        if (!isInit)
            init( pCell.getModel() );
        double director_signal = pCell.getModel().getSignals().getSingleSignal( pCell, "director signal" );

        signals.setSingleBehavior( pCell, "cell-cell adhesion elastic constant", elasticCoefficient );

        // have I arrived? If so, release my cargo set chemotaxis weights to seek cargo set migration bias 
        if( director_signal > threshold )
        {
            // set receptor = 0 for cells we're detaching from and set their cycle rate to zero 
            for( Cell pTemp : pCell.state.attachedCells )
            {
                signals.setSingleBehavior( pTemp, "custom:receptor", 0.0 );
                signals.setSingleBehavior( pTemp, "cycle entry", 0.0 );
            }
            pCell.removeAllAttachedCells();

            signals.setSingleBehavior( pCell, "chemotactic response to director signal", 0.0 );
            signals.setSingleBehavior( pCell, "chemotactic response to cargo signal", 1.0 );
            signals.setSingleBehavior( pCell, "migration bias", unattachedMigrationBias );
        }

        // am I searching for cargo? if so, see if I've found it
        if( pCell.state.numberAttachedCells() == 0 )
        {
            Set<Cell> nearby = pCell.cells_in_my_container();
            for( Cell cell : nearby )
            {
                // if it is expressing the receptor, dock with it set chemotaxis weights set migration bias 
                if( cell == pCell )
                    continue;
                double receptor = signals.getSingleSignal( cell, "custom:receptor" );

                if( receptor > 0.5 )
                {
                    Cell.attachcCells( pCell, cell );
                    signals.setSingleBehavior( cell, "custom:receptor", 0.0 );
                    signals.setSingleBehavior( cell, "director signal secretion", 0.0 );
                    signals.setSingleBehavior( cell, "cargo signal secretion", 0.0 );

                    signals.setSingleBehavior( pCell, "chemotactic response to director signal", 1.0 );
                    signals.setSingleBehavior( pCell, "chemotactic response to cargo signal", 0.0 );
                    signals.setSingleBehavior( pCell, "migration bias", attachedMigrationBias );
                }
            }
        }
    }
}