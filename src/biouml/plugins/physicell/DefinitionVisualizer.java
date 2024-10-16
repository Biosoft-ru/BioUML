package biouml.plugins.physicell;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.graphics.Brush;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.AgentColorer;

public class DefinitionVisualizer implements AgentColorer
{
    private Map<String, Brush> typeToColor = new HashMap<>();

    public void setColor(String type, Brush b)
    {
        this.typeToColor.put( type, b );
    }

    @Override
    public Color[] findColors(Cell cell)
    {
        if( typeToColor.containsKey( cell.typeName ) )
        {
            Brush b = typeToColor.get( cell.typeName );
            return new Color[] {b.getColor()};
        }
        return new Color[] {Color.white, Color.black};
    }
}