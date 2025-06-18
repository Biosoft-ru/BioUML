package biouml.plugins.physicell;

import java.awt.Color;
import java.io.IOException;

import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.TextDataElement;
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

        parent = DataCollectionUtils.createSubCollection( dc.getChildPath( "Cells" ) );
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
        String name = generateResultName( t, "Cells" );
        TextDataElement tde = new TextDataElement( name, parent );
        StringBuffer buffer = new StringBuffer();
        buffer.append( StreamEx.of("X", "Y", "Z", "Radius", "r", "Color1", "Color2", "Color3", "Color4").joining( "\t" ));
        buffer.append( "\n" );
        for( Cell cell : m.getAgents( Cell.class ) )
        {
            Object[] row = null;
            Color[] colors = colorer.findColors( cell );
            if( colors.length == 1)
            {
                row = new Object[] {toString( cell.position[0] ), toString( cell.position[1] ), toString( cell.position[2] ),
                        toString( cell.getRadius() ), toString( cell.phenotype.geometry.getNuclearRadius() ), encodeColor( colors[0] )};
            }
            else if( colors.length == 2 )
            {
                row = new Object[] {toString( cell.position[0] ), toString( cell.position[1] ), toString( cell.position[2] ),
                        toString( cell.getRadius() ), toString( cell.phenotype.geometry.getNuclearRadius() ), encodeColor( colors[0] ),
                        encodeColor( colors[1] )};
            }   
            else
            {
                row = new Object[] {toString( cell.position[0] ), toString( cell.position[1] ), toString( cell.position[2] ),
                        toString( cell.getRadius() ), toString( cell.phenotype.geometry.getNuclearRadius() ), encodeColor( colors[0] ),
                        encodeColor( colors[1] ), encodeColor( colors[2] ), encodeColor( colors[3] )};
            }

            buffer.append( StreamEx.of( row ).map( s -> String.valueOf( s ) ).joining( "\t" ) );
            buffer.append( "\n" );
            tde.setContent( buffer.toString() );
        }
        parent.put( tde );
    }
    
    public String toString(double value)
    {
        return String.valueOf( (int)(value * 10)/10.0 );
    }

    private static String encodeColor(Color color)
    {
        if( color == null || color.getAlpha() == 0 )
            return "";
        return "[" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "]";
    }
    
}