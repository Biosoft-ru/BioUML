package biouml.plugins.simulation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;

/**
 * @author anna
 *
 */
@ClassIcon ( "resources/simulation-analysis.gif" )
public class SimulationAnalysis extends AnalysisMethodSupport<SimulationAnalysisParameters>
{
    private SimulationAnalysisJobControl jobControl;
    private List<PropertyChangeListener> listenersList;
    private List<ResultListener> resultListenerList;

    public SimulationAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new SimulationAnalysisParameters());
        log = Logger.getLogger( SimulationAnalysis.class.getName() );
        jobControl = new SimulationAnalysisJobControl( Logger.getLogger( SimulationAnalysis.class.getName() ) );
        listenersList = new ArrayList<>();
        resultListenerList = new ArrayList<>();
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        SimulationAnalysisParameters params = getParameters();
        if( params.getModelPath() != null )
        {
            super.validateParameters();
            Diagram diagram = parameters.getModelPath().getDataElement(Diagram.class);
            if( diagram.getRole() == null || ! ( diagram.getRole() instanceof EModel ) )
                throw new IllegalArgumentException("Diagram does not contain a model. Please, select valid diagram.");
        }
        SimulationEngine engine = parameters.getSimulationEngine();
        if( engine == null )
            throw new IllegalArgumentException("Wrong simulation engine type");
    }

    @Override
    public SimulationAnalysisJobControl getJobControl()
    {
        return jobControl;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        listenersList.add(l);
    }

    public void addResultListenerList(ResultListener resultListener)
    {
        this.resultListenerList.add(resultListener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        listenersList.remove(l);
    }

    @Override
    public SimulationResult justAnalyzeAndPut() throws Exception
    {
        validateParameters();
        SimulationAnalysisParameters params = getParameters();

        //        double outputStartTime = params.getOutputStartTime();
        //        int skipPoints = params.getSkipPoints();

        SimulationEngine engine = params.getSimulationEngine();
        engine.setTerminated(false);
        Model model = engine.createModel();

        if( model == null )
        {
            engine.log.error( "Model was not generated!" );
            return null;
        }
        List<ResultListener> listeners = new ArrayList<>();
        listeners.add(jobControl);
        DataElementPath resultPath = params.getSimulationResultPath();

        int skipPoints = params.getSkipPoints();
        if(params.getOutputStartTime() > engine.getInitialTime() && engine instanceof JavaSimulationEngine)
        {
            UniformSpan span = new UniformSpan( params.getOutputStartTime(), engine.getCompletionTime(), engine.getTimeIncrement() );
            span.addPoints( new double[]{ engine.getInitialTime() } );
            ( (JavaSimulationEngine)engine ).setSpan( span );
            skipPoints++;
        }

        SimulationResult result;
        ResultWriter writer = null;
        if( resultPath != null )
        {
            result = new SimulationResult(resultPath.optParentCollection(), resultPath.getName());
            writer = new ResultWriter(result);
            writer.setSkipPoints( skipPoints );

            engine.initSimulationResult( result );
            jobControl.setPreparedness(5);

            listeners.add(writer);
            writer.start(model);
            jobControl.pushProgress(10, 95);
            jobControl.setPercentStep(100.0 / ( engine.getCompletionTime() - engine.getInitialTime() ));

            for( PropertyChangeListener l : listenersList )
            {
                listeners.add(new ResultListenerAdapter(l, result));
            }
        }

        listeners.addAll(resultListenerList);


        try
        {
            if( !engine.isTerminated() )
                engine.simulate(model, listeners.toArray(new ResultListener[listeners.size()]));
        }
        catch( Exception e )
        {
            engine.log.error("ERROR_SIMULATION", new String[] {engine.getDiagram().getName(), e.toString()}, e);
            return null;
        }
        jobControl.popProgress();
        if( resultPath != null && writer != null )
        {
            result = writer.getResults();
            resultPath.save(result);

            for( PropertyChangeListener l : listenersList )
            {
                PropertyChangeEvent evt = new PropertyChangeEvent(this, "end", null, result);
                l.propertyChange(evt);
            }
            return result;
        }
        else
        {
            for( ResultListener l : resultListenerList )
            {
                if( l instanceof ResultWriter )
                    return ( (ResultWriter)l ).getResults();
            }
        }

        return null;
    }

    //TODO: remove adapter, rewrite ResultListener
    public static class ResultListenerAdapter implements ResultListener
    {
        PropertyChangeListener listener = null;
        private SimulationResult result;
        public ResultListenerAdapter(PropertyChangeListener pcl, SimulationResult result)
        {
            listener = pcl;
            this.result = result;
        }

        @Override
        public void add(double t, double[] y) throws Exception
        {
            PropertyChangeEvent evt = new PropertyChangeEvent(result, "change", null, null);
            listener.propertyChange(evt);
        }

        @Override
        public void start(Object model)
        {
            PropertyChangeEvent evt = new PropertyChangeEvent(result, "start", null, null);
            listener.propertyChange(evt);
        }
    }

    public class SimulationAnalysisJobControl extends AnalysisJobControl implements ResultListener
    {
        private double percentStep;

        public SimulationAnalysisJobControl(Logger l)
        {
            super( SimulationAnalysis.this );
        }

        @Override
        public void add(double t, double[] y) throws Exception
        {
            setPreparedness((int) ( t * percentStep ));
        }

        @Override
        public void start(Object model)
        {
        }

        public void setPercentStep(double step)
        {
            percentStep = step;
        }

        @Override
        protected void setTerminated(int status)
        {
            SimulationAnalysisParameters params = getParameters();
            SimulationEngine engine = params.getSimulationEngine();
            engine.stopSimulation();
            super.setTerminated(status);
        }
    }
}
