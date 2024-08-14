package ru.biosoft.galaxy.parameters;

import org.json.JSONObject;


public class FloatParameter extends ParameterSupport
{
    private Float value;

    public FloatParameter()
    {
        super(false);
    }

    @Override
    public void setValue(String value)
    {
        try
        {
            this.value = Float.parseFloat(value);
            fields.put("value", value);
        }
        catch( NumberFormatException e )
        {
            this.value = null;
            fields.put("value", JSONObject.NULL);
        }
    }

    @Override
    public Parameter cloneParameter()
    {
        FloatParameter clone = new FloatParameter();
        doCloneParameter(clone);
        return clone;
    }

    @Override
    public String toString()
    {
        return value == null ? "" : String.valueOf(value);
    }
}
