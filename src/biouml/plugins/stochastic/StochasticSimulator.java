package biouml.plugins.stochastic;

import java.util.Arrays;
import java.util.Date;

import java.util.logging.Logger;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.ode.OdeModel;
import biouml.standard.simulation.ResultListener;

/**
 * Base interface for stochastic solver
 */
public abstract class StochasticSimulator extends SimulatorSupport
{
    protected final static int SUCCESS = 0;
    protected final static int STEP_LIMIT = 2;
    protected final static int CONTINUE = 3;
    protected final static int EVENT_OCCURED = 1;
    protected final static int TERMINATED = -1;
    protected int n;
    protected int nextSpanIndex = 0;
    protected double nextEventTime = Double.POSITIVE_INFINITY;
    protected int nextEvent = -1;
    protected int spanLength;
    protected Options options = (Options)getDefaultOptions();
    protected double[] x; //inner x value
    protected double[] xOld; //simulation result x value
    protected double time;
    protected boolean running = true;
    protected int[] eventInfo;
    protected static final Logger log = Logger.getLogger(SimulatorSupport.class.getName());
    protected Stochastic stochastic = new Stochastic(new Date());
    protected StochasticModel model;

    protected boolean modelHasEvents = false;
    
    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( ! ( model instanceof StochasticModel ) )
            throw new IllegalArgumentException("Wrong model class" + model.getClass() + ". Only StochasticModel class allowed for solver "
                    + this.getClass());
        this.odeModel = (OdeModel)model;
        if( !odeModel.isInit() )
            odeModel.init();
        this.model = (StochasticModel)model;
        this.span = tspan;
        this.x = x0.clone();
        this.xOld = x0.clone();
        this.n = x0.length;
        this.time = span.getTimeStart();
        spanLength = span.getLength();
        this.running = true;
        this.profile = new SimulatorProfile();
        this.profile.init(x,time);
        this.nextSpanIndex = 1;
        this.resultListeners = listeners;
        double[] eventsArray = this.model.checkEvent(span.getTimeStart(), x0);
        modelHasEvents = ( eventsArray.length != 0 );
        eventInfo = new int[eventsArray.length];
        this.fireInitialValues = true;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    @Override
    public boolean doStep() throws Exception
    {
        if( fireInitialValues || eventAtSpanPoint )
            fireSolutionUpdate(span.getTimeStart(), x);
        
        if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
            return false;
        
        fireInitialValues = false;
        try
        {
            double nextTime = span.getTime(nextSpanIndex);
            int flag = routine(nextTime);

            if( flag == SUCCESS )
            {
                fireSolutionUpdate(nextTime, xOld);
                nextSpanIndex++;
                return nextSpanIndex < span.getLength();
            }
            else if( flag == EVENT_OCCURED )
            {
                return false;
            }
        }
        catch( Exception ex )
        {
            if( jobControl != null )
                jobControl.terminate();
            ex.printStackTrace();
            profile.setErrorMessage(ex.getLocalizedMessage());
            profile.setUnstable(true);
            log.info(ex.getMessage());
            return false;
        }
        return false;
    }


    @Override
    public int[] getEvents()
    {
        return eventInfo;
    }

    @Override
    public Options getOptions()
    {
        return options;
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        model.setCurrentValues(x0);
        this.x = Arrays.copyOf(model.getY(), model.getY().length);
    }


    @Override
    public void stop()
    {
        running = false;
    }

    /**
     * Time stepping main method to be defined in each particular stochastic solver
     * @param timeLimit finish time
     * @return success flag
     * @throws Exception
     */
    public int routine(double timeLimit) throws Exception
    {
        return SUCCESS;
    }

    private int eventLocationIter = 40;

    protected int checkEvent(double[] eventsOnStart, double startTime, double finishTime, double timeLimit) throws Exception
    {
        if( !modelHasEvents )
            return CONTINUE;

        double[] eventsOnFinish = odeModel.checkEvent(finishTime, x);

        if( !compare(eventsOnFinish, eventsOnStart) )
            return CONTINUE;

        double eventTime = locateEvent(startTime, finishTime, xOld, x);

        eventAtSpanPoint = Math.abs(eventTime - timeLimit) < 1E-10;

        if( eventTime < timeLimit || eventAtSpanPoint )
        {
            profile.setX(eventAtSpanPoint ? x : xOld);
            profile.setTime(eventAtSpanPoint ? timeLimit : eventTime);
            for( int j = 0; j < eventsOnFinish.length; j++ )
            {
                eventInfo[j] = (int)eventsOnFinish[j];
            }
            return EVENT_OCCURED;
        }
        //event detected but later than time limit
        else
        {
            x = xOld.clone();
            time = startTime;
            profile.setX(xOld);
            profile.setTime(timeLimit);
            fireSolutionUpdate(time, xOld);
            nextSpanIndex++;
            return SUCCESS;//return TERMINATED;
        }
    }

    /**
     * Method for event time point locating, it is based on the assumption that x is not changing at time interval [start, finish)
     * @param start
     * @param finish
     * @param x
     * @return
     * @throws Exception
     */
    protected double locateEvent(double start, double finish, double[] startX, double[] finishX) throws Exception
    {
        double[] events = odeModel.checkEvent(finish, finishX);
           
        double middle = 0;
        for( int i = 0; i < eventLocationIter; i++ )
        {
            middle = ( start + finish ) / 2;

            double[] middleEvents = odeModel.checkEvent(middle, startX);

            if( compare(events, middleEvents) )
            {
                start = middle;
            }
            else
            {
                finish = middle;
                events = middleEvents;
                System.arraycopy(startX, 0, finishX, 0, finishX.length);
            }
        }
        return finish;
    }
    
    protected boolean compare(double[] ev1, double[] ev2)
    {
        for( int i = 0; i < ev1.length; i++ )
        {
            if( ev1[i] > ev2[i] )
                return true;
        }
        return false;
    }

    public void setSeed(int seed)
    {
        stochastic.setSeed( seed );
    }

    public void setSeed(Date seed)
    {
        stochastic.setSeed( seed );
    }
}
