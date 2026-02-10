import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.biofvm.Microenvironment;

public class Initial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
          Microenvironment m = model.getMicroenvironment();
          int nVirus = m.findDensityIndex( "virus" );
          PhysiCellUtilities.place2D( model, "epithelial cell", model.getParameterInt( "number_of_infected_cells" ) );
          for( Cell cell : m.getAgents( Cell.class ) )
              cell.phenotype.molecular.internSubstrates[nVirus] = 1;

          PhysiCellUtilities.place2D( model, "epithelial cell", model.getParameterInt( "number_of_uninfected_cells" ) );
          PhysiCellUtilities.place2D( model, "macrophage", model.getParameterInt( "number_of_macrophages" ) );
      }
}