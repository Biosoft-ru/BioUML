package ru.biosoft.graphics.chart;

import org.json.JSONException;
import org.json.JSONObject;

public class AxisOptions
{
    public static enum Transform
    {
        LINEAR,
        LOGARITHM
    };
    
    private Transform transform = Transform.LINEAR;
    private Double min = null, max = null;
    private String label = null;
    private boolean rightOrTop = false;
    
    public AxisOptions()
    {
    }
    
    public AxisOptions(boolean rightOrTop)
    {
        setRightOrTop(rightOrTop);
    }
    
    public AxisOptions(JSONObject json)
    {
        if(json == null) return;
        if(json.has("min") && !json.isNull("min"))
            min = json.optDouble("min");
        if(json.has("max") && !json.isNull("max"))
            max = json.optDouble("max");
        if(json.has("label") && !json.isNull("label"))
            label = json.optString("label");
        if(json.optString("transform", "").equals("log"))
            transform = Transform.LOGARITHM;
        String pos = json.optString("position", "");
        if(pos.equals("right") || pos.equals("top"))
            rightOrTop = true;
    }
    
    public boolean isRightOrTop()
    {
        return rightOrTop;
    }
    
    public void setRightOrTop(boolean rightOrTop)
    {
        this.rightOrTop = rightOrTop;
        json = null;
    }

    public Double getMin()
    {
        return min;
    }

    public void setMin(Double min)
    {
        this.min = min;
        json = null;
    }

    public Double getMax()
    {
        return max;
    }

    public void setMax(Double max)
    {
        this.max = max;
        json = null;
    }
    
    public Transform getTransform()
    {
        return transform;
    }
    
    public void setTransform(Transform transform)
    {
        this.transform = transform;
        json = null;
    }
    
    public String getLabel()
    {
        return label;
    }
    
    public void setLabel(String label)
    {
        this.label = label;
        json = null;
    }
    
    private JSONObject json = null;
    public JSONObject toJSON() throws JSONException
    {
        if(json == null)
        {
            json = new JSONObject();
            if(min != null) json.put("min", min);
            if(max != null) json.put("max", max);
            if(transform == Transform.LOGARITHM) json.put("transform", "log");
            if(label != null && !label.equals("")) json.put("label", label);
            if(rightOrTop) json.put("position", "right");
        }
        return json.length()==0?null:json;
    }
}