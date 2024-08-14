
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 07.02.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */
package ru.biosoft.bsa.view;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;

import one.util.streamex.IntStreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.resources.Resources;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.Ruler;
import ru.biosoft.graphics.RulerOptions;
import ru.biosoft.graphics.TextView;

/**
 * Class for visualisation of the siqences.
 *
 * @author Igor V. Tyazhev
 */
public class SequenceView extends CompositeView
{
    private static final Logger log = Logger.getLogger(SequenceView.class.getName());
    
    /**
     * Sequence for which this view is created.
     */
    protected Sequence sequence;

    /**
     * Settings of the view.
     */
    protected SequenceViewOptions options;

    /**
     * Begin of this sequence relative to its parent.
     */
    protected int start;

    /**
     * End of this sequence relative to its parent.
     */
    protected int end;

    protected Graphics graphics;

    private BoxView selection;

    /**
     * Creates SequenceView for the specified <code>sequence</code>
     *
     * @param sequence sequence for which this view is created.
     * @param start    begin of this sequence relative to its parent.
     * @param end      end of this sequence relative to its parent.
     * @param graphics graphics for drawing.
     */
    public SequenceView(Sequence sequence, SequenceViewOptions options, int start, int end, Graphics graphics)
    {
        this.sequence = sequence;
        this.options = options;
        this.start = start;
        this.end = end;
        this.graphics = graphics;

        Color backColor = new Color(170, 200, 250);

        CompositeView front = new CompositeView();

        FontMetrics fontMetrics = graphics.getFontMetrics(options.getFont().getFont());

        String s = null;
        double factor = 0;

        RulerOptions rulerOptions = options.getRulerOptions();

        int textLength = 0;
        if( options.getType() == SequenceViewOptions.PT_RULER )
        {
            factor = getNucleotideWidth();
        }
        else
        {
            s = getSubSequence(start, end);
            textLength = fontMetrics.stringWidth(s);
            factor = (float)textLength / (float) ( end - start + 1 );
        }

        int offset = 0;
        int selectionHeight = 0;

        if( options.getType() != 0 )
        {
            int type = Ruler.HORIZONTAL | Ruler.TICKS_MAJOR_UP | Ruler.TICKS_MINOR_UP | Ruler.LABELS_MAJOR_SHOW | Ruler.LABELS_MAJOR_UP;
            //| Ruler.LABELS_MINOR_SHOW;

            int shift = (int) ( getNucleotideWidth() / 2f );

            Ruler ruler = new Ruler(type, new Point(shift, 0), factor, start, end, rulerOptions, options.getDensity(), graphics);
            selectionHeight = ruler.getBounds().height / 4;
            offset = selectionHeight;
            front.add(ruler);
        }
        if( options.getType() != SequenceViewOptions.PT_RULER )
        {
            TextView text = new TextView(s, options.getFont(), graphics);
            front.add(text, CompositeView.Y_BT);
            selectionHeight += text.getBounds().height;
        }
        selection = new BoxView(new Pen(0, backColor), new Brush(backColor), 0, 0, 30, selectionHeight);
        add(selection);
        selection.move(0, -offset);
        selection.setVisible(false);
        updateBounds();
        add(front);

    }

    public double getNucleotideWidth()
    {
        return getNucleotideWidth(options, graphics);
    }

    public static double getNucleotideWidth(SequenceViewOptions options, Graphics graphics)
    {
        if( options.getType() == SequenceViewOptions.PT_RULER )
        {
            double pixelsPerNucleotide = options.getDensity();
            if( pixelsPerNucleotide == 0. )
            {
                pixelsPerNucleotide = 1f;
            }
            return pixelsPerNucleotide;
        }
        FontMetrics fontMetrics = graphics.getFontMetrics(options.getFont().getFont());
        return fontMetrics.stringWidth("a");
    }

    /**
     * Calculate width of the single letter in pixels for the specified <code>graphics</code>
     * IMPORTANT: assumed that monospaced font is ised (Courier for example)
     *
     * @param graphics graphics for which letter width is calculated
     *
     * @return width of the single letter in pixel
     */
    /*
        public int getLetterWidth(Graphics graphics)
        {
            FontMetrics fontMetrics = graphics.getFontMetrics(options.getFont().getFont());
            return fontMetrics.stringWidth("a");
        }
    */
    /**
     * Calculate height of the single letter in pixels for the specified <code>graphics</code>
     *
     * @param graphics graphics for which letter height is calculated
     *
     * @return height of the single letter in pixel
     */
    /*
        public int getLetterHeight(Graphics graphics)
        {
            FontMetrics fontMetrics = graphics.getFontMetrics(options.getFont().getFont());
            return fontMetrics.getHeight();
        }
    */
    /**
     * Calculate coordinate of the begin of the interesting subsequence
     * in absolute coordinates.
     *
     * @param position where subsequence starts
     * @param graphics graphics for which position is calculated
     *
     * @return The left bound of the symbol in the specified <CODE>position</CODE>.
     */
    public Point getStartPoint(int position, Graphics graphics)
    {
        int shift = (int) ( getNucleotideWidth() / 2f );

        if( position == 1 )
        {
            return new Point(shift, 0);
        }

        int len = position - 1;

        int start = (int) ( getNucleotideWidth() * len );
        return new Point(start, 0);
    }

