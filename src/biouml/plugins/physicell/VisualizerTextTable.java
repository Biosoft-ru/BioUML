package biouml.plugins.physicell;

import java.awt.Color;
import java.io.IOException;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.AgentColorer;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;


public class VisualizerTextTable
{
    private String name;
    private AgentColorer colorer;
    private double timeMax;
    private double timeInterval;
    private String format;
    DataElementPath dc;
    DataCollection parent;
    public VisualizerTextTable(String name)
    {
        this.name = name;
    }

    public VisualizerTextTable(DataElementPath dc, String name, AgentColorer colorer, double timeMax, double timeInterval)
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
    //
    //    public VisualizerTextTable setTimeMax(double timeMax)
    //    {
    //        this.timeMax = timeMax;
    //        return this;
    //    }
    //
    //    public VisualizerTextTable setTimeInterval(double timeInterval)
    //    {
    //        this.timeInterval = timeInterval;
    //        return this;
    //    }
    //
    //    public VisualizerTextTable setAgentColorer(AgentColorer colorer)
    //    {
    //        this.colorer = colorer;
    //        return this;
    //    }

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
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( parent, name );

        tdc.getColumnModel().addColumn( "X", DataType.Float );
        tdc.getColumnModel().addColumn( "Y", DataType.Float );
        tdc.getColumnModel().addColumn( "Z", DataType.Float );
        tdc.getColumnModel().addColumn( "Radius", DataType.Float );
        tdc.getColumnModel().addColumn( "r", DataType.Float );
        tdc.getColumnModel().addColumn( "Color1", DataType.Text );
        tdc.getColumnModel().addColumn( "Color2", DataType.Text );
        tdc.getColumnModel().addColumn( "Color3", DataType.Text );
        tdc.getColumnModel().addColumn( "Color4", DataType.Text );


        for( Cell cell : m.getAgents( Cell.class ) )
        {
            Color[] colors = colorer.findColors( cell );
            Object[] row = new Object[] {cell.position[0], cell.position[1], cell.position[2], cell.getRadius(),
                    cell.phenotype.geometry.getNuclearRadius(), encodeColor( colors[0] ), encodeColor( colors[1] ),
                    encodeColor( colors[2] ), encodeColor( colors[3] )};
            TableDataCollectionUtils.addRow( tdc, String.valueOf( cell.ID ), row );
        }
        parent.put( tdc );
    }

    private static String encodeColor(Color color)
    {
        if( color == null || color.getAlpha() == 0 )
            return "";
        return "[" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "]";
    }
}