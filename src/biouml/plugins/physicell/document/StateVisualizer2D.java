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
    private int xShift;
    private int yShift;
    private int zShift;
    private int width;
    private int height;
    private int extraWidth = 130;
    private int textOffset = 10;
    protected ModelState modelState;

    private void init(ModelState state, ModelData data)
    {
        double dx = data.getXDim().getStep();
        double dy = data.getYDim().getStep();
        double dz = data.getZDim().getStep();
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
        switch( options.getOptions2D().getSection() )
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

    public BufferedImage draw(ModelState state, ModelData data)
    {
        init( state, data );
        BufferedImage img = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics g = img.getGraphics();
        g.setColor( Color.white );
        g.fillRect( 0, 0, width, height );
        if( options.getOptions2D().isDrawAgents() )
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
        if( options.getOptions2D().getSection() == Section.X )
            g.drawString( "X = " + options.getOptions2D().getSlice(), x, 100 );
        else if( options.getOptions2D().getSection() == Section.Y )
            g.drawString( "Y = " + options.getOptions2D().getSlice(), x, 100 );
        else
            g.drawString( "Z = " + options.getOptions2D().getSlice(), x, 100 );
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
            double d = Math.abs( z - options.getOptions2D().getSlice() ); //distance from slice;
            switch( options.getOptions2D().getSection() )
            {
                case X:
                {
                    c1 = y + yShift;
                    c2 = z + zShift;
                    d = Math.abs( x - options.getOptions2D().getSlice() );
                    break;
                }
                case Y:
                {
                    c1 = x + xShift;
                    c2 = z + yShift;
                    d = Math.abs( y - options.getOptions2D().getSlice() );
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
        g.setColor( agent.getColors()[0] );
        g.fillOval( x - r, y - r, 2 * r, 2 * r );
        g.setColor( agent.getColors()[1] );
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
        return draw( modelState, modelData );
    }
}