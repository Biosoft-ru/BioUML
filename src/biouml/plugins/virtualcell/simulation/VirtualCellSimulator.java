package biouml.plugins.virtualcell.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.agentmodeling.Scheduler;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * @author Damag
 */
public class VirtualCellSimulator implements Simulator
{
    private Logger log = Logger.getLogger( Scheduler.class.getName() );

    private DataElementPath resultPath;
    private Map<String, DataCollection> poolToCollection = new HashMap<>();

    private VirtualCellModel model;

    protected List<ProcessAgent> agents = new ArrayList<>(); //the same array as in model
    protected List<MapPool> pools = new ArrayList<>();

    protected double currentTime = 0;
    protected double completionTime = 100;
    protected double timeIncrement = 1;

    private boolean isAlive;
    private String format;

    @Override
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        init( model, null, tspan, listeners, jobControl );

        while( doStep() )
            ;
    }

    public boolean doStep() throws Exception
    {
        if( currentTime >= completionTime || !isAlive )
            return false;

        currentTime += timeIncrement;

        for( int i = 0; i < agents.size(); i++ )
            execute( agents.get( i ) );

        for( int i = 0; i < pools.size(); i++ )
            execute( pools.get( i ) );

        return currentTime < completionTime && isAlive;
    }


    private void execute(SimulationAgent agent) throws Exception
    {
        if( !agent.isAlive() )
            return;

        agent.iterate();
    }

    public void setResultPath(DataElementPath resultPath)
    {
        this.resultPath = resultPath;
    }


    private void execute(MapPool pool) throws Exception
    {
        if( !pool.isSaved() )
            return;

        double step = pool.getSaveStep();

        if( Math.round( currentTime % step ) == 0 )
        {
            DataCollection tdc = poolToCollection.get( pool.getName() );
            String suffix = String.format( format, (int)Math.round( currentTime ) );
            String resultName = pool.getName() + "_" + suffix;
            DataElementPath dep = DataElementPath.create( tdc, resultName );
            pool.save( dep, "Value" );
        }
    }

    @Override
    public SimulatorInfo getInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDefaultOptions()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        currentTime = 0;
        completionTime = tspan.getTimeFinal();
        timeIncrement = tspan.getTime( 1 ) - tspan.getTimeStart();
        int nums = String.valueOf( Math.round( completionTime ) ).length() + 1;
        format = "%0" + nums + "d";
        DataCollectionUtils.createSubCollection( resultPath );
        this.model = (VirtualCellModel)model;
        this.pools = this.model.getPools();
        this.agents = this.model.getAgents();
        this.isAlive = true;
        for( MapPool pool : pools )
        {
            init( (MapPool)pool );
        }
        
        for( ProcessAgent agent : agents )
        {
            agent.init();
        }

    }

    private void init(MapPool pool) throws Exception
    {
        if( !pool.isSaved() )
            return;
        DataCollection poolCollection = DataCollectionUtils.createSubCollection( resultPath.getChildPath( pool.getName() ) );
        this.poolToCollection.put( pool.getName(), poolCollection );
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void stop()
    {
        isAlive = false;
        for( SimulationAgent agent : agents )
            agent.die();
        log.info( "Simulation terminated" );
    }

    @Override
    public SimulatorProfile getProfile()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Options getOptions()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOptions(Options options)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLogLevel(Level level)
    {
        // TODO Auto-generated method stub

    }

}
