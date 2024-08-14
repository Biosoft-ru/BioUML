package biouml.plugins.sbml.extensions;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.Pen;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Module;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;

import com.developmentontheedge.beans.DynamicProperty;

public class PlotExtension extends SbmlExtensionSupport
{
    public static final String PLOT_ELEMENT = "plot";
    public static final String AXIS_ELEMENT = "axis";
    public static final String SERIES_ELEMENT = "series";

    public static final String ID_ATTR = "id";
    public static final String TITLE_ATTR = "title";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String VERSION_ATTR = "version";
    public static final String TYPE_ATTR = "type";
    public static final String FROM_ATTR = "from";
    public static final String TO_ATTR = "to";
    public static final String NAME_ATTR = "name";
    public static final String SOURCE_ATTR = "sourceID";
    public static final String SOURCE_NATURE_ATTR = "sourceNature";
    public static final String XVAR_ATTR = "xVar";
    public static final String YVAR_ATTR = "yVar";
    public static final String SPEC_ATTR = "spec";
    public static final String LEGEND_ATTR = "legend";

    public static final String DIAGRAM_PLOTS_PROPERTY = "plots";

    public static final String VERSION = "0.8.0";

    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        if( !element.getNodeName().equals(PLOT_ELEMENT) )
            return;

        String id = element.getAttribute(ID_ATTR);
        String title = element.getAttribute(TITLE_ATTR);

        if( id.isEmpty() || title.isEmpty() )
            return;

        DataCollection<?> parent = Module.getModule(diagram);
        try
        {
            parent = (DataCollection<?>)parent.get(Module.SIMULATION);
            parent = (DataCollection<?>)parent.get(Module.PLOT);
        }
        catch( Exception e )
        {
        }
        Plot plot = new Plot(parent, id);
        plot.setTitle(title);
        plot.setDescription(element.getAttribute(DESCRIPTION_ATTR));

        NodeList axisList = element.getElementsByTagName(AXIS_ELEMENT);
        int length = axisList.getLength();
        for( int i = 0; i < length; i++ )
        {
            readAxis((Element)axisList.item(i), plot);
        }

        NodeList seriesList = element.getElementsByTagName(SERIES_ELEMENT);
        int seriesLength = seriesList.getLength();
        for( int i = 0; i < seriesLength; i++ )
        {
            Series series = readSeries((Element)seriesList.item(i));
            plot.addSeries(series);
        }

        Object object = diagram.getAttributes().getValue(DIAGRAM_PLOTS_PROPERTY);
        if( object != null && ( object instanceof Plot[] ) )
        {
            //add experiment to current list
            Plot[] oldPlots = (Plot[])object;
            Plot[] newPlots = new Plot[oldPlots.length + 1];
            System.arraycopy(oldPlots, 0, newPlots, 0, oldPlots.length);
            newPlots[oldPlots.length] = plot;
            diagram.getAttributes().getProperty(DIAGRAM_PLOTS_PROPERTY).setValue(newPlots);
        }
        else
        {
            //create new experiments list
            Plot[] newPlots = new Plot[1];
            newPlots[0] = plot;
            try
            {
                diagram.getAttributes().add(new DynamicProperty(DIAGRAM_PLOTS_PROPERTY, Plot[].class, newPlots));
            }
            catch( Exception e )
            {
            }
        }
    }

    protected void readAxis(Element element, Plot plot)
    {
        String type = element.getAttribute(TYPE_ATTR).trim().toLowerCase();
        double from = Double.parseDouble( element.getAttribute( FROM_ATTR ).trim() );
        double to = Double.parseDouble( element.getAttribute( TO_ATTR ).trim() );
        String title = element.getAttribute(TITLE_ATTR);
        if( type.equals("x") )
        {
            plot.setXFrom(from);
            plot.setXTo(to);
            plot.setXTitle(title);
        }
        else if( type.equals("y") )
        {
            plot.setYFrom(from);
            plot.setYTo(to);
            plot.setYTitle(title);
        }
    }

    protected Series readSeries(Element element)
    {
        String name = element.getAttribute(NAME_ATTR);
        if( name.isEmpty() )
            return null;

        Series series = new Series(name);

        series.setSource(element.getAttribute(SOURCE_ATTR));
        series.setSourceNature(Series.SourceNature.valueOf(element.getAttribute(SOURCE_NATURE_ATTR)));
        series.setXVar(element.getAttribute(XVAR_ATTR));
        series.setYVar(element.getAttribute(YVAR_ATTR));
        series.setSpec(Pen.createInstance(element.getAttribute(SPEC_ATTR)));
        series.setLegend(element.getAttribute(LEGEND_ATTR));

        return series;
    }

    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        if( ! ( specie instanceof Diagram ) )
        {
            return null;
        }

        Object object = ( (Diagram)specie ).getAttributes().getValue(DIAGRAM_PLOTS_PROPERTY);
        if( object != null && ( object instanceof Plot[] ) )
        {
            Element[] elements = new Element[ ( (Plot[])object ).length];
            for( int i = 0; i < ( (Plot[])object ).length; i++ )
            {
                Element element = document.createElement(PLOT_ELEMENT);
                Plot plot = ( (Plot[])object )[i];

                element.setAttribute(ID_ATTR, plot.getName());
                element.setAttribute(TITLE_ATTR, plot.getTitle());

                String description = plot.getDescription();
                if( description != null )
                {
                    element.setAttribute(DESCRIPTION_ATTR, description);
                }

                element.setAttribute(VERSION_ATTR, VERSION);

                element.appendChild(getAxisElement(plot, document, "x"));
                element.appendChild(getAxisElement(plot, document, "y"));

                if( plot.getSeries() != null )
                {
                    for( Series series : plot.getSeries() )
                    {
                        element.appendChild(getSeriesElement(series, document));
                    }
                }

                elements[i] = element;
            }
            return elements;
        }
        return null;
    }

    protected Element getAxisElement(Plot plot, Document document, String type)
    {
        Element result = document.createElement(AXIS_ELEMENT);

        if( type.equals("x") )
        {
            result.setAttribute(TYPE_ATTR, "X");
            result.setAttribute(FROM_ATTR, String.valueOf(plot.getXFrom()));
            result.setAttribute(TO_ATTR, String.valueOf(plot.getXTo()));
            String title = plot.getXTitle();
            if( title != null )
            {
                result.setAttribute(TITLE_ATTR, title);
            }
        }
        else if( type.equals("y") )
        {
            result.setAttribute(TYPE_ATTR, "Y");
            result.setAttribute(FROM_ATTR, String.valueOf(plot.getYFrom()));
            result.setAttribute(TO_ATTR, String.valueOf(plot.getYTo()));
            String title = plot.getYTitle();
            if( title != null )
            {
                result.setAttribute(TITLE_ATTR, title);
            }
        }

        return result;
    }

    protected Element getSeriesElement(Series series, Document document)
    {
        Element result = document.createElement(SERIES_ELEMENT);

        result.setAttribute(NAME_ATTR, series.getName());
        String param = series.getSource();
        if( param != null )
        {
            result.setAttribute(SOURCE_ATTR, param);
        }
        param = series.getSourceNature().toString();
        if( param != null )
        {
            result.setAttribute(SOURCE_NATURE_ATTR, param);
        }
        param = series.getXVar();
        if( param != null )
        {
            result.setAttribute(XVAR_ATTR, param);
        }
        param = series.getYVar();
        if( param != null )
        {
            result.setAttribute(YVAR_ATTR, param);
        }
        param = series.getSpec().toString();
        if( param != null )
        {
            result.setAttribute(SPEC_ATTR, param);
        }
        param = series.getLegend();
        if( param != null )
        {
            result.setAttribute(LEGEND_ATTR, param);
        }

        return result;
    }
}
