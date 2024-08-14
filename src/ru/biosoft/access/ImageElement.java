package ru.biosoft.access;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import ru.biosoft.access.core.DataElement;

/**
 * ru.biosoft.access.core.DataElement which has an Image representing it
 * @author lan
 */
public interface ImageElement extends DataElement
{
    public static final int MAX_IMAGE_SIZE = 4000;
    
    /**
     * Returns image of given (or less size)
     * @param dimension wanted image size. If null then image of canonical size is returned
     * @return image of given (or less size)
     */
    public BufferedImage getImage(Dimension dimension);
    
    /**
     * Returns canonical image size
     * @return
     */
    public Dimension getImageSize();
}
