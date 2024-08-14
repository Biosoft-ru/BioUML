
package ru.biosoft.bsa.view;

import java.awt.Color;
import java.awt.Rectangle;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;


public class TrackBackgroundView extends BoxView
{
    private static Color COLOR = new Color(200, 200, 200, 50);
    
    public TrackBackgroundView(Rectangle trackBounds, int width)
    {
        super(null, new Brush(COLOR), convertRectangle(trackBounds, width));
    }

    private static Rectangle convertRectangle(Rectangle trackBounds, int width)
    {
        return new Rectangle(0, trackBounds.y, width, trackBounds.height);
    }
}
