package biouml.plugins.physicell;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.AgentColorer;

public class DefaultColorer implements AgentColorer
{
    private Map<String, List<CellDefinitionVisualizerProperties>> properties;
    private Color[] defaultColors = new Color[] {Color.white, Color.black};

    public DefaultColorer(VisualizerProperties visualizerProperties)
    {
        properties = new HashMap<>();
        Stream.of( visualizerProperties.getProperties() )
                .forEach( prop -> properties.computeIfAbsent( prop.getCellType(), k -> new ArrayList<>() ).add( prop ) );

        for( List<CellDefinitionVisualizerProperties> list : properties.values() )
            list.sort( new VisualizerPropertiesComparator() );
    }

    @Override
    public Color[] findColors(Cell cell)
    {
        List<CellDefinitionVisualizerProperties> colors = properties.get( cell.typeName );
        if( colors.size() == 0 )
            return defaultColors;

        ColorScheme scheme = colors.get( 0 ).calculate( cell );
        return new Color[] {scheme.getColor(), scheme.getBorderColor(), scheme.getCoreColor(), scheme.getCoreBorderColor()};
    }

    public class VisualizerPropertiesComparator implements Comparator<CellDefinitionVisualizerProperties>
    {

        @Override
        public int compare(CellDefinitionVisualizerProperties o1, CellDefinitionVisualizerProperties o2)
        {
            return Double.compare( o2.getPriority(), o1.getPriority() );
        }

    }

}