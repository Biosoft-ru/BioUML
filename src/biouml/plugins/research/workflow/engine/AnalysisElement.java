package biouml.plugins.research.workflow.engine;

import java.beans.PropertyChangeListener;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Node;
import biouml.plugins.research.workflow.RunWorkflowAnalysis;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersExceptionEvent;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.ParameterException;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * {@link WorkflowElement} implementation for {@link AnalysisMethod}
 */
public class AnalysisElement extends WorkflowElement
{
    protected AnalysisJobControl jobControl;
    private final AnalysisMethod analysisMethod;
    private boolean skipCompleted;
    private boolean failed;
    private AnalysisParameters parameters;
    private final Node node;

    public AnalysisElement(AnalysisMethod analysisMethod, Node node, DynamicProperty statusProperty)
    {
        super(statusProperty);
        this.analysisMethod = analysisMethod;
        this.jobControl = analysisMethod.getJobControl();
        this.node = node;
        failed = false;
    }

    public void readParametersFromDiagram() throws Exception
    {
        if( parameters == null )
            parameters = WorkflowEngine.getAnalysisParametersByNode(node, false);
    }

    public void setSkipCompleted(boolean skipCompleted)
    {
        this.skipCompleted = skipCompleted;
    }

    @Override
    public boolean isComplete()
    {
        return JobControl.COMPLETED == jobControl.getStatus() || isCompleteFailed();
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        setPreparedness(0);

        try
        {
            readParametersFromDiagram();
            if( parameters instanceof RunWorkflowAnalysis.RunWorkflowParameters )
                ( (RunWorkflowAnalysis.RunWorkflowParameters)parameters ).setIgnoreFail( ignoreFail );
            if( parameters instanceof RunWorkflowAnalysis.RunWorkflowParameters )
                ( (RunWorkflowAnalysis.RunWorkflowParameters)parameters ).setSkipCompleted( skipCompleted );
            analysisMethod.setParameters(parameters);
            analysisMethod.validateParameters();
        }
        catch(Exception e)
        {
            if( ignoreFail )
            {
                setFailed( e.getMessage() );
                listener.jobTerminated( null );
                terminate();
            }
            else
            {
                if( e instanceof IllegalArgumentException || e instanceof ParameterException )
                {
                    analysisMethod.getLogger().log( Level.SEVERE, e.getMessage() );
                    listener.jobTerminated( new AnalysisParametersExceptionEvent( jobControl, e.getMessage() ) );
                }
                else
                {
                    analysisMethod.getLogger().log( Level.SEVERE, ExceptionRegistry.log( e ) );
                    listener.jobTerminated( new JobControlEvent( jobControl, e.getMessage() ) );
                }
            }
            return;
        }
        jobControl.addListener(new JobControlListener()
        {

            @Override
            public void jobPaused(JobControlEvent event)
            {
            }

            @Override
            public void jobResumed(JobControlEvent event)
            {
            }

            @Override
            public void jobStarted(JobControlEvent event)
            {
            }

            @Override
            public void jobTerminated(JobControlEvent event)
            {
                if(JobControl.COMPLETED == jobControl.getStatus())
                {
                    setPreparedness(100);
                }
                else if( JobControl.TERMINATED_BY_ERROR == jobControl.getStatus() && ignoreFail )
                {
                    setFailed( event.getMessage() );
                }
            }

            @Override
            public void resultsReady(JobControlEvent event)
            {
            }

            @Override
            public void valueChanged(JobControlEvent event)
            {
                setPreparedness(event.getPreparedness());
            }

        });
        jobControl.addListener(listener);
        if( skipCompleted )
            jobControl.setRecoverMode( true );
        jobControl.run();
    }

    private boolean isCompleteFailed()
    {
        return ignoreFail && failed;
    }

    private void setFailed(String message)
    {
        log.info( "Skip failed analysis " + analysisMethod.getName() + " with following error: " + message );
        analysisMethod.getLogger().info( "Skip failed analysis " + analysisMethod.getName() + " with following error: " + message );
        failed = true;
        setPreparedness( 100 );
    }

    @Override
    public double getWeight()
    {
        return analysisMethod.estimateWeight();
    }

    @Override
    public void terminate()
    {
        if(jobControl != null)
            jobControl.terminate();
    }

    @Override
    public void addListener(WorkflowElement element)
    {
        if(element instanceof PropertyChangeListener)
            analysisMethod.addPropertyChangeListener((PropertyChangeListener)element);
    }

    @Override
    public JobControl getJobControl()
    {
        return this.jobControl;
    }
}
