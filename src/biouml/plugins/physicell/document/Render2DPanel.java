package biouml.plugins.physicell.document;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import ru.biosoft.physicell.ui.ModelData;
import ru.biosoft.physicell.ui.ModelState;


public class Render2DPanel extends JPanel implements PropertyChangeListener
{
    private int time;
    private PhysicellSimulationResult result;
    private View2DOptions options;
    private StateVisualizer2D visualizer = new StateVisualizer2D();
    private ModelState modelState;
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
        this.options = (View2DOptions)result.getOptions();
        visualizer.setOptions( options );
        addMouseListener( rotateListener );
        addMouseMotionListener( rotateListener );
        options.addPropertyChangeListener( this );
    }

    public void read(String content)
    {
        modelState = ModelState.fromString( content );
    }

    public void update()
    {
        img = visualizer.draw( modelState, modelData );
        this.repaint();
        System.out.println( "Repaint" );
    }

    public void paintComponent(Graphics g)
    {
        g.clearRect( 0, 0, getWidth(), getHeight() );
        g.drawImage( img, 0, 0, null );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals( "quality" ) || evt.getPropertyName().equals( "time" ) )
            read( result.getPoint( options.getTime() ).getContent() );

        update();
    }
}