package ru.biosoft.graphics.chart;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.json.JSONArray;

/**
 * @author lan
 *
 */
public class ChartConnection extends URLConnection
{
    private BufferedImage image;

    public ChartConnection(URL url)
    {
        super(url);
        try
        {
            String data = url.getPath().substring(1);
            int pos = data.indexOf(":");
            String[] size = data.substring(0, pos).split("x");
            int width = Integer.parseInt(size[0]);
            int height = Integer.parseInt(size[1]);
            if(width > 1000) width = 1000;
            if(height > 1000) height = 1000;
            JSONArray chart = new JSONArray(data.substring(pos+1));
            image = new Chart(chart).getImage(width, height);
        }
        catch( Exception e )
        {
        }
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
        ImageIO.write(image, "PNG", out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
