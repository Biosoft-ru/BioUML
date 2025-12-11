import java.awt.Color;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.PhysiCellConstants;
import ru.biosoft.physicell.ui.AgentColorer;

public class CancerImmunityVisualizer implements AgentColorer 
{
    @Override
    public Color[] findColors(Cell cell)
    {
        int oncoproteinIndex = cell.customData.findVariableIndex( "oncoprotein" );
        Color[] output = new Color[] {Color.black, Color.black, Color.black, Color.black}; // immune are black

        if( cell.type == 1 )
        {
            Color lime = new Color( 50, 205, 50 );
            return new Color[] {lime, lime, Color.green, Color.green};
        }

        // if I'm under attack, color me 
        if( cell.state.attachedCells.size() > 0 )
        {
            output[0] = Color.cyan.darker(); // orangered // "purple"; // 128,0,128
            output[2] = Color.cyan; // "magenta"; //255,0    
        }
        // live cells are green, but shaded by oncoprotein value 
        if( !cell.phenotype.death.dead )
        {
            int oncoprotein = (int)Math.round( 0.5 * cell.customData.get( oncoproteinIndex ) * 255.0 );
            Color c = new Color( oncoprotein, oncoprotein, 255 - oncoprotein );
            output[0] = c;
            output[1] = c;
            output[2] = new Color( oncoprotein / 2, oncoprotein / 2, ( 255 - oncoprotein ) / 2 );
            return output;
        }

        // if not, dead colors 
        if( cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.apoptotic ) // Apoptotic - Red
        {
            output[0] = new Color( 255, 0, 0 );
            output[2] = new Color( 125, 0, 0 );
            return output;
        }

        // Necrotic - Brown
        if( cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic_swelling
                || cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic_lysed
                || cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic )
        {
            output[0] = new Color( 250, 138, 38 );
            output[0] = new Color( 139, 69, 19 );
            return output;
        }
        return output;
    }
}