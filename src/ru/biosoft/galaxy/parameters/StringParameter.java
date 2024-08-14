package ru.biosoft.galaxy.parameters;

/**
 * Simple parameter
 */
public class StringParameter extends ParameterSupport
{
    protected String value = "";

    public StringParameter(boolean output)
    {
        super(output);
    }

    public StringParameter(boolean output, String value)
    {
        this(output);
        setValue(value);
    }

    @Override
    public void setValue(String value)
    {
        this.value = value;
        fields.put("value", value);
    }

    @Override
    public String toString()
    {
        return value;
    }

    @Override
    public Parameter cloneParameter()
    {
        StringParameter result = new StringParameter(output, value);
        doCloneParameter(result);
        return result;
    }
}
