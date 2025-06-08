package biouml.plugins.wdl.parser.validator;

import java.util.HashMap;
import java.util.Map;

public class TaskPrototype extends Scope
{
    Map<String, Field> inputs = new HashMap<String, Field>();
    Map<String, Field> outputs = new HashMap<String, Field>();
    private String command;
    Map<String, Field> runtime = new HashMap<String, Field>(); // or requirements in versions > 1.1
    Map<String, Field> hints = new HashMap<String, Field>();

    public TaskPrototype(String name)
    {
        super(name);
    }

    public void addInput(Field input)
    {
        inputs.put(input.getName(), input);
    }

    public void addOutput(Field output)
    {
        outputs.put(output.getName(), output);
    }

    public void setCommand(String command)
    {
        this.command = command;
    }

    public String getCommand()
    {
        return command;
    }

    public Map<String, Field> getOutputs()
    {
        return outputs;
    }
}
