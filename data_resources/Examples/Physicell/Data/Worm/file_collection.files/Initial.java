import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.Cell;

public class Initial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
          PhysiCellUtilities.place( model, "worm", model.getParameterInt( "number_of_cells" ) );

          for( Cell cell : model.getMicroenvironment().getAgents( Cell.class ) )
          {
              cell.customData.set( "head", model.getRNG().UniformRandom() );
              cell.customData.set( "head_initial", cell.customData.get( "head" ) );
              cell.phenotype.mechanics.attachmentElasticConstant = model.getParameterDouble( "attachment_elastic_constant" );
          }
      }
}