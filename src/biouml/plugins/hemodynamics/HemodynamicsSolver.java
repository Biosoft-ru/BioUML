package biouml.plugins.hemodynamics;

import java.util.logging.Level;

import biouml.plugins.simulation.CycledResultListener;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

public abstract class HemodynamicsSolver implements Simulator
{
    @Override
    public Object getDefaultOptions()
    {
        return new HemodynamicsOptions();
    }


    @Override
    public SimulatorInfo getInfo()
    {
        return null;
    }

    public double tDelta, tMin = 0, tMax;
    
    SimulatorProfile profile;
    @Override
    public SimulatorProfile getProfile()
    {
        return profile;
    }
    
       
    @Override
    public Options getOptions()
    {
        return parameters;
    }
    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof HemodynamicsOptions ) )
            throw new IllegalArgumentException("Illegal options class " + options.getClass().toString() + " for HemodynamicsModelSolver");
        this.parameters = (HemodynamicsOptions)options;
    }

    HemodynamicsOptions parameters = new HemodynamicsOptions();
    

    @Override
    public void setInitialValues(double[] x0)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl)
            throws Exception
    {
        if( ! model.isInit() )
            model.init(); //need to init the model for correct initial values getting
        
        this.init( model, null, tspan, listeners, jobControl );
        
        if( jobControl != null )
            jobControl.functionStarted();

        while( doStep() )
            ;

        for( ResultListener resultListener : listeners )
            if( resultListener instanceof CycledResultListener )
                ( (CycledResultListener)resultListener ).finish();

        if( jobControl != null )
            jobControl.functionFinished();
    }   

    @Override
    public void setLogLevel(Level level)
    {
    }
}
