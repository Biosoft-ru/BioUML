import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.RandomGenerator;

public class Initial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
        Microenvironment m = model.getMicroenvironment();
        double xMin = m.mesh.boundingBox[0];
        double yMin = m.mesh.boundingBox[1];
        double zMin = m.mesh.boundingBox[2];
        double xMax = m.mesh.boundingBox[3];
        double yMax = m.mesh.boundingBox[4];
        double zMax = m.mesh.boundingBox[5];

        double maxRadius = model.getParameterDouble( "max_distance_from_origin" );
        xMax = Math.min( xMax, maxRadius );
        xMin = Math.max( xMin, -maxRadius );
        yMax = Math.min( yMax, maxRadius );
        yMin = Math.max( yMin, -maxRadius );
        zMax = Math.min( zMax, maxRadius );
        zMin = Math.max( zMin, -maxRadius );

        if( m.options.simulate2D )
        {
            zMin = 0.0;
            zMax = 0.0;
        }

        double[] range = new double[] {xMin, yMin, zMin, xMax, yMax, zMin, zMax};

        CellDefinition aCD = model.getCellDefinition( "A" );
        CellDefinition bCD = model.getCellDefinition( "B" );
        CellDefinition cCD = model.getCellDefinition( "C" );

        int number = model.getParameterInt( "number_of_A" );
        placeInRadius( aCD, model, number, range, maxRadius );

        number = model.getParameterInt( "number_of_B" );
        placeInRadius( bCD, model, number, range, maxRadius );

        number = model.getParameterInt( "number_of_C" );
        placeInRadius( cCD, model, number, range, maxRadius );

        for( Cell cell : m.getAgents( Cell.class ) )
        {
            for( int k = 0; k < cell.phenotype.death.rates.size(); k++ )
            {
                cell.phenotype.death.rates.set( k, 0.0 );
            }
        }
    }

    /**
     * Places cells in given range but not exceeding given maxRadius 
     * @param cd - CellDefinition for cells
     * @param m - microenvironment
     * @param number - number of cells to place
     * @param range - bounding box: xMin, yMin, zMin, xMax, yMax, zMax
     * @param maxRadius - maximum radius
     */
    private static void placeInRadius(CellDefinition cd, Model m, int number, double[] range, double maxRadius) throws Exception
    {
        RandomGenerator rng = m.getRNG();
        for( int n = 0; n < number; n++ )
        {
            double[] position = {0, 0, 0};
            double r = maxRadius + 1;
            while( r > maxRadius )
            {
                position[0] = rng.UniformRandom( range[0], range[3] );
                position[1] = rng.UniformRandom( range[1], range[4] );
                position[2] = rng.UniformRandom( range[2], range[5] );
                r = VectorUtil.norm( position );
            }
            Cell.createCell( cd, m, position );
        }
    }
}