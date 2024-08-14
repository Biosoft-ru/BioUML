package ru.biosoft.graphics.chart;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author lan
 *
 */
public class BarsOptions
{
    boolean show = false;
    boolean centered = false;
    double width = 1;
    
    public BarsOptions()
    {
    }
    
    public BarsOptions(JSONObject from)
    {
        if(from == null) return;
        show = from.optBoolean("show", false);
        centered = from.optString("align", "left").equals("center");
        width = from.optDouble("barWidth", 1);
    }
    
    public JSONObject toJSON() throws JSONException
    {
        if(show)
        {
            JSONObject result = new JSONObject();
            result.put("show", true);
            if(centered) result.put("align", "center");
            result.put("barWidth", width);
            return result;
        }
        return null;
    }

    public boolean isShow()
    {
        return show;
    }

    public void setShow(boolean show)
    {
        this.show = show;
    }

    public boolean isCentered()
    {
        return centered;
    }

    public void setCentered(boolean centered)
    {
        this.centered = centered;
    }

    public double getWidth()
    {
        return width;
    }

    public void setWidth(double width)
    {
        this.width = width;
    }
}
