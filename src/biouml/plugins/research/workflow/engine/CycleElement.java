package biouml.plugins.research.workflow.engine;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowItem;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * @author lan
 *
 */
public class CycleElement extends WorkflowElement implements JobControlListener
{
    private ExecutionMap map;
    private Compartment compartment;
    private WorkflowEngine engine;
    private int count = -1;
    private int iteration;
    private WorkflowCycleVariable var;
    private FunctionJobControl fjc;
    private JobControlListener listener;
    private double weight = -1;
    private boolean terminated = false;
    private boolean failed = false;

    /**
     * @param statusProperty
     */
    public CycleElement(WorkflowEngine engine, Compartment compartment, DynamicProperty statusProperty)
    {
        super(statusProperty);
        this.engine = engine;
        this.compartment = compartment;
    }

    @Override
    public boolean isComplete()
    {
        return iteration == count || ( isIgnoreFail() && failed );
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        getWeight();
        fjc = new FunctionJobControl(log);
        this.listener = listener;
        fjc.addListener(listener);
        try
        {
            fjc.functionStarted();
            initCycle();
            iteration = 0;
            runAvailableAnalysis();
        }
        catch(Exception e)
        {
            if( isIgnoreFail() )
            {
                failed = true;
                fjc.terminate();
                setPreparedness( 100 );
            }
            else
                fjc.functionTerminatedByError( e );
        }
    }

    private synchronized void runAvailableAnalysis()
    {
        if(terminated) return;
        if( count == 0 )
        {
            fjc.functionFinished();
            return;
        }
        if(map != null && map.isComplete())
        {
            map = null;
            iteration++;
            if(iteration == count)
            {
                fjc.functionFinished();
                return;
            }
        }
        if(map == null && iteration < count)
        {
            WorkflowEngine.clearStatusProperty(compartment, false);
            var.setIteration(iteration);
            map = new ExecutionMap();
            engine.buildCompartmentMap(map, compartment);
        }
        WorkflowElement analysis = null;
        while( map != null && ( analysis = map.getAvailableElement() ) != null )
        {
            engine.scheduleElementExecution(analysis, this);
        }
    }

    public static WorkflowCycleVariable findCycleVariable(Compartment compartment)
    {
        WorkflowCycleVariable var = null;
        for(Object obj: compartment)
        {
            if(obj instanceof Node)
            {
                WorkflowItem item = WorkflowItemFactory.getWorkflowItem((Node)obj);
                if(item instanceof WorkflowCycleVariable)
                {
                    var = (WorkflowCycleVariable)item;
                    break;
                }
            }
        }
        return var;
    }

    private void initCycle() throws Exception
    {
        var = findCycleVariable(compartment);
        if(var == null)
            throw new Exception("Malformed cycle: "+compartment.getName()+" cannot continue.");
        var.reset();
        count = var.getCount();
    }

    @Override
    public double getWeight()
    {
        if(weight == -1)
            synchronized(this)
            {
                if( weight == -1 )
                    initWeight();
            }
        return weight;
    }

    private void initWeight()
    {
        weight = 0;
        try
        {
            initCycle();
            for( int i = 0; i < count; i++ )
            {
                var.setIteration( i );
                ExecutionMap map = new ExecutionMap();
                engine.buildCompartmentMap( map, compartment );
                for( WorkflowElement we : map )
                {
                    weight += we.getWeight();
                }
            }
        }
        catch( Exception e )
        {
        }
        if( weight < 1 )
            weight = 1;
        count = -1;
    }

    @Override
    public int getPreparedness()
    {
        return count <=0 ? 0:(iteration*100+(map == null?0:map.getCompletePercent()))/count;
    }

    @Override
    public void valueChanged(JobControlEvent event)
    {
        fjc.setPreparedness(getPreparedness());
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        if( ( event == null ) || ( event.getStatus() == JobControl.COMPLETED ) )
        {
            runAvailableAnalysis();
            fjc.setPreparedness(getPreparedness());
        }
        else if( isIgnoreFail() && event.getStatus() == JobControl.TERMINATED_BY_ERROR )
        {
            runAvailableAnalysis();
            fjc.setPreparedness( getPreparedness() );
        }
        else
        {
            listener.jobTerminated(event);
        }
    }

    @Override
    public void jobPaused(JobControlEvent event)
    {
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {
    }

    @Override
    public void resultsReady(JobControlEvent event)
    {
        fjc.resultsAreReady(event.getResults());
    }

    @Override
    public void terminate()
    {
        terminated = true;
        if(map != null)
        {
            for(WorkflowElement we: map)
            {
                we.terminate();
            }
        }
        iteration = count;
    }
}
