package ru.biosoft.galaxy.parameters;

/**
 * Class to represent HiddenParameter
 * @author lan
 */
public class HiddenParameter extends StringParameter
{
    public HiddenParameter(boolean output, String value)
    {
        super(output, value);
    }

    public HiddenParameter(boolean output)
    {
        super(output);
    }

    @Override
    public Parameter cloneParameter()
    {
        HiddenParameter result = new HiddenParameter(output, value);
        doCloneParameter(result);
        return result;
    }
}
