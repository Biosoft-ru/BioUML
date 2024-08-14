package biouml.standard.simulation.plot.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;

import com.eclipsesource.json.ParseException;

import biouml.plugins.simulation.plot.TableSimulationResultsWrapper;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.table.access.TableResolver;

public class SeriesTableResolver extends TableResolver
{
    private String what;
    private Map<String, List<String>> resultVariables;
    private double[] timeFilter;

    public SeriesTableResolver(BiosoftWebRequest arguments)
    {
        String columns = arguments.get("names");
        String times = arguments.get("times");
        what = "series";
        if( columns != null && times != null )
        {


            try
            {
                JSONArray timesArr = new JSONArray( times );
                timeFilter = StreamEx.of( timesArr.spliterator() ).mapToDouble( val -> Double.parseDouble( val.toString() ) ).toArray();
                JSONArray columnsArr = new JSONArray( columns );
                resultVariables = StreamEx.of( columnsArr.spliterator() ).groupingBy( col -> ( (JSONArray)col ).getString( 0 ),
                        Collectors.mapping( col -> ( (JSONArray)col ).getString( 1 ), Collectors.toList() ) );
                what = "data";
            }
            catch( UnsupportedOperationException | ParseException e )
            {
            }
        }
    }

    @Override
    public String getRowId(DataElement de, String elementName)
    {
        return elementName;
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        Plot plot = de.cast( Plot.class );
        if( what.equals("series") )
        {
            VectorDataCollection<Series> vdc = new VectorDataCollection<>("Series", Series.class, null);
            List<Series> series = plot.getSeries();
            int i = 1;
            for(Series s: series)
            {
                s.setName("series_" + i);
                i++;
                vdc.put(s);
            }
            return vdc;
        }
        // what = "data"
        return new TableSimulationResultsWrapper(resultVariables, timeFilter);
    }
}