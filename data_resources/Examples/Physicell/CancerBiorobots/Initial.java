import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.biofvm.VectorUtil;
import ru.biosoft.physicell.core.standard.StandardModels;

public class Initial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
        CellDefinition defaults = StandardModels.getDefaultCellDefinition();
        double cellRadius = defaults.phenotype.geometry.radius;
        double cellSpacing = 0.95 * 2.0 * cellRadius;
        double tumorRadius = model.getParameterDouble( "tumor_radius" ); // 200.0;

        CellDefinition cd = model.getCellDefinition( "cancer cell" );
        double x = 0.0;
        double xOuter = tumorRadius;
        double y = 0.0;

        int n = 0;
        while( y < tumorRadius )
        {
            x = 0.0;
            if( n % 2 == 1 )
            {
                x = 0.5 * cellSpacing;
            }
            xOuter = Math.sqrt( tumorRadius * tumorRadius - y * y );

            while( x < xOuter )
            {
                Cell.createCell( cd, model, new double[] {x, y, 0.0} ); // tumor cell

                if( Math.abs( y ) > 0.01 )
                    Cell.createCell( cd, model, new double[] {x, -y, 0.0} ); // tumor cell			

                if( Math.abs( x ) > 0.01 )
                {
                    Cell.createCell( cd, model, new double[] { -x, y, 0.0} );

                    if( Math.abs( y ) > 0.01 )
                        Cell.createCell( cd, model, new double[] { -x, -y, 0.0} );
                }
                x += cellSpacing;
            }

            y += cellSpacing * Math.sqrt( 3.0 ) / 2.0;
            n++;
        }
    } 
}