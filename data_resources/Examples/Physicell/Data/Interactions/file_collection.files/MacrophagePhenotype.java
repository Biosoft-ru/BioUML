import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.BasicSignaling;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Phenotype;

/* https://www.karger.com/Article/Fulltext/494069 */
public class MacrophagePhenotype extends UpdatePhenotype
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        CellDefinition pCD = pCell.getModel().getCellDefinition( pCell.typeName );
        Microenvironment m = pCell.getMicroenvironment();
        int nPIF = m.findDensityIndex( "pro-inflammatory" );
        int nDebris = m.findDensityIndex( "debris" );
        int nQ = m.findDensityIndex( "quorum" );

        if( phenotype.death.dead == true )
        {
            phenotype.secretion.netExportRates[nDebris] = phenotype.volume.total;
            pCell.functions.updatePhenotype = null;
            return;
        }

        double[] samples = pCell.nearest_density_vector();
        double debris = samples[nDebris];
        double q = samples[nQ];

        int bacteriaType = pCell.getModel().getCellDefinition( "bacteria" ).type;
        int numBacteria = 0;
        int numDead = 0;
        for( Cell pC : pCell.state.neighbors )
        {
            if( pC.phenotype.death.dead )
                numDead++;
            else if( pC.type == bacteriaType )
                numBacteria++;
        }

        // contact with dead cells or bacteria, or debris increases secretion of pro-inflammatory 
        double secretionDeadSens = 1;
        double secretionBacteriaSens = 1;
        double secretionDebrisSens = 2;
        double secretionQuorumSens = 5;

        double base = pCD.phenotype.secretion.secretionRates[nPIF];
        double max = 10; // phenotype.volume.total; 
        double signal = secretionDeadSens * numDead + secretionBacteriaSens * numBacteria
                + secretionDebrisSens * debris + secretionQuorumSens * q;
        double half = pCD.custom_data.get( "secretion_halfmax" ); // 0.5; // 0.5; 
        double hill = BasicSignaling.Hill_response_function( signal, half, 1.5 );
        phenotype.secretion.secretionRates[nPIF] = base + ( max - base ) * hill;

        // chemotaxis bias increases with debris or quorum factor 
        double biasDebrisSensitivity = 0.1;
        double biasQuorumSensitivity = 1;

        base = pCD.phenotype.motility.migrationBias;
        max = 0.75;
        signal = biasDebrisSensitivity * debris + biasQuorumSensitivity * q; // + 10 * PIF; 
        half = pCD.custom_data.get( "migration_bias_halfmax" ); // 0.01 // 0.005 //0.1 // 0.05
        hill = BasicSignaling.Hill_response_function( signal, half, 1.5 );
        phenotype.motility.migrationBias = base + ( max - base ) * hill;

        // migration speed slows down in the presence of debris or quorum factor 
        base = pCD.phenotype.motility.migrationSpeed;
        max = 0.1 * base;
        signal = biasDebrisSensitivity * debris + biasQuorumSensitivity * q; // + 10 * PIF; 
        half = pCD.custom_data.get( "migration_speed_halfmax" ); // 0.1 // 0.05 
        hill = BasicSignaling.Hill_response_function( signal, half, 1.5 );
        phenotype.motility.migrationSpeed = base + ( max - base ) * hill;
    }

    @Override
    public String display()
    {
        return "Contact with dead cells, bacteria or debris increases secretion of pro-inflammatory. "
                + "Debris and quorum increase chemotaxis bias. " + "Migration speed slows down in the presence of debris or quorum factor.";
    }
}