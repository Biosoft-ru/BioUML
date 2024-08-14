package biouml.plugins.research.workflow.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biouml.model.Compartment;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

public class ParallelCycleElement extends WorkflowElement
{
    private final WorkflowEngine engine;
    private final Compartment compartment;
    private List<ExecutionMap> executionMaps;
    private FunctionJobControl fjc;
    private JobControlListener listener;
    private boolean terminated;
    private double weight = 1;

    public ParallelCycleElement(WorkflowEngine engine, Compartment compartment, DynamicProperty statusProperty)
    {
        super(statusProperty);
        this.engine = engine;
        this.compartment = compartment;
    }

    @Override
    public synchronized boolean isComplete()
    {
        if( executionMaps == null )
            return false;
        for( ExecutionMap eMap : executionMaps )
            if( !eMap.isComplete() )
                return false;
        return true;
    }

    @Override
    public double getWeight()
    {
        return weight;
    }

    private synchronized void init() throws Exception
    {
        WorkflowCycleVariable var = CycleElement.findCycleVariable(compartment);
        if(var == null)
            throw new Exception("Malformed cycle: "+compartment.getName()+" cannot continue.");
        var.reset();

        executionMaps = new ArrayList<>();
        for( int i = 0; i < var.getCount(); i++ )
        {
            var.setIteration(i);
            ExecutionMap map = new ExecutionMap();
            engine.buildCompartmentMap(map, compartment);
            for( WorkflowElement e : map )
                if( e instanceof AnalysisElement )
                    ( (AnalysisElement)e ).readParametersFromDiagram();
            executionMaps.add(map);
        }
        
        weight = 0;
        try
        {
            for( ExecutionMap eMap : executionMaps )
                for( WorkflowElement we : eMap )
                    weight += we.getWeight();
        }
        catch( Exception e )
        {
        }
        if( weight < 1 )
            weight = 1;
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        terminated = false;
        this.listener = listener;
        fjc = new FunctionJobControl(log);
        fjc.addListener(listener);
        fjc.functionStarted();
        try
        {
            init();
            for( ExecutionMap map : executionMaps )
                runExecutionMap(map);
        }
        catch( Exception e )
        {
            fjc.functionTerminatedByError(e);
        }
    }

    @Override
    public synchronized void terminate()
    {
        terminated = true;
        if( executionMaps != null )
        {
            for( ExecutionMap eMap : executionMaps )
                for( WorkflowElement we : eMap )
                    we.terminate();
            executionMaps = Collections.emptyList();
        }
    }

    @Override
    public synchronized int getPreparedness()
    {
        if( executionMaps == null )
            return 0;
        if( executionMaps.isEmpty() )
            return 100;
        int result = 0;
        for( ExecutionMap eMap : executionMaps )
            result += eMap.getCompletePercent();
        return result / executionMaps.size();
    }

    private void runExecutionMap(final ExecutionMap map)
    {
        if( terminated )
            return;
        if(isComplete()) {
            fjc.functionFinished();
            return;
        }
        WorkflowElement analysis;
        synchronized( map )
        {
            while( ( analysis = map.getAvailableElement() ) != null )
            {
                engine.scheduleElementExecution(analysis, new JobControlListener()
                {

                    @Override
                    public void valueChanged(JobControlEvent event)
                    {
                        fjc.setPreparedness(getPreparedness());
                    }

                    @Override
                    public void resultsReady(JobControlEvent event)
                    {
                        fjc.resultsAreReady(event.getResults());
                    }

                    @Override
                    public void jobTerminated(JobControlEvent event)
                    {
                        if( ( event == null ) || ( event.getStatus() == JobControl.COMPLETED ) )
                        {
                            runExecutionMap(map);
                            fjc.setPreparedness(getPreparedness());
                        }
                        else
                        {
                            listener.jobTerminated(event);
                        }
                    }

                    @Override
                    public void jobStarted(JobControlEvent event)
                    {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void jobResumed(JobControlEvent event)
                    {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void jobPaused(JobControlEvent event)
                    {
                        // TODO Auto-generated method stub

                    }
                });
            }
            map.notify();
        }
    }

}
