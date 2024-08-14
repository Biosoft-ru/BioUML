package biouml.plugins.simulation.ode.radau5;

import java.util.logging.Level;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.ode.OdeModel;
import biouml.plugins.simulation.ode.StdMet;
import biouml.standard.simulation.ResultListener;


public class Radau5 extends SimulatorSupport
{
    private StiffIntegratorT engine;
    private int[] events; // number of event in g function, -1 if event was not located

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Radau5";
        info.eventsSupport = true;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new Radau5Options();
    }

    Options options = (Options)getDefaultOptions();

    @Override
    public Options getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        this.options = options;
    }

    @Override
    public boolean doStep()
    {
        try
        {
            if( fireInitialValues )
            {
                fireSolutionUpdate(span.getTimeStart(), engine.y);
                fireInitialValues = false;
            }

            routine();

            return ! ( terminated || engine.failed || engine.x <= engine.xend || engine.eventTriggered );
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, ex.getMessage());
            return false;
        }
    }


    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        if( ! ( model instanceof OdeModel ) )
            throw new Exception("Radau5 solver can be used only for OdeModels");

        odeModel = (OdeModel)model;
        if( !odeModel.isInit() )
            odeModel.init();

        if( this.preprocessFastReactions && odeModel.hasFastOde() )
            x0 = preprocessFastReactions();
        
        profile.setStiff(false);
        profile.setUnstable(false);
        this.span = tspan;

        resultListeners = listeners;

        Radau5Options radauOpts = (Radau5Options)options;

        double[] atol = StdMet.generateArray(radauOpts.getAtol(), x0.length);
        double[] rtol = StdMet.generateArray(radauOpts.getRtol(), x0.length);

        engine = new StiffIntegratorT(odeModel, x0, tspan, rtol, atol, radauOpts, resultListeners, profile);
    }

    /**Method computes the solution to the ODE depending on parameters and calculations given to and done by the constructor*/
    public void routine() throws Exception
    {
        engine.doIntegration();
        if( engine.eventTriggered )
            this.events = engine.events;
    }

    @Override
    public int[] getEvents()
    {
        return events;
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        odeModel.setCurrentValues(x0);
        double time = odeModel.getTime();
        double[] rateVars = odeModel.getY();
        Radau5Options radauOpts = (Radau5Options)options;
        double[] atol = StdMet.generateArray(radauOpts.getAtol(), x0.length);
        double[] rtol = StdMet.generateArray(radauOpts.getRtol(), x0.length);
        span = span.getRestrictedSpan(time, span.getTimeFinal());
        engine = new StiffIntegratorT(odeModel, rateVars, span, rtol, atol, radauOpts, resultListeners, profile);
    }
}
