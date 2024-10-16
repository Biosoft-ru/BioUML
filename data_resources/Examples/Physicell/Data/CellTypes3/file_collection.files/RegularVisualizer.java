import ru.biosoft.physicell.core.PhysiCellConstants;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.xml.ModelReader;
import ru.biosoft.physicell.ui.AgentColorer;
import java.awt.Color;

public class RegularVisualizer extends AgentColorer
{
    private Model model;
    private Color aColor;
    private Color bColor;
    private Color cColor;
    private boolean isInit = false;

    public void init(Model model)
    {
        this.model = model;
        aColor = ModelReader.readColor( model.getParameterString( "A_color" ) );
        bColor = ModelReader.readColor( model.getParameterString( "B_color" ) );
        cColor = ModelReader.readColor( model.getParameterString( "C_color" ) );
        this.isInit = true;
    }

    @Override
    public Color[] findColors(Cell cell)
    {
        if (!isInit)
           init(cell.getModel());
        
        Color[] output = new Color[] {Color.black, Color.black, Color.black, Color.black};

        if( cell.type == model.getCellDefinition( "A" ).type )
        {
            output[0] = aColor;
            output[2] = aColor;
        }
        else if( cell.type == model.getCellDefinition( "B" ).type )
        {
            output[0] = bColor;
            output[2] = bColor;
        }
        else if( cell.type == model.getCellDefinition( "C" ).type )
        {
            output[0] = cColor;
            output[2] = cColor;
        }

        if( cell.phenotype.death.dead )
        {
            // Necrotic - Brown
            if( cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic_swelling
                    || cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic_lysed
                    || cell.phenotype.cycle.currentPhase().code == PhysiCellConstants.necrotic )
                output[2] = new Color( 123, 63, 0 );//  "chocolate";
            else
                output[2] = Color.black;
        }
        return output;
    }
}