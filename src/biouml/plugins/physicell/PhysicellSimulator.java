package biouml.plugins.physicell;


import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;
import ru.biosoft.jobcontrol.FunctionJobControl;


public class PhysicellSimulator implements Simulator
{
    private PhysicellModel model;
    private PhysicellOptions options = new PhysicellOptions();
    private boolean running = false;
    protected static final Logger log = Logger.getLogger( Simulator.class.getName() );
    private PhysicellResultWriter writer = new PhysicellResultWriter();

    @Override
    public SimulatorInfo getInfo()
    {
        return null;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new PhysicellOptions();
    }

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        this.model = (PhysicellModel)model;
        this.options = (PhysicellOptions)getOptions();
        this.running = true;
        writer.init(  this.model, this.options );  
        if( !this.model.isInit() )
            this.model.init();
        writer.saveAllResults( this.model );
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean doStep() throws Exception
    {
        double curTime = model.getCurrentTime();
        while( curTime < options.getFinalTime() && running )
        {
            model.doStep();
            model.executeEvents();
            curTime += options.getDiffusionDt();
            writer.saveAllResults( this.model );
        }
        return false;
    }

    @Override
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        try
        {
            init( model, null, tspan, listeners, jobControl );

            while( this.model.getCurrentTime() < options.getFinalTime() && running )
                doStep();

            writer.finish();
        }
        catch( Exception ex )
        {
            log.info( "Simulation failed: " + ex.getMessage() );
            ex.printStackTrace();
        }
    }

    @Override
    public void stop()
    {
        running = false;
    }

    @Override
    public SimulatorProfile getProfile()
    {
        return null;
    }

    @Override
    public PhysicellOptions getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        this.options = (PhysicellOptions)options;
    }

    @Override
    public void setLogLevel(Level level)
    {
        // TODO Auto-generated method stub
    }
    
    public void addTableVisualizer(VisualizerTextTable tableVisualizer)
    {
        writer.addTableVisualizer( tableVisualizer );
    }
    
    public void addTextVisualizer(VisualizerText textVisualizer)
    {
        writer.addTextVisualizer( textVisualizer );
    }
}