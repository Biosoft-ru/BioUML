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
        time += result.getStep();
        if( time > this.result.getMaxTime() )
        {
            playing = false;
            listener.stop();
            return;
        }
        int existing = result.floorTime( time );
        this.result.getOptions().setTime( existing );
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