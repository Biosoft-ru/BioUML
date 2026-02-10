import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Phenotype;

public class Epithelial extends UpdatePhenotype
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        // bookkeeping
        Microenvironment microenvironment = pCell.getMicroenvironment();
        int nVirus = microenvironment.findDensityIndex( "virus" );
        int nInterferon = microenvironment.findDensityIndex( "interferon" );

        // compare against viral load. Should I commit apoptosis? 
        double virus = phenotype.molecular.internSubstrates[nVirus];
        if( virus >= pCell.customData.get( "burst_virion_count" ) )
        {
            pCell.lyseCell(); // start_death( apoptosis_model_index );
            pCell.functions.updatePhenotype = null;
            return;
        }

        // replicate virus particles inside me 
        if( virus >= pCell.customData.get( "min_virion_count" ) )
        {
            double new_virus = pCell.customData.get( "viral_replication_rate" ) * dt;
            phenotype.molecular.internSubstrates[nVirus] += new_virus;
        }

        if( virus >= pCell.customData.get( "virion_threshold_for_interferon" ) )
        {
            phenotype.secretion.secretionRates[nInterferon] = pCell.customData.get( "max_interferon_secretion_rate" );
        }
    }
}