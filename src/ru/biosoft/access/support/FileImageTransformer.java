package ru.biosoft.access.support;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.generic.PriorityTransformer;

/**
 * {@link ImageDataElement} to {@link FileDataElement} transformer
 */
public class FileImageTransformer extends AbstractFileTransformer<ImageDataElement> implements PriorityTransformer
{
    private static final Pattern EXTENSION_REGEXP = Pattern.compile( "\\.(png|gif|jpg|bmp)$", Pattern.CASE_INSENSITIVE );

    @Override
    public Class<ImageDataElement> getOutputType()
    {
        return ImageDataElement.class;
    }

    public static String getFormatName(File file)
    {
        String format = null;
        try
        {
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if( iter.hasNext() )
            {
                ImageReader reader = iter.next();
                iis.close();
                format = reader.getFormatName();
            }
        }
        catch( IOException e )
        {
        }
        return format;
    }

    public static IIOMetadata getImageMetadata(File file)
    {
        IIOMetadata meta = null;
        try
        {
            ImageInputStream iis = ImageIO.createImageInputStream( file );
            Iterator it = ImageIO.getImageReaders( iis );
            if( it.hasNext() )
            {
                ImageReader reader = (ImageReader)it.next();
                reader.setInput( iis );
                meta = reader.getImageMetadata( 0 );
            }
        }
        catch( IOException e )
        {
        }
        return meta;
    }

    @Override
    public ImageDataElement load(File input, String name, DataCollection<ImageDataElement> origin) throws Exception
    {
        try (FileInputStream fis = new FileInputStream( input ))
        {
            BufferedImage bi = ImageIO.read( fis );
            String formatName = getFormatName( input );
            IIOMetadata metadata = getImageMetadata( input );
            return new ImageDataElement( name, origin, bi, formatName, metadata );
        }
    }

    @Override
    public void save(File output, ImageDataElement imageDE) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( output ))
        {
            //TODO: refactor code duplication with biouml.model.util.ImageGenerator
            IIOMetadata metadata = imageDE.getMetadata();
            if( metadata != null )
            {
                ImageWriter writer = ImageIO.getImageWritersByFormatName( imageDE.getFormat() ).next();
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                try (ImageOutputStream stream = ImageIO.createImageOutputStream( fos ))
                {
                    writer.setOutput( stream );
                    writer.write( metadata, new IIOImage( imageDE.getImage(), null, metadata ),
                            writeParam );
                }
            }
            else
                ImageIO.write( imageDE.getImage( null ), imageDE.getFormat(), fos );
        }
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return 1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(EXTENSION_REGEXP.matcher( name ).find())
            return 3;
        return 0;
    }
}
