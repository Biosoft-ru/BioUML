import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.ui.Visualizer;
import ru.biosoft.physicell.core.InitialCellsArranger;

public class Initial extends InitialCellsArranger
{      
      private static final String CUSTOM_ONCOPROTEIN = "custom:oncoprotein";

      @Override
      public void arrange(Model model) throws Exception
      {
        CellDefinition pCD = model.getCellDefinition( "cancer cell" );
        double cellRadius = pCD.phenotype.geometry.radius;
        pCD.parameters.o2_proliferation_saturation = 38;
        pCD.parameters.o2_reference = 38;
        double cellSpacing = 0.95 * 2.0 * cellRadius;
        double tumorRadius = model.getParameterDouble( "tumor_radius" ); // 250.0; 
        double x = 0.0;
        double xOuter = tumorRadius;
        double y = 0.0;
        double pMean = model.getParameterDouble( "oncoprotein_mean" );
        double pSD = model.getParameterDouble( "oncoprotein_sd" );
        double pMin = model.getParameterDouble( "oncoprotein_min" );
        double pMax = model.getParameterDouble( "oncoprotein_max" );
        Cell cell;
        int n = 0;
        while( y < tumorRadius )
        {
            x = 0.0;
            if( n % 2 == 1 )
                x = 0.5 * cellSpacing;
            xOuter = Math.sqrt( tumorRadius * tumorRadius - y * y );

            while( x < xOuter )
            {
                cell = Cell.createCell( pCD, model, new double[] {x, y, 0.0} ); // tumor cell 
                double p = model.getRNG().NormalRestricted( pMean, pSD, pMin, pMax );
                model.signals.setSingleBehavior( cell, CUSTOM_ONCOPROTEIN, p );

                if( Math.abs( y ) > 0.01 )
                {
                    cell = Cell.createCell( pCD, model, new double[] {x, -y, 0.0} ); // tumor cell 
                    p = model.getRNG().NormalRestricted( pMean, pSD, pMin, pMax );
                    model.signals.setSingleBehavior( cell, CUSTOM_ONCOPROTEIN, p );
                }
                if( Math.abs( x ) > 0.01 )
                {
                    cell = Cell.createCell( pCD, model, new double[] { -x, y, 0} ); // tumor cell 
                    p = model.getRNG().NormalRestricted( pMean, pSD, pMin, pMax );
                    model.signals.setSingleBehavior( cell, CUSTOM_ONCOPROTEIN, p );

                    if( Math.abs( y ) > 0.01 )
                    {
                        cell = Cell.createCell( pCD, model, new double[] { -x, -y, 0} ); // tumor cell
                        p = model.getRNG().NormalRestricted( pMean, pSD, pMin, pMax );
                        model.signals.setSingleBehavior( cell, CUSTOM_ONCOPROTEIN, p );
                    }
                }
                x += cellSpacing;
            }
            y += cellSpacing * Math.sqrt( 3.0 ) / 2.0;
            n++;
        }
    }
}