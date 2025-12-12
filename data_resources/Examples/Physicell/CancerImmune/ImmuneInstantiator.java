import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.Instantiator;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.SignalBehavior;
import ru.biosoft.physicell.biofvm.Microenvironment;

public class ImmuneInstantiator extends Instantiator
{
    @Override
    public Cell execute(CellDefinition cd, Model model) throws Exception
    {
        Cell cell = new Cell( cd, model );
        CellDefinition cancerCellCD = model.getCellDefinition( "cancer cell" );
        int oxygen_ID = model.getMicroenvironment().findDensityIndex( "oxygen" );
        cell.phenotype.secretion.uptakeRates[oxygen_ID] *= model.getParameterDouble( "immune_o2_relative_uptake" );
        cell.phenotype.mechanics.cellCellAdhesionStrength *= model.getParameterDouble( "immune_relative_adhesion" );
        cell.phenotype.mechanics.cellCellRepulsionStrength *= model.getParameterDouble( "immune_relative_repulsion" );
        cell.phenotype.mechanics.relMaxAttachmentDistance = cancerCellCD.custom_data.get( "max_attachment_distance" ) / cd.phenotype.geometry.radius;
        cell.phenotype.mechanics.attachmentElasticConstant = cancerCellCD.custom_data.get( "elastic_coefficient" );
        cell.phenotype.mechanics.relDetachmentDistance = cancerCellCD.custom_data.get( "max_attachment_distance" ) / cd.phenotype.geometry.radius;
        return cell;
    }

}