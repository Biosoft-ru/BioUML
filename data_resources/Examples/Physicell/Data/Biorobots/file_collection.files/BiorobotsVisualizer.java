import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.ui.AgentColorer;
import ru.biosoft.physicell.xml.ModelReader;

public class BiorobotsVisualizer implements AgentColorer
{
    private boolean isInit = false;
    Map<Integer, Color> colors = new HashMap<>();

    public void init(Model model)
    {
        int cargoType = model.getCellDefinition( "cargo cell" ).type;
        int workerType = model.getCellDefinition( "worker cell" ).type;
        int directorType = model.getCellDefinition( "director cell" ).type;
        colors.put( cargoType, ModelReader.readColor( model.getParameterString( "cargo_color" ) ) );
        colors.put( workerType, ModelReader.readColor( model.getParameterString( "worker_color" ) ) );
        colors.put( directorType, ModelReader.readColor( model.getParameterString( "director_color" ) ) );
    }

    @Override
    public Color[] findColors(Cell cell)
    { 
        if (!isInit)
            init(cell.getModel());
        return new Color[] {colors.get( cell.type )};
    }
}