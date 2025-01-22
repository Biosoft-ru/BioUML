import java.awt.Color;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.ui.AgentColorer;

public class Visualizer extends AgentColorer
{
    @Override
    public Color[] findColors(Cell pCell)
    {
        int nVirus = pCell.getModel().getMicroenvironment().findDensityIndex( "virus" );
        Color[] output = new Color[] {Color.magenta, Color.black, Color.magenta, Color.black};

        double min_virus = pCell.customData.get( "min_virion_count" );
        double max_virus = pCell.customData.get( "burst_virion_count" );
        double denominator = max_virus - min_virus + 1e-15;

        CellDefinition pMacrophage = pCell.getModel().getCellDefinition( "macrophage" );

        if( pCell.phenotype.death.dead )
        {
            output[0] = Color.red;
            output[2] = Color.red.darker();
            return output;
        }

        if( pCell.type != pMacrophage.type )
        {
            output[0] = Color.blue;
            output[2] = Color.blue.darker();

            double virus = pCell.phenotype.molecular.internSubstrates[nVirus];

            if( pCell.phenotype.molecular.internSubstrates[nVirus] >= min_virus )
            {
                double interp = ( virus - min_virus ) / denominator;
                interp = Math.min( 1.0, interp );
                int Red = (int)Math.floor( 255.0 * interp );
                int Green = (int)Math.floor( 255.0 * interp );
                int Blue = (int)Math.floor( 255.0 * ( 1 - interp ) );
                Color c = new Color( Red, Green, Blue );
                output[0] = c;
                output[2] = c;
            }

        }

        return output;
    }
}