
package ru.biosoft.bsa.view.sitelayout;

/**
 */
abstract public class AbstractLayoutAlgorithm implements SiteLayoutAlgorithm
{
    /** Length of buffered position(used with advanced algorithms */
    protected final static int LENGTH = 50000;

    /** Starting y coordinate */
    protected final static int STARTYPOSITION = 10;

    /** step responding for frequency of adjusting starting position of buffer */
    protected final static int ADJUST_START_STEP = 100;
}

