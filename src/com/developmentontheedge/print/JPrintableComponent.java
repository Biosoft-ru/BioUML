package com.developmentontheedge.print;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.swing.JComponent;

public class JPrintableComponent extends LargePageable implements Printable
{
    private double mScaleX;
    private double mScaleY;

    /**
     * The Swing component to print.
     */
    private JComponent mComponent;

    /**
     * Create a Pageable that can print a
     * Swing JComponent over multiple pages.
     *
     * @param c The swing JComponent to be printed.
     *
     * @param format The size of the pages over which
     * the componenent will be printed.
     */
    public JPrintableComponent(JComponent c, PageFormat format)
    {
        setPageFormat(format);
        setPrintable(this);
        setComponent(c);

        //Tell the LargePageable we subclassed the size of the canvas.
        //Rectangle componentBounds = c.getBounds(null);
        Dimension componentBounds = c.getPreferredSize();
        setSize(componentBounds.width, componentBounds.height);
        setScale(1, 1);

        //Listen for component's bounds changes
        c.addComponentListener(sizeListener);
    }

    protected void setComponent(JComponent c)
    {
        mComponent = c;
    }

    protected void setScale(double scaleX, double scaleY)
    {
        mScaleX = scaleX;
        mScaleY = scaleY;
    }

    public void scaleToFitX()
    {
        PageFormat format = getPageFormat();
        Rectangle componentBounds = mComponent.getBounds(null);
        double scaleX = format.getImageableWidth() /componentBounds.width;
        double scaleY = scaleX;
        if(scaleX < 1)
        {
            setSize((float)format.getImageableWidth(),
                    (float)(componentBounds.height * scaleY));
            setScale(scaleX, scaleY);
        }
    }

    public void scaleToFitY()
    {
        PageFormat format = getPageFormat();
        Rectangle componentBounds = mComponent.getBounds(null);
        double scaleY = format.getImageableHeight() /componentBounds.height;
        double scaleX = scaleY;
        if(scaleY < 1)
        {
            setSize( (float) (componentBounds.width * scaleX),(float) format.getImageableHeight());
            setScale(scaleX, scaleY);
        }
    }

    public void scaleToFit(boolean useSymmetricScaling)
    {
        PageFormat format = getPageFormat();
        Rectangle componentBounds = mComponent.getBounds(null);
        double scaleX = format.getImageableWidth() /componentBounds.width;
        double scaleY = format.getImageableHeight() /componentBounds.height;
        if(scaleX < 1 || scaleY < 1)
        {
            if(useSymmetricScaling)
            {
                if(scaleX < scaleY)
                {
                    scaleY = scaleX;
                }
                else
                {
                    scaleX = scaleY;
                }
            }
            setSize( (float) (componentBounds.width * scaleX), (float) (componentBounds.height * scaleY) );
            setScale(scaleX, scaleY);
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
    {
        Graphics2D g2 = (Graphics2D)graphics.create();

        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        Rectangle componentBounds = mComponent.getBounds(null);
        g2.translate(-componentBounds.x, -componentBounds.y);
        g2.scale(mScaleX, mScaleY);

        boolean wasBuffered = mComponent.isDoubleBuffered();
        mComponent.setDoubleBuffered(false);
        mComponent.paint(g2);
        mComponent.setDoubleBuffered(wasBuffered);

        g2.dispose();

        return PAGE_EXISTS;
    }

    public void preview(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
    {
        Printable printable = getPrintable(pageIndex);
        printable.print(graphics, pageFormat, pageIndex);
    }

    private ComponentListener sizeListener = new ComponentListener()
                {
                    @Override
                    public void componentResized(ComponentEvent e)
                    {
                        updateBounds();
                    }
                    @Override
                    public void componentMoved(ComponentEvent e)
                    {
                        updateBounds();
                    }
                    @Override
                    public void componentShown(ComponentEvent e)
                    {
                        updateBounds();
                    }
                    @Override
                    public void componentHidden(ComponentEvent e)
                    {
                        updateBounds();
                    }
                    protected void updateBounds()
                    {
                        Rectangle componentBounds = mComponent.getBounds(null);
                        setSize(componentBounds.width, componentBounds.height);
                    }
                };

}
