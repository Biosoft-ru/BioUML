package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.List;

public class WorkflowInfo extends ContainerInfo
{
    private String name;
    private MetaInfo meta = new MetaInfo();
    private List<InputInfo> inputs = new ArrayList<>();
    private List<OutputInfo> outputs = new ArrayList<>();

    public WorkflowInfo(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public MetaInfo getMeta()
    {
        return meta;
    }

    public void setMeta(MetaInfo metaInfo)
    {
        meta = metaInfo;
    }

    public void addInput(InputInfo input)
    {
        inputs.add(input);
    }

    public void addOutput(OutputInfo output)
    {
        outputs.add(output);
    }

    public List<InputInfo> getInputs()
    {
        return List.copyOf(inputs);
    }

    public List<OutputInfo> getOutputs()
    {
        return List.copyOf(outputs);
    }

    public Object findStep(String name)
    {
        for( Object object : getObjects() )
        {
            if( object instanceof CallInfo && ( (CallInfo)object ).getAlias().equals(name) )
                return object;
            else if( object instanceof ExpressionInfo && ( (ExpressionInfo)object ).getName().equals(name) )
                return object;
        }
        return null;
    }
}
