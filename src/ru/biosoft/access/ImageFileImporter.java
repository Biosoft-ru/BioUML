
package ru.biosoft.access;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.support.FileImageTransformer;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author anna
 *
 */
public class ImageFileImporter extends FileImporter
{
    @Override
    public int accept(DataCollection parent, File file)
    {
        if( file == null || isImageFile( file ) )
            return super.accept( parent, file ) == ACCEPT_UNSUPPORTED ? ACCEPT_UNSUPPORTED : ACCEPT_HIGH_PRIORITY;
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return ImageDataElement.class;
    }

    protected boolean isImageFile(File file)
    {
        String formatName = FileImageTransformer.getFormatName( file );
        return ( formatName != null );
    }

    @Override
    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        String formatName = FileImageTransformer.getFormatName( file );
        BufferedImage bi = ImageIO.read( file );
        IIOMetadata metadata = FileImageTransformer.getImageMetadata( file );
        return new ImageDataElement( name, parent, bi, formatName, metadata );
    }
}
