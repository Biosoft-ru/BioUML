package biouml.plugins.simulation.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import one.util.streamex.IntStreamEx;

import ru.biosoft.jobcontrol.FunctionJobControl;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.ode.OdeModel;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.simulation.ResultListener;

public class EventLoopSimulator extends SimulatorSupport
{
    private SimulatorSupport solver = new JVodeSolver(); //inner solver to actually perform problem solving   
    private boolean modelHasEvents = true;
    private double curTime;
    private double restTime;
    private double[] x;
    private Uniform uniform = new Uniform( new MersenneTwister( new Date() ) ); //random numbers generator. Used to choose one of several events with the same priority 
    
    public EventLoopSimulator()
    {
        
    }
    
    public EventLoopSimulator(SimulatorSupport solver)
    {
        setSolver(solver);
    }
    
    public Simulator getSolver()
    {
        return solver;
    }
    public void setSolver(Simulator solver)
    {
        this.solver = (SimulatorSupport)solver;
    }
    
    @Override
    public SimulatorInfo getInfo()
    {
        return solver.getInfo();
    }
    
    @Override
    public void setStatisticsMode(String mode)
    {
        solver.setStatisticsMode(mode);
        statisticsMode = mode;
    }


    @Override
    public void setFireInitialValues(boolean val)
    {
        solver.setFireInitialValues(val);
        fireInitialValues = val;
    }

    @Override
    public SimulatorProfile getProfile()
    {
        return solver.getProfile();
    }

    @Override
    public Object getDefaultOptions()
    {
        return solver.getDefaultOptions();
    }

    @Override
    public Options getOptions()
    {
        return solver.getOptions();
    }

    @Override
    public void setOptions(Options options)
    {
        solver.setOptions(options);
    }

    @Override
    public int[] getEvents()
    {
        return solver.getEvents();
    }

    @Override
    public void stop()
    {
        terminated = true;
        solver.stop();
    }

    @Override
    public void init(Model model, double[] initialValues, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception
    {
        this.jobControl = jobControl;
        this.odeModel = (OdeModel)model;
        if( !odeModel.isInit() )
            odeModel.init();
        this.span = tspan;
        this.resultListeners = listeners;

        curTime = odeModel.getTime();
        restTime = span.getTimeFinal() - curTime;
        x = Arrays.copyOf(initialValues, initialValues.length);
        solver.init(model, initialValues, tspan, listeners, jobControl);
        solver.setFireInitialValues( true );

        if( curTime == 0 )
            checkInitialEvents( curTime, initialValues );
    }
    
    @Override
    public void start(Model model, double[] initialValues, Span timeSpan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception
    {
        setStarted();
        super.start(model, initialValues, timeSpan, listeners, jobControl);
        
    }

    @Override
    public boolean doStep() throws Exception
    {
        boolean flag = solver.doStep();

        if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
            return false;

        if( !modelHasEvents )
            return flag;

        this.profile = getProfile();

        if( profile.isUnstable() || profile.isStiff() )
            return false;

        System.arraycopy(profile.getX(), 0, x, 0, x.length);
        curTime = profile.getTime();
        span = solver.getSpan();
        restTime = span.getTimeFinal() - curTime;

        if( restTime <= 0 )
            return false;

        int[] triggeredEvents = solver.getEvents();
        if( triggeredEvents != null )
        {
            //list of events waiting to be executed, it may contain several
            //inclusions of the same event in the case when it was triggered
            //more then once during other events execution
            ArrayList<Integer> pendingEvents = new ArrayList<>();
            for( int i = 0; i < triggeredEvents.length; i++ )
            {
                if( triggeredEvents[i] == 1 )
                    pendingEvents.add(i);
            }
            if( !pendingEvents.isEmpty() )
            {
                executeEvents(pendingEvents);
                recalculateSpan();
                solver.init(odeModel, x, span, resultListeners, jobControl); //TODO: do not init solver, use more simple procedure
                odeModel.updateHistory(curTime);
                solver.setFireInitialValues(false);
                solver.getProfile().setX(x);
            }
            
            if (odeModel.isConstraintViolated())
            {
                log.info("Constraints were violated, simulation halted");
                return false;
            }
        }
        return restTime > 0;
    }
    
    //if we met event than recreate span
    private void recalculateSpan()
    {
        Span newSpan = span.getRestrictedSpan(curTime, span.getTimeFinal());
        span = ( newSpan != null ) ? newSpan : new ArraySpan(curTime, span.getTimeFinal());
    }

    private void checkInitialEvents(double t, double[] x) throws Exception
    {
        double[] initialEvents = odeModel.checkEvent(t, x);

        if( initialEvents == null || initialEvents.length == 0 )
        {
            modelHasEvents = false;
            return;
        }
        modelHasEvents = true;

        List<Integer> pendingEvents = new ArrayList<>();
        for( int i = 0; i < initialEvents.length; i++ )
        {
            if( initialEvents[i] == 1 && !odeModel.getEventsInitialValue(i) )
                pendingEvents.add(i);
        }
        if( !pendingEvents.isEmpty() )
        {
            executeEvents(pendingEvents);
            solver.init(odeModel, this.x, span, resultListeners, jobControl);
            solver.getProfile().setX(this.x);
        }
    }

    /**
     * Method for iterative event executing:<br>
     * 1. pending events are calculated, those are events which are triggered now but were not triggered at the previous step,<br>
     * 2. one event is chosen from pending events according to their priorities,<br>
     * 3. chosen event is fired,<br>
     * 4. if some pending events are not persistent and not triggered any more, they are removed from pending,<br>
     * 5. if some events are now triggered but were not before step 3, they are added to pending,<br>
     * 6. if there are no pending events algorithm is finished,<br>
     * 7. goto 2.
     * @throws Exception
     */
    private void executeEvents(List<Integer> pendingEvents) throws Exception
    {
        ArrayList<Integer> switchedOffEvents = new ArrayList<>();

        while( !pendingEvents.isEmpty() )
        {
            double[] eventsBeforeFiring = odeModel.checkEvent(curTime, x);

            outStatistics("Triggered events: "+ IntStreamEx.of(pendingEvents).joining(","));
            
            //chose event with the most priority
            Integer eventToFire = chooseEvent(pendingEvents);
            outStatistics("Fire event: "+ eventToFire);
            odeModel.processEvent(eventToFire); //firing event

            String message = odeModel.getEventMessage(eventToFire);
            if( message != null )
                log.info("At time " + curTime + " : " + message);

            //dealing with event consequences
            if (odeModel.isConstraintViolated())
                return;

            double[] eventsAfterFiring = odeModel.checkEvent(curTime, x);

            switchedOffEvents.clear();
            for( int i = 0; i < eventsAfterFiring.length; i++ )
            {
                //event is not persistent and it was switched off  by current event
                if( !odeModel.isEventTriggerPersistent(i) && eventsAfterFiring[i] < eventsBeforeFiring[i] )
                {
                    outStatistics("Event was switched off: " + i);
                    switchedOffEvents.add(i);

                }
                //event was triggered by current event
                else if( eventsAfterFiring[i] > eventsBeforeFiring[i] )
                {
                    outStatistics("Event was switched on: "+ i);
                    pendingEvents.add(i);
                }
            }

            for( Integer switchedOff : switchedOffEvents )
                ( (JavaBaseModel)odeModel ).removeDelayedEvent( switchedOff );

            //we should remove all inclusions of triggered off event from pending events
            pendingEvents.removeAll(switchedOffEvents);
            //event should not be executed twice
            pendingEvents.remove(eventToFire);
        }
    }

    /**
     * Choose event to fire basing on their priority
     * If some events have the same priority - chose one of them randomly
     * @param availableEvents - available events (their indexes)
     * @return index of event which should be fired
     */
    private Integer chooseEvent(List<Integer> pendingEvents) throws Exception
    {
        double[] priorities = odeModel.getEventsPriority(curTime, x);

        if( priorities == null )
            return pendingEvents.get(0);

        double maxVal = Double.NEGATIVE_INFINITY;

        for( int i : pendingEvents )
            maxVal = Math.max(maxVal, priorities[i]);
        
        HashSet<Integer> candidadeEvents = new HashSet<>();

        for( int i : pendingEvents )
        {
            if( priorities[i] == maxVal )
                candidadeEvents.add(i);
        }
        return getRandom(candidadeEvents);
    }

    private Integer getRandom(HashSet<Integer> objects)
    {
        int size = objects.size();
        Integer[] array = objects.toArray(new Integer[size]);
        if( size == 1 )
            return array[0];
        Arrays.sort(array);
        int index = uniform.nextIntFromTo(0, objects.size() - 1);
        return array[index];
    }
    
    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        if (!modelHasEvents)
        {
            solver.setInitialValues(x0);
            this.x = odeModel.getY().clone();
            return;    
        }
        double[] before = odeModel.checkEvent( curTime, x );
        solver.setInitialValues( x0 );
        this.x = odeModel.getY().clone();
        double[] after = odeModel.checkEvent( curTime, x );
        executeEvents( curTime, before, curTime, after );
    }


    /**
     * Execute all events that are triggered at time t2, but not triggered at t1 
     */
    private void executeEvents(double t1, double[] e1, double t2, double[] e2) throws Exception
    {
        List<Integer> pendingEvents = new ArrayList<>();
        for( int i = 0; i < e1.length; i++ )
        {
            if( e2[i] == 1 && e1[i] != 1 )
                pendingEvents.add( i );
        }
        if( !pendingEvents.isEmpty() )
        {
            executeEvents( pendingEvents );
            recalculateSpan();
            solver.init( odeModel, x, span, resultListeners, jobControl ); //TODO: do not init solver, use more simple procedure
            odeModel.updateHistory( curTime );
            solver.setFireInitialValues( false );
            solver.getProfile().setX( x );
        }
    }
    
    @Override
    public void setStarted()
    {
        solver.setStarted();
        terminated = false;
    }
    
    @Override
    public void setPresimulateFastReactions(boolean preprocess)
    {
        super.setPresimulateFastReactions(preprocess);
        solver.setPresimulateFastReactions(preprocess);
    }

}