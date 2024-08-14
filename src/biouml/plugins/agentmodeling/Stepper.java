package biouml.plugins.agentmodeling;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.agentmodeling.covid19.AgentPopulationModel;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 * @author axec
 * Simple Scheduler
 */
public class Stepper implements Simulator
{
    private Logger log = Logger.getLogger( Scheduler.class.getName() );

    private AgentPopulationModel model;

    protected List<SimulationAgent> agents = new ArrayList<>(); //the same array as in model
    protected SimulationAgent controllerAgent; //additional agents TODO: maybe move this to model as well
    protected List<SimulationAgent> observerAgents = new ArrayList<>(); //additional agents TODO: maybe move this to model as well
    protected List<SimulationAgent> pendingAgents; //agents created on current step that will be added to the model
    protected List<SimulationAgent> diedAgents;

    ResultListener[] listeners;

    protected double currentTime = 0;
    protected double completionTime = 100;
    protected double timeIncrement = 1;

    private List<StatCollector> collectors = new ArrayList<>();
    private boolean isAlive;
    
    public void addObserverAgent(SimulationAgent agent)
    {
        this.observerAgents.add( agent );
    }
    
    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( ! ( model instanceof AgentPopulationModel ) )
            throw new IllegalArgumentException(
                    "Illegal model type" + model.getClass() + ". Only AgentPopulationModel allowed for this type of simulator." );
        this.model = (AgentPopulationModel)model;
        this.listeners = listeners;
        if( !model.isInit() )
            this.model.init();
        this.agents = this.model.getAgents();//.getAnew ArrayList<>( );  
        this.observerAgents = this.model.getObserverAgents();
        this.controllerAgent = this.model.getControllerAgent( );              
        this.pendingAgents = new ArrayList<>();
        this.diedAgents = new ArrayList<>();
        currentTime = tspan.getTimeStart();
        this.completionTime = tspan.getTimeFinal();
        this.timeIncrement = tspan.getTime( 1 );
        this.isAlive = true;

        for( StatCollector collector : collectors )
            collector.init( tspan, this.model );

        for( StatCollector collector : collectors )
            collector.update( tspan.getTimeStart() );
        
        for( int i = 0; i < listeners.length; i++ )
            listeners[i].add( currentTime, model.getCurrentValues() );
    }

    @Override
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        init( model, null, tspan, listeners, jobControl );

        while( doStep() )
            ;

        for( StatCollector collector : collectors )
            collector.finish();

        for( SimulationAgent agent : agents )
            agent.die();

        clearStatisticsCollector();

        agents.clear();
    }

    @Override
    public boolean doStep() throws Exception
    {
        if( currentTime >= completionTime || !isAlive )
            return false;

        currentTime += timeIncrement;

        //perform step for all additional agents
       if (controllerAgent != null)
            execute( controllerAgent );
       
        //prepare new step in model
        model.initNewStep();       
            
        //perform step for all usual agents
        for( int i = 0; i < agents.size(); i++ )
            execute( agents.get( i ) );

        //add created agents
        agents.addAll( pendingAgents );
        pendingAgents.clear();
        
        //perform step for all additional agents
        for( int i = 0; i < observerAgents.size(); i++ )
            execute( observerAgents.get( i ) );
        
        //update model values from agents
        model.updateValues( currentTime );
        
        //step is finished - notify listeners
        for( StatCollector collector : collectors )
            collector.update( currentTime );

        if( listeners != null )
        {
            for( int i = 0; i < listeners.length; i++ )
                listeners[i].add( currentTime, model.getCurrentValues() );
        }

        return currentTime < completionTime && isAlive;
    }

    public void addStatisticsCollector(StatCollector collector)
    {
        collectors.add( collector );
    }

    public void clearStatisticsCollector()
    {
        collectors.clear();
    }

    private void execute(SimulationAgent agent) throws Exception
    {
        if( !agent.isAlive )
            return;

        agent.iterate();

        if( agent.shouldDivide() )
        {
            SimulationAgent[] newAgents = agent.divide();
            if( newAgents != null )
            {
                for( SimulationAgent newAgent : newAgents )
                    pendingAgents.add( newAgent );
            }
        }

        //        if( !agent.isAlive )
        //        {
        //            diedAgents.add( agent );
        //            return;
        //        }
        //        
        for( StatCollector collector : collectors )
            collector.update( currentTime, agent );
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
    public Object getDefaultOptions()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SimulatorInfo getInfo()
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
    public SimulatorProfile getProfile()
    {
        return null;
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        this.model.setCurrentValues( x0 ); // TODO Auto-generated method stub

    }

    @Override
    public void setOptions(Options options)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void setLogLevel(Level level)
    {
        log.setLevel( level );
    }
}