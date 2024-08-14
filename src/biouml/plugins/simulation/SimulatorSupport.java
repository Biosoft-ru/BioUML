package biouml.plugins.simulation;

import java.util.logging.Level;
import java.util.stream.DoubleStream;

import java.util.logging.Logger;

import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.ode.OdeModel;
import biouml.standard.simulation.ResultListener;

import ru.biosoft.jobcontrol.FunctionJobControl;

public abstract class SimulatorSupport implements Simulator
{
    protected ResultListener[] resultListeners = null;
    protected OdeModel odeModel;
    protected static final Logger log = Logger.getLogger(SimulatorSupport.class.getName());
    protected SimulatorProfile profile = new SimulatorProfile();
    protected boolean debug = false;
    protected FunctionJobControl jobControl;
    protected Span span;
    protected String statisticsMode = OdeSimulatorOptions.STATISTICS_INTERMEDIATE;
    protected boolean locateEvents;// whether to locate events or not

    protected boolean terminated; // termination switch
    protected boolean fireInitialValues = true;
    protected boolean eventAtSpanPoint = false;
    protected boolean preprocessFastReactions = true;
    
    public void setStarted()
    {
        terminated = false;
    }

    public void setFireInitialValues(boolean val)
    {
        fireInitialValues = val;
    }

    public void setStatisticsMode(String mode)
    {
        statisticsMode = mode;
    }

    public SimulatorSupport()
    {
    }

    public SimulatorSupport(OdeModel odeModel, ResultListener[] resultListeners) throws Exception
    {
        this.resultListeners = resultListeners;
        this.odeModel = odeModel;
        try
        {
            this.profile.setX(odeModel.getInitialValues());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get initial values", e);
        }
    }

    public Span getSpan()
    {
        return span;
    }

    protected void fireSolutionUpdate(double t, double[] x) throws Exception
    {
        if( odeModel != null )
        {
            double[] y = odeModel.extendResult(t, x.clone());

            odeModel.updateHistory(t);

            if( resultListeners != null )
            {
                for( int i = 0; i < resultListeners.length; i++ )
                    resultListeners[i].add(t, y);
            }
        }
    }

    @Override
    public SimulatorProfile getProfile()
    {
        return profile;
    }

    @Override
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( ! model.isInit() )
            model.init(); //need to init the model for correct initial values getting

        start(model, model.getInitialValues(), tspan, listeners, jobControl);
    }

    public void start(Model model, double[] initialValues, Span timeSpan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception
    {
        this.init(model, initialValues, timeSpan, listeners, jobControl);
        if( jobControl != null )
            jobControl.functionStarted();

       setStarted();

        while( doStep() )
            ;

        if( jobControl != null )
            jobControl.functionFinished();
    }


    @Override
    public void stop()
    {
        if (terminated)
            return;
        if( jobControl != null )
            jobControl.terminate();
        terminated = true;
    }

    /**
     * Inner method for numerical integration: calculating of new x value basing on old value, old time point, step size
     * Is used by RootFinder. If simulator detect events by itself it may ignore this method
     * @param time
     * @param x
     * @param step
     * @return
     */
    public void integrationStep(double[] xNew, double[] xOld, double tOld, double step, double theta) throws Exception
    {
    }

    /**
     * Method for out of errors during simulation
     * @param messages
     */
    protected void outError(String message)
    {
        log.log(Level.SEVERE, message);
        System.out.println(message);
        System.out.println();
    }

    /**
     * Method for out of detailed statistics during simulation
     * @param messages
     */
    protected void outStatistics(String ... messages)
    {
        if( !statisticsMode.equals(OdeSimulatorOptions.STATISTICS_ON) )
            return;
        for( String message : messages )
        {
            log.info(message);
            System.out.println(message);
        }
        System.out.println();
    }

    /**
     * Method for out of several most important statistics during simulation
     * @param messages
     */
    protected void outIntermediate(String ... messages)
    {
        if( statisticsMode.equals(OdeSimulatorOptions.STATISTICS_OFF) )
            return;
        for( String message : messages )
        {
            log.info(message);
            System.out.println(message);
        }
        System.out.println();
    }
    
    public static boolean checkNaNs(double[] x)
    {
        return DoubleStream.of(x).anyMatch(v->Double.isNaN(v) || Double.isInfinite(v));
    }
    
    public static double[] getNaNs(double[] x)
    {
        return DoubleStream.of(x).filter(v->Double.isNaN(v) || Double.isInfinite(v)).toArray();
    }

    @Override
    public boolean doStep() throws Exception
    {
        throw new UnsupportedOperationException("do Step is not supported for solver: " + getClass().getName());
    }
    
    public abstract int[] getEvents();
    
    public void setPresimulateFastReactions(boolean preprocess)
    {
        preprocessFastReactions = preprocess;
    }

    protected double[] preprocessFastReactions() throws Exception
    {
        OdeModel tempModel = (OdeModel)odeModel.clone(); //TODO: check thorough cloning
        double[] y = tempModel.getY();
        double t = tempModel.getTime();
        double h = 1; //TODO: make a parameter
        SimulatorSupport simulator = getClass().newInstance();
        ((JavaBaseModel)tempModel).setFastRegime(true);
        simulator.setPresimulateFastReactions(false);
        Span span = new UniformSpan(t, t + h, h);
        simulator.start(tempModel, y, span, null, null);
        ((JavaBaseModel)tempModel).setFastRegime(false);
        return tempModel.getY();
    }

    @Override
    public void setLogLevel(Level level)
    {
        log.setLevel( level );
    }
}
