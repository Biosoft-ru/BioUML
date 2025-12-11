import ru.biosoft.physicell.core.ReportGenerator;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.SignalBehavior;

public class Report extends ReportGenerator
{
    public String[] getReportHeaderElements()
    {
        return new String[] {"ID", "State", "Lactate", "Glucose", "Oxygen", "Energy"};
    }

    public Object[] getReportElements(Cell cell) throws Exception
    {
        Model model = cell.getModel();
        SignalBehavior signals = model.getSignals();
        return new Object[] {cell.ID, cell.phenotype.cycle.code, signals.getSingleSignal( cell, "intracellular lactate" ), 
                                      signals.getSingleSignal( cell, "intracellular glucose" ),
                                      signals.getSingleSignal( cell, "intracellular oxygen" ),
                                      cell.phenotype.intracellular.getParameterValue( "$Intracellular.Energy" )};
    }
}