package biouml.plugins.physicell;

import java.awt.Color;
import java.io.IOException;

import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.AgentColorer;

public class VisualizerText
{
    private String name;
    private AgentColorer colorer;
    private double timeMax;
    private double timeInterval;
    private String format;
    DataElementPath dc;
    DataCollection parent;

    public VisualizerText(String name)
    {
        this.name = name;
    }

    public VisualizerText(DataElementPath dc, String name, AgentColorer colorer, double timeMax, double timeInterval)
    {
        this.name = name;
        this.colorer = colorer;
        this.timeInterval = timeInterval;
        this.timeMax = timeMax;
        this.dc = dc;
    }

    public String getName()
    {
        return name;
    }

    public void init() throws Exception
    {
        int nums = String.valueOf( Math.round( timeMax ) ).length() + 1;
        format = "%0" + nums + "d";

        parent = DataCollectionUtils.createSubCollection( dc.getChildPath( "Image text" ) );
    }

    private String generateResultName(double t, String baseName)
    {
        String suffix;
        if( timeInterval >= 1 )
        {
            suffix = String.format( format, (int)Math.round( t ) );
        }
        else
        {
            suffix = Double.toString( Math.round( t * 100 ) / 100 );
        }
        return baseName + "_" + suffix;
    }

    public void saveResult(Microenvironment m, double t) throws IOException
    {
        String name = generateResultName( t, "Image" );
        TextDataElement tde = new TextDataElement( name, parent );
        StringBuffer buffer = new StringBuffer();
        buffer.append( StreamEx.of("X", "Y", "Z", "Radius", "r", "Color1", "Color2", "Color3", "Color4").joining( "\t" ));
        buffer.append( "\n" );
        for( Cell cell : m.getAgents( Cell.class ) )
        {
            Color[] colors = colorer.findColors( cell );
            Object[] row = new Object[] {cell.position[0], cell.position[1], cell.position[2], cell.getRadius(),
                    cell.phenotype.geometry.getNuclearRadius(), encodeColor( colors[0] ), encodeColor( colors[1] ),
                    encodeColor( colors[2] ), encodeColor( colors[3] )};

            buffer.append( StreamEx.of( row ).map( s -> String.valueOf( s ) ).joining( "\t" ) );
            buffer.append( "\n" );
            tde.setContent( buffer.toString() );
        }
        parent.put( tde );
    }

    private static String encodeColor(Color color)
    {
        if( color == null || color.getAlpha() == 0 )
            return "";
        return "[" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "]";
    }
}