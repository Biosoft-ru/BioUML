package ru.biosoft.access;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.imageio.metadata.IIOMetadata;

import ru.biosoft.access.exception.Assert;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.util.ImageUtils;

import com.developmentontheedge.beans.annot.PropertyName;

@ClassIcon ( "resources/plot.gif" )
@PropertyName("image")
public class ImageDataElement extends DataElementSupport implements ImageElement, CloneableDataElement
{
    public ImageDataElement(String name, DataCollection<?> origin, BufferedImage image)
    {
        this(name, origin, image, "PNG");
    }
    
    public ImageDataElement(String name, DataCollection<?> origin, BufferedImage image, String format)
    {
        this( name, origin, image, format, null );
    }

    public ImageDataElement(String name, DataCollection<?> origin, BufferedImage image, String format, IIOMetadata metadata)
    {
        super(name, origin);
        this.image = Assert.notNull( "image", image );
        this.format = format;
        this.metadata = metadata;
    }
    
    private final BufferedImage image;
    
    @Override
    public Dimension getImageSize()
    {
        return ImageUtils.correctImageSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    @Override
    public BufferedImage getImage(Dimension dimension)
    {
        Dimension d = dimension == null ? getImageSize() : ImageUtils.correctImageSize(dimension);
        if(d.width == image.getWidth() && d.height == image.getHeight())
            return image;
        BufferedImage resized = new BufferedImage(d.width, d.height, image.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.fillRect(0, 0, d.width, d.height);
        g.drawImage(image, 0, 0, d.width, d.height, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return resized;
    }

    public BufferedImage getImage()
    {
        return getImage( getImageSize() );
    }

    protected String format = "PNG";

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    protected IIOMetadata metadata = null;

    public IIOMetadata getMetadata()
    {
        return metadata;
    }

    public void setMetadata(IIOMetadata metadata)
    {
        this.metadata = metadata;
    }

}
