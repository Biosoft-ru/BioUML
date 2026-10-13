package ru.biosoft.math.view._test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.TestCase;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.math.view.FormulaViewBuilder;

public class FormulaViewBuilderTest extends TestCase
{
    /**
     * A diagram's formula views share their fonts, and a font's JSON is serialized by concurrent web requests.
     * Building a title with subscripts ("rate_re1") used to change the font temporarily, which reset its cached
     * JSON: a request serializing the diagram at that moment failed with an NPE in ColorFont.toJSON.
     */
    public void testTitleWithSubscriptsKeepsFont() throws Exception
    {
        FormulaViewBuilder builder = new FormulaViewBuilder();
        Graphics g = ApplicationUtils.getGraphics();
        ColorFont font = new ColorFont( "Arial", Font.PLAIN, 11, Color.black );
        Font original = font.getFont();
        JSONObject json = font.toJSON();

        builder.createTitleView( "$$rate_re_vaccination", font, g );

        assertSame( "font unchanged", original, font.getFont() );
        assertSame( "cached JSON kept", json, font.toJSON() );
    }
}
