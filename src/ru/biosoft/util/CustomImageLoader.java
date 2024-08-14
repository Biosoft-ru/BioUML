package ru.biosoft.util;

import javax.swing.ImageIcon;

/**
 * Interface for custom image providers
 * @author lan
 */
public interface CustomImageLoader
{
    final String DATA_COLLECTION_PROPERTY = "CustomImageLoader";
    ImageIcon loadImage(String imageId);
}
