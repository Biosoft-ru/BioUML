import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.BasicSignaling;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.PhysiCellConstants;

public class StemPhenotype extends UpdatePhenotype
{
    private boolean isInit = false;
    double maxStemDiff;

    private void init(Model model)
    {
        maxStemDiff = model.getParameterDouble( "max_stem_differentiation" );
        isInit = true;
    }

    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        if ( !isInit )
            init(pCell.getModel());

        CellDefinition pCD = pCell.getModel().getCellDefinition( pCell.typeName );
        Microenvironment m = pCell.getMicroenvironment();

        int nR = m.findDensityIndex( "resource" );
        int nTox = m.findDensityIndex( "toxin" );
        int nDebris = m.findDensityIndex( "debris" );

        // if dead, release debris
        if( phenotype.death.dead == true )
        {
            phenotype.secretion.netExportRates[nDebris] = phenotype.volume.total;
            pCell.functions.updatePhenotype = null;
            return;
        }

        double[] samples = pCell.nearest_density_vector();
        double r = samples[nR];
        double toxin = samples[nTox];

        int stemType = pCell.getModel().getCellDefinition( "stem" ).type;
        int diffType = pCell.getModel().getCellDefinition( "differentiated" ).type;
        int numStem = 0;
        int numDifferentiated = 0;
        for( Cell pC : pCell.state.neighbors )
        {
            if( pC.type == stemType )
                numStem++;
            else if( pC.type == numDifferentiated )
                numDifferentiated++;
        }

        // contact with a stem cell increases differentiation 
        //        double max_stem_diff = parameters.doubles( "max_stem_differentiation" ); // 0.0075 
        double stemDiffHalfmax = pCD.custom_data.get( "differentiation_contact_halfmax" ); // 0.1 
        double base = 0; // phenotype.cell_transformations.transformation_rates[diff_type]; 
        double max = maxStemDiff; // 0.0075;
        double signal = numStem;
        double halfMax = stemDiffHalfmax; // 0.1; 
        double hill = BasicSignaling.Hill_response_function( signal, halfMax, 1.5 );
        phenotype.cellTransformations.transformationRates[diffType] = base + ( max - base ) * hill;

        // contact with a differentiated cell reduces proliferation 
        // high rate of proliferation unless in contact with a differentiated cell 
        double stemCyclingHalfmax = pCD.custom_data.get( "cycling_contact_halfmax" ); // 0.1; 
        base = pCD.phenotype.cycle.data.getExitRate( 0 ); // 0.002; 
        max = 0.0;
        signal = numDifferentiated;
        halfMax = stemCyclingHalfmax; //  0.1; 
        hill = BasicSignaling.Hill_response_function( signal, halfMax, 1.5 );
        phenotype.cycle.data.setExitRate( 0, base + ( max - base ) * hill );

        // resource reduces necrotic death 
        max = 0.0028;
        int nNecrosis = phenotype.death.findDeathModelIndex( PhysiCellConstants.necrosis_death_model );
        double stemSaturationNecrosis = pCD.custom_data.get( "necrosis_saturation_resource" );
        double stemThresholdNecrosis = pCD.custom_data.get( "necrosis_threshold_resource" );
        phenotype.death.rates.set( nNecrosis,
                max * BasicSignaling.decreasing_linear_response_function( r, stemSaturationNecrosis, stemThresholdNecrosis ) );

        // toxin increases apoptotic death 
        int nApoptosis = phenotype.death.findDeathModelIndex( PhysiCellConstants.apoptosis_death_model );
        double toxicityHalfmax = pCD.custom_data.get( "toxicity_halfmax" ); // 0.4 
        double relativeMaxToxicity = pCD.custom_data.get( "relative_max_toxicity" );
        signal = toxin;
        base = pCD.phenotype.death.rates.get( nApoptosis );
        max = base * relativeMaxToxicity; // 100*base_val;
        hill = BasicSignaling.Hill_response_function( signal, toxicityHalfmax, 1.5 );
        phenotype.death.rates.set( nApoptosis, base + ( max - base ) * hill );
    }

    @Override
    public String display()
    {
        return "Contact with a stem cell increases differentiation." + " Contact with a differentiated cell reduces proliferation."
                + " Toxin increases apoptosis.";
    }
}