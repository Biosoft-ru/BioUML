package biouml.plugins.agentmodeling;

import java.util.logging.Level;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.standard.simulation.ResultListener;

/**
 * Steady state agent - agent which performs search for steady state at each time point of span
 * For now steady state is reached by naive method - simulating of inner model until predefined time point <b>timeBeforeSteadyState</b> with step <b>timeStep</b>
 * Note: <b>timeBeforeSteadyState</b> should not be confused with actual model time of the agent, it is used only as a tool for reaching steady state!
 * Two main methods of Agent are 
 * <ul>
 * <li>iterate - which usually is used to perform standard routine of the agent 
 * <li>setUpdated - which is used to apply changes made by other agents
 * </ul> 
 * In this agent <b>iterate</b> method only advance time to the next span point while most of the work is done by <b>setUpdated</b> method.
 * This is because when we advance time or change variable values by external agents we should reach steady state anew. 
 * This will be done at the end of agent simulation time step i.e. after all other agents performed their steps and all messages were passed to this agent.
 * 
 *  Thus simulation step from t_i to t_(i+1) is done as follows:
 *  <ol>
 *  <li> apply changes made by other agents at time t_i to variables of this agent
 *  <li> reach steady state at time point t = 0 (by simulating inner model from t_inner = 0 to t_inner = <b>timeBeforeSteadyState</b>)
 *  <li> advance time to t_(i+1)
 *  <li> apply changes made by other agents at time t_(i+1) to variables of this agent
 *  <li> reach steady state at time point t = 0 (by simulating inner model from t_inner = 0 to t_inner = <b>timeBeforeSteadyState</b>)
 *  <li> and so on...
 *  </ol>
 *  
 *  This agent may change its behavior to standard behavior of ModelAgent in order to perform control step. 
 *  This may be needed if steady state behavior is performed using very large time step and we need to check how agent is working on finer time span
 *  when time reaches <b>timeControlStart</b> it will simulate the rest of time span with step <b>timeControlStep</b> doing steps as usual ModelAgent
 *  <br><br>
 *  <b>Important!</b> Current implementation does not support models with explicit time variable in equations or events! 
 *  To proper implement this it is necessary to replace time by some special variable TIME_REPLACED and at each time point set its valut to current 
 *  time point value. 
 * @author Ilya
 *
 */
public class SteadyStateAgent extends ModelAgent
{
    private boolean isStandard = false;
    private double timeBeforeSteadyState = 100;
    private double timeStep = 1;
    private double timeControlStart = 100;
    private double timeControlStep = 1;
    private double[] x;

    /** Inner span to simulate until steady state*/
    private Span steadySpan;
    
    public SteadyStateAgent(SimulationEngine engine, String name) throws Exception
    {
        super( engine, name );
        x = model.getInitialValues();
    }

    public SteadyStateAgent(Model model, Simulator simulator, Span span, String name) throws Exception
    {
        super( model, simulator, span, name );
        x = model.getInitialValues();
    }

    @Override
    public void init() throws Exception
    {
        super.init();
        nextSpanIndex = 1;
        x = model.getInitialValues();
        steadySpan = new UniformSpan( 0, timeBeforeSteadyState, timeStep );
    }
    
    @Override
    public void applyChanges() throws Exception
    {
        simulator.setInitialValues( currentValues );
        findSteadyState();
        currentValues = model.getCurrentValues();
    }
    
    /**
     * Finds steady state at current time point
     */
    public void findSteadyState() throws Exception
    {
        if( updatedFromOutside )
            simulator.setInitialValues( currentValues );
   
        if( model instanceof JavaBaseModel ) //TODO: maybe create method in the model to reset time?
            ( (JavaBaseModel)model ).time = 0;

        ( (SimulatorSupport)simulator ).start( model, x, steadySpan, new ResultListener[] {}, null );
        x = simulator.getProfile().getX(); //store new initial values; TODO: reset model inner time
        currentValues = model.getCurrentValues();
        if( currentTime == 0 )
            model.setCurrentValues( currentValues );

        updatedFromOutside = false;
    }

    @Override
    public void iterate()
    {
        if( isStandard )
        {
            super.iterate();
            return;
        }

        try
        {
            if( nextSpanIndex >= span.getLength() )
                isAlive = false;

            if( !isAlive )
                return;

            if( recalculateStepIndex != -1 )
                recalculateSpan();
            
//            findSteadyState();
            previousTime = currentTime;
            currentTime = span.getTime( nextSpanIndex++ );
//                            System.out.println( "Agent Step" + getName() + " from "+getScaledPreviousTime()+" to "+getScaledCurrentTime());

            if( currentTime >= timeControlStart )
            {
                isStandard = true;
                currentTime = timeControlStart;
                this.span = new UniformSpan( currentTime, this.span.getTimeFinal(), timeControlStep );
                this.simulator.init( model, x, this.span, new ResultListener[] {}, null );
                nextSpanIndex = 1;
                isAlive = currentTime < this.span.getTimeFinal(); //it is possible that we rolled back a little           
            }
            isAlive = nextSpanIndex < span.getLength();

        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, ex.getMessage() );
            currentTime = Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public double getPriority()
    {
        return STEADYSTATE_AGENT_PRIORITY;
    }

    /**
     * If true then this agent will behave as usual ModelAgent
     */
    public boolean isStandard()
    {
        return isStandard;
    }

    public void setTimeBeforeSteadyState(double timeBeforeSteadyState)
    {
        this.timeBeforeSteadyState = timeBeforeSteadyState;
    }

    public void setTimeStep(double timeStep)
    {
        this.timeStep = timeStep;
    }

    public double getTimeControlStep()
    {
        return timeControlStep;
    }
    public void setTimeControlStep(double timeControlStep)
    {
        this.timeControlStep = timeControlStep;
    }

    public double getTimeControlStart()
    {
        return timeControlStart;
    }
    public void setTimeControlStart(double timeControlStart)
    {
        this.timeControlStart = timeControlStart;
    }
}