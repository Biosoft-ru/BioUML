package biouml.standard.simulation.plot.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.simulation.plot.PlotEditorPane;
import biouml.plugins.simulation.plot.PlotEx;
import biouml.plugins.simulation.plot.PlotPane;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.Pen;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class PlotProvider extends WebJSONProviderSupport
{

    private static BufferedImage drawChart(Plot plot)
    {
        JFreeChart chart = ChartFactory.createXYLineChart("", "Axis (X)", "Axis (Y)", null, //dataset,
                PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
                );
    
        chart.setBackgroundPaint(Color.white);
        chart.getXYPlot().setBackgroundPaint( Color.white );
        PlotPane.redrawChart(plot, chart);
        return chart.createBufferedImage(550, 350);
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElementPath dePath = arguments.getDataElementPath();
        String action = arguments.getAction();
        if( "variables".equals(action) )
        {
            DataElement dc = dePath.optDataElement();
            if( dc != null )
            {
                JSONArray xVariable = new JSONArray();
                JSONArray yVariable = new JSONArray();
                if( dc instanceof SimulationResult )
                {
                    //  xVariable.put("time");
                    //   yVariable.put("time");

                    String path = arguments.getString( "path" );
                    for( String s : PlotEditorPane.getVariablesForPath( path, (SimulationResult)dc ) )
                    {
                        xVariable.put(s);
                        yVariable.put(s);
                    }
                }
                else if( dc instanceof TableDataCollection )
                {
                    boolean first = true;
                    for( TableColumn column : ( (TableDataCollection)dc ).getColumnModel() )
                    {
                        (first ? xVariable : yVariable).put( column.getName() );
                        first = false;
                    }
                } else
                    throw new IllegalArgumentException("Incorrect data element " + dePath.toString());
                JSONObject result = new JSONObject();
                result.put("x", xVariable);
                result.put("y", yVariable);
                response.sendJSON(result);
                return;
            }
            throw new IllegalArgumentException("Incorrect data element " + dePath.toString());
        }
        else if( "paths".equals( action ) )
        {
            DataElement dc = dePath.optDataElement();
            if( dc != null )
            {

                if( dc instanceof SimulationResult )
                {
                    Set<String> paths = ( (SimulationResult)dc ).getPaths();
                    response.sendStringArray( paths.toArray( new String[paths.size()] ) );
                    return;
                }
                else if( dc instanceof TableDataCollection )
                {
                    response.sendString( "" );
                    return;
                }
                else
                    throw new IllegalArgumentException( "Incorrect data element " + dePath.toString() );
            }
            throw new IllegalArgumentException( "Incorrect data element " + dePath.toString() );
        }
        else if( "savenew".equals( action ) )
        {
            Plot plot = new Plot( null, dePath.getName() );
            PlotEx.savePlot( plot, dePath );
            response.sendString( "" );
            return;
        }
        Object de = WebBeanProvider.getBean(dePath.toString());
        if(!(de instanceof Plot))
            throw new IllegalArgumentException("Object is not a plot: "+dePath);
        Plot plot = (Plot)de;
        if( "add".equals(action) )
        {
            String source = arguments.get("source");
            String xVar = arguments.get("x");
            String yVar = arguments.get("y");
            String xPath = arguments.getOrDefault( "xPath", "" );
            String yPath = arguments.getOrDefault( "yPath", "" );
            if( source != null && xVar != null && yVar != null )
            {
                DataElement sourceDe = DataElementPath.create(source).optDataElement();
                Series.SourceNature nature = Series.SourceNature.EXPERIMENTAL_DATA;

                if( sourceDe instanceof SimulationResult )
                    nature = Series.SourceNature.SIMULATION_RESULT;
                Series series = Plot.getDefaultSeries( xPath, xVar, yPath, yVar, source, nature );
                Series existing = StreamEx.of( plot.getSeries() ).findAny( s -> s.equals( series ) ).orElse( null );
                if( existing != null )
                {
                    response.error( "Series " + yVar + " for " + sourceDe.getName() + " already exist." );
                    return;
                }
                else
                {
                    String legend = yVar;
                    if( StreamEx.of( plot.getSeries() ).anyMatch( s -> s.getLegend().equals( yVar ) ) )
                    {
                        legend += " (" + sourceDe.getName() + ")";
                    }
                    series.setLegend( legend );
                    Color seriesColor = PlotsInfo.POSSIBLE_COLORS[plot.getSeries().size() % PlotsInfo.POSSIBLE_COLORS.length];
                    series.setSpec( new Pen( 1.0f, seriesColor ) );
                    plot.addSeries( series );
                    response.sendString( "" );
                }
            }
        }
        else if( "remove".equals(action) )
        {
            String[] snames = arguments.optStrings("series");
            if( snames != null )
            {
                List<Series> series = plot.getSeries();
                Set<String> names = new HashSet<>(Arrays.asList(snames));
                series.removeIf( s -> names.contains(s.getName()) );
                response.sendString( "" );
            }
        }
        else if( "save".equals(action) )
        {
            String path = arguments.getString("plot_path");
            PlotEx.savePlot(plot, DataElementPath.create(path));
            response.sendString( "" );
        }
        else if( "plot".equals(action) )
        {
            BufferedImage resultImage = drawChart(plot);
            if( resultImage != null )
            {
                String imageName = dePath + "_img";
                WebSession.getCurrentSession().putImage(imageName, resultImage);
                JSONObject result = new JSONObject();
                result.put("image", imageName);
                result.put("needUpdate", plot.needUpdate());
                DataElementPath path = plot.getDefaultSource();
                if( path != null && !path.isEmpty() )
                    result.put( "path", path.toString() );
                response.sendJSON(result);
            }
        }
        else if( "new".equals(action) )
        {
            plot.removeAllSeries();
            response.sendString( "" );
        }
        else if( "filters".equals( action ) ) //TODO:
        {
            JSONObject result = new JSONObject();
            JSONArray seriesColumns = new JSONArray();
            JSONArray timeFilter = new JSONArray();
            List<Series> series = plot.getSeries();
            for( Series s : series )
            {
                if( s.getSource() != null && s.getSourceNature().equals(Series.SourceNature.SIMULATION_RESULT) )
                {
                    if( "time".equalsIgnoreCase(s.getYVar()) )
                        continue;
                    JSONArray col = new JSONArray();
                    col.put(s.getSource());
                    col.put( s.getYPath().isEmpty() ? s.getYVar() : s.getYPath() + "/" + s.getYVar() );
                    seriesColumns.put(col);

                    if( s.getSourceNature().equals(Series.SourceNature.SIMULATION_RESULT) && timeFilter.length() == 0 )
                    {
                        SimulationResult sResult = DataElementPath.create(s.getSource()).getDataElement(SimulationResult.class);
                        double times[] = sResult.getTimes();
                        timeFilter.put(times[0]);
                        timeFilter.put(times[times.length - 1]);
                        timeFilter.put(times[1] - times[0]);
                    }
                }
            }
            if( seriesColumns.length() > 0 && timeFilter.length() > 0 )
            {
                result.put("seriesColumns", seriesColumns);
                result.put("timeFilter", timeFilter);
            }
            response.sendJSON(result);
        } else
            throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
    }
}