    /**
     * Calculate coordinate of the end of the interesting subsequence
     * in absolute coordinates.
     *
     * @param position where subsequence ends
     * @param graphics graphics for which position is calculated
     *
     * @return The right bound of the symbol in the specified <CODE>position</CODE>.
     */
    public Point getEndPoint(int position, Graphics graphics)
    {
        Point pointStartThis = getStartPoint(position, graphics);
        Point pointStartNext = getStartPoint(position+1, graphics);
        if(pointStartNext.x > pointStartThis.x)
            pointStartNext.x--;
        return pointStartNext;
    }

    /**
     * Calculate position in sequence which corresponds to the specified Point in absolute coordinates
     *
     * @param pt Point for which postion is calculated
     * @param graphics graphics where position is calculated
     *
     * @return position of the <code>pt</code> in the sequence (1-based)
     */
    public int getPosition(Point pt, Graphics graphics)
    {
        double nw = getNucleotideWidth();
        int div = (int) ( pt.x / nw );
        if(div * nw < pt.x)
            return div + 1;
        return div;
    }

    /**
     * Calculate position in sequence which corresponds to the specified x-coordinate in absolute coordinates
     *
     * @param x x-coordinate for whic postion is calculated
     * @param graphics graphics where position is calculated
     *
     * @return position of the x-coordinate in the sequence
     */
    public int getPosition(int x, Graphics graphics)
    {
        return getPosition(new Point(x, 0), graphics);
    }

    /**
     * Return nucletide in sequence which corresponds to the specified Point in absolute coordinates
     *
     * @param pt Point for whic postion is calculated
     * @param graphics graphics where position is calculated
     *
     * @return nucleotide of the <code>pt</code> in the sequence
     */
    public String getLetter(Point pt, Graphics graphics)
    {
        return "" + (char)sequence.getLetterAt(getPosition(pt, graphics));
    }

    /**
     * Get subsequence from <CODE>start</CODE> to <CODE>end</CODE> in
     * string format.
     *
     * @param start  start position of the subsequence relative to the parent
     * @param end    start position of the subsequence relative to the parent
     * @return <CODE>String</CODE> representing interesting subsequence
     */
    public String getSubSequence(int start, int end)
    {
        // PENDING (fedor) from champ : is this condition correct?
        if( start > sequence.getLength()+sequence.getStart()-1 || end > sequence.getLength()+sequence.getStart()-1 || start > end || start < sequence.getStart() )
        {
            log.log(Level.SEVERE, Resources.getResourceString("ASSERT_INDEX_OUT_OF_RANGE"));
            return null;
        }

        return IntStreamEx.rangeClosed( start, end ).map( sequence::getLetterAt ).charsToString();
    }
    
    public Sequence getSequence()
    {
        return sequence;
    }


    public void hideSelection()
    {
        selection.setVisible(false);
    }


    private int previousSelectionX = 0;
    public void setSelection(int x1, int x2, Graphics graphics)
    {
        if( x1 < start )
            x1 = start;
        if( x2 > end )
            x2 = end;
        if( x1 > x2 )
            return;
        selection.setVisible(true);
        x1 = getStartPoint(x1 - start + 1, graphics).x;
        x2 = getEndPoint(x2 - start + 1, graphics).x;
        selection.setVisible(true);
        int w = selection.getBounds().width;
        selection.resize(x2 - x1 - w, 0);
        selection.move(x1 - previousSelectionX, 0);
        previousSelectionX = x1;
    }


    /**
    * @todo make setSelection to correctly print site parts.
    */
    public void setSelection(Site site, Graphics graphics)
    {
        int start = 0, end = 0;
        if( site.getStrand() != Site.STRAND_MINUS )
        {
            start = site.getStart();
            end = start + site.getLength() - 1;
        }
        else
        {
            end = site.getStart();
            start = end - site.getLength() + 1;
        }
        setSelection(start, end, graphics);
    }

}
