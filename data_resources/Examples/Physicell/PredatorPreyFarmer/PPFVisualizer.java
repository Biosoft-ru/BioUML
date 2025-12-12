import java.awt.Color;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.ui.AgentColorerDefault;

public class PPFVisualizer extends AgentColorerDefault
{
    @Override
    public Color[] findColors(Cell pCell)
    {
        CellDefinition pFarmerDef = pCell.getModel().getCellDefinition( "farmer" );
        CellDefinition pPreyDef = pCell.getModel().getCellDefinition( "prey" );
        CellDefinition pPredDef = pCell.getModel().getCellDefinition( "predator" );

        if( pCell.type == pFarmerDef.type )
            return new Color[] {Color.gray};

        if( pCell.type == pPreyDef.type )
            return new Color[] {Color.blue};

        if( pCell.type == pPredDef.type )
            return new Color[] {Color.orange};
        return super.findColors( pCell );
    }
}