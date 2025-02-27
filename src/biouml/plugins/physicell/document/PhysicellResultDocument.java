package biouml.plugins.physicell.document;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;

import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.gui.Document;

/**
 * @author axec
 *
 */
//@ClassIcon ( "resources/simulationDocument.gif" )
public class PhysicellResultDocument extends Document implements PropertyChangeListener
{

    //    private static int curTime;

    //    private static JSlider timeSlider;

    private Render3DPanel renderPanel;
    private JScrollPane scrollPane;
    private RotateListener rotateListener;
    private View3DOptions options;
    PhysicellSimulationResult result;

    public PhysicellResultDocument(PhysicellSimulationResult result) throws IllegalArgumentException
    {
        super( result );
        this.result = result;
        options = result.getOptions();
        options.addPropertyChangeListener( this );

        viewPane = new ViewPane();
        //        timeSlider = new JSlider( 0, 100, 0 );
        renderPanel = new Render3DPanel( 1500, 1500, result );
        renderPanel.setPreferredSize( new Dimension( 1500, 1500 ) );
        rotateListener = new RotateListener( result.getOptions() );
        renderPanel.addMouseListener( rotateListener );
        renderPanel.addMouseMotionListener( rotateListener );
        scrollPane = new JScrollPane( renderPanel );
        scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
        scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        viewPane.add( scrollPane );
        result.init();

        renderPanel.read( result.getPoint( options.getTime() ).getContent() );
        renderPanel.update();
    }

    //    private void setTime(int time)
    //    {
    //        curTime = files.navigableKeySet().floor( time );
    //    }



    @Override
    public String getDisplayName()
    {
        return "Simulation result";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals( "quality" ) || evt.getPropertyName().equals( "time" ) )
            renderPanel.read( result.getPoint( options.getTime() ).getContent() );

        renderPanel.update();
    }
}
