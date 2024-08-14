package ru.biosoft.graphics.access;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.json.JSONException;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.ImageUtils;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("chart")
@ClassIcon("resources/plot.gif")
public class ChartDataElement extends DataElementSupport implements ImageElement, CloneableDataElement
{
    private Chart chart;

    public ChartDataElement(String name, DataCollection origin)
    {
        super(name, origin);
    }
    
    public ChartDataElement(String name, DataCollection origin, Chart chart)
    {
        super(name, origin);
        this.chart = chart;
    }
    
    public Chart getChart()
    {
        return chart;
    }

    @Override
    public BufferedImage getImage(Dimension dimension)
    {
        Dimension d = dimension == null ? getImageSize() : ImageUtils.correctImageSize(dimension);
        return chart.getImage(d.width, d.height);
    }

    @Override
    public Dimension getImageSize()
    {
        return new Dimension(600,400);
    }

    @Override
    public ChartDataElement clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        ChartDataElement clone = (ChartDataElement)super.clone(origin, name);
        try
        {
            clone.chart = new Chart(chart.toJSON());
        }
        catch( JSONException e )
        {
            throw new CloneNotSupportedException();
        }
        return clone;
    }
}
