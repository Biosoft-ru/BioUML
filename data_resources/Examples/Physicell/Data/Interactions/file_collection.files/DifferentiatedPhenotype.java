import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.BasicSignaling;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.PhysiCellConstants;

public class DifferentiatedPhenotype extends UpdatePhenotype
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        CellDefinition pCD = pCell.getModel().getCellDefinition( pCell.typeName );
        Microenvironment m = pCell.getMicroenvironment();
        int nR = m.findDensityIndex( "resource" );
        int nTox = m.findDensityIndex( "toxin" );
        int nDebris = m.findDensityIndex( "debris" );

        if( phenotype.death.dead )
        {
            phenotype.secretion.netExportRates[nDebris] = phenotype.volume.total;
            pCell.functions.updatePhenotype = null;
            return;
        }

        double[] samples = pCell.nearest_density_vector();
        double r = samples[nR];
        double toxin = samples[nTox];
        double signal = 0.0;
        double hill = 0.0;

        // pressure reduces proliferation 
        signal = pCell.state.simplePressure;
        double pressure_halfmax = pCD.custom_data.get( "cycling_pressure_halfmax" ); // 0.5 
        hill = BasicSignaling.Hill_response_function( signal, pressure_halfmax, 1.5 );
        double base_val = pCD.phenotype.cycle.data.getExitRate( 0 );

        phenotype.cycle.data.setExitRate( 0, ( 1 - hill ) * base_val );

        // resource reduces necrotic death 
        double max = 0.0028;
        int nNecrosis = phenotype.death.findDeathModelIndex( PhysiCellConstants.necrosis_death_model );

        // get same from bacteria
        double necrosisSaturation = pCD.custom_data.get( "necrosis_saturation_resource" ); // 0.075 
        double necrosisThreshold = pCD.custom_data.get( "necrosis_threshold_resource" ); // 0.15 

        phenotype.death.rates.set( nNecrosis,
                max * BasicSignaling.decreasing_linear_response_function( r, necrosisSaturation, necrosisThreshold ) );

        // toxin increases apoptotic death 
        int nApoptosis = phenotype.death.findDeathModelIndex( PhysiCellConstants.apoptosis_death_model );

        double toxicityHalf = pCD.custom_data.get( "toxicity_halfmax" ); // 0.2 
        double relativeMaxToxDeath = pCD.custom_data.get( "relative_max_toxicity" ); // 100 

        signal = toxin;
        base_val = pCD.phenotype.death.rates.get( nApoptosis );
        double maxResponse = base_val * relativeMaxToxDeath;
        hill = BasicSignaling.Hill_response_function( signal, toxicityHalf, 1.5 );
        // System.out.println( "tox: " + signal + " " + hill); 
        phenotype.death.rates.set( nApoptosis, base_val + ( maxResponse - base_val ) * hill );
    }

    @Override
    public String display()
    {
        return "Resource reduces necrosis. " + "Toxin increases apoptosis.";
    }
}