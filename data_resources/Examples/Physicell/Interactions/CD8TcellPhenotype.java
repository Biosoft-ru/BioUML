import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.BasicSignaling;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Phenotype;

public class CD8TcellPhenotype extends UpdatePhenotype
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        CellDefinition pCD = pCell.getModel().getCellDefinition( pCell.typeName );
        Microenvironment m = pCell.getMicroenvironment();
        int nDebris = m.findDensityIndex( "debris" );
        int nPIF = m.findDensityIndex( "pro-inflammatory" );
        double[] samples = pCell.nearest_density_vector();
        double PIF = samples[nPIF];

        // if dead, release debris
        if( phenotype.death.dead == true )
        {
            phenotype.secretion.netExportRates[nDebris] = phenotype.volume.total;
            pCell.functions.updatePhenotype = null;
            return;
        }

        // migration bias increases with pro-inflammatory 
        double base = pCD.phenotype.motility.migrationBias;
        double max = 0.75;
        double half = pCD.custom_data.get( "migration_bias_halfmax" ); // 0.05 // 0.25 
        double hill = BasicSignaling.Hill_response_function( PIF, half, 1.5 );
        phenotype.motility.migrationBias = base + ( max - base ) * hill;
    }

    @Override
    public String display()
    {
        return "Pro-inflammatory increases migration." + " Release debris upon death.";
    }
}