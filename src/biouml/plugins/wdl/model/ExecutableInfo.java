package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.List;

public class ExecutableInfo
{
    private String name;
    private MetaInfo meta = new MetaInfo();
    private List<ExpressionInfo> inputs = new ArrayList<>();
    private List<ExpressionInfo> outputs = new ArrayList<>();

    public ExecutableInfo(String name)
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

    public void addInput(ExpressionInfo input)
    {
        inputs.add(input);
    }

    public void addOutput(ExpressionInfo output)
    {
        outputs.add(output);
    }

    public List<ExpressionInfo> getInputs()
    {
        return List.copyOf(inputs);
    }

    public List<ExpressionInfo> getOutputs()
    {
        return List.copyOf(outputs);
    }
}
