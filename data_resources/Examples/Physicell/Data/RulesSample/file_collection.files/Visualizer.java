import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

import ru.biosoft.physicell.core.PhysiCellConstants;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.ui.AgentColorer;

public class Visualizer extends AgentColorer
{
    List<Color> colors = new ArrayList<>();
    boolean isInit = false;
    public void init()
    {
        colors.add( Color.gray );//"grey" ); // default color will be grey 

        colors.add( Color.red );//"red" );
        colors.add( Color.yellow );///"yellow" );
        colors.add( Color.green );//"green" );
        colors.add( Color.blue );//"blue" );

        colors.add( Color.magenta );//"magenta" );
        colors.add( Color.orange );//"orange" );
        colors.add( new Color( 50, 205, 50 ) );//"lime" );
        colors.add( Color.cyan );//"cyan" );

        colors.add( new Color( 255, 105, 180 ) );//"hotpink" );
        colors.add( new Color( 255, 218, 185 ) );//"peachpuff" );
        colors.add( new Color( 143, 188, 143 ) );//"darkseagreen" );
        colors.add( new Color( 135, 206, 250 ) );//"lightskyblue" );
        isInit = true;
    }

    @Override
    public Color[] findColors(Cell cell)
    {
        if( !isInit )
            init();
      
        Color interiorColor = Color.white;
        if( cell.type < 13 )
            interiorColor = colors.get( cell.type );

        Color output = interiorColor; // set cytoplasm color     

        if( cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic_swelling
                || cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic_lysed
                || cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic )
        {
            interiorColor = new Color( 139, 69, 19 );//"saddlebrown";
        }

        if( cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.apoptotic )
            interiorColor = Color.pink;//"black";
        output = interiorColor; 
        return new Color[] {output};
    }

}