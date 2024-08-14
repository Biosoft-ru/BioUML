package ru.biosoft.bsa.view;

/**
 * Describes bounds of visible part of view
 */
public class ViewSlice
{
    /**
     * Left part of view in pixels
     */
    public int left;
    /**
     * Width of visible part in pixels
     */
    public int width;

    public ViewSlice(int left, int width)
    {
        this.left = left;
        this.width = width;
    }
}
