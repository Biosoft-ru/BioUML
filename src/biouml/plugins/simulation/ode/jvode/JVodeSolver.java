package biouml.plugins.simulation.ode.jvode;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;

import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.ode.OdeModel;
import biouml.standard.simulation.ResultListener;

/**
 *    @author axec
 */
public class JVodeSolver extends SimulatorSupport
{
    private int nextIndex;
    private JVode jvode;

    @Override
    public void stop()
    {
        super.stop();
        if( jvode != null )
            jvode.stop();
    }

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "JVode";
        info.eventsSupport = true;
        return info;
    }

    @Override
    public JVodeOptions getDefaultOptions()
    {
        return new JVodeOptions();
    }

    JVodeOptions options = getDefaultOptions();

    @Override
    public JVodeOptions getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        this.options = (JVodeOptions)options;
    }

    @Override
    public int[] getEvents()
    {
        return jvode.getEventInfo();
    }

    @Override
    public void init(Model model, double[] u0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( ! ( model instanceof OdeModel ) )
            throw new Exception("JVode solver can be used only for OdeModels");

        span = tspan;
        nextIndex = 1;
        resultListeners = listeners;
        odeModel = (OdeModel)model;
        double time = odeModel.getTime();
        for( int i = 0; i < tspan.getLength(); i++ )
        {
            if( time < tspan.getTime( i ) )
            {
                nextIndex = i;
                break;
            }
        }
        if( odeModel.hasFastOde() && preprocessFastReactions )
            u0 = preprocessFastReactions();

        profile.init( u0, time );//span.getTimeStart());
    
        jvode = JVode.createJVode( options, time, u0, odeModel );//tspan.getTimeStart(), u0, odeModel);
        jvode.setMaxSteps(options.getStepsLimit());
        jvode.setTolerances(options.getRtol(), options.getAtol());
        jvode.setHMin(options.getHMin());
        jvode.setHMaxInv(options.getHMaxInv());
        jvode.setDetectIncorrectNumbers(options.isDetectIncorrectNumbers());
    }

    @Override
    public boolean doStep()
    {
        try
        {            
            if( fireInitialValues || eventAtSpanPoint )
                fireSolutionUpdate(span.getTime(nextIndex - 1), jvode.getY());

            fireInitialValues = false;

            double nextTime = span.getTime(nextIndex);
            int flag = ( !terminated ) ? jvode.start(nextTime) : JVode.TERMINATED;
            profile.setX(jvode.getY());
            profile.setTime(jvode.getTime());

            if( flag == JVode.ROOT_RETURN ) //Success type output: we found a root
            {
                eventAtSpanPoint = Math.abs(jvode.getTime() - nextTime) < 1E-8;
                return false;
            }
            else if( flag == JVode.SUCCESS || flag == JVode.TSTOP_RETURN ) //Success type output: final time reached
            {
                profile.setTime(nextTime);
                eventAtSpanPoint = false;
                fireSolutionUpdate(nextTime, jvode.getY());
                nextIndex++;
                return ( nextIndex < span.getLength() );
            }
            else if( flag == JVode.TERMINATED )
            {
                return false;
            }
            else
            //Error type outputs
            {
                if( flag == JVode.CONV_FAILURE || flag == JVode.ERR_FAILURE )
                    throw new Exception("Unrecoverable convergence error");
                else if( flag == JVode.TOO_MUCH_ACC )
                    throw new Exception("Too much accuracy requested");
                else if( flag == JVode.TOO_MUCH_WORK )
                    throw new Exception("Steps limit (" + jvode.maxSteps + ") exceeded");
                else if( flag == JVode.RHSFUNC_FAIL )
                    throw new Exception("ODE Right hand side function failed");
                else if( flag == JVode.INCORRECT_CALCULATIONS )
                    throw new Exception("Incorrect calculations were made in the model.");
                else
                    throw new Exception("Unknown error");
            }
        }
        catch( Exception ex )
        {
            //ex.printStackTrace();
            if( jobControl != null )
                jobControl.terminate();
            profile.setErrorMessage(ex.getLocalizedMessage());
            profile.setUnstable(true);
            profile.setX(jvode.getY());
            log.info(ex.getMessage());
            return false;
        }
    }
    
    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        odeModel.setCurrentValues(x0);
        jvode.setInitialValues(odeModel.getY(), odeModel.getTime());
        jvode.reset();
    }
}
