package biouml.plugins.agentmodeling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulationEngineLogger;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 * @author axec
 * Scheduler maintenance agent model simulation.
 */
public class PopulationScheduler implements Simulator
{
    private Logger log = Logger.getLogger( Scheduler.class.getName() );

    private AgentBasedModel model;

    private boolean debugMode = false;

    private FunctionJobControl jobControl;

    protected ArrayList<SimulationAgent> agents = new ArrayList<>();
    protected Queue<SimulationAgent> aliveAgents = new ConcurrentLinkedDeque<>();

    private boolean isAlive = true;

    protected Span span;

    private List<StatCollector> collectors = new ArrayList<>();

    private static final double SPAN_STEP_ERROR = 1E-9;

    private final SimulatorProfile profile = new SimulatorProfile();

    private boolean saveResult = true;
    //    private Simulator simulator;
    private List<Simulator> simulators;
    private int threads = 4;
    ExecutorService executor;

    public PopulationScheduler()
    {

    }

    public PopulationScheduler(Simulator simulator, int threads) throws Exception
    {
        //        this.simulator = simulator;
        this.simulators = new ArrayList<>();
        for( int i = 0; i < threads; i++ )
            simulators.add( (Simulator)simulator.getClass().newInstance() );
    }

    /**
     * Time frame at which scheduler remembers messages from agents.
     * Must be equal to the smallest previousTime of all Agents.
     * All messages which finishTime is smaller than memorizedTime are deleted from history due to space economy
     * 
     * @see SimulationAgent.previousTime
     * @see Message
     */
    public double memorizedTime;

    private int nextIndex = 1;

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( ! ( model instanceof AgentBasedModel ) )
            throw new IllegalArgumentException(
                    "Illegal model type" + model.getClass() + ". Only AgentBasedModel allowed for this type of simulator." );
        this.model = (AgentBasedModel)model;

        if( !model.isInit() )
            this.model.init();

        this.agents = new ArrayList<>( this.model.getAgents() );
        aliveAgents = new ConcurrentLinkedDeque<>( agents );
        this.span = tspan;
        this.isAlive = true;
        this.nextIndex = 1;
        this.memorizedTime = 0;
        this.profile.init( x0, span.getTimeStart() );
        executor = (ExecutorService)Executors.newFixedThreadPool( threads );
        //        if( tspan.getLength() < AgentModelSimulationEngine.SPAN_LENGTH_THRESHOLD )
        if( saveResult )
            addStatisticsCollector( new ResultCollector( listeners, tspan, this.model ) );

        for( StatCollector collector : collectors )
            collector.init( span, this.model );

        this.jobControl = jobControl;

        for( SimulationAgent agent : agents )
            agent.init();

        for( SimulationAgent agent : agents )
        {
            if( ! ( agent instanceof SteadyStateAgent ) || ( (SteadyStateAgent)agent ).isStandard() )
            {
                sendMessagesFromAgent( agent );
                agent.applyChanges();
                updateMessagesFromAgent( agent );
            }
        }

        for( SimulationAgent agent : agents )
        {
            if( ( agent instanceof SteadyStateAgent ) && ! ( (SteadyStateAgent)agent ).isStandard() )
            {
                sendMessagesToAgent( agent );
                agent.setUpdated();
                sendMessagesFromAgent( agent );
                updateMessagesFromAgent( agent );
            }
        }

        for( SimulationAgent agent : agents )
        {
            if( ! ( agent instanceof SteadyStateAgent ) || ( (SteadyStateAgent)agent ).isStandard() )
            {
                sendMessagesToAgent( agent );
                agent.applyChanges();
                agent.setUpdated();
            }
        }

        for( StatCollector collector : collectors )
            collector.update( span.getTimeStart() );
    }

    @Override
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        init( model, null, tspan, listeners, jobControl );

        //        while( doStep() )
        //            ;

        while( doStepParallel() )
            ;

        for( StatCollector collector : collectors )
            collector.finish();

        for( SimulationAgent agent : agents )
            agent.die();

        clearStatisticsCollector();

        agents.clear();
    }



    public boolean doStepParallel() throws Exception
    {
        if( !isAlive || ( jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST ) )
            return false;

        if( nextIndex >= span.getLength() )
            return false;

        double nextSpanTime = span.getTime( nextIndex );

        List<StepTask> tasks = new ArrayList<>();
        for( int i = 0; i < simulators.size(); i++ )
        {
            tasks.add( new StepTask( simulators.get( i ), nextSpanTime ) );
        }

        executor.invokeAll( tasks );

        profile.setTime( nextSpanTime );
        nextIndex++;

        if( !isAlive )
            return false;

        //step is finished - need to update values for all agents
        for( SimulationAgent agent : agents )
        {
            sendMessagesToAgent( agent );
            agent.setUpdated();
        }

        this.aliveAgents = new ConcurrentLinkedDeque<>( agents );

        //step is finished - notify listeners
        for( StatCollector collector : collectors )
            collector.update( nextSpanTime );
        System.out.println( SimulationEngineLogger.getCurrentTime() + " " + profile.getTime() );
        return !aliveAgents.isEmpty() && isAlive;
    }

    private class StepTask extends Task
    {
        protected String error;
        protected boolean finished = false;
        protected double timeLimit;
        protected boolean isAlive = true;
        protected Simulator simulator;

        public StepTask(Simulator simulator, double timeLimit)
        {
            this.timeLimit = timeLimit;
            this.simulator = simulator;
        }

        @Override
        public Void call() throws Exception
        {
            SimulationAgent currentAgent = getNextAgent();

            while( currentAgent != null )
            {
                if( currentAgent.getScaledCurrentTime() >= timeLimit ) //agent with minimum time is ahead of span point - step is done
                    break;

                //                memorizedTime = currentAgent.getScaledPreviousTime();

                if( !currentAgent.isAlive )
                    finilize( currentAgent );
                else //agent step is here
                {
                    if( Math.abs( currentAgent.getScaledCurrentTime() - span.getTime( nextIndex - 1 ) ) > SPAN_STEP_ERROR )
                    {
                        sendMessagesToAgent( currentAgent );
                        currentAgent.setUpdated();
                    }
                    execute( currentAgent, simulator );
                }

                if( currentAgent.getErrorMessage() != null )
                {
                    this.error = "Agent " + currentAgent.getName() + " failed: " + currentAgent.getErrorMessage() + " at time "
                            + currentAgent.getScaledCurrentTime();
                    System.out.println( error );
                    isAlive = false;
                    break;
                }

                currentAgent = getNextAgent();
            }
            return null;
        }
    }

    @Override
    public boolean doStep() throws Exception
    {
        if( !isAlive || ( jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST ) )
            return false;

        if( nextIndex >= span.getLength() )
            return false;

        double nextSpanTime = span.getTime( nextIndex );

        SimulationAgent currentAgent = getNextAgent();

        if( currentAgent == null )
            return false;

        while( currentAgent != null )
        {
            if( currentAgent.getScaledCurrentTime() >= nextSpanTime ) //agent with minimum time is ahead of span point - step is done
                break;

            memorizedTime = currentAgent.getScaledPreviousTime();

            if( !currentAgent.isAlive )
                finilize( currentAgent );
            else //agent step is here
            {
                if( Math.abs( currentAgent.getScaledCurrentTime() - span.getTime( nextIndex - 1 ) ) > SPAN_STEP_ERROR )
                {
                    sendMessagesToAgent( currentAgent );
                    currentAgent.setUpdated();
                }
                execute( currentAgent, simulators.get( 0 ) );
                //                execute( currentAgent, simulator );
            }

            if( currentAgent.getErrorMessage() != null )
            {
                String error = "Agent " + currentAgent.getName() + " failed: " + currentAgent.getErrorMessage() + " at time "
                        + currentAgent.getScaledCurrentTime();
                this.getProfile().setErrorMessage( error );
                System.out.println( error );
                isAlive = false;
                break;
            }

            currentAgent = getNextAgent();
        }

        profile.setTime( nextSpanTime );
        nextIndex++;

        if( !isAlive )
            return false;

        //step is finished - need to update values for all agents
        for( SimulationAgent agent : agents )
        {
            sendMessagesToAgent( agent );
            agent.setUpdated();
        }

        this.aliveAgents = new ConcurrentLinkedDeque<>( agents );

        //step is finished - notify listeners
        for( StatCollector collector : collectors )
            collector.update( nextSpanTime );
        System.out.println( SimulationEngineLogger.getCurrentTime() + " " + profile.getTime() );
        return !aliveAgents.isEmpty() && isAlive;
    }

    public void setSaveResult(boolean saveResult)
    {
        this.saveResult = saveResult;
    }

    public void finilize(SimulationAgent agent) throws Exception
    {
        //agent already finished its work, but we need to update it in the end to take into account influence of other agents
        sendMessagesToAgent( agent );
        agent.setUpdated();
        sendMessagesFromAgent( agent );
        updateMessagesFromAgent( agent );

        //        log.info( agent.getScaledCurrentTime() + " Agent " + agent.getName() + " died" );
        if( debugMode )
            System.out.println( "Agent " + agent.getName() + " died, inner scaled time:" + agent.getScaledCurrentTime() );
        agent.die();

        aliveAgents.remove( agent );
    }

    public void setDebugMode(boolean mode)
    {
        debugMode = mode;
    }

    public SimulationAgent getNextAgent()
    {
        return aliveAgents.poll();//Collections.max( aliveAgents );
    }


    private void sendMessagesFromAgent(SimulationAgent agent) throws Exception
    {
        HashSet<Link> links = model.agentToOutputLinks.get( agent );
        if( links == null )
            return;
        for( Link link : links )
            link.receiveMessage();
    }

    private void updateMessagesFromAgent(SimulationAgent agent) throws Exception
    {
        HashSet<Link> links = model.agentToOutputLinks.get( agent );
        if( links == null )
            return;
        for( Link link : links )
            link.updateMessage();
    }

    public void sendMessagesToAgent(SimulationAgent receiver) throws Exception
    {
        HashSet<Link> nameToLink = model.agentToInputLinks.get( receiver );
        if( nameToLink == null )
            return;
        for( Link link : nameToLink )
        {
            if( link.getSender().isAlive( receiver.getScaledCurrentTime() ) )
            {
                link.checkOutdatedMessages( memorizedTime );
                link.sendMessage();
            }
        }

    }

    public List<SimulationAgent> getAgents()
    {
        return agents;
    }

    public void addStatisticsCollector(StatCollector collector)
    {
        collectors.add( collector );
    }

    public void clearStatisticsCollector()
    {
        collectors.clear();
    }

    private void execute(SimulationAgent agent, Simulator simulator) throws Exception
    {
        if( agent instanceof SteadyStateAgent && ! ( (SteadyStateAgent)agent ).isStandard() )
        {
            executeSteadyState( (SteadyStateAgent)agent );
            return;
        }
        sendMessagesFromAgent( agent );

        //do agent routine...

        agent.setupPreviousTime();

        if( !agent.isAlive )
            return;

        if( simulator != null )
            agent.iterate( simulator );
        else
            agent.iterate();

        //        if( agent.shouldDivide() )
        //        {
        //            SimulationAgent[] newAgents = agent.divide( );
        //            if( newAgents != null )
        //            {
        //                for( SimulationAgent newAgent : newAgents )
        //                {
        //                    model.addAgent( newAgent );
        //                    agents.add( newAgent );
        //                    aliveAgents.add( newAgent );
        //
        //                    if( model.agentToOutputLinks.containsKey( agent ) )
        //                        for( Link link : model.agentToOutputLinks.get( agent ) )
        //                        model.addUndirectedLink( newAgent, link.nameAtSender, link.reciever, link.nameAtReciever );
        //                    
        //                    if( debugMode )
        //                        System.out.println( "Agent " + agent.getName() + " produced " + newAgents.length + agents + ", time = \t"
        //                                + agent.getScaledCurrentTime() + "\t N = " + agents.size() );
        //                }
        //            }
        //        }

        updateMessagesFromAgent( agent );
    }


    private void executeSteadyState(SteadyStateAgent agent) throws Exception
    {
        sendMessagesToAgent( agent );
        agent.iterate();
        sendMessagesFromAgent( agent );
        updateMessagesFromAgent( agent );
    }


    public void checkAlive(SimulationAgent agent)
    {
        if( !agent.isAlive )
        {
            log.info( "Agent " + agent.getName() + " died, inner scaled time:" + agent.getScaledCurrentTime() );
            if( debugMode )
                System.out.println( "Agent " + agent.getName() + " died, inner scaled time:" + agent.getScaledCurrentTime() );
            agent.die();

            aliveAgents.remove( agent );
        }
        //if all agents died then stop
        if( aliveAgents.isEmpty() )
            isAlive = false;
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
        return profile;
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
    public void stop()
    {
        isAlive = false;
        for( SimulationAgent agent : agents )
            agent.die();
        log.info( "Simulation terminated" );
    }

    protected void setSpan(Span span)
    {
        this.span = span;
    }

    @Override
    public void setLogLevel(Level level)
    {
        log.setLevel( level );
    }

    public class Task implements Callable<Void>
    {

        @Override
        public Void call() throws Exception
        {
            return null;
        }

    }
}
