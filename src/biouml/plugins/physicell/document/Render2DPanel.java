package biouml.plugins.physicell.document;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import ru.biosoft.physicell.ui.ModelData;

public class Render2DPanel extends JPanel implements PropertyChangeListener
{
    private int time;
    private PhysicellSimulationResult result;
    private ViewOptions options;
    private StateVisualizer visualizer = new StateVisualizer2D();
    private ModelData modelData;
    private BufferedImage img;
    private boolean isRendereing = false;
    private RotateListener rotateListener;

    public Render2DPanel(int width, int height, PhysicellSimulationResult result)
    {
        setPreferredSize( new Dimension( width, height ) );
        modelData = new ModelData();
        modelData.setXDim( 0, 1500, 20 );
        modelData.setYDim( 0, 1500, 20 );
        modelData.setZDim( 0, 1500, 20 );
        this.result = result;
        this.options = result.getOptions();
//        visualizer.seResult( result );
        addMouseListener( rotateListener );
        addMouseMotionListener( rotateListener );
        options.addPropertyChangeListener( this );
    }

    public void read(String content)
    {
        //        modelState = ModelState.fromString( content );
    }

    public void update()
    {
        img = visualizer.draw();
        this.repaint();
    }

    public void paintComponent(Graphics g)
    {
        g.clearRect( 0, 0, getWidth(), getHeight() );
        g.drawImage( img, 0, 0, null );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        try
        {
            if( evt.getPropertyName().equals( "quality" ) || evt.getPropertyName().equals( "time" ) )
                visualizer.readAgents( result.getPoint( options.getTime() ).getContent(), "" );

            update();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}