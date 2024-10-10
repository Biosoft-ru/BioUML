import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.PhysiCellUtilities;
import ru.biosoft.physicell.core.InitialCellsArranger;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.VectorUtil;
import java.util.List;
import java.util.ArrayList;

public class Initial extends InitialCellsArranger
{      
      @Override
      public void arrange(Model model) throws Exception
      {
        Microenvironment m = model.getMicroenvironment();
        int oxygenIndex = m.findDensityIndex( "oxygen" );
        int glucoseIndex = m.findDensityIndex( "glucose" );
        int lactateIndex = m.findDensityIndex( "lactate" );

        CellDefinition cd = model.getCellDefinition( "default" );
        double cellRadius = cd.phenotype.geometry.radius;
        double initialTumorRadius = 100;

        List<double[]> positions = createCirclePositions( cellRadius, initialTumorRadius );
        for( int i = 0; i < positions.size(); i++ )
        {
            Cell pCell = Cell.createCell( cd, model, positions.get( i ) );
            model.getSignals().setSingleBehavior( pCell, "custom:intra_oxy", model.getParameterDouble( "initial_internal_oxygen" ) );
            model.getSignals().setSingleBehavior( pCell, "custom:intra_glu", model.getParameterDouble( "initial_internal_glucose" ) );
            model.getSignals().setSingleBehavior( pCell, "custom:intra_lac", model.getParameterDouble( "initial_internal_lactate" ) );
            model.getSignals().setSingleBehavior( pCell, "custom:intra_energy", model.getParameterDouble( "initial_energy" ) );
            double cellVolume = pCell.phenotype.volume.total;
            double[] substrates = pCell.phenotype.molecular.internSubstrates;
            substrates[oxygenIndex] = model.getSignals().getSingleSignal( pCell, "custom:intra_oxy" ) * cellVolume;
            substrates[glucoseIndex] = model.getSignals().getSingleSignal( pCell, "custom:intra_glu" ) * cellVolume;
            substrates[lactateIndex] = model.getSignals().getSingleSignal( pCell, "custom:intra_lac" ) * cellVolume;
            pCell.phenotype.intracellular.start();
            pCell.phenotype.intracellular.setParameterValue( "$Intracellular.Energy", model.getSignals().getSingleSignal( pCell, "custom:intra_energy" ) );
        }
    }

    public static List<double[]> createCirclePositions(double cellRadius, double sphereRadius)
    {
        List<double[]> result = new ArrayList<>();
        int xc = 0;
        double xSpacing = cellRadius * Math.sqrt( 3 );
        double ySpacing = cellRadius * Math.sqrt( 3 );

        for( double x = -sphereRadius; x < sphereRadius; x += xSpacing, xc++ )
        {
            for( double y = -sphereRadius; y < sphereRadius; y += ySpacing )
            {
                double[] tempPoint = new double[3];
                tempPoint[1] = y + ( xc % 2 ) * cellRadius;
                tempPoint[0] = x;
                tempPoint[2] = 0;
                if( Math.sqrt( VectorUtil.norm_squared( tempPoint ) ) < sphereRadius )
                {
                    result.add( tempPoint );
                }
            }
        }
        return result;
    }
}