import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.Contact;
import ru.biosoft.physicell.core.Phenotype;

public class AdhesionContact extends Contact
{
    public void execute(Cell pActingOn, Phenotype pao, Cell pAttachedTo, Phenotype pat, double dt)
    {
        double[] displacement = VectorUtil.newDiff( pAttachedTo.position, pActingOn.position );
        double maxElasticDisplacement = pao.geometry.radius * pao.mechanics.relDetachmentDistance;
        double maxDisplacementSquared = maxElasticDisplacement * maxElasticDisplacement;

        // detach cells if too far apart 
        if( VectorUtil.norm_squared( displacement ) > maxDisplacementSquared )
        {
            Cell.detachCells( pActingOn, pAttachedTo );
            return;
        }
        VectorUtil.axpy( pActingOn.velocity, pao.mechanics.attachmentElasticConstant, displacement );
    }
}