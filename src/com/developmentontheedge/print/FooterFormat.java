package com.developmentontheedge.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class FooterFormat extends PageFormat
{
    /**
     * The font we use for the footer.
     */
    private static final Font mFooterFont = new Font("Serif", Font.ITALIC, 14);

    /**
     * The amount of space at the bottom of the imageable area that we
     * reserve for the footer.
     */
    private static final float mFooterHeight = (float) (0.3 * 72);

    public double getImageableHeightAtFooter()
    {
        double imageableHeight = getImageableHeight() - mFooterHeight;
        if(imageableHeight < 0) imageableHeight = 0;
        return imageableHeight;
    }

    /**
     * Draws the footer text which has the following format:
     * <date>
     */
    //public int print(Graphics g, PageFormat format, int pageIndex)
    public int print(Graphics g, PageFormat format, String textToPrint)
    {
        /* Make a copy of the passed in Graphics instance so
         * that we do not upset the caller's current Graphics
         * settings such as the current color and font.
         */
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setPaint(Color.black);
        g2d.setFont(mFooterFont);
        LineMetrics metrics = mFooterFont.getLineMetrics(textToPrint, g2d.getFontRenderContext());

        /* We will draw the footer at the bottom of the imageable
         * area. We subtract off the font's descent so that the bottoms
         * of descenders remain visable.
         */
        float y = (float) (super.getImageableY() + super.getImageableHeight()- metrics.getDescent() - metrics.getLeading());

        // Cast to an int because of printing bug in drawString(String, float, float)!
        g2d.drawString(textToPrint, (int)super.getImageableX(), (int)y);
        g2d.dispose();
        return Printable.PAGE_EXISTS;
    }
}
