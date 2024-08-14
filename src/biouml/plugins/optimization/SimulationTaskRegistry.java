package biouml.plugins.optimization;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;
import biouml.model.Diagram;
import biouml.plugins.simulation.ParallelSimulationEngine;
import biouml.plugins.simulation.SimulationTask;
import biouml.plugins.simulation.SimulationTaskParameters;

public class SimulationTaskRegistry extends ExtensionRegistrySupport<SimulationTaskRegistry.SimulationTaskInfo>
{
    private static final SimulationTaskRegistry instance = new SimulationTaskRegistry();
    public static final String TYPE_ATTR = "type";
    public static final String CLASS_ATTR = "class";
    public static final String PARAMETERS_CLASS_ATTR = "parametersClass";

    private static Logger log = Logger.getLogger(SimulationTaskRegistry.class.getName());

    private SimulationTaskRegistry()
    {
        super("biouml.plugins.optimization.task", TYPE_ATTR);
    }

    @Override
    protected SimulationTaskInfo loadElement(IConfigurationElement element, String type) throws Exception
    {
        Class<? extends SimulationTask> taskClass = getClassAttribute(element, CLASS_ATTR, SimulationTask.class);
        Class<? extends SimulationTaskParameters> parametersClass = getClassAttribute(element, PARAMETERS_CLASS_ATTR,
                SimulationTaskParameters.class);
        return new SimulationTaskInfo(type, taskClass, parametersClass);
    }

    public static SimulationTask getSimulationTask(OptimizationExperiment experiment, ParallelSimulationEngine parallelEngine,
            String[] names)
    {
        try
        {
            String type = experiment.getExperimentType();
            SimulationTaskInfo info = instance.getExtension(type);
            if( info == null )
            {
                log.log(Level.SEVERE, "Can not find appropriate simulation task for experiment type: " + type);
                return null;
            }
            Class<? extends SimulationTask> taskClass = info.taskClass;

            Constructor<? extends SimulationTask> constructor = taskClass
                    .getConstructor(ParallelSimulationEngine.class, String[].class);
            return constructor.newInstance(parallelEngine, names);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during simulation task loading: " + ex.getMessage());
            return null;
        }
    }

    public static SimulationTaskParameters getSimulationTaskParameters(OptimizationExperiment experiment)
    {
        try
        {
            String type = experiment.getExperimentType();
            SimulationTaskInfo info = instance.getExtension(type);
            if( info == null )
            {
                log.log(Level.SEVERE, "Can not find appropriate simulation task for experiment type: " + type);
                return null;
            }
            Class<? extends SimulationTaskParameters> parametersClass = info.parametersClass;
            return parametersClass.getConstructor().newInstance();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during simulation task loading: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Returns mapping names of experiments into simulation task parameters for each experiment.
     */
    public static Map<String, SimulationTaskParameters> getSimulationTaskParameters(List<OptimizationExperiment> experiments,
            Map<String, SimulationTaskParameters> stp, Diagram diagram)
    {
        if( stp == null )
            stp = new HashMap<>();

        for( OptimizationExperiment experiment : experiments )
        {
            if( !stp.containsKey( experiment.getName() ) )
            {
                String type = experiment.getExperimentType();
                try
                {
                    SimulationTaskInfo info = instance.getExtension( type );
                    if( info == null )
                    {
                        log.log( Level.SEVERE, "Can not find appropriate simulation task for experiment type: " + type );
                        continue;
                    }

                    Class<? extends SimulationTaskParameters> parametersClass = info.parametersClass;
                    SimulationTaskParameters parameters = parametersClass.getConstructor().newInstance();
                    parameters.setDiagram( diagram );
                    stp.put( experiment.getName(), parameters );
                }
                catch( Exception ex )
                {
                    log.log( Level.SEVERE, "Can not load task parameters for experiment type:" + type + ", error: " + ex.getMessage() );
                    continue;
                }
            }
        }
        List<String> toRemove = new ArrayList<String>();
        for( String key : stp.keySet() )
        {
            boolean unused = true;
            for( OptimizationExperiment experiment : experiments )
            {
                if( experiment.getName().equals( key ) )
                    unused = false;
            }
            if( unused )
                toRemove.add( key );
        }
        for( String del : toRemove )
        {
            stp.remove( del );
        }
        return stp;
    }

    public static class SimulationTaskInfo
    {
        protected String type;
        protected Class<? extends SimulationTask> taskClass;
        protected Class<? extends SimulationTaskParameters> parametersClass;

        public SimulationTaskInfo(String type, Class<? extends SimulationTask> taskClass,
                Class<? extends SimulationTaskParameters> parametersClass)
        {
            this.type = type;
            this.taskClass = taskClass;
            this.parametersClass = parametersClass;
        }
    }
}
