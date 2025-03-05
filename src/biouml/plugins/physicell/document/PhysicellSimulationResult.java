package biouml.plugins.physicell.document;

import java.util.TreeMap;

import biouml.standard.type.BaseSupport;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.GenericDataCollection;

public class PhysicellSimulationResult extends BaseSupport
{
    private GenericDataCollection dc;
    private ViewOptions options;
    private static TreeMap<Integer, TextDataElement> files = new TreeMap<>();
    private static int step;
    public boolean playing;
    private boolean is3D = false;
    private Player player = new Player();

    public PhysicellSimulationResult(String name, GenericDataCollection de)
    {
        super( null, name );
        this.dc = de;
        this.options = is3D ? new View3DOptions() : new View2DOptions();
    }

    public GenericDataCollection getCollection()
    {
        return dc;
    }

    public ViewOptions getOptions()
    {
        return options;
    }

    public class Player extends Thread
    {
        private int time;
        @Override
        public void run()
        {
            while( playing )
            {
                doStep();
            }
        }
        private void doStep()
        {
            time += step;
            int existing = files.floorKey( time );
            System.out.println( "Inner: "+time+ " Exitsing: "+existing );
            options.setTime( existing );
        }
    }

    public void init()
    {
        files.clear();
        for( DataElement de : dc )
        {
            if( de instanceof TextDataElement )
            {
                String name = de.getName();
                Integer time = Integer.parseInt( name.split( "_" )[1] );
                files.put( time, (TextDataElement)de );
            }
        }
        step = files.navigableKeySet().higher( 0 );
        options.setSize( 1500, 1500, 1500, files.navigableKeySet().last() );

    }

    public TextDataElement getPoint(int time)
    {
        return files.floorEntry( time ).getValue();
    }

    public void play()
    {
        if( playing )
            return;
        playing = true;
        player = new Player();
        player.time = options.getTime();
        System.out.println( "Play" );
        player.start();
    }

    public void stop()
    {
        playing = false;
        System.out.println( "Stop" );
    }

    public void pause()
    {
        playing = false;
        System.out.println( "Stop" );
    }

}