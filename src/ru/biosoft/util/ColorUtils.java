package ru.biosoft.util;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * @author lan
 *
 */
public class ColorUtils
{
    private static final Pattern PATTERN_HEX3 = Pattern.compile("#([A-F0-9])([A-F0-9])([A-F0-9])", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_HEX6 = Pattern.compile("#([A-F0-9][A-F0-9])([A-F0-9][A-F0-9])([A-F0-9][A-F0-9])", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_RGB = Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_RGBA = Pattern.compile("rgba\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    @SuppressWarnings ( "serial" )
    private static final Map<String, Color> colors = new HashMap<String, Color>()
    {{
            put( "black", Color.BLACK );
            put( "blue", Color.BLUE );
            put( "red", Color.RED );
            put( "green", Color.GREEN );
            put( "yellow", Color.YELLOW );
            put( "gray", Color.GRAY );
            put( "white", Color.WHITE );
            put( "cyan", Color.CYAN );
            put( "darkGray", Color.DARK_GRAY );
            put( "lightGray", Color.LIGHT_GRAY );
            put( "magenta", Color.MAGENTA );
            put( "orange", Color.ORANGE );
            put( "pink", Color.PINK );
    }};
    
    public static @Nonnull Color parseColor(String colorStr)
    {
        Matcher m;
        colorStr = colorStr.trim();
        m = PATTERN_HEX3.matcher(colorStr);
        if(m.matches())
        {
            return new Color(Integer.parseInt(m.group(1), 16)*17, Integer.parseInt(m.group(2), 16)*17, Integer.parseInt(m.group(3), 16)*17);
        }
        m = PATTERN_HEX6.matcher(colorStr);
        if(m.matches())
        {
            return new Color(Integer.parseInt(m.group(1), 16), Integer.parseInt(m.group(2), 16), Integer.parseInt(m.group(3), 16));
        }
        m = PATTERN_RGB.matcher(colorStr);
        if(m.matches())
        {
            return new Color(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        }
        m = PATTERN_RGBA.matcher(colorStr);
        if(m.matches())
        {
            return new Color(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
        }
        Color c = colors.get(colorStr.toLowerCase());
        if(c != null) return c;
        return Color.BLACK;
    }
    
    public static String colorToString(Color color)
    {
        for(Entry<String, Color> entry: colors.entrySet())
        {
            if(entry.getValue().equals(color)) return entry.getKey();
        }
        if(color.getAlpha() == 255)
        {
            if(color.getRed() % 17 == 0 && color.getGreen() % 17 == 0 && color.getBlue() % 17 == 0)
                return String.format("#%X%X%X", color.getRed()/17, color.getGreen()/17, color.getBlue()/17);
            return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        }
        return "rgba("+color.getRed()+","+color.getGreen()+","+color.getBlue()+","+color.getAlpha()+")";
    }
    
    public static String paintToString(Paint paint)
    {
        if(paint instanceof Color)
        {
            return colorToString( (Color)paint );
        } else if(paint instanceof GradientPaint)
        {
            Point2D p1 = ( (GradientPaint)paint ).getPoint1();
            Point2D p2 = ( (GradientPaint)paint ).getPoint2();
            int angle = (int)(Math.atan2(p2.getX()-p1.getX(), p2.getY()-p1.getY()) * 180 / Math.PI);
            String result = colorToString( ( (GradientPaint)paint ).getColor1() ) + ":" + colorToString( ( (GradientPaint)paint ).getColor2() );
            if(angle == 0)
                return result;
            return result + ":" + angle;
        }
        // Unsupported paint
        return colorToString( Color.BLACK );
    }
    
    public static Paint parsePaint(String paint)
    {
        String[] tokens = TextUtil2.split( paint, ':' );
        if(tokens.length == 1)
            return parseColor( tokens[0] );
        float angle = 0;
        if(tokens.length > 2)
        {
            try
            {
                angle = (float) ( Double.parseDouble( tokens[2] ) * (Math.PI / 180) );
            }
            catch( NumberFormatException e )
            {
            }
        }
        return new GradientPaint(0, 0, parseColor(tokens[0]), (float) ( 100 * Math.sin(angle) ), (float) ( 100 * Math.cos(angle) ), parseColor(tokens[1]));
    }

    private static final Color[] DEFAULT_COLORS = new Color[] {
        Color.BLUE, Color.RED, Color.CYAN, Color.GRAY, Color.YELLOW, Color.MAGENTA, Color.ORANGE, Color.DARK_GRAY,
        Color.PINK, Color.BLACK, Color.BLUE.darker(), Color.RED.darker(), Color.GREEN.darker(), Color.CYAN.darker(), Color.YELLOW.darker(),
        Color.MAGENTA.darker(), Color.ORANGE.darker(), Color.PINK.darker()
    };
    
    /**
     * Return a color from some predefined colors palette
     * @param n number of color, must be >= 0. For large n values returned colors may repeat
     * @return Color object.
     */
    public static Color getDefaultColor(int n)
    {
        return DEFAULT_COLORS[n % DEFAULT_COLORS.length];
    }

    /**
     * Mixes two colors in the specified ratio
     * @param color1 first color to mix
     * @param color2 second color to mix
     * @param ratio fraction of second color in result (0 => result is color1, 1 => result is color2)
     * @return mixed color
     */
    public static Color mix(Color color1, Color color2, double ratio)
    {
        float[] comp1 = color1.getRGBColorComponents(null);
        float[] comp2 = color2.getRGBColorComponents(null);
        float[] compResult = new float[3];
        for(int i=0; i<3; i++)
            compResult[i] = (float) ( comp1[i]*(1-ratio)+comp2[i]*ratio );
        return new Color(compResult[0], compResult[1], compResult[2]);
    }
}
