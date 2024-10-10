import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.CustomCellRule;
import ru.biosoft.physicell.core.Phenotype;

/*
 * Cell Rule to avoid microenvironment boundaries
 */
public class AvoidBoundariesRule extends CustomCellRule
{
    @Override
    public void execute(Cell pCell, Phenotype phenotype, double dt)
    {
        avoidBoundaries( pCell );
    }

    public static void avoidBoundaries(Cell pCell)
    {
        Microenvironment m = pCell.getMicroenvironment();
        double xMin = m.mesh.boundingBox[0];
        double yMin = m.mesh.boundingBox[1];
        double zMin = m.mesh.boundingBox[2];
        double xMax = m.mesh.boundingBox[3];
        double yMax = m.mesh.boundingBox[4];
        double zMax = m.mesh.boundingBox[5];

        double avoiZone = 25;
        double avoidSpeed = -0.5; // must be negative 

        boolean nearEdge = false;
        if( pCell.position[0] < xMin + avoiZone || pCell.position[0] > xMax - avoiZone )
        {
            nearEdge = true;
        }

        if( pCell.position[1] < yMin + avoiZone || pCell.position[1] > yMax - avoiZone )
        {
            nearEdge = true;
        }

        if( !m.options.simulate2D )
        {
            if( pCell.position[2] < zMin + avoiZone || pCell.position[2] > zMax - avoiZone )
            {
                nearEdge = true;
            }
        }

        if( nearEdge )
        {
            pCell.velocity = VectorUtil.newProd( pCell.position, avoidSpeed );
        }
    }

    @Override
    public String display()
    {
        return "Avoid microenvironment boundaries";
    }
}