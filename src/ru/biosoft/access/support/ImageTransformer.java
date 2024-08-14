package ru.biosoft.access.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.Entry;
import ru.biosoft.access.ImageDataElement;

public class ImageTransformer extends AbstractTransformer<Entry, ImageDataElement>
{
    @Override
    public ImageDataElement transformInput(Entry entry) throws Exception
    {
        byte[] encodedBytes = Base64.getDecoder().decode( entry.getData() );
        ByteArrayInputStream is = new ByteArrayInputStream(encodedBytes);
        BufferedImage bi = ImageIO.read(is);

        return new ImageDataElement(entry.getName(), entry.getOrigin(), bi);
    }

    @Override
    public Entry transformOutput(ImageDataElement ide) throws Exception
    {
        BufferedImage bi = ide.getImage(null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        ImageIO.write(bi, "PNG", os);

        byte bytes[] = os.toByteArray();

        String strValue = Base64.getEncoder().encodeToString( bytes );
        return new Entry(ide.getOrigin(), ide.getName(), strValue);
    }

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<ImageDataElement> getOutputType()
    {
        return ImageDataElement.class;
    }
}
