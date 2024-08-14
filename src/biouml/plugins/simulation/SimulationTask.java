package biouml.plugins.simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.simulation.ParallelSimulationEngine.ModelEngine;
import biouml.plugins.simulation.java.JavaLargeModel;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.ObjectPool.PooledObject;
import ru.biosoft.util.TempFiles;

public class SimulationTask
{
    protected static final Logger log = Logger.getLogger(SimulationTask.class.getName());

    protected ParallelSimulationEngine parallelEngine;

    protected SimulationTaskParameters parameters;

    public SimulationTask(ParallelSimulationEngine parallelEngine, String[] names)
    {
        this.parallelEngine = parallelEngine;
        this.names = names;
    }

    /**
     * Parameters and variables names
     */
    protected String[] names;

    /**
     * Parameters and variables values
     */
    protected double[] values;

    protected double[] result;

    private File f = TempFiles.path( "simulationTask" );
    
    public void setValues(double[] values)
    {
        this.values = values;
    }

    public void run(FunctionJobControl jobControl) throws Exception
    {
        this.result = parallelEngine.processResult(getResult(values, names, jobControl));
//        this.writeToFile( f, names, values, result[0], status );
    }

    public double[] getResult()
    {
        return result;
    }

    public Object getResult(double[] values, final String[] names, FunctionJobControl jobControl) throws Exception
    {
        try (PooledObject<ModelEngine> obj = parallelEngine.allocModelEngine())
        {
            ModelEngine modelEngine = obj.get();
            if( jobControl != null )
            {
                modelEngine.getEngine().setJobControl(jobControl);
            }
            setValues(modelEngine.getEngine(), modelEngine.getBaseModel(), values, names);
            Object result = getResult(modelEngine.getEngine(), modelEngine.getBaseModel());
            modelEngine.getEngine().setJobControl(null);
            return result;
        }
    }

    protected Object getResult(SimulationEngine engine, Model baseModel)
    {
        try
        {
            SimulationResult sr = engine.generateSimulationResult();
            status = engine.simulate(baseModel, sr);

            SimulatorProfile profile = ( (Simulator)engine.getSolver() ).getProfile();

            if( ( status != null && status.length() > 0 ) || profile.isStiff() || profile.isUnstable() )
                sr = null;

            return sr;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get simulation results: " + ExceptionRegistry.log(e));
        }
        return null;
    }

    protected Map<String, Double> parameterValues;
    protected double[] variableValues;

    protected void setValues(SimulationEngine engine, Model baseModel, double[] values, String[] names) throws Exception
    {
    	baseModel.init();
        double[] currValues = baseModel.getCurrentValues();
        for(int i = 0; i < values.length; ++i)
        {
            if( !engine.getVarPathIndexMapping().containsKey(names[i]) )
            {
                throw new Exception("Variable " + names[i] + " not found in the model");
            }
          int index = engine.getVarPathIndexMapping().get(names[i]);
          currValues[index] = values[i];
        }
        baseModel.setCurrentValues(currValues);
        
    }

    public static SimulationEngine initEngine(Diagram diagram, SimulationTaskParameters simulationParameters, Model model)
    {
    	SimulationEngine engineSettings = simulationParameters.getSimulationEngine();
    	return initEngine(diagram, engineSettings, model);
    }

    public static SimulationEngine initEngine(Diagram diagram, SimulationEngine engineSettings, Model model)
    {
        SimulationEngine engine = null;
        try
        {
            engine = engineSettings.getClass().newInstance();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not initialize the solver settings: " + e.getMessage());
        }

        if( engine != null )
        {
            engine.setDiagram(diagram);
            if( model != null )
            {
                try
                {
                    //TODO: use appropriate clone here
                    Model copy = model.getClass().newInstance();
                    if (model instanceof JavaLargeModel)
                    {
                        ((JavaLargeModel)copy).setNameToIndex(((JavaLargeModel)model).getNameToIndex());
                        engine.setModel(copy);
                    }
                }
                catch( Exception ex )
                {
                    engine.setModel(null);
                }
            }
            engine.setTimeIncrement(engineSettings.getTimeIncrement());
            engine.setInitialTime(engineSettings.getInitialTime());
            engine.setCompletionTime(engineSettings.getCompletionTime());
            engine.setLogLevel( Level.SEVERE );
            Simulator solver = initSolver(engineSettings);
            if( solver != null )
                engine.setSolver(solver);
        }
        return engine;

    }

    public static Simulator initSolver(SimulationEngine engineSettings)
    {
        try
        {
            Simulator solver = (Simulator)engineSettings.getSolver().getClass().newInstance();
            solver.setOptions( ( (Simulator)engineSettings.getSolver() ).getOptions());
            return solver;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not initialize the solver settings: " + e.getMessage());
        }
        return null;
    }

    public void setParameters(SimulationTaskParameters parameters)
    {
        this.parameters = parameters;
    }
    
    private boolean firstLine = true;
    private String status;
    
    private void writeToFile(File file, String[] names, double[] values, double deviation, String error)
    {
        try
        {
            if( !file.exists() )
                file.createNewFile();

            try (BufferedWriter bw = ApplicationUtils.utfAppender( file ))
            {
                if( firstLine )
                {

                    bw.append( StreamEx.of( names ).prepend( "Result" ).joining( "\t" ) + "\n" );

                    firstLine = false;
                }
                String prefix = ( status != null && !status.isEmpty() ) ? status : String.valueOf( deviation );
                String result = DoubleStreamEx.of( values ).mapToObj( v -> String.valueOf( v ) ).prepend( prefix ).joining( "\t" ) + "\n";

                bw.append( result );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}
