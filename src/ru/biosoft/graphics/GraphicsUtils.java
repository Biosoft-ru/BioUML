package ru.biosoft.graphics;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.HtmlView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;

public class GraphicsUtils
{
	public static boolean isNotDefault(Pen pen)
    {
        return pen != null && !Color.WHITE.equals( pen.getColor() );
    }

    public static ComplexTextView getAsComplexTextView(HtmlView view)
    {
        // Dirty solution: rough estimate of string length
        // ComplexTextView should support wrapping by pixels-width, not only text width
        Graphics graphics = ApplicationUtils.getGraphics();
        Rectangle rect = (Rectangle)view.getShape();
        Map<String, ColorFont> fontRegistry = new HashMap<>();
        ColorFont cf = view.getColorFont();
        FontMetrics fm = graphics.getFontMetrics();
        String text = view.getText();
        return new ComplexTextView( text, new Point( rect.x, rect.y ), View.LEFT | View.BASELINE, cf, fontRegistry, ComplexTextView.TEXT_ALIGN_LEFT,
                rect.width * text.length() / fm.stringWidth( text ), graphics );
    }
}
