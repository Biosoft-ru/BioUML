package biouml.plugins.physicell.document;

/**
 * Player for simulation result
 */
public class Player extends Thread
{
    private boolean playing;
    private int time;
    private PlayerListener listener; 
    private final PhysicellSimulationResult result;

    public Player(PhysicellSimulationResult result)
    {
        this.result = result;
        this.time = result.getOptions().getTime();
    }
    
    public void setListener(PlayerListener listener)
    {
        this.listener = listener;
    }
    

    @Override
    public void run()
    {
        listener.start();
        while( playing )
        {
            doStep();
        }
        listener.stop();
    }
    private void doStep()
    {
        time += PhysicellSimulationResult.step;
        if( time > this.result.maxTime )
        {
            playing = false;
            listener.stop();
            return;
        }
        int existing = PhysicellSimulationResult.files.floorKey( time );
        this.result.options.setTime( existing );
    }

    public void setPlaying(boolean playing)
    {
        this.playing = playing;
    }
    
    public boolean isPlaying()
    {
        return playing;
    }
}