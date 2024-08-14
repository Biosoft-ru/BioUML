package ru.biosoft.util;

import java.awt.Dimension;

import ru.biosoft.access.ImageElement;

/**
 * @author lan
 *
 */
public class ImageUtils
{

    public static Dimension correctImageSize(Dimension size)
    {
        double scale = Math.min(((double)ImageElement.MAX_IMAGE_SIZE)/size.width, ((double)ImageElement.MAX_IMAGE_SIZE)/size.height);
        if(scale < 1)
            return new Dimension((int)(size.width*scale), (int)(size.height*scale));
        return size;
    }

    public static double correctScale(double scale, int width, int height)
    {
        Dimension corrected = correctImageSize(new Dimension(width, height));
        scale = Math.min(scale, ((double)ImageElement.MAX_IMAGE_SIZE)/corrected.width);
        scale = Math.min(scale, ((double)ImageElement.MAX_IMAGE_SIZE)/corrected.height);
        return scale;
    }
}
