package ru.biosoft.graphics.chart;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author lan
 *
 */
public class LinesOptions
{
    boolean show = true;
    boolean isShapesVisible = false;
    
    public LinesOptions()
    {
    }
    
    public LinesOptions(JSONObject from)
    {
        if(from == null) return;
        show = from.optBoolean("show", true);
        isShapesVisible = from.optBoolean("isShapesVisible", false );
    }
    
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = null;
        if(!show)
        {
            result = new JSONObject();
            result.put("show", false);
        }
        if(isShapesVisible)
        {
            if(result == null)
                result = new JSONObject();
            result.put("isShapesVisible", true);
        }
        return result;
    }

    public boolean isShow()
    {
        return show;
    }

    public void setShow(boolean show)
    {
        this.show = show;
    }

    public boolean isShapesVisible()
    {
        return isShapesVisible;
    }

    public void setShapesVisible(boolean isShapesVisible)
    {
        this.isShapesVisible = isShapesVisible;
    }
}
