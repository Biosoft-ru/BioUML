import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.Instantiator;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.biofvm.Microenvironment;

public class MacrophageInstantiator extends Instantiator
{

    @Override
    public Cell execute(CellDefinition cd, Model model) throws Exception
    {
        Cell cell = new Cell( cd, model );
        Microenvironment m = model.getMicroenvironment();
        int virus_index = m.findDensityIndex( "virus" );
        cell.phenotype.mechanics.cellCellAdhesionStrength *= model.getParameterDouble( "macrophage_relative_adhesion" );
        cell.phenotype.molecular.fractionReleasedDeath[virus_index] = 0.0;
        cell.phenotype.molecular.fractionTransferredIngested[virus_index] = 0.0; 
        return cell;
    }

}