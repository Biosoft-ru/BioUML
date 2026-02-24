package biouml.plugins.wdl.model;

import java.util.Iterator;

public class WorkflowInfo extends ExecutableInfo implements Iterable<Object>
{
    private ContainerInfo steps = new ContainerInfo();
    
    public WorkflowInfo(String name)
    {
        super(name);
    }

    public Object findStep(String name)
    {
        for( Object object : steps.getObjects() )
        {
            if( object instanceof CallInfo && ( (CallInfo)object ).getAlias().equals(name) )
                return object;
            else if( object instanceof ExpressionInfo && ( (ExpressionInfo)object ).getName().equals(name) )
                return object;
        }
        return null;
    }

    @Override
    public Iterator<Object> iterator()
    {
        return steps.iterator();
    }
    
    public void addObject(Object object)
    {
        steps.addObject( object );
    }
    
    public int size()
    {
        return steps.size();
    }
    
    public boolean isEmpty()
    {
        return steps.isEmpty();
    }
}
