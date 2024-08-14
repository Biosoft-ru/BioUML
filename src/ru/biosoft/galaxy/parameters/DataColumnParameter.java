package ru.biosoft.galaxy.parameters;

import one.util.streamex.StreamEx;

public class DataColumnParameter extends SelectParameter
{
    private boolean numerical;

    public DataColumnParameter(boolean output)
    {
        super(output);
    }
    
    @Override
    public void setValue(String value)
    {
        super.setValue(preprocessValue(value));
    }
    
    @Override
    public void setValueFromTest(String value)
    {
        super.setValueFromTest(preprocessValue(value));
    }
    
    private String preprocessValue(String value)
    {
        if(isMultiple())
        {
            return StreamEx.split(value, ',').map( DataColumnParameter::removePrefix ).joining( "," );
        }
        else
        {
            return removePrefix(value);
        }
    }
    
    private static String removePrefix(String value)
    {
        if(value.startsWith("c"))
            return value.substring(1);
        return value;
    }
    
    public void setNumerical(boolean numerical)
    {
        this.numerical = numerical;
    }

    public boolean isNumerical()
    {
        return numerical;
    }

    @Override
    public Parameter cloneParameter()
    {
        DataColumnParameter result = new DataColumnParameter(output);
        doCloneParameter(result);
        return result;
    }

    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter(clone);
        DataColumnParameter result = (DataColumnParameter)clone;
        result.setMultiple(multiple);
        result.setNumerical(numerical);
    }
}
