import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.Instantiator;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.core.Model;

public class ODEInstantiator extends Instantiator
{

    @Override
    public Cell execute(CellDefinition cd, Model model) throws Exception
    {
        int oxygenIndex = model.getMicroenvironment().findDensityIndex( "oxygen" );
        int glucoseIndex = model.getMicroenvironment().findDensityIndex( "glucose" );
        int lactateIndex = model.getMicroenvironment().findDensityIndex( "lactate" );

        Cell cell = new Cell( cd, model );
        SignalBehavior signals = model.getSignals();
        signals.setSingleBehavior( cell, "custom:intra_oxy", model.getParameterDouble( "initial_internal_oxygen" ) );
        signals.setSingleBehavior( cell, "custom:intra_glu", model.getParameterDouble( "initial_internal_glucose" ) );
        signals.setSingleBehavior( cell, "custom:intra_lac", model.getParameterDouble( "initial_internal_lactate" ) );
        signals.setSingleBehavior( cell, "custom:intra_energy", model.getParameterDouble( "initial_energy" ) );
        double cellVolume = cell.phenotype.volume.total;
        double[] substrates = cell.phenotype.molecular.internSubstrates;
        substrates[oxygenIndex] = signals.getSingleSignal( cell, "custom:intra_oxy" ) * cellVolume;
        substrates[glucoseIndex] = signals.getSingleSignal( cell, "custom:intra_glu" ) * cellVolume;
        substrates[lactateIndex] = signals.getSingleSignal( cell, "custom:intra_lac" ) * cellVolume;
        cell.phenotype.intracellular.start();
        cell.phenotype.intracellular.setParameterValue( "$Intracellular.Energy", signals.getSingleSignal( cell, "custom:intra_energy" ) );
        return cell;
    }

}