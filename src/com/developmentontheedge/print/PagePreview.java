package com.developmentontheedge.print;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;

import javax.swing.JPanel;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings ( "serial" )
public class PagePreview extends JPanel
{
    protected static final Logger log = Logger.getLogger( PagePreview.class.getName() );

    public PagePreview(JPrintableComponent component, PageFormat format, int index)
    {
        this.component = component;
        this.format = format;
        this.index = index;
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension((int)format.getWidth(), (int)format.getHeight());
    }

    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    @Override
    public Dimension getSize()
    {
        return getPreferredSize();
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        Graphics2D g2 = (Graphics2D)g;

        int width  = (int)format.getWidth();
        int height = (int)format.getHeight();

        // draw page background and border
        g2.setColor(Color.white);
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.black);
        g2.drawRect(0, 0, width-1, height-1);

        int iX  = (int)format.getImageableX();
        int iY  = (int)format.getImageableY();
        int iWidth  = (int)format.getImageableWidth();
        int iHeight = (int)format.getImageableHeight();

        // draw border of viewable area
        g2.setColor(Color.gray);
        g2.drawLine(       iX-1,            0,        iX-1,       height);
        g2.drawLine(iX+iWidth+1,            0, iX+iWidth+1,       height);
        g2.drawLine(          0,         iY-1,       width,         iY-1);
        g2.drawLine(          0, iY+iHeight+1,       width, iY+iHeight+1);

        // draw border of footer
        if(format instanceof FooterFormat)
        {
            iHeight = (int)((FooterFormat)format).getImageableHeightAtFooter();
            g2.drawLine(0, iY+iHeight+1, width, iY+iHeight+1);
        }

        // draw contents
        try
        {
            component.preview(g, format, index);
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private JPrintableComponent component = null;
    private PageFormat format = null;
    private int index = 0;
}

