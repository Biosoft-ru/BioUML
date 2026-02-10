import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.BasicSignaling;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.CustomCellData;
import ru.biosoft.physicell.core.Phenotype;
import ru.biosoft.physicell.core.PhysiCellConstants;

public class BacterialPhenotype extends UpdatePhenotype
{
    @Override
    public void execute(Cell cell, Phenotype phenotype, double dt)
    {
        CellDefinition cd = cell.getModel().getCellDefinition( cell.typeName );
        CustomCellData data = cd.custom_data;
        Microenvironment m = cell.getMicroenvironment();
        int nR = m.findDensityIndex( "resource" );
        int nDebris = m.findDensityIndex( "debris" );
        int nQuorum = m.findDensityIndex( "quorum" );
        int nToxin = m.findDensityIndex( "toxin" );

        // if dead: stop exporting quorum factor also, replace phenotype function 
        if( phenotype.death.dead == true )
        {
            phenotype.secretion.netExportRates[nQuorum] = 0;
            phenotype.secretion.netExportRates[nToxin] = 0;
            phenotype.secretion.netExportRates[nDebris] = phenotype.volume.total;
            cell.functions.updatePhenotype = null;
            return;
        }

        double[] samples = cell.nearest_density_vector();
        double r = samples[nR];
        double q = samples[nQuorum];

        // resource increases cycle entry 
        double base = cd.phenotype.cycle.data.getExitRate( 0 );
        double max = base * 10.0;
        double minCycleResource = data.get( "cycling_entry_threshold_resource" ); // 0.15 
        phenotype.cycle.data.setExitRate( 0, max * BasicSignaling.linear_response_function( r, minCycleResource, 1 ) );

        // resource decreses necrosis
        max = 0.0028;
        int nNecrosis = phenotype.death.findDeathModelIndex( PhysiCellConstants.necrosis_death_model );
        double saturationNecrosisResource = data.get( "necrosis_saturation_resource" ); //0.075
        double thresholdNecrosisResource = data.get( "necrosis_threshold_resource" ); // 0.15
        phenotype.death.rates.set( nNecrosis,
                max * BasicSignaling.decreasing_linear_response_function( r, saturationNecrosisResource, thresholdNecrosisResource ) );

        // resource decreases motile speed  
        double signal = r;
        base = cd.phenotype.motility.migrationSpeed;
        double maxResponse = 0.0;
        double motilityResourceHalfmax = data.get( "migration_speed_halfmax" ); // 0.25
        double hill = BasicSignaling.Hill_response_function( signal, motilityResourceHalfmax, 1.5 );
        phenotype.motility.migrationSpeed = base + ( maxResponse - base ) * hill;

        // quorum and resource increases motility bias 
        signal = q + r;
        base = cd.phenotype.motility.migrationSpeed;
        maxResponse = 1.0;
        double biasHalfmax = data.get( "migration_bias_halfmax" );
        // 0.5 //  parameters.doubles("bacteria_migration_bias_halfmax");
        hill = BasicSignaling.Hill_response_function( signal, biasHalfmax, 1.5 );
        phenotype.motility.migrationBias = base + ( maxResponse - base ) * hill;

        // damage increases death 
        int nApoptosis = phenotype.death.findDeathModelIndex( PhysiCellConstants.apoptosis_death_model );

        signal = cell.phenotype.cellIntegrity.damage;
        base = cd.phenotype.death.rates.get( nApoptosis );

        double damageHalfmax = data.get( "damage_halfmax" );
        double relativeMaxDamageDeath = data.get( "relative_max_damage_death" );
        maxResponse = base * relativeMaxDamageDeath;

        // 36 // parameters.doubles("bacteria_damage_halfmax");
        hill = BasicSignaling.Hill_response_function( signal, damageHalfmax, 1.5 );
        phenotype.death.rates.set( nApoptosis, base + ( maxResponse - base ) * hill );
    }

    @Override
    public String display()
    {
        return "Resource decrease necrosis and motility, also stimulate division." + " Resource AND quorum increase motility."
                + " Damage stimulates apoptosis";
    }
}