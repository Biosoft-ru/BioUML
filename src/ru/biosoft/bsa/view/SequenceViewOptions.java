
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 13.02.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */

package ru.biosoft.bsa.view;


import ru.biosoft.graphics.RulerOptions;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.Option;

/**
 * Class to store options for sequence painting.
 *
 * @author Igor V. Tyazhev
 */
@SuppressWarnings ( "serial" )
public class SequenceViewOptions extends Option
{

    // Printing type constants
    public static final int PT_LETTERS = 0;
    public static final int PT_RULER = 1;
    public static final int PT_BOTH = 2;

    /**
     * Creates <CODE>SequenceViewOptions</CODE> and initializes it to the specified font.
     *
     * @param parent   parent property
     * @param font   the specified font
     */
    public SequenceViewOptions(Option parent, ColorFont font)
    {
        this(parent, font, new RulerOptions(font), PT_LETTERS);
    }

    /**
     * Creates <CODE>SequenceViewOptions</CODE> and initializes it to the specified options.
     *
     * @param parent   parent property
     * @param font     the specified font
     * @param RulerOptions
     *                 the specified <CODE>RulerOptions</CODE> to paint ruler (if needed)
     */
    public SequenceViewOptions(Option parent, ColorFont font, RulerOptions rulerOptions)
    {
        this(parent, font, rulerOptions, PT_RULER);
    }

    /**
     * Creates <CODE>SequenceViewOptions</CODE> and initializes it to the specified options.
     *
     * @param parent   parent property
     * @param font     the specified font
     * @param RulerOptions
     *                 the specified <CODE>RulerOptions</CODE> to paint ruler (if needed)
     * @param type     type of the sequence
     */
    public SequenceViewOptions(Option parent, ColorFont font, RulerOptions rulerOptions, int type)
    {
        super(parent);
        this.font = font;
        this.rulerOptions = rulerOptions;
        //this.rulerOptions.setParent(this);
        this.type = type;
        this.density = 1f;
        if( font.getFont().getSize2D() == 0.0 )
        {
            this.font = new ColorFont(font.getFont().deriveFont(1f), font.getColor());
        }
    }

    /**
     * <CODE>ColorFont</CODE> for sequence painting.
     */
    private ColorFont font;
    public ColorFont getFont()
    {
        return font;
    }

    public void setFont(ColorFont font)
    {
        ColorFont oldValue = this.font;
        this.font = font;
        if( font.getFont().getSize2D() == 0.0 )
        {
            this.font = new ColorFont(font.getFont().deriveFont(1f), font.getColor());
        }
        firePropertyChange("font", oldValue, font);
    }

    /*
     *   Indicates is sequence should be painted as ruler
     */

    /**
     * <CODE>RulerOptions</CODE> to paint ruler (if needed).
     */
    private RulerOptions rulerOptions;

    public RulerOptions getRulerOptions()
    {
        return rulerOptions;
    }

    public void setRulerOptions(RulerOptions rulerOptions)
    {
        RulerOptions oldValue = this.rulerOptions;
        this.rulerOptions = rulerOptions;
        //TODO: check if parent was important
        //this.rulerOptions.setParent(this);
        firePropertyChange("rulerOptions", oldValue, rulerOptions);
    }

    /**
    * Style of the sequence
    */
    private int type;
    public int getType()
    {
        return type;
    }
    public void setType(int type)
    {
        int oldValue = this.type;
        this.type = type;
        if(oldValue != type)
        {
            firePropertyChange("type", oldValue, type);
        }
    }

    double density;
    public double getDensity()
    {
        return density;
    }
    public void setDensity(double density)
    {
        double oldValue = this.density;
        this.density = density;
        firePropertyChange("density", oldValue, density);
    }

    public boolean isDensityReadOnly()
    {
        return ( getType() != SequenceViewOptions.PT_RULER );
    }

    public SequenceViewOptions clone(Option parent)
    {
        SequenceViewOptions sequenceViewOptions = new SequenceViewOptions(parent, new ColorFont(font.getFont(), font.getColor()));
        sequenceViewOptions.setRulerOptions(rulerOptions.clone());
        sequenceViewOptions.setType(type);
        sequenceViewOptions.setDensity(density);

        return sequenceViewOptions;
    }
}
