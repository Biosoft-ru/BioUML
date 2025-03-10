package biouml.plugins.physicell.document;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import biouml.plugins.physicell.document.View2DOptions.Section;
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
    private int extraWidth = 130;
    private int textOffset = 10;
    protected ModelState modelState;
    private double maxDensity = 1E-13;//6.06;
    private View2DOptions options2D;

    @Override
    public void setResult(PhysicellSimulationResult result)
    {
        super.setResult( result );
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
        this.xShift = -(int) ( Math.floor( startX - dx / 2.0 ) );
        this.yShift = -(int) ( Math.floor( startY - dy / 2.0 ) );
        this.zShift = -(int) ( Math.floor( startZ - dz / 2.0 ) );

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
        width += extraWidth;
    }

    public BufferedImage draw(ModelState state)
    {
        BufferedImage img = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics g = img.getGraphics();
        g.setColor( Color.white );
        g.fillRect( 0, 0, width, height );
        if( options2D.isDrawDensity() )
            drawDensity( densityState.getDensity( "food" ), g );
        if( options2D.isDrawAgents() )
            drawAgents( state, g );
        if( options.isStatistics() )
            drawText( state.getSize(), state.getTime(), width - extraWidth + textOffset, g );
       
        return img;
    }

    private void drawText(int agentsCount, double time, int x, Graphics g)
    {
        g.setFont( new Font( "TimesRoman", Font.PLAIN, 20 ) );
        g.setColor( Color.BLACK );
        g.drawString( "Time: " + time, x, 40 );
        g.drawString( "Cells: " + agentsCount, x, 70 );
        if( options2D.getSection() == Section.X )
            g.drawString( "X = " + options2D.getSlice(), x, 100 );
        else if( options2D.getSection() == Section.Y )
            g.drawString( "Y = " + options2D.getSlice(), x, 100 );
        else
            g.drawString( "Z = " + options2D.getSlice(), x, 100 );
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
                    c2 = z + yShift;
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
            if( d > nuclearRadius )
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

        int n = (int) ( ( options2D.getSlice() + shift ) / size3 ) - 1;

        double actualMaxDensity = 0;
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
                        index = i + n * j + j * n1 * n2;
                        break;
                    default: //Z
                        index = i + n1 * j + n * n1 * n2;
                }
                double density = densities[index];
                //                double density = density[i][j];
                if( density > actualMaxDensity )
                    actualMaxDensity = density;

                double ratio = ( density / maxDensity );
                ratio = Math.min( 1, ratio );
                red = (int) ( ( 1 - ratio ) * 255 );

                g.setColor( new Color( 255, red, red ) );
                g.fillRect( i * size1, j * size2, size1, size2 );
            }
        }
        if( actualMaxDensity > 0 )
        {
            //            System.out.println( "Max density: " + actualMaxDensity );
            maxDensity = actualMaxDensity;
            if( maxDensity < 1E-20 )
                maxDensity = 1E-20;
        }
    }
}