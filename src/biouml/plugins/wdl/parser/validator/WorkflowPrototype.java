package biouml.plugins.wdl.parser.validator;

import java.util.ArrayList;
import java.util.List;

public class WorkflowPrototype extends TaskPrototype
{
    private List<CallPrototype> calls = new ArrayList<CallPrototype>();
    public WorkflowPrototype(String name) throws Exception
    {
        super(name);
    }

    public void addCall(CallPrototype callPrototype)
    {
        calls.add(callPrototype);

    }

    public List<CallPrototype> getCalls()
    {
        return calls;
    }


}
