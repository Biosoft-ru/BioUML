package ru.biosoft.table;

import java.awt.Color;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author lan
 *
 */
public class DescribedString
{
    private String title;
    private String html;
    private Color color;
    
    public DescribedString(String content)
    {
        try
        {
            JSONArray jsonArray = new JSONArray(content);
            title = jsonArray.getString(0);
            html = jsonArray.getString(1);
            if(jsonArray.length() > 2)
            {
                JSONArray jsonColor = jsonArray.getJSONArray(2);
                color = new Color(jsonColor.getInt(0), jsonColor.getInt(1), jsonColor.getInt(2));
            }
        }
        catch( JSONException e )
        {
            title = "";
            html = "";
        }
    }

    public DescribedString(String title, String html)
    {
        this(title, html, null);
    }
    
    public DescribedString(String title, String html, Color color)
    {
        this.title = title;
        this.html = html;
        this.color = color;
    }

    public String getTitle()
    {
        return title;
    }

    public String getHtml()
    {
        return html;
    }
    
    public Color getColor()
    {
        return color;
    }

    @Override
    public String toString()
    {
        if(color == null)
        {
            return new JSONArray(Arrays.asList(title, html)).toString();
        }
        JSONArray jsonColor = new JSONArray(Arrays.asList(color.getRed(), color.getGreen(), color.getBlue()));
        return new JSONArray(Arrays.asList(title, html, jsonColor)).toString();
    }
}
