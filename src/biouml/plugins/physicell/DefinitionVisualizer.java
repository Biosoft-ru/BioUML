package biouml.plugins.physicell;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.AgentVisualizer2;

public class DefinitionVisualizer extends AgentVisualizer2
{
    private static List<Color> colors = new ArrayList<>();
    {
        colors.add( Color.green );
        colors.add( Color.cyan );
        colors.add( Color.gray );
        colors.add( Color.magenta );
        colors.add( Color.orange );
        colors.add( Color.pink );
    }

    @Override
    public Color[] findColors(Cell cell)
    {
        if( cell.type <= colors.size() )
        {
            Color c = colors.get( cell.type );
            return new Color[] {c, Color.black, c.darker(), Color.black};
        }
        return new Color[] {Color.white, Color.black};
    }
}