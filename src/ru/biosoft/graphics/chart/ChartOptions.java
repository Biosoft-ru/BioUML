package ru.biosoft.graphics.chart;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * ChartOptions is a container for chart options
 * @author lan
 */
public class ChartOptions
{
    private final List<AxisOptions> xAxes = new ArrayList<>(), yAxes = new ArrayList<>();
    
    public ChartOptions()
    {
        xAxes.add(new AxisOptions());
        yAxes.add(new AxisOptions());
    }
    
    public ChartOptions(JSONObject json)
    {
        if(json == null)
        {
            xAxes.add(new AxisOptions());
            yAxes.add(new AxisOptions());
            return;
        }
        // TODO: support default options in x/yaxis and overriding them in x/yaxes as in flot documentation
        if(json.has("xaxis") && !json.isNull("xaxis"))
            xAxes.add(new AxisOptions(json.optJSONObject("xaxis")));
        JSONArray jsonXAxes = json.optJSONArray("xaxes");
        if(jsonXAxes != null)
        {
            for(int i=0; i<jsonXAxes.length(); i++)
                xAxes.add(new AxisOptions(jsonXAxes.optJSONObject(i)));
        }
        if(json.has("yaxis") && !json.isNull("yaxis"))
            yAxes.add(new AxisOptions(json.optJSONObject("yaxis")));
        JSONArray jsonYAxes = json.optJSONArray("yaxes");
        if(jsonYAxes != null)
        {
            for(int i=0; i<jsonYAxes.length(); i++)
                yAxes.add(new AxisOptions(jsonYAxes.optJSONObject(i)));
        }
        if(xAxes.isEmpty()) xAxes.add(new AxisOptions());
        if(yAxes.isEmpty()) yAxes.add(new AxisOptions());
    }
    
    public AxisOptions getXAxis(int n)
    {
        return xAxes.get(n-1);
    }
    
    public AxisOptions getXAxis()
    {
        return getXAxis(1);
    }
    
    public int getXAxisCount()
    {
        return Math.max(xAxes.size(),1);
    }
    
    public void addXAxis(AxisOptions xaxis)
    {
        xAxes.add(xaxis);
    }

    public void setXAxis(AxisOptions xaxis)
    {
        xAxes.clear();
        addXAxis(xaxis);
    }
    
    public AxisOptions getYAxis(int n)
    {
        return yAxes.get(n-1);
    }

    public AxisOptions getYAxis()
    {
        return getYAxis(1);
    }

    public int getYAxisCount()
    {
        return Math.max(yAxes.size(),1);
    }
    
    public void setYAxis(AxisOptions yaxis)
    {
        yAxes.clear();
        addYAxis(yaxis);
    }
    
    public void addYAxis(AxisOptions yaxis)
    {
        yAxes.add(yaxis);
    }

    public JSONObject toJSON() throws JSONException
    {
        JSONObject json = new JSONObject();
        if(!xAxes.isEmpty())
        {
            if(xAxes.size() == 1)
            {
                JSONObject axisJson = getXAxis().toJSON();
                if(getXAxis() != null && axisJson!=null) json.put("xaxis", correctAxis(axisJson, true));
            } else
            {
                JSONArray jsonXAxes = new JSONArray();
                for(int i=0; i<xAxes.size(); i++)
                {
                    JSONObject jsonXAxis = xAxes.get(i).toJSON();
                    jsonXAxes.put(jsonXAxis == null?new JSONObject():correctAxis(jsonXAxis, true));
                }
                json.put("xaxes", jsonXAxes);
            }
        }
        if(!yAxes.isEmpty())
        {
            if(yAxes.size() == 1)
            {
                JSONObject axisJson = getYAxis().toJSON();
                if(getYAxis() != null && axisJson!=null) json.put("yaxis", correctAxis(axisJson, false));
            } else
            {
                JSONArray jsonYAxes = new JSONArray();
                for(int i=0; i<yAxes.size(); i++)
                {
                    JSONObject jsonYAxis = yAxes.get(i).toJSON();
                    jsonYAxes.put(jsonYAxis == null?new JSONObject():correctAxis(jsonYAxis, false));
                }
                json.put("yaxes", jsonYAxes);
            }
        }
        return json;
    }
    
    private JSONObject correctAxis(JSONObject axisOptions, boolean isX)
    {
        if(!isX) return axisOptions;
        if(axisOptions.optString("position", "").equals("right"))
        {
            try
            {
                axisOptions.put("position", "top");
            }
            catch( JSONException e )
            {
            }
        }
        return axisOptions;
    }
}
