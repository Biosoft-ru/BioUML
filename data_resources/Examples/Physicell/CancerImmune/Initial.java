import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.standard.StandardModels;
import java.util.List;
import java.util.ArrayList;

public class Initial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
        CellDefinition cd = model.getCellDefinition( "cancer cell" );
        double cellRadius = cd.phenotype.geometry.radius;
        //        double cell_spacing = 0.95 * 2.0 * cell_radius;

        double tumorRadius = model.getParameterDouble( "tumor_radius" );// 250.0;  

        List<double[]> positions = createSpherePositions( new double[] {0,0,0}, cellRadius, tumorRadius );
        //        System.out.println( "creating " + positions.size() + " closely-packed tumor cells ... " );

        double imm_mean = model.getParameterDouble( "tumor_mean_immunogenicity" );
        double imm_sd = model.getParameterDouble( "tumor_immunogenicity_standard_deviation" );

        for( double[] position : positions )
        {
            Cell pCell = Cell.createCell( cd, model, position ); // tumor cell 
            double oncoprotein = Math.max( 0, model.getRNG().NormalRandom( imm_mean, imm_sd ) );
            pCell.customData.set( "oncoprotein", oncoprotein );
        }
    } 

    private List<double[]> createSpherePositions(double[] center, double cellRadius, double sphereRadius)
    {
        List<double[]> cells = new ArrayList<>();
        int xc = 0, zc = 0;
        double xSpacing = cellRadius * Math.sqrt( 3 );
        double ySpacing = cellRadius * 2;
        double zSpacing = cellRadius * Math.sqrt( 3 );

            for( double z = -sphereRadius; z < sphereRadius; z += zSpacing, zc++ )
            {
                for( double x = -sphereRadius; x < sphereRadius; x += xSpacing, xc++ )
                {
                    for( double y = -sphereRadius; y < sphereRadius; y += ySpacing )
                    {
                        double[] tempPoint = new double[3];
                        tempPoint[0] = x + ( zc % 2 ) * 0.5 * cellRadius + center[0];
                        tempPoint[1] = y + ( xc % 2 ) * cellRadius + center[1];
                        tempPoint[2] = z + center[2];

                        if( VectorUtil.dist( tempPoint, center ) < sphereRadius )
                        {
                            cells.add( tempPoint );
                        }
                    }
                }
            }
        return cells;
    }
}