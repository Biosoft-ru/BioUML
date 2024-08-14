package biouml.plugins.simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.task.ExceptionalConsumer;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.util.ObjectPool;
import ru.biosoft.util.ObjectPool.PooledObject;
import biouml.model.Diagram;

import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

@CodePrivilege ( CodePrivilegeType.THREAD )
public class ParallelSimulationEngine
{
    private final SimulationTaskFactory factory;
    private final EnginePool engines = new EnginePool();

    public static interface SimulationTaskFactory
    {
        public SimulationTask createSimulationTask(String[] names);

        public double[] processResult(Object result);
    }

    public ParallelSimulationEngine(Diagram diagram, SimulationTaskParameters simulationParameters, SimulationTaskFactory factory) throws Exception
    {
        this.factory = factory;
        this.diagram = diagram;
        this.simulationParameters = simulationParameters;

        SimulationEngine engine = SimulationTask.initEngine(getDiagram(), getEngineSettings(), null);
        this.model = engine.createModel();
    }

    private final Diagram diagram;
    public Diagram getDiagram()
    {
        return this.diagram;
    }

    private final SimulationTaskParameters simulationParameters;
    public SimulationTaskParameters getEngineSettings()
    {
        return this.simulationParameters;
    }

    private final Model model;
    public Model getModel()
    {
        return model;
    }

    public double[][] simulate(double[][] values, String[] names, final JobControl jobControl)
    {
        return simulate(values, names, jobControl, false);
    }

    public double[][] simulate(double[][] values, String[] names, final JobControl jobControl, boolean returnIncomplete)
    {
        try
        {
            if( init(values, names) )
            {

                for( int i = 0; i < values.length; ++i )
                {
                    tasks.get(i).setValues(values[i]);
                }
                final FunctionJobControl fjc = jobControl == null ? null : new SubFunctionJobControl(jobControl,
                        jobControl.getPreparedness(), jobControl.getPreparedness());

                ExceptionalConsumer<SimulationTask> iteration = element -> element.run( fjc );
                if( jobControl == null )
                {
                    TaskPool.getInstance().iterate(tasks, iteration);
                }
                else
                {
                    TaskPool.getInstance().iterate(tasks, iteration, fjc);
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        if( !returnIncomplete )
                            return null;
                    }
                }
                return tasks.stream().map( SimulationTask::getResult ).toArray( double[][]::new );
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        return null;
    }

    public double[] simulate(double[] values, String[] names, JobControl jobControl)
    {
        Object sr = getResult(values, names, jobControl);
        return processResult(sr);
    }

    private List<SimulationTask> tasks;
    private SimulationTask singleTask;

    private boolean init(double[][] values, String[] names)
    {
        if( diagram == null )
        {
            return false;
        }

        if( values != null && names != null )
        {
            if( tasks == null )
            {
                tasks = new ArrayList<>();
            }

            if( values.length != tasks.size() )
            {
                tasks.clear();
                for( double[] value : values )
                {
                    SimulationTask task = factory.createSimulationTask(names);
                    task.setParameters(simulationParameters);
                    tasks.add(task);
                }
            }
        }
        else if( singleTask == null )
        {
            singleTask = factory.createSimulationTask(names);
            singleTask.setParameters(simulationParameters);
        }

        return true;
    }


    private boolean init()
    {
        return init(null, null);
    }

    public Object getResult(double[] values, String[] names)
    {
        return getResult(values, names, null);
    }

    public Object getResult(double[] values, String[] names, JobControl jobControl)
    {
        if( init() )
        {
            singleTask.setValues(values);
            Object sr;
            try
            {
                sr = singleTask.getResult(
                        values,
                        names,
                        jobControl == null ? null : new SubFunctionJobControl(jobControl, jobControl.getPreparedness(), jobControl
                                .getPreparedness()));
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return sr;
        }
        return null;
    }

    public PooledObject<ModelEngine> allocModelEngine() throws Exception
    {
        return engines.get();
    }

    public double[] processResult(Object sr)
    {
        return factory.processResult(sr);
    }

    public static class ModelEngine
    {
        private final SimulationEngine engine;
        private final Model baseModel;

        public ModelEngine(SimulationEngine engine) throws Exception
        {
            this.engine = engine;
            this.baseModel = engine.createModel();
            baseModel.init();
            ApplicationUtils.removeDir( new File(engine.getOutputDir()) );
        }
        public SimulationEngine getEngine()
        {
            return engine;
        }
        public Model getBaseModel()
        {
            return baseModel;
        }
    }

    public class EnginePool extends ObjectPool<ModelEngine>
    {
        @Override
        protected ModelEngine createObject() throws Exception
        {
            return new ModelEngine(SimulationTask.initEngine(getDiagram(), getEngineSettings(), getModel()));
        }
    }
}
