package ru.biosoft.server.servlets.webservices.providers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.swing.ImageIcon;

import org.json.JSONException;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.graphics.HtmlView;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebSession;
import biouml.model.util.ImageGenerator;
import biouml.plugins.server.access.AccessProtocol;

/**
 * @author lan
 *
 */
public class ImageProvider extends WebProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        String pathStr = arguments.get(AccessProtocol.KEY_DE);
        String htmlStr = null;
        DataElementPath path = null;

        if( pathStr == null )
        {
            pathStr = arguments.get("id");
            if( pathStr == null )
            {
                htmlStr = arguments.get("html");
                if( htmlStr == null )
                {
                    throw new IllegalArgumentException("Either de, or id, or html must be present");
                }
            }
            else //workaround for graph.js to display image of ImageDataElement
            {
                DataElementPath imgPath = DataElementPath.create( pathStr );
                if( imgPath.exists() && imgPath.getDataElement() instanceof ImageDataElement )
                {
                    path = imgPath;
                }
            }
        }
        else
        {
            path = DataElementPath.create(pathStr);
        }
        Map<String, Object> images = WebSession.getCurrentSession().getImagesMap();
        BufferedImage image = null;
        ImageElement ie = null;
        IIOMetadata metadata = null;
        if( pathStr == null && htmlStr != null )
        {
            //Render HtmlView as image
            ColorFont font = new ColorFont("Arial", Font.PLAIN, 12, Color.black);
            if(arguments.get("font") != null)
            {
                try
                {
                    font = new ColorFont(arguments.get("font"));
                }
                catch( JSONException e )
                {
                }
            }
            Dimension preferredSize = null;
            try
            {
                preferredSize = new Dimension(Integer.parseInt(arguments.get("w")), Integer.parseInt(arguments.get("h")));
            }
            catch( NumberFormatException e )
            {
            }
            HtmlView view = new HtmlView(htmlStr, font, new Point(0,0), preferredSize);
            image = ImageGenerator.generateImage(view, 1.0, false, true);
        }
        else
        {
            Object obj = images.get(pathStr);
            if(obj instanceof BufferedImage)
            {
                image = (BufferedImage)obj;
            } else if(obj instanceof ImageElement)
            {
                ie = (ImageElement)obj;
            }
            if( image == null )
            {
                if( path == null )
                {
                    if( pathStr.contains("://") && !pathStr.startsWith("http") && !pathStr.startsWith("ftp") )
                    {
                        try
                        {
                            URL url = new URL(pathStr);
                            image = ImageIO.read(url.openStream());
                        }
                        catch( Exception e )
                        {
                        }
                    }
                    if( image == null )
                    {
                        ImageIcon icon = IconFactory.getIconById(pathStr);
                        if( icon != null )
                        {
                            image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                            image.getGraphics().drawImage(icon.getImage(), 0, 0, null);
                            images.put(pathStr, image);
                        }
                    }
                }
                else
                {
                    Object value = WebBeanProvider.getBean(pathStr);
                    if( value instanceof Chart )
                    {
                        ie = new ChartDataElement("", null, (Chart)value);
                    }
                }
            }

            if( image == null )
            {
                //try to load image from data element
                if( ie == null )
                {
                    DataElement de = path.optDataElement();
                    if(de instanceof ImageElement)
                    {
                        ie = (ImageElement)de;
                    }
                    if( de instanceof ImageDataElement )
                        metadata = ( (ImageDataElement)de ).getMetadata();
                }
                if( ie != null )
                {
                    Dimension dimension = null;
                    int width = arguments.optInt("w");
                    if(width > 0)
                    {
                        int height = arguments.optInt("h");
                        if(height <= 0)
                        {
                            Dimension size = ie.getImageSize();
                            height = size.height*width/size.width;
                        }
                        dimension = new Dimension(width, height);
                    }
                    image = ie.getImage(dimension);
                }
            }
        }
        if( image == null )
            return;

        String actionStr = arguments.optAction();
        if( actionStr == null )
        {
            //return this image
            resp.setContentType( "image/png" );
            try (OutputStream out = resp.getOutputStream())
            {
                ImageGenerator.encodeImage( image, "PNG", metadata, out );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not send image", e);
            }
        }
        else
        {
            if( actionStr.equals("close") )
            {
                if(pathStr != null)
                {
                    images.remove(pathStr);
                    if(ie instanceof Closeable)
                    {
                        ((Closeable)ie).close();
                    }
                }
            } else if( actionStr.equals("save") )
            {
                String newPathObj = arguments.getString("path");
                JSONResponse response = new JSONResponse(resp);
                try
                {
                    DataElementPath newPath = DataElementPath.create(newPathObj);

                    DataElement imageDE = ie instanceof CloneableDataElement ? ( (CloneableDataElement)ie ).clone(
                            newPath.optParentCollection(), newPath.getName()) : new ImageDataElement(newPath.getName(),
                            newPath.optParentCollection(), image);
                    newPath.save(imageDE);
                    response.sendString("Image exported successfully");
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not save image", e);
                }
            }
        }
    }
}