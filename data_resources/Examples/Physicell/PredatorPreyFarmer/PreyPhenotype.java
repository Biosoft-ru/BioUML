import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Phenotype;

/**
 * Simple phenotype - death and division are based on food density
 */
public class PreyPhenotype extends UpdatePhenotype
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        Microenvironment microenvironment = pCell.getMicroenvironment();
        int nFood = microenvironment.findDensityIndex( "food" );
        double food = pCell.nearest_density_vector()[nFood];

        // death based on food
        int nNecrosis = phenotype.death.findDeathModelIndex( "necrosis" );
        if( food < 0.1 )
        {
            pCell.startDeath( nNecrosis );
            pCell.functions.updatePhenotype = null;
            return;
        }

        // division based on food
        CellDefinition pCD = pCell.getModel().getCellDefinition( "prey" );
        double multiplier = ( food - 0.1 ) / 0.9;
        phenotype.cycle.data.setExitRate( 0, pCD.phenotype.cycle.data.getExitRate( 0 ) * multiplier );
    }

    public String display()
    {
        return "Death and division are based on food density";
    }
}