import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.PhysiCellConstants;
import ru.biosoft.physicell.core.standard.O2based;
import ru.biosoft.physicell.core.standard.StandardModels;

/**
 * Custom cell phenotype function to scale immunostimulatory factor with hypoxia 
 * Secretion of immunostimulatory factor is constant for now
 * Cell division is stimulated by oncoprotein
 */
public class TumorPhenotype extends O2based
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt) throws Exception
    {
        super.execute( pCell, phenotype, dt );

        Microenvironment m = pCell.getMicroenvironment();
        int cycle_start_index = StandardModels.live.findPhaseIndex( PhysiCellConstants.live );
        int cycle_end_index = StandardModels.live.findPhaseIndex( PhysiCellConstants.live );
        int oncoprotein_i = pCell.customData.findVariableIndex( "oncoprotein" );

        // update secretion rates based on hypoxia 
        int immune_factor_index = m.findDensityIndex( "immunostimulatory factor" );
        phenotype.secretion.secretionRates[immune_factor_index] = 10.0;

        // if cell is dead, don't bother with future phenotype changes, set it to secrete the immunostimulatory factor 
        if( phenotype.death.dead == true )
        {
            phenotype.secretion.secretionRates[immune_factor_index] = 10;//TODO?
            pCell.functions.updatePhenotype = null;
            return;
        }

        // multiply proliferation rate by the oncoprotein 
        double rate = phenotype.cycle.data.getTransitionRate( cycle_start_index, cycle_end_index );
        double factor = pCell.customData.get( oncoprotein_i );
        //        System.out.println( pCell.toString() + " " + rate + " " + factor + " " + rate * factor );
        phenotype.cycle.data.setTransitionRate( cycle_start_index, cycle_end_index, rate * factor );
        //            phenotype.cycle.data.modifyTransitionRate( cycle_start_index, cycle_end_index, pCell.custom_data.get( oncoprotein_i ) );
    }

    @Override
    public String display()
    {
        return "O2 based phenotype; Secretes immunostimulatory factor; cell division is stimulated by oncoprotein";
    }
}