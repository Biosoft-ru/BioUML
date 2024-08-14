
package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

/** BoxText class is used for Legend generation in color schemes. */
public class BoxText extends CompositeView
{
    public BoxText(Brush brush, String text, ColorFont font, Graphics graphics)
    {
        init(brush, text, font, graphics);
    }

    public BoxText(Brush brush, String text, Graphics graphics)
    {
        ColorFont font = ru.biosoft.graphics.GraphicProperties.getInstance().fontDefault;
        init(brush, text, font, graphics);
    }

    protected void init(Brush brush, String text, ColorFont font, Graphics graphics)
    {
        Pen pen = new Pen(1, Color.black);
        add(new BoxView(pen, brush, 2, 2, 12, 12));
        add(new TextView(text, new Point(18, 0), View.TOP, font, graphics));
    }
    
    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = super.toJSON();
        return result;
    }
    
}
