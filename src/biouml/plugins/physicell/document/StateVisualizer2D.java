package biouml.plugins.physicell.document;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import biouml.plugins.physicell.document.View2DOptions.Section;
import one.util.streamex.DoubleStreamEx;
import ru.biosoft.physicell.ui.ModelData;
import ru.biosoft.physicell.ui.ModelState;
import ru.biosoft.physicell.ui.ModelState.AgentState;

public class StateVisualizer2D extends StateVisualizer
{
    private int xCells;
    private int yCells;
    private int zCells;
    private int dx;
    private int dy;
    private int dz;
    private int xShift;
    private int yShift;
    private int zShift;
    private int width;
    private int height;
    protected ModelState modelState;
    private double maxDensity = 1E-13;//6.06;
    private View2DOptions options2D;
    private BufferedImage legend = null;

    public void setLegend(BufferedImage legend)
    {
        this.legend = legend;
    }

    @Override
    public void setResult(PhysicellSimulationResult result)
    {
        super.setResult( result );
        this.legend = result.getLegend();
        try
        {
            modelState = ModelState.fromString( result.getPoint( 0 ).getContent() );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        ModelData data = result.getModelData();
        options2D = result.getOptions().getOptions2D();
        dx = (int)data.getXDim().getStep();
        dy = (int)data.getYDim().getStep();
        dz = (int)data.getZDim().getStep();
        xCells = (int)data.getXDim().getLength() / dx;
        yCells = (int)data.getYDim().getLength() / dx;
        zCells = (int)data.getZDim().getLength() / dx;
        double startX = data.getXDim().getFrom();
        double startY = data.getYDim().getFrom();
        double startZ = data.getZDim().getFrom();
        this.xShift = -(int) ( Math.floor( startX ) );
        this.yShift = -(int) ( Math.floor( startY ) );
        this.zShift = -(int) ( Math.floor( startZ ) );
        int xLength = (int)data.getXDim().getLength();
        int yLength = (int)data.getYDim().getLength();
        int zLength = (int)data.getZDim().getLength();
        width = xLength;
        height = yLength;
        switch( options2D.getSection() )
        {
            case X:
                width = yLength;
                height = zLength;
                break;
            case Y:
                width = xLength;
                height = zLength;
                break;
            default:
                break;
        }

        if( result.getLegend() != null )
        {
            options.setLegendX( width );
            width += result.getLegend().getWidth();
        }
    }
    
    public Dimension getSize()
    {
        return new Dimension(width, height);
    }

    public BufferedImage draw(ModelState state)
    {
        BufferedImage img = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics g = img.getGraphics();
        g.setColor( Color.white );
        g.fillRect( 0, 0, width, height );
        if( options.isDrawDensity() && densityState != null )
            drawDensity( densityState.getDensity( options.getSubstrate() ), g );
        if( options.isCells() )
            drawAgents( state, g );
        if( options.isStatistics() )
            drawText( state.getSize(), state.getTime(), new Point( options.getStatisticsX(), options.getStatisticsY() ), g );
        if( options.isAxes() )
            drawLines( g );

        if( options.isShowLegend() && legend != null )
            drawLegend( legend, g );

        return img;
    }

    private void drawText(int agentsCount, double time, Point location, Graphics g)
    {
        String timeString = "Time: " + options.getTime();
        String cellString = "Cells: " + agentsCount;
        String sliceString = "X = " + options2D.getSlice();
        if( options2D.getSection() == Section.Y )
            sliceString = "Y = " + options2D.getSlice();
        else if( options2D.getSection() == Section.Z )
            sliceString = "Z = " + options2D.getSlice();

        int x = location.x;
        int y = location.y;
        Font font = options.getStatisticsFont().getFont();
        FontMetrics fm = g.getFontMetrics( font );
        Rectangle2D timeBounds = fm.getStringBounds( timeString, g );
        Rectangle2D cellBounds = fm.getStringBounds( cellString, g );
        Rectangle2D sliceBounds = fm.getStringBounds( sliceString, g );

        int yDelta = 10;
        int xDelta = 10;
        double height = timeBounds.getHeight() + yDelta + cellBounds.getHeight() + yDelta + sliceBounds.getHeight()+yDelta;
        double width = DoubleStreamEx.of(timeBounds.getWidth() , cellBounds.getWidth() , sliceBounds.getWidth()).max().orElse( 0 )+xDelta;
        if( options.isStatisticsBackground() )
        {
            g.setColor( Color.white );
            g.fillRect( x - 5, y - 25, (int)width, (int)height );
        }
        g.setFont( font );
        g.setColor( Color.BLACK );
        g.drawString( timeString, x, y );
        y += (int)timeBounds.getHeight() + yDelta;
        g.drawString( cellString, x, y );
        y += (int)cellBounds.getHeight() + yDelta;
        g.drawString( sliceString, x, y  );
    }

    private void drawAgents(ModelState state, Graphics g)
    {
        g.setColor( Color.black );
        for( AgentState agent : state.getAgents() )
        {
            double[] position = agent.getPosition();
            int x = (int)position[0];
            int y = (int)position[1];
            int z = (int)position[2];
            int c1 = x + xShift; //first coordinate
            int c2 = y + yShift; //second coordinate
            double d = Math.abs( z - options2D.getSlice() ); //distance from slice;
            switch( options2D.getSection() )
            {
                case X:
                {
                    c1 = y + yShift;
                    c2 = z + zShift;
                    d = Math.abs( x - options2D.getSlice() );
                    break;
                }
                case Y:
                {
                    c1 = x + xShift;
                    c2 = z + zShift;
                    d = Math.abs( y - options2D.getSlice() );
                    break;
                }
                default:
                    break;
            }

            double radius = agent.getRadius(); //we consider agents to be spheres
            if( d > radius ) //it does not intersect slice;
                continue;
            int r = (int)Math.sqrt( radius * radius - d * d );
            double nuclearRadius = agent.getInnerRadius();
            if( !options.isDrawNuclei() || d > nuclearRadius )
                drawAgent( agent, c1, c2, r, g );
            else
            {
                int nr = (int)Math.sqrt( nuclearRadius * nuclearRadius - d * d );
                drawAgent( agent, c1, c2, r, nr, g );
            }
        }
    }

    private void drawAgent(AgentState agent, int x, int y, int r, Graphics g)
    {
        Color[] colors = agent.getColors();
        Color outer = null;
        Color inner = null;
        if( colors.length == 1 )
        {
            outer = Color.black;
            inner = colors[0];
        }
        else
        {
            inner = colors[0];
            outer = colors[1];
        }
        g.setColor( inner );
        g.fillOval( x - r, y - r, 2 * r, 2 * r );
        g.setColor( outer );
        g.drawOval( x - r, y - r, 2 * r, 2 * r );
    }

    private void drawAgent(AgentState agent, int x, int y, int r, int nr, Graphics g)
    {
        g.setColor( agent.getColors()[0] );
        g.fillOval( x - r, y - r, 2 * r, 2 * r );
        if( agent.getColors().length > 1 )
            g.setColor( agent.getColors()[1] );
        else
            g.setColor( Color.black );
        g.drawOval( x - r, y - r, 2 * r, 2 * r );
        if( agent.getColors().length > 2 )
        {
            g.setColor( agent.getColors()[2] );
            g.fillOval( x - nr, y - nr, 2 * nr, 2 * nr );
            g.setColor( agent.getColors()[3] );
            g.drawOval( x - nr, y - nr, 2 * nr, 2 * nr );
        }
    }

    private void drawGrid(int xNumber, int yNumber, int xSize, int ySize, Graphics g)
    {
        for( int i = 0; i < xNumber; i++ )
        {
            for( int j = 0; j < yNumber; j++ )
            {
                g.setColor( Color.black );
                g.drawRect( i * xSize, j * ySize, xSize, ySize );
            }
        }
    }

    @Override
    public void readAgents(String content, String name)
    {
        if( currentName.equals( name ) )
            return;
        this.currentName = name;
        modelState = ModelState.fromString( content );
    }

    @Override
    public BufferedImage draw()
    {
        return draw( modelState );
    }

    private void drawDensity(double[] densities, Graphics g)
    {
        int n1 = xCells;
        int n2 = yCells;
        int size1 = dx;
        int size2 = dy;
        int size3 = dz;
        int shift = this.zShift;
        switch( options2D.getSection() )
        {
            case X:
                n1 = yCells;
                n2 = zCells;
                size1 = dy;
                size2 = dz;
                size3 = dx;
                shift = xShift;
                break;
            case Y:
                n1 = xCells;
                n2 = zCells;
                size1 = dx;
                size2 = dz;
                size3 = dy;
                shift = yShift;
                break;
            default:
                break;
        }

        int n = (int) ( ( options2D.getSlice() + shift ) / size3 );

        double maxDensity = DoubleStreamEx.of( densities ).max().orElse( 0 );
        if( maxDensity == 0 )
            maxDensity = 1;
        for( int i = 0; i < n1; i++ )
        {
            for( int j = 0; j < n2; j++ )
            {
                int red;
                int index;
                switch( options2D.getSection() )
                {
                    case X:
                        index = n + n1 * i + n1 * n2 * j;
                        break;
                    case Y:
                        index = i + n * n1 + j * n1 * n2;
                        break;
                    default: //Z
                        index = i + n1 * j + n * n1 * n2;
                }
                double density = densities[index];
                double ratio = ( density / maxDensity );
                ratio = Math.min( 1, ratio );
                Color c = options.getDensityColor();
                Color actual = new Color( calculateComponent( c.getRed(), ratio ), calculateComponent( c.getGreen(), ratio ),
                        calculateComponent( c.getBlue(), ratio ) );
                g.setColor( actual );
                g.fillRect( i * size1, j * size2, size1, size2 );
            }
        }
    }

    public int calculateComponent(int colorComponent, double ratio)
    {
        int result = (int) ( ( colorComponent - 255 ) * ratio + 255 );
        return Math.min( Math.max( result, 0 ), 255 );
    }

    private void drawLines(Graphics g)
    {
        g.setFont( new Font( "TimesRoman", Font.BOLD, 20 ) );
        g.setColor( Color.BLACK );
        int w = width - 100;
        int h = 100;
        int x1 = xShift;
        int y1 = yShift;
        String title1 = "X";
        String title2 = "Y";
        int arrowLength = 50;
        int arrowWidth = 10;
        switch( options2D.getSection() )
        {
            case X:
                x1 = yShift;
                y1 = zShift;
                title1 = "Y";
                title2 = "Z";
                break;
            case Y:
                x1 = xShift;
                y1 = zShift;
                title1 = "X";
                title2 = "Z";
                break;
            default:
        }
        g.drawLine( x1, y1, w, y1 );
        g.drawString( title1, w - 20, y1 - 20 );
        g.drawLine( x1, y1, x1, h );
        g.drawString( title2, x1 + 10, h );
        g.fillPolygon( new int[] {w, w - arrowLength, w - arrowLength}, new int[] {y1, y1 + arrowWidth, y1 - arrowWidth}, 3 );
        g.fillPolygon( new int[] {x1, x1 - arrowWidth, x1 + arrowWidth}, new int[] {h, h + arrowLength, h + arrowLength}, 3 );
    }

    private void drawLegend(BufferedImage legend, Graphics g)
    {
        g.setColor( Color.white );
        g.fillRect(  options.getLegendX(), options.getLegendY(), legend.getWidth(), legend.getHeight() );
        g.drawImage( legend, options.getLegendX(), options.getLegendY(), null );
    }
}