import java.awt.Color;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.AgentColorer;

public class Visualizer implements AgentColorer
{
    public Color[] findColors(Cell cell)
    {
        if( cell.state.numberAttachedCells() == 0 )
            return new Color[]  {Color.gray};

        if( cell.state.numberAttachedCells() == 1 )
        {
            if( cell.customData.get( "head" ) > cell.state.attachedCells.iterator().next().customData.get( "head" ) )
                return new Color[] {Color.red};
            return new Color[] {Color.orange};
        }

        if( cell.state.numberAttachedCells() == 2 )
        {
            int intensity = (int)Math.floor( 255.0 * cell.customData.get( "head" ) );
            Color c = new Color( intensity, intensity, 255 );
            return new Color[] {c};
        }

        if( cell.state.numberAttachedCells() > 2 )
            return new Color[] {Color.yellow};
        return new Color[] {Color.yellow};
    }
}