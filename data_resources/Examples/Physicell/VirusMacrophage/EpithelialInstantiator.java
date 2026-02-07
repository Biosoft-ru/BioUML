import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.Instantiator;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.biofvm.Microenvironment;

public class EpithelialInstantiator extends Instantiator
{
    @Override
    public Cell execute(CellDefinition cd, Model model) throws Exception
    {
        Cell cell = new Cell( cd, model );
        Microenvironment m = model.getMicroenvironment();
        int virus_index = m.findDensityIndex( "virus" );
        cell.phenotype.molecular.fractionReleasedDeath[virus_index] = model.getParameterDouble( "fraction_released_at_death" );
        cell.phenotype.molecular.fractionTransferredIngested[virus_index] = model.getParameterDouble( "fraction_transferred_when_ingested" );       
        return cell;
    }

}