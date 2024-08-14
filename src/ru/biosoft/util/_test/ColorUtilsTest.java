package ru.biosoft.util._test;

import java.awt.Color;
import java.util.Random;

import ru.biosoft.util.ColorUtils;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class ColorUtilsTest extends TestCase
{
    public void testColorToString() throws Exception
    {
        assertEquals("gray", ColorUtils.colorToString(Color.GRAY));
        assertEquals("#123456", ColorUtils.colorToString(new Color(0x12, 0x34, 0x56)));
        assertEquals("rgba(12,34,56,78)", ColorUtils.colorToString(new Color(12, 34, 56, 78)));
        Random random = new Random();
        for(int i=0; i<10; i++)
        {
            Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), random.nextBoolean()?255:131);
            assertEquals(color, ColorUtils.parseColor(ColorUtils.colorToString(color)));
        }
    }
    
    public void testParseColor() throws Exception
    {
        assertEquals(Color.GRAY, ColorUtils.parseColor("gray"));
        assertEquals(Color.BLACK, ColorUtils.parseColor("black"));
        assertEquals(Color.BLACK, ColorUtils.parseColor("invalid"));
        assertEquals(new Color(0x11,0x22,0x33), ColorUtils.parseColor("#123"));
        assertEquals(new Color(0x12,0x34,0x56), ColorUtils.parseColor("#123456"));
        assertEquals(new Color(100,200,255), ColorUtils.parseColor("rgb(100, 200,255   )"));
        assertEquals(new Color(100,200,255,1), ColorUtils.parseColor("rgba(100, 200,255 , 1  )"));
    }
    
    public void testMixColors() throws Exception
    {
        assertEquals(Color.GRAY, ColorUtils.mix(Color.BLACK, Color.WHITE, 0.5));
    }
}
