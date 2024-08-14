package biouml.plugins.research.workflow.engine;

/**
 * Listener interface for workflow execution process
 */
public interface WorkflowEngineListener
{
    /**
     * Indicates when execution started
     */
    public void started();

    /**
     * Indicates when execution finished
     */
    public void finished();
    
    /**
     * Indicates when execution state changed
     */
    public void stateChanged();
    
    /**
     * Indicates about error in workflow process
     */
    public void errorDetected(String error);
    
    /**
     * Indicates about error in workflow parameters
     */
    public void parameterErrorDetected(String error);
    
    /**
     * Indicates about ready to open results
     */
    public void resultsReady(Object[] results);
}
