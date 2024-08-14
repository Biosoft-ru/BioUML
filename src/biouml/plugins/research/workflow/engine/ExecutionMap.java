package biouml.plugins.research.workflow.engine;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Structure for saving element status and element dependencies
 */
public class ExecutionMap implements Iterable<WorkflowElement>
{
    protected Set<WorkflowElement> elementSet;

    public ExecutionMap()
    {
        this.elementSet = new HashSet<>();
    }

    /**
     * Add workflow element
     */
    public void addElement(WorkflowElement element)
    {
        elementSet.add(element);
    }

    /**
     * Get next available workflow or null if no available analysis
     */
    public WorkflowElement getAvailableElement()
    {
        for( WorkflowElement element : elementSet )
        {
            if( !element.isComplete() && !element.isStarted() && element.readyToExecute() )
            {
                return element;
            }
        }
        return null;
    }

    /**
     * Get complete elements percent
     */
    public int getCompletePercent()
    {
        double i = 0;
        double total = 0;
        for( WorkflowElement we : elementSet )
        {
            double weight = we.getWeight();
            if( weight > 0 )
            {
                total += weight;
                if( we.isComplete() )
                {
                    i+=weight;
                } else
                {
                    i+=(weight*we.getPreparedness())/100;
                }
            }
        }
        return total == 0?100:(int) ( i / total * 100.0 );
    }

    /**
     * Check if workflow execution complete
     */
    public boolean isComplete()
    {
        for( WorkflowElement ae : elementSet )
        {
            if( !ae.isComplete() )
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<WorkflowElement> iterator()
    {
        return elementSet.iterator();
    }
}
