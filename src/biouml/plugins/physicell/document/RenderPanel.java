package biouml.plugins.physicell.document;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import ru.biosoft.access.TextDataElement;

public class RenderPanel extends JPanel implements PropertyChangeListener
{
    private PhysicellSimulationResult result;
    private ViewOptions options;
    private StateVisualizer2D visualizer2D = new StateVisualizer2D();
    private StateVisualizer3D visualizer3D = new StateVisualizer3D();
    private BufferedImage img;
    private RotateListener rotateListener;

    public RenderPanel(int width, int height, PhysicellSimulationResult result)
    {
        setPreferredSize( new Dimension( width, height ) );
     
        this.result = result;
        this.options = result.getOptions();
        visualizer2D.setResult( result );
        visualizer3D.setResult( result );
        rotateListener = new RotateListener( options );
        addMouseListener( rotateListener );
        addMouseMotionListener( rotateListener );
        options.addPropertyChangeListener( this );
    }

    private StateVisualizer getCurrentVisualizer()
    {
        return options.is3D() ? visualizer3D : visualizer2D;
    }

    public void readAgents(TextDataElement agentsData)
    {
        getCurrentVisualizer().readAgents( agentsData.getContent(), agentsData.getName() );
    }

    public void readDensity(DensityState densityData)
    {
        getCurrentVisualizer().setDensityState( densityData );
    }
    
    public void update()
    {
        img = getCurrentVisualizer().draw();
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
            if( evt.getPropertyName().equals( "quality" ) || evt.getPropertyName().equals( "time" )
                    || evt.getPropertyName().equals( "is3D" ) )
                readAgents( result.getPoint( options.getTime() ) );

            if( evt.getPropertyName().equals( "time" ))
                    readDensity(result.getDensity(options.getTime()));
            
            update();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}