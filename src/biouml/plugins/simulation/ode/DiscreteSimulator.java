package biouml.plugins.simulation.ode;

import java.util.Arrays;
import com.developmentontheedge.beans.BeanInfoEx;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

/**
 * Discrete simulator iterating through time 
 * @author axec
 */
public class DiscreteSimulator extends SimulatorSupport
{
    private DiscreteOptions options = (DiscreteOptions)getDefaultOptions();
    private int nextSpanIndex = 1;
    private double t; //current model time value
    private double tFinal; //final time

    private int[] events;
    private double[] x = new double[] {};
    private boolean eventFired = false;

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Discrete";
        info.eventsSupport = true;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new DiscreteOptions();
    }

    @Override
    public DiscreteOptions getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof DiscreteOptions ) )
            throw new IllegalArgumentException( "Only ESOptions are comaptible with EulerSimple Simulator" );
        this.options = (DiscreteOptions)options;
    }

    @Override
    public int[] getEvents()
    {
        return events;
    }

    @Override
    public void init(Model model, double[] initialValues, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception
    {
        odeModel = (OdeModel)model;
        if( !odeModel.isInit() )
            odeModel.init();
        resultListeners = listeners;

        span = tspan;
        t = span.getTimeStart();
        tFinal = span.getTimeFinal();
        x = StdMet.copyArray( initialValues );
        profile.init( x, t );
        nextSpanIndex = 1;
        
        eventsBeforeStep = odeModel.checkEvent( t, x );
        events = new int[eventsBeforeStep.length];
    }

    private double[] eventsBeforeStep;

    @Override
    public boolean doStep() throws Exception
    {

        if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
            return false;

        if( eventFired ) //event fired at the end of previous step
            fireSolutionUpdate( t, new double[] {} );
        eventFired = false;

        double nextSpanPoint = span.getTime( nextSpanIndex );
        nextSpanIndex++;

        t = nextSpanPoint;

        double[] eventsNew = odeModel.checkEvent( t, x );

        for( int i = 0; i < eventsBeforeStep.length; i++ )
        {
            if( eventsNew[i] > eventsBeforeStep[i] )
            {
                events[i] = 1;
                eventFired = true;
            }
            else
                events[i] = -1;
        }
        
        eventsBeforeStep = eventsNew;
        
        if( eventFired )
            return false;
        
        fireSolutionUpdate( t, new double[] {} );

        profile.setTime( t );
        return t < tFinal;
    }

    public static class DiscreteOptions extends Options
    {
    }

    public static class DiscreteOptionsBeanInfo extends BeanInfoEx
    {
        public DiscreteOptionsBeanInfo()
        {
            super( DiscreteOptions.class, true );
        }

        @Override
        public void initProperties() throws Exception
        {
        }
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        odeModel.setCurrentValues( x0 );
        x = Arrays.copyOf( odeModel.getY(), odeModel.getY().length );
    }
}
