package ru.biosoft.galaxy.parameters;

public class BooleanParameter extends ParameterSupport
{
    private boolean value;
    private String trueValue;
    private String falseValue;

    public BooleanParameter(boolean output)
    {
        super(output);
    }
    
    @Override
    public void setValue(String value)
    {
        this.value = Boolean.valueOf(value);
        fields.put("value", this.value);
    }
    
    public boolean getValue()
    {
        return value;
    }

    /**
     * @return the trueValue
     */
    public String getTrueValue()
    {
        return trueValue;
    }

    /**
     * @param trueValue the trueValue to set
     */
    public void setTrueValue(String trueValue)
    {
        this.trueValue = trueValue;
    }

    /**
     * @return the falseValue
     */
    public String getFalseValue()
    {
        return falseValue;
    }

    /**
     * @param falseValue the falseValue to set
     */
    public void setFalseValue(String falseValue)
    {
        this.falseValue = falseValue;
    }
    
    @Override
    public String toString()
    {
        String result = value?trueValue:falseValue;
        if(result == null)
            result = String.valueOf(value);
        return result;
    }

    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter(clone);
        BooleanParameter result = (BooleanParameter)clone;
        result.setFalseValue(falseValue);
        result.setTrueValue(trueValue);
        result.value = value;
    }

    @Override
    public Parameter cloneParameter()
    {
        BooleanParameter result = new BooleanParameter(output);
        doCloneParameter(result);
        return result;
    }
}
