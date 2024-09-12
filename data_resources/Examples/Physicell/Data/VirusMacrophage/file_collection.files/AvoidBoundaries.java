import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellFunctions.CustomCellRule;
import ru.biosoft.physicell.core.Phenotype;

/**
 *Adds velocity to steer clear of the boundaries 
 */
public class AvoidBoundaries extends CustomCellRule
{
    static double avoidZone = 25;
    static double avoidSpeed = -0.5; // must be negative 

    @Override
    public void execute(Cell cell, Phenotype phenotype, double dt)
    {
        if( nearEdge( cell ) )
            cell.velocity = VectorUtil.newProd( cell.position, avoidSpeed );
    }

    public static boolean nearEdge(Cell cell)
    {
        Microenvironment m = cell.getMicroenvironment();
        double[] position = cell.position;
        double Xmin = m.mesh.boundingBox[0];
        double Ymin = m.mesh.boundingBox[1];
        double Zmin = m.mesh.boundingBox[2];
        double Xmax = m.mesh.boundingBox[3];
        double Ymax = m.mesh.boundingBox[4];
        double Zmax = m.mesh.boundingBox[5];
        if( position[0] < Xmin + avoidZone || position[0] > Xmax - avoidZone )
            return true;
        if( position[1] < Ymin + avoidZone || position[1] > Ymax - avoidZone )
            return true;
        if( !m.options.simulate2D && ( position[2] < Zmin + avoidZone || position[2] > Zmax - avoidZone ) )
            return true;
        return false;
    }
}