import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.UpdateMigrationBias;
import ru.biosoft.physicell.core.Phenotype;

/**
 * If not attached move towards immunostimulatory factor, otherwise do not move
 */
public class ImmuneCellMotility extends UpdateMigrationBias
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        Microenvironment m = pCell.getMicroenvironment();
        int immuneFactorIndex = m.findDensityIndex( "immunostimulatory factor" );
        if( pCell.state.attachedCells.size() == 0 )
        {
            phenotype.motility.isMotile = true;
            phenotype.motility.migrationBiasDirection = VectorUtil.newNormalize( pCell.nearest_gradient( immuneFactorIndex ) );
        }
        else
        {
            phenotype.motility.isMotile = false;
        }
    }

    @Override
    public String display()
    {
        return "If not attached move towards immunostimulatory factor, otherwise do not move";
    }
}