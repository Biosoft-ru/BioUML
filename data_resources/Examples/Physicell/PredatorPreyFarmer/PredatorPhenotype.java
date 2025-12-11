import java.util.List;

import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.PhysiCellUtilities;

/**
 * Predator phenotype:  
 * 1. If prey is in range - eat it and gain energy 
 * 2. Energy decays through time 
 * 2. If low on energy - die 
 */
public class PredatorPhenotype extends UpdatePhenotype
{
    private double maxDetectionDistance = 2;
    private double decayRate = 0.00025;
    CellDefinition pPreyDef = null;

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        if( pPreyDef == null )
            pPreyDef = pCell.getModel().getCellDefinition( "prey" );
        List<Cell> nearby = PhysiCellUtilities.getNeighbors( pCell );
        for( Cell prey : nearby )
        {
            if( prey.type == pPreyDef.type )
            {
                double distance = VectorUtil.dist( prey.position, pCell.position );
                if( distance <= pCell.phenotype.geometry.radius + prey.phenotype.geometry.radius + maxDetectionDistance )
                {
                    pCell.ingestCell( prey );
                    pCell.customData.set( "energy", pCell.customData.get( "energy" ) + 100 );
                }
            }
        }

        pCell.customData.set( "energy", pCell.customData.get( "energy" ) / ( 1.0 + dt * decayRate ) );

        int nNecrosis = phenotype.death.findDeathModelIndex( "necrosis" );
        if( pCell.customData.get( "energy" ) < 0.1 )
        {
            pCell.startDeath( nNecrosis );
            pCell.functions.updatePhenotype = null;
            return;
        }
    }
}