package ru.biosoft.graphics;

import java.awt.Color;

import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class ColorEditor extends CustomEditorSupport
{
    private Color color;

    @Override public void setValue(Object value)
    {
        Color color = (Color) value;
        this.color = color;
        firePropertyChange();
    }

    @Override public Object getValue()
    {
        return color;
    }

    @Override public void setAsText(String text) throws IllegalArgumentException
    {
        Color color = parseColor(text);
        this.color = color;
        firePropertyChange();
    }

    /**
     * Parses Color string
     * 
     * @param str color in format [r,g,b] or empty string for absent color
     * @return
     * @throws JSONException
     */
    public static Color parseColor(String str)
    {
        Color newColor;
        if( str == null || str.isEmpty() )
            newColor = new Color(0, 0, 0, 0);
        else
        {
            String[] strArr = str.substring(1, str.length() - 1).replaceAll("[^\\d,.]", "").split(",");
            if( strArr.length < 3 )
                newColor = new Color(0, 0, 0, 0);
            else
                newColor = new Color(Integer.valueOf(strArr[0]), Integer.valueOf(strArr[1]), Integer.valueOf(strArr[2]), strArr.length > 3 ? Integer.valueOf(strArr[3]) : 0);
        }
        return newColor;
    }

    /**
     * Counterpart for parseColor
     * 
     * @param color color to encode
     * @return
     */
    private static String encodeColor(Color color)
    {
        if( color == null || color.getAlpha() == 0 )
            return "";
        return "[" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "]";
    }
}
