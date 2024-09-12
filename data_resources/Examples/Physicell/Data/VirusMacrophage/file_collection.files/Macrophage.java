import java.util.ArrayList;
import java.util.List;

import ru.biosoft.physicell.biofvm.CartesianMesh;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellContainer;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellFunctions.UpdatePhenotype;
import ru.biosoft.physicell.core.Phenotype;

public class Macrophage extends UpdatePhenotype
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        Microenvironment microenvironment = pCell.getMicroenvironment();
        int nVirus = microenvironment.findDensityIndex( "virus" );

        CellDefinition pMacrophage = pCell.getModel().getCellDefinition( "macrophage" );

        // digest virus particles inside me 
        double implicitEulerConstant = ( 1.0 + dt * pCell.customData.get( "virus_digestion_rate" ) );
        phenotype.molecular.internSubstrates[nVirus] /= implicitEulerConstant;

        // check for contact with a cell
        List<Cell> neighbors = get_possible_neighbors( pCell );
        for( Cell neighbor : neighbors )
        {
            if( neighbor != pCell && neighbor.type != pMacrophage.type )
            {
                double dist = VectorUtil.dist( neighbor.position, pCell.position );
                double maxDistance = 1.1 * ( pCell.phenotype.geometry.radius + neighbor.phenotype.geometry.radius );

                // if it is not a macrophage, test for viral load if high viral load, eat it. 
                if( neighbor.phenotype.molecular.internSubstrates[nVirus] > pCell.customData.get( "min_virion_detection_threshold" )
                        && dist < maxDistance )
                {
                    pCell.ingestCell( neighbor );
                }
            }
        }
    }

    public String display()
    {
        return "Ingests infected cells.";
    }

    public List<Cell> get_possible_neighbors(Cell pCell)
    {
        int mechanicsVoxelIndex = pCell.get_current_mechanics_voxel_index();
        CellContainer container = pCell.get_container();
        CartesianMesh mesh = container.mesh;
        List<Cell> neighbors = new ArrayList<>( container.agentGrid.get( mechanicsVoxelIndex ) );

        for( int neighborVoxel : mesh.moore_connected_voxel_indices[mechanicsVoxelIndex] )
        {
            if( !Cell.isNeighborVoxel( pCell,
                    mesh.voxels[mechanicsVoxelIndex].center, mesh.voxels[neighborVoxel].center, neighborVoxel ) )
                continue;
            neighbors.addAll( container.agentGrid.get( neighborVoxel ) );
        }
        return neighbors;
    }
}