import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.CustomCellRule;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.RandomGenerator;

public class ImmuneCellRule extends CustomCellRule
{
    private RandomGenerator rng;

    public ImmuneCellRule(Model model)
    {
        rng = model.getRNG();
    }
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        int attach_lifetime_i = pCell.customData.findVariableIndex( "attachment_lifetime" );

        if( phenotype.death.dead == true )
        {
            // the cell death functions don't automatically turn off custom functions, since those are part of mechanics. 
            // Let's just fully disable now. 
            pCell.functions.customCellRule = null;
            return;
        }

        // if I'm docked
        if( pCell.state.numberAttachedCells() > 0 )
        {
            // attempt to kill my attached cell
            Cell attached = pCell.state.attachedCells.iterator().next();///[0];
            boolean detachMe = false;

            if( attemptApoptosis( pCell, attached, dt ) )
            {
                triggerApoptosis( pCell, attached );
                detachMe = true;
            }

            // decide whether to detach 
            if( rng.checkRandom( dt / ( pCell.customData.get( attach_lifetime_i ) + 1e-15 ) ) )
                detachMe = true;

            // if I dettach, resume motile behavior 
            if( detachMe )
            {
                Cell.detachCells( pCell, attached );
                phenotype.motility.isMotile = true;
            }
            return;
        }

        // I'm not docked, look for cells nearby and try to docked if this returns non-NULL, we're now attached to a cell 
        if( checkNeighborsForAttachment( pCell, dt ) != null )
        {
            phenotype.motility.isMotile = false;
            return;
        }
        phenotype.motility.isMotile = true;
    }

    static boolean triggerApoptosis(Cell pAttacker, Cell pTarget)
    {
        if( pTarget.phenotype.death.dead )
            return false;
        pTarget.startDeath( pTarget.phenotype.death.findDeathModelIndex( "apoptosis" ) );
        return true;
    }

    boolean attemptApoptosis(Cell pAttacker, Cell pTarget, double dt)
    {
        int oncoproteinIndex = pTarget.customData.findVariableIndex( "oncoprotein" );
        int killRateIndex = pAttacker.customData.findVariableIndex( "kill_rate" );

        double oncoproteinSaturation = pAttacker.customData.get( "oncoprotein_saturation" ); // 2.0; 
        double oncoproteinThreshold = pAttacker.customData.get( "oncoprotein_threshold" ); // 0.5; // 0.1; 
        double oncoproteinDifference = oncoproteinSaturation - oncoproteinThreshold;

        double targetOconoprotein = pTarget.customData.get( oncoproteinIndex );
        if( targetOconoprotein < oncoproteinThreshold )
            return false;

        double scale = ( targetOconoprotein - oncoproteinThreshold ) / oncoproteinDifference;
        scale = Math.min( scale, 1.0 );

        if( rng.checkRandom( pAttacker.customData.get( killRateIndex ) * scale * dt ) )
            return true;
        return false;
    }

    public Cell checkNeighborsForAttachment(Cell pAttacker, double dt)
    {
        for( Cell nearbyCell : pAttacker.cells_in_my_container() )
        {
            if( nearbyCell != pAttacker )// don't try to kill yourself 
            {
                if( attemptAttachment( pAttacker, nearbyCell, dt ) )
                    return nearbyCell;
            }
        }
        return null;
    }

    boolean attemptAttachment(Cell pAttacker, Cell pTarget, double dt)
    {
        double oncoprotein_saturation = pAttacker.customData.get( "oncoprotein_saturation" );
        double oncoprotein_threshold = pAttacker.customData.get( "oncoprotein_threshold" );
        double maxAttachmentDistance = pAttacker.customData.get( "max_attachment_distance" );
        double minAttachmentDistance = pAttacker.customData.get( "min_attachment_distance" );
        double targetOncoprotein = pTarget.customData.get( "oncoprotein" );
        if( targetOncoprotein > oncoprotein_threshold && !pTarget.phenotype.death.dead )
        {
            double distance = VectorUtil.dist( pTarget.position, pAttacker.position );
            if( distance > maxAttachmentDistance )
                return false;

            double attachRate = pAttacker.customData.get( "attachment_rate" );
            double scale = ( targetOncoprotein - oncoprotein_threshold ) / ( oncoprotein_saturation - oncoprotein_threshold );
            double distanceScale = ( maxAttachmentDistance - distance ) / ( maxAttachmentDistance - minAttachmentDistance );
            attachRate *= Math.min( scale, 1.0 ) * Math.min( distanceScale, 1.0 );
            if( rng.checkRandom( attachRate * dt ) )
                Cell.attachcCells( pAttacker, pTarget );
            return true;//TODO: should we return true only if attached successfully?
        }
        return false;
    }
}