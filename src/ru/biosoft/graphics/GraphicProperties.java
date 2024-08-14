package ru.biosoft.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Hashtable;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.LazyValue;

/**
 * <b>GraphicContext</b> stores graphic attributes
 * used by graphics classes  for painting.<p>
 *
 * This attributes can include next objects:
 *
 * <li> <b>Pen</b>      - specifies pen for <b>Line</b>,
 *                        <b>Rect</b> and border of <b>BoxFramed</b>
 *                        painting </li>
 * <li> <b>Brush</b>    - specifies brush for <b>Box</b> and
 *                        <b>BoxFramed</b> painting</li>
 * <li> <b>Font</b>     - specifies font for <b>Text</b> painting </li>
 * <li> <b>Dimension</b>- specifies different margins</li><p>
 *
 * Next default attributes are set:
 *
 * <pre>
 * <b>Attribute       Attribute Name            Graphic objects, that use it</b>
 * -----------------------------------------------------------------------
 * Pen              Pen_Site                    SiteBase
 * Pen              Pen_SiteComposite
 * Pen              Pen_Ruler_Axis
 * Pen              Pen_Ruler_Ticks
 *
 * Brush            Brush_Site                  SiteBase
 * Brush            Brush_SiteCore
 * Brush            Brush_SiteFP
 *
 * ColorFont            Font_Title
 * ColorFont            Font_SiteTitle
 * ColorFont            Font_SitePosition           SiteBase
 * ColorFont            Font_SiteSequence           SiteBase
 * ColorFont            Font_DB_H1
 * ColorFont            Font_DB_H2
 * ColorFont            Font_DB_H3
 * ColorFont            Font_DB_Field
 * ColorFont            Font_DB_Value
 * ColorFont            Font_DB_Reference
 * ColorFont            Font_Preformat
 * ColorFont            Font_PreformatBold
 * ColorFont            Font_Error
 * ColorFont            Font_Ruler_Labels_Major
 * ColorFont            Font_Ruler_Labels_Minor
 * ColorFont            Font_Field_Label
 * ColorFont            Font_Field_Label_Obligatory
 * ColorFont            Font_Field_Value
 *
 * Dimension        Margin_Text
 * Dimension        Margin_Sites
 * Dimension        Margin_SiteComposite
 * Dimension        Margin_Ruler_Text
 *
 * Dimension        Size_BP
 * Dimension        Size_Scrollbar
 * Dimension        Size_Ruler_TickSize
 * Dimension        Size_Ruler_DecDig
 *
 * </pre>
 *
 * @see Pen
 * @see Brush
 *
 */
public class GraphicProperties
{
    protected GraphicProperties()
    {
        setFont("Font_Title", new ColorFont(new Font("SansSerif", Font.PLAIN, 20), Color.black));
        setFont("Font_Bold_Title", new ColorFont(new Font("SansSerif", Font.BOLD, 20), Color.black));

        setFont("Font_RulerDefault", new ColorFont(new Font("SansSerif", Font.PLAIN, 11), Color.black));
        setFont("Font_SequenceViewDefault", new ColorFont(new Font("Monospaced", Font.PLAIN, 14), Color.black));
        setFont("Font_SiteViewDefault", new ColorFont(new Font("Monospaced", Font.PLAIN, 14), Color.red));

        setFont("Font_SiteColorScheme", new ColorFont(new Font("SansSerif", Font.PLAIN, 12), Color.black));

        setFont("Font_RulerOverview", new ColorFont(new Font("Monospaced", Font.PLAIN, 10), Color.black));
        setFont("Font_SequenceViewOverview", new ColorFont(new Font("Monospaced", Font.PLAIN, 0), Color.black));
        setFont("Font_SiteViewOverview", new ColorFont(new Font("Monospaced", Font.PLAIN, 16), Color.red));

        setFont("Font_RulerDetailed", new ColorFont(new Font("Monospaced", Font.PLAIN, 14), Color.black));
        setFont("Font_SequenceViewDetailed", new ColorFont(new Font("Monospaced", Font.PLAIN, 14), Color.black));
        setFont("Font_SiteViewDetailed", new ColorFont(new Font("Monospaced", Font.PLAIN, 14), Color.red));
    }

    /** Hash table to store attributes. */
    protected Hashtable<String, Object> _hs = new Hashtable<>();

    ////////////////////////////////////////
    // Default Atributes
    //

    /** Default pen: black, 1 line thickness, solid. */
    public final Pen penDefault = new Pen(1, Color.black);

    /** Default brush: black, solid. */
    public final Brush brushDefault = new Brush(Color.black);

    /** Default ColorFont: SansSerif, PLAIN, 12, black. */
    public final ColorFont fontDefault = new ColorFont(new Font("SansSerif", Font.PLAIN, 12), Color.black);

    /** Default margin: (5,5). */
    public final Dimension marginDefault = new Dimension(5, 5);
    private static LazyValue<GraphicProperties> instance = new LazyValue<>( "GraphicProperties", GraphicProperties::new );

    /**
     * Creates graphic context with default settings.
     *
     * <pre>
     * <b>Attribute       Attribute Name            Default value</b>
     * ---------------------------------------------------------------------------
     * Pen              Pen_Site                    solid, 1, black
     * Pen              Pen_SiteComposite           solid, 1, blue
     * Pen              Pen_Ruler_Axis              solid, 1, black
     * Pen              Pen_Ruler_Ticks             solid, 1, black
     *
     * Brush            Brush_Site                  solid, magenta
     * Brush            Brush_SiteCore              solid, magenta
     * Brush            Brush_SiteFP                solid, yellow
     *
     * ColorFont            Font_Title                  TimesRoman, 16, PLAIN, black
     * ColorFont            Font_SiteTitle              TimesRoman, 14, PLAIN, red
     * ColorFont            Font_SitePosition           TimesRoman, 12, PLAIN, black
     * ColorFont            Font_SiteSequence           Courier,    12, PLAIN, blue
     * ColorFont            Font_DB_H1                  TimesRoman, 16, BOLD,  black
     * ColorFont            Font_DB_H2                  TimesRoman, 14, BOLD,  green
     * ColorFont            Font_DB_H3                  TimesRoman, 12, BOLD,  black
     * ColorFont            Font_DB_Field               TimesRoman, 12, PLAIN, blue
     * ColorFont            Font_DB_Value               TimesRoman, 12, PLAIN, black
     * ColorFont            Font_DB_Reference           TimesRoman, 12, PLAIN, blue
     * ColorFont            Font_Preformat              Courier,    12, PLAIN, black
     * ColorFont            Font_PreformatBold          Courier,    12, BOLD,  black
     * ColorFont            Font_Error                  TimesRoman, 12, PLAIN, red
     * ColorFont            Font_Ruler_Labels_Major     TimesRoman, 12, PLAIN, black
     * ColorFont            Font_Ruler_Labels_Minor     TimesRoman, 10, PLAIN, black
     * ColorFont            Font_Field_Label            Arial,      12, PLAIN, blue
     * ColorFont            Font_Field_Label_Obligatory Arial,      12, PLAIN, red
     * ColorFont            Font_Field_Value            Arial,      12, PLAIN, black
     *
     * Dimension        Margin_Graphics             ( 5, 5)
     * Dimension        Margin_Text                 ( 2, 2)
     * Dimension        Margin_Sites                (10,10)
     * Dimension        Margin_SiteComposite        ( 3, 3)
     * Dimension        Margin_Ruler_Text           ( 2, 2)
     *
     * Dimension        Size_BP                     ( 5,10)
     * Dimension        Size_Scrollbar              (15,15)
     * Dimension        Size_Ruler_TickSize         ( 5, 3)
     * Dimension        Size_Ruler_DecDig           ( 0, 0)
     *
     * </pre>
     *
     *     public GraphicContext()
     *     {
     *         setPen( "Pen_Site",          new Pen(Color.black, 1, Pen.SOLID) );
     *         setPen( "Pen_SiteComposite", new Pen(Color.blue,  1, Pen.SOLID) );
     *         setPen( "Pen_Ruler_Axis",    new Pen(Color.black, 1, Pen.SOLID) );
     *         setPen( "Pen_Ruler_Ticks",   new Pen(Color.black, 1, Pen.SOLID) );
     * 
     *         setBrush( "Brush_Site",      new Brush(Color.magenta, Brush.SOLID) );
     *         setBrush( "Brush_SiteCore",  new Brush(Color.magenta, Brush.SOLID) );
     *         setBrush( "Brush_SiteFP",    new Brush(Color.yellow,  Brush.SOLID) );
     * 
     *         setFont( "Font_Title",                  new ColorFont( new Font("TimesRoman", Font.PLAIN, 16), Color.black) );
     *         setFont( "Font_SiteTitle",              new ColorFont( new Font("TimesRoman", Font.BOLD,  14), Color.red  ) );
     *         setFont( "Font_SitePosition",           new ColorFont( new Font("TimesRoman", Font.PLAIN, 12), Color.black) );
     *         setFont( "Font_SiteSequence",           new ColorFont( new Font("Courier",    Font.PLAIN, 12), Color.blue ) );
     *         setFont( "Font_DB_H1",                  new ColorFont( new Font("TimesRoman", Font.BOLD,  16), Color.black) );
     *         setFont( "Font_DB_H2",                  new ColorFont( new Font("TimesRoman", Font.BOLD,  14), Color.black) );
     *         setFont( "Font_DB_H3",                  new ColorFont( new Font("TimesRoman", Font.BOLD,  14), Color.black) );
     *         setFont( "Font_DB_Field",               new ColorFont( new Font("Aryal",      Font.PLAIN, 14), Color.blue ) );
     *         setFont( "Font_DB_Value",               new ColorFont( new Font("Aryal",      Font.PLAIN, 14), Color.black) );
     *         setFont( "Font_DB_Reference",           new ColorFont( new Font("Aryal",      Font.PLAIN, 14), Color.blue ) );
     *         setFont( "Font_Preformat",              new ColorFont( new Font("Courier",    Font.PLAIN, 14), Color.black) );
     *         setFont( "Font_PreformatBold",          new ColorFont( new Font("Courier",    Font.BOLD,  14), Color.black) );
     *         setFont( "Font_Error",                  new ColorFont( new Font("Arial",      Font.BOLD,  12), Color.red  ) );
     *         setFont( "Font_Ruler_Labels_Major",     new ColorFont( new Font("TimesRoman", Font.PLAIN, 12), Color.black) );
     *         setFont( "Font_Ruler_Labels_Minor",     new ColorFont( new Font("TimesRoman", Font.PLAIN, 10), Color.black) );
     *         setFont( "Font_Field_Label",            new ColorFont( new Font("Arial",      Font.PLAIN, 12), Color.blue ) );
     *         setFont( "Font_Field_Label_Obligatory", new ColorFont( new Font("Arial",      Font.PLAIN, 12), Color.red  ) );
     *         setFont( "Font_Field_Value",            new ColorFont( new Font("Arial",      Font.PLAIN, 12), Color.black) );
     * 
     *         setMargin( "Margin_Graphics",      new Dimension(5, 5 ) );
     *         setMargin( "Margin_Text",          new Dimension(2, 2 ) );
     *         setMargin( "Margin_Sites",         new Dimension(10,10) );
     *         setMargin( "Margin_SiteComposite", new Dimension( 3, 3) );
     *         setMargin( "Margin_Ruler_Text",    new Dimension( 2, 2) );
     *         setMargin( "Size_BP",              new Dimension( 5,10) );
     *         setMargin( "Size_Scrollbar",       new Dimension(15,15) );
     *         setMargin( "Size_Ruler_TickSize",  new Dimension( 5, 3) );
     *         setMargin( "Size_Ruler_DecDig",    new Dimension( 0, 0) );
     *     }
    */

    ////////////////////////////////////////
    //  get methods
    //
    /**
     * Returns pen with specified name.
     * If such pen is absent, returns penDefault.
     *
     * @param penName the pen name
     */
    public Pen getPen(String penName)
    {
        Pen p;
        try
        {
            p = (Pen)_hs.get(penName);
        }
        catch( Exception e )
        {
            p = penDefault;
        }
        return p;
    }

    /**
     * Returns brush with specified name.
     * If such brush is absent, returns brushDefault.
     *
     * @param brushName the brush name
     */
    public Brush getBrush(String brushName)
    {
        Brush b;
        try
        {
            b = (Brush)_hs.get(brushName);
        }
        catch( Exception e )
        {
            b = brushDefault;
        }
        return b;
    }

    /**
     * Returns font with specified name.
     * If such font is absent, returns fontDefault.
     *
     * @param fontName the font name
     */
    public ColorFont getFont(String fontName)
    {
        ColorFont f;
        try
        {
            f = (ColorFont)_hs.get(fontName);
        }
        catch( Exception e )
        {
            f = fontDefault;
        }
        return f;
    }

    /**
     * Returns margin (or size) with specified name.
     * If such margin is absent, returns marginDefault.
     *
     * @param marginName the margin name
     */
    public Dimension getMargin(String marginName)
    {
        Dimension d;
        try
        {
            d = (Dimension)_hs.get(marginName);
        }
        catch( Exception e )
        {
            d = marginDefault;
        }
        return d;
    }


    ////////////////////////////////////////
    //  set methods
    //

    /**
     * Set (add if such pen absent) pen with specified name.
     *
     * @param penName the pen name
     * @param pen the pen
     */
    public void setPen(String penName, Pen pen)
    {
        _hs.remove(penName);
        _hs.put(penName, pen);
    }

    /**
     * Set (add if such brush absent) brush with specified name.
     *
     * @param brushName the brush name
     * @param brush the brush
     */
    public void setBrush(String brushName, Brush brush)
    {
        _hs.remove(brushName);
        _hs.put(brushName, brush);
    }

    /**
     * Set (add if such font absent) font with specified name.
     *
     * @param fontName the font name
     * @param font the font
     */
    public void setFont(String fontName, ColorFont font)
    {
        _hs.remove(fontName);
        _hs.put(fontName, font);
    }

    /**
     * Set (add if such margin absent) margin with specified name.
     *
     * @param marginName the margin name
     * @param margin the margin
     */
    public void setMargin(String marginName, Dimension margin)
    {
        _hs.remove(marginName);
        _hs.put(marginName, margin);
    }

    /**
     * @pending high Remove this method.
     *
     *
     */
    public static GraphicProperties getInstance()
    {
        return instance.get();
    }
}
