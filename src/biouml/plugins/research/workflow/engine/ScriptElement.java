package biouml.plugins.research.workflow.engine;

import java.util.logging.Level;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowVariable;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * JavaScript element implementation for {@link WorkflowElement}
 */
public class ScriptElement extends WorkflowElement
{

    protected Logger log;

    public static final String SCRIPT_SOURCE = "source";
    public static final String SCRIPT_PATH = "sourcePath";
    public static final String SCRIPT_TYPE = "scriptType";

    protected boolean complete = false;
    private boolean failed = false;
    protected ScriptDataElement script;

    private Collection<WorkflowVariable> vars;
    private Collection<WorkflowExpression> outputVars;

    private Map<String, Object> getVarValues(Collection<WorkflowVariable> vars)
    {
        Map<String, Object> result = new HashMap<>();
        for(WorkflowVariable var: vars)
        {
            try
            {
                result.put(var.getName(), var.getValue());
            }
            catch( Exception e )
            {
            }
        }
        return result;
    }

    public ScriptElement(ScriptDataElement script, DynamicProperty statusProperty, Logger log, Collection<WorkflowVariable> vars, Collection<WorkflowExpression> outputVars)
    {
        super(statusProperty);
        this.script = script;
        this.log = log;
        this.vars = vars;
        this.outputVars = outputVars;
    }


    @Override
    public boolean isComplete()
    {
        return complete || ( isIgnoreFail() && failed );
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        setPreparedness(0);
        final FunctionJobControl fjc = new FunctionJobControl( log );
        fjc.addListener(listener);
        try
        {
            fjc.functionStarted();
            LogScriptEnvironment environment = new LogScriptEnvironment(log, false);

            Map<String, Object> outVars = new HashMap<>();
            for(WorkflowExpression wfe : outputVars)
                outVars.put( wfe.getName(), null );

            script.execute( script.getContent(), environment, getVarValues( vars ), outVars , false );
            if( environment.isFailed() )
                if( isIgnoreFail() )
                {
                    setFailed( null, listener );
                }
                else
                    listener.jobTerminated( new JobControlEvent( fjc, "Script failed" ) );
            else
            {
                for(WorkflowExpression wfe : outputVars)
                {
                    Object value = outVars.get( wfe.getName() );
                    if(value != null)
                        wfe.setExpression( value.toString() );
                }

                fjc.resultsAreReady(environment.getImages().toArray());
                complete = true;
                setPreparedness( 100 );
                fjc.functionFinished();
            }
        }
        catch( Exception e )
        {
            if( isIgnoreFail() )
                setFailed( e.getMessage(), listener );
            else
            {
                log.log(Level.SEVERE,  ExceptionRegistry.log( e ) );
                listener.jobTerminated( new JobControlEvent( fjc, e.getMessage() ) );
            }
        }
    }

    @Override
    public double getWeight()
    {
        return 1;
    }

    private void setFailed(String message, JobControlListener listener)
    {
        failed = true;
        setPreparedness( 100 );
        if( listener != null )
            listener.jobTerminated( null );
        String msg = "Script " + script.getName() + " failed";
        if( message != null )
            msg += ": " + message;
        log.info( msg );
    }
}
