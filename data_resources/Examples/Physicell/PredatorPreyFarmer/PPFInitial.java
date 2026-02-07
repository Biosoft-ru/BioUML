import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;

public class PPFInitial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
	  PhysiCellUtilities.place( model, "farmer", model.getParameterInt( "number_of_farmers" ) );
          PhysiCellUtilities.place( model, "prey", model.getParameterInt( "number_of_prey" ) );
          PhysiCellUtilities.place( model, "predator", model.getParameterInt( "number_of_predators" ) );
      }
}