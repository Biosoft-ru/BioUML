package ru.biosoft.graphics._test;

import java.awt.Color;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;
import junit.framework.TestCase;

public class ViewTest extends TestCase
{
    public void testEquals()
    {
        View v1 = new BoxView(new Pen(1, Color.BLACK), null, 0, 0, 10, 10);
        v1.setModel("test");
        View v2 = new BoxView(new Pen(1, Color.BLACK), null, 0, 0, 10, 10);
        assertFalse(v1.equals(v2));
        assertFalse(v2.equals(v1));
        v2.setModel("test");
        assertTrue(v2.equals(v1));
        View v3 = new BoxView(new Pen(1, Color.BLACK), null, 0, 0, 10, 11);
        v3.setModel("test");
        assertFalse(v3.equals(v1));
        View v4 = new BoxView(new Pen(1, Color.BLACK), new Brush(Color.BLACK), 0, 0, 10, 10);
        v4.setModel("test");
        assertFalse(v4.equals(v1));
        assertFalse(v1.equals(v4));
    }
}
