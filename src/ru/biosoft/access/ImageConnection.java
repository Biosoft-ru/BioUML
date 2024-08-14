package ru.biosoft.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import ru.biosoft.access.core.CollectionFactory;

public class ImageConnection extends URLConnection
{
    private ImageElement de;

    public ImageConnection(URL url)
    {
        super(url);
        de = ((ImageElement)CollectionFactory.getDataElement(url.getPath()));
    }

    @Override
    public void connect() throws IOException
    {
    }

    @Override
    public String getContentType()
    {
        return "image/png";
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(de.getImage(null), "PNG", out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}