package ru.biosoft.bsa;

/**
 * Indicates precision of <code>Site</code> boundories.
 */
public interface Precision
{
    public final static int PRECISION_EXACTLY   = 0;
    public final static int PRECISION_CUT_LEFT  = 1;
    public final static int PRECISION_CUT_RIGHT = 2;
    public final static int PRECISION_CUT_BOTH  = 3;
    public final static int PRECISION_NOT_KNOWN = 4;
}
