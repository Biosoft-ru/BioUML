package biouml.plugins.physicell.document;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import ru.biosoft.physicell.ui.render.Renderer3D;
import ru.biosoft.physicell.ui.render.Scene;
import ru.biosoft.physicell.ui.render.SceneHelper;

public class Render3DPanel extends JPanel implements PropertyChangeListener
{
    private Scene scene;
    private int time;
    private PhysicellSimulationResult result;
    private View3DOptions options;
    private Renderer3D renderer;
    private BufferedImage img;
    private boolean isRendereing = false;
    private RotateListener rotateListener;

    public Render3DPanel(int width, int height, PhysicellSimulationResult result)
    {
        setPreferredSize( new Dimension( width, height ) );
        renderer = new Renderer3D( width, height, 0, 0 );
        this.result = result;
        this.options = result.getOptions();
        rotateListener = new RotateListener( result.getOptions() );
        addMouseListener( rotateListener );
        addMouseMotionListener( rotateListener );
        options.addPropertyChangeListener( this );
    }
    
    public void read(String content)
    {
        scene = Util.readScene( content, options.getQualityInt() );
    }

    public void update()
    {
        renderer.setAngle( options.getHead(), options.getPitch() );
        renderer.setCutOff( options.getCutOff() );
        renderer.setAxes( options.isAxes() );
        renderer.setStatistics( options.isStatistics() );
        if( !isRendereing )
        {
            isRendereing = true;
            SceneHelper.addDisks( scene, options.getXCutOff(), SceneHelper.PLANE_YZ );
            SceneHelper.addDisks( scene, options.getYCutOff(), SceneHelper.PLANE_XZ );
            SceneHelper.addDisks( scene, options.getZCutOff(), SceneHelper.PLANE_XY );
            img = renderer.render( scene, time );
            this.repaint();
            isRendereing = false;
        }
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