package ru.biosoft.graphics.access;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.json.JSONException;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.util.ImageUtils;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("picture")
@ClassIcon("resources/plot.gif")
public class ViewDataElement extends DataElementSupport implements ImageElement, CloneableDataElement
{
    private CompositeView view;

    public ViewDataElement(String name, DataCollection<?> origin)
    {
        super(name, origin);
    }
    
    public ViewDataElement(String name, DataCollection<?> origin, CompositeView view)
    {
        super(name, origin);
        this.view = view;
    }
    
    public CompositeView getView()
    {
        return view;
    }

    @Override
    public BufferedImage getImage(Dimension dimension)
    {
        Dimension d = dimension == null ? getImageSize() : ImageUtils.correctImageSize(dimension);
        Rectangle r = view.getBounds();
        r.grow(2, 2);
        BufferedImage image = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        double scale = Math.min(((double)d.width)/r.width, ((double)d.height)/r.height);
        Graphics2D graphics = image.createGraphics();
        graphics.fill(new Rectangle(0, 0, d.width, d.height));
        graphics.translate(d.width/2, d.height/2);
        graphics.scale(scale, scale);
        graphics.translate(-r.getCenterX(), -r.getCenterY());
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(1, 1, 1, 0.f));
        view.paint(graphics);
        return image;
    }

    @Override
    public Dimension getImageSize()
    {
        Rectangle r = view.getBounds();
        r.grow(2, 2);
        return ImageUtils.correctImageSize(r.getSize());
    }

    @Override
    public ViewDataElement clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        ViewDataElement clone = (ViewDataElement)super.clone(origin, name);
        try
        {
            clone.view = new CompositeView(view.toJSON());
        }
        catch( JSONException e )
        {
            throw new CloneNotSupportedException();
        }
        return clone;
    }
}
