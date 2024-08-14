package ru.biosoft.galaxy;

import java.io.File;
import javax.swing.ImageIcon;

import ru.biosoft.util.CustomImageLoader;

/**
 * @author lan
 *
 */
public class GalaxyImageLoader implements CustomImageLoader
{
    @Override
    public ImageIcon loadImage(String imageId)
    {
        try
        {
            return new ImageIcon(new File(GalaxyDataCollection.getGalaxyDistFiles().getRootFolder(), imageId).toURI().toURL());
        }
        catch( Exception e )
        {
            return null;
        }
    }
}
