package biouml.plugins.physicell.document;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import ru.biosoft.access.TextDataElement;
import ru.biosoft.physicell.ui.ModelData;


public class RenderPanel extends JPanel implements PropertyChangeListener
{
    private PhysicellSimulationResult result;
    private ViewOptions options;
    private StateVisualizer2D visualizer2D = new StateVisualizer2D();
    private StateVisualizer3D visualizer3D = new StateVisualizer3D();
    private ModelData modelData;
    private BufferedImage img;
    private RotateListener rotateListener;

    public RenderPanel(int width, int height, PhysicellSimulationResult result)
    {
        setPreferredSize( new Dimension( width, height ) );
        modelData = new ModelData();
        modelData.setXDim( 0, 1500, 20 );
        modelData.setYDim( 0, 1500, 20 );
        modelData.setZDim( 0, 1500, 20 );
        this.result = result;
        this.options = result.getOptions();
        visualizer2D.setOptions( options );
        visualizer2D.setModelData( modelData );
        visualizer3D.setOptions( options );
        visualizer3D.setModelData( modelData );
        rotateListener = new RotateListener( options );
        addMouseListener( rotateListener );
        addMouseMotionListener( rotateListener );
        options.addPropertyChangeListener( this );
    }
    
    private StateVisualizer getCurrentVisualizer()
    {
        return options.is3D()? visualizer3D: visualizer2D;
    }

    public void readAgents(TextDataElement agentsData)
    {
        getCurrentVisualizer().readAgents( agentsData.getContent(), agentsData.getName() );
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
        if( evt.getPropertyName().equals( "quality" ) || evt.getPropertyName().equals( "time" ) || evt.getPropertyName().equals( "is3D" ) )
            readAgents( result.getPoint( options.getTime() ) );

        update();
    }
}