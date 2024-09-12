import java.awt.Color;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.FalseCellCytometryVisualizer;

public class ODEVisualizer extends FalseCellCytometryVisualizer
{
    @Override
    public Color[] findColors(Cell pCell)
    {
        int energy_vi = pCell.customData.findVariableIndex( "intra_energy" );
        Color[] output = super.findColors( pCell );
        // proliferative cell
        if( pCell.type == 0 )
        {
            if( !pCell.phenotype.death.dead && pCell.customData.get( energy_vi ) > 445 )
            {
                output[0] = new Color( 255, 255, 0 );
                output[2] = new Color( 125, 125, 0 );
            }
            // arrested cell
            if( !pCell.phenotype.death.dead && pCell.customData.get( energy_vi ) <= 445 )
            {
                output[0] = new Color( 255, 0, 0 );
                output[2] = new Color( 125, 0, 0 );
            }
            // dead cell
            if( pCell.phenotype.death.dead )
            {
                output[0] = new Color( 20, 20, 20 );
                output[2] = new Color( 10, 10, 10 );
            }
        }

            if (pCell.phenotype.death.dead)
 return new Color[]{new Color(255, 0, 0)};
        return output;
    }
}