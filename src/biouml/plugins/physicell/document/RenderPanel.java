package biouml.plugins.physicell.document;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JPanel;

import biouml.plugins.physicell.PhysicellResultWriter;
import biouml.plugins.physicell.VideoGenerator;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.util.TempFiles;

public class RenderPanel extends JPanel implements PropertyChangeListener
{
    private PhysicellSimulationResult result;
    private ViewOptions options;
    private StateVisualizer2D visualizer2D = new StateVisualizer2D();
    private StateVisualizer3D visualizer3D = new StateVisualizer3D();
    private BufferedImage img;
    private RotateListener rotateListener;
    private VideoGenerator videoGenerator;
    private File tempVideoFile;

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
        if (options.isSaveResult())
           updateVideo(img);
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
                    || evt.getPropertyName().equals( "is2D" ) )
                readAgents( result.getPoint( options.getTime() ) );

            if( evt.getPropertyName().equals( "time" ) )
                readDensity( result.getDensity( options.getTime() ) );

            if( evt.getPropertyName().equals( "saveResult" ) )
            {
                if ((Boolean)evt.getNewValue())
                    startVideo();
                else if (videoGenerator != null)
                    finishVideo();
                return;
            }

            if( evt.getPropertyName().equals( "result" ) )
                return;

            update();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private void startVideo()
    {
        try
        {
            tempVideoFile = TempFiles.file( "Video.mp4" );
            videoGenerator = new VideoGenerator( tempVideoFile , options.getFps());
            videoGenerator.init();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private void updateVideo(BufferedImage img)
    {
        try
        {
            videoGenerator.update( img );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private void finishVideo()
    {
        try
        {
            videoGenerator.finish();
            PhysicellResultWriter.uploadMP4( tempVideoFile, options.getResult().getParentCollection(), options.getResult().getName() );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        finally
        {
            videoGenerator = null;
        }
    }
}