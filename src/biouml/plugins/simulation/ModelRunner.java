package biouml.plugins.simulation;


import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.security.SessionThread;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;


public class ModelRunner
{
    protected static final Logger log = Logger.getLogger(ModelRunner.class.getName());

    protected int threadsNumber = 1;

    protected ThreadTask[] threads;
    protected Semaphore[] semaphores;

    public ModelRunner(SimulationEngine simulationEngine, DataGenerator generator, int threadsNum)
    {
        if( threadsNum > 1 )
        {
            threadsNumber = threadsNum;
        }

        dataGenerator = generator;

        semaphores = new Semaphore[threadsNumber];
        threads = new ThreadTask[threadsNumber];
        for( int i = 0; i < threadsNumber; ++i )
        {
            threads[i] = new ThreadTask(simulationEngine, i);
            threads[i].setDaemon(true);
        }

        initListeners();
    }

    protected ModelRunnerListener modelRunnerListener;
    public void addModelRunnerListener(ModelRunnerListener l)
    {
        modelRunnerListener = l;
    }

    private JobControlListener jobControlListener;
    private void initListeners()
    {
        jobControlListener = new JobControlListenerAdapter()
        {
            @Override
            public void jobTerminated(JobControlEvent event)
            {
                for( int i = 0; i < threadsNumber; ++i )
                    threads[i].terminate();
            }
        };
    }

    public void simulate(JobControl jobControl) throws Exception
    {
        if( jobControl != null )
            jobControl.addListener(jobControlListener);

        for( int i = 0; i < threadsNumber; i++ )
        {
            semaphores[i] = new Semaphore();
        }

        synchronized( ThreadTask.class )
        {
            synchronized( semaphores )
            {
                for( int i = 0; i < threadsNumber; i++ )
                {
                    threads[i].start();
                }

                /*
                 * Waiting of all threads starting
                 */
                boolean isAllStarted = false;
                while( !isAllStarted )
                {
                    try
                    {
                        semaphores.wait();
                    }
                    catch( InterruptedException e )
                    {
                        e.printStackTrace();
                    }

                    isAllStarted = true;
                    for( int i = 0; i < threadsNumber; i++ )
                    {
                        if( !semaphores[i].isStarted )
                        {
                            isAllStarted = false;
                            break;
                        }
                    }
                }
            }
        }

        /*
         * Waiting for all threads ending
         */
        for( int i = 0; i < threadsNumber; i++ )
        {
            try
            {
                threads[i].join();
            }
            catch( InterruptedException e )
            {
                e.printStackTrace();
                return;
            }
        }

        if( jobControl != null )
            jobControl.removeListener(jobControlListener);
    }

    protected DataGenerator dataGenerator;
    abstract public static class DataGenerator
    {
        abstract public double[] getValues();
        abstract public String[] getNames();

        abstract public double getInitialTime();
        abstract public double getCompletionTime();
    }

    protected static class Semaphore
    {
        public boolean isStarted = false;
    }

    protected class ThreadTask extends SessionThread
    {
        protected SimulationEngine simulationEngine;
        protected int threadNumber;

        ThreadTask(SimulationEngine simulationEngine, int threadNumber)
        {
            setName("Thread N" + threadNumber);
            this.threadNumber = threadNumber;

            this.simulationEngine = simulationEngine.clone();
            this.simulationEngine.setJobControl( new FunctionJobControl( log ) );

            Diagram diagram = simulationEngine.getDiagram();
            if( diagram != null )
            {
                try
                {
                    Diagram cloneDiagram = diagram.clone(diagram.getOrigin(), diagram.getName());
                    this.simulationEngine.setDiagram(cloneDiagram);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Model runner: can not process diagram '" + diagram.getName() + "'", e);
                }
            }

            generateModel();
        }

        @Override
        public void run()
        {
            synchronized( semaphores )
            {
                semaphores[this.threadNumber].isStarted = true;
                semaphores.notify();
            }

            try
            {
                double[] values = dataGenerator.getValues();
                while( values != null )
                {
                    SimulationResult simulationResult = new SimulationResult(null, "Simulation Result");
                    setVariables(values, dataGenerator.getNames());

                    simulationEngine.setInitialTime(dataGenerator.getInitialTime());
                    simulationEngine.setCompletionTime(dataGenerator.getCompletionTime());

                    String status = simulationEngine.simulate(model, simulationResult);

                    if( modelRunnerListener != null )
                    {
                        SimulatorProfile profile = ( (Simulator)simulationEngine.getSolver() ).getProfile();
                        if( ( status == null || status.length() == 0 ) && !profile.isStiff() && !profile.isUnstable() )
                        {
                            modelRunnerListener.resultReady(simulationResult, values, dataGenerator.getNames());
                        }
                    }

                    values = dataGenerator.getValues();
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "ModelRunner: error during the diagram simulation.", e);
            }
        }

        public void terminate()
        {
            simulationEngine.getJobControl().terminate();
        }

        protected Model model;
        private void generateModel()
        {
            try
            {
                model = simulationEngine.createModel();
                Map<String, Double> parameterValues = new HashMap<>();
                double[] variableValues = initVariables(parameterValues);
                model.init(variableValues, parameterValues);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "ModelRunner: base models generation error" + ", error=" + t);
            }
        }

        private double[] initVariables(Map<String, Double> parameterValues)
        {
            double[] variableValues = null;
            Map<String, Integer> varMapping = null;

            if( simulationEngine instanceof OdeSimulationEngine )
            {
                varMapping = ( (OdeSimulationEngine)simulationEngine ).varNameRateIndexMapping;
                variableValues = new double[varMapping.size()];
            }

            EModel emodel = simulationEngine.getDiagram().getRole( EModel.class );
            DataCollection<Variable> variables = emodel.getVariables();

            for( Variable var : variables )
            {
                String name = var.getName();
                if( varMapping != null && varMapping.containsKey(name) )
                {
                    int index = varMapping.get(name);
                    variableValues[index] = var.getInitialValue();
                }
                else
                {
                    String codeName = simulationEngine.getVariableCodeName(name);
                    parameterValues.put(codeName, var.getInitialValue());
                }
            }

            return variableValues;
        }

        public void setVariables(double[] values, String[] names) throws Exception
        {
            Map<String, Double> parameterValues = new HashMap<>();
            double[] variableValues = initVariables(parameterValues);

            Map<String, Integer> varMapping = null;
            if( simulationEngine instanceof OdeSimulationEngine )
            {
                varMapping = ( (OdeSimulationEngine)simulationEngine ).varNameRateIndexMapping;
            }

            for( int j = 0; j < values.length; ++j )
            {
                if( varMapping != null && varMapping.containsKey(names[j]) )
                {
                    int index = varMapping.get(names[j]);
                    variableValues[index] = values[j];
                }
                else
                {
                    String codeName = simulationEngine.getVariableCodeName(names[j]);
                    parameterValues.put(codeName, values[j]);
                }
            }

            model.init(variableValues, parameterValues);
        }
    }
}
