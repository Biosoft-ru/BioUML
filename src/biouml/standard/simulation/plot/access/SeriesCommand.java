package biouml.standard.simulation.plot.access;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.diagram.Util;
import biouml.standard.simulation.ScriptDataGenerator;
import biouml.standard.simulation.SimulationDataGenerator;
import biouml.standard.simulation.plot.DataGeneratorSeries;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import ru.biosoft.access.support.TagCommandSupport;
import ru.biosoft.access.support.TagEntryTransformer;
import ru.biosoft.graphics.Pen;

class SeriesCommand extends TagCommandSupport<Plot>
{
    protected static final Logger log = Logger.getLogger(SeriesCommand.class.getName());
    protected static String endl = System.getProperty("line.separator");
    
    private static final String TOKEN = "_SPACE_";

    public SeriesCommand(TagEntryTransformer<Plot> transformer)
    {
        super("SE", transformer);
    }

    @Override
    public String getTaggedValue()
    {
        Plot pl = transformer.getProcessedObject();

        StringBuffer result = new StringBuffer();
        List<Series> series = pl.getSeries();
        int seriesNumber = series.size();
        for( int i = 0; i < seriesNumber; i++ )
        {
            Series s = series.get(i);
            String plotName = s.getPlotName();
            if (plotName == null)
                plotName = pl.getName();
            String spec = null;
            if( s.getSpec() != null )
                spec = s.getSpec().toString();
            result.append(getTag() + "    " + replaceSpaces(plotName) + " " + replaceSpaces(spec) + " "
                    + replaceSpaces(s.getLegend()) + " ");
            if( s instanceof DataGeneratorSeries )
            {
                result.append(replaceSpaces(((DataGeneratorSeries)s).getXGenerator().toString()) + " " + replaceSpaces(((DataGeneratorSeries)s).getYGenerator().toString()));
            }
            else
            {
                String xVar = s.getXPath().isEmpty()? s.getXVar(): s.getXPath() +"/" + s.getXVar();
                String yVar = s.getYPath().isEmpty()? s.getYVar(): s.getYPath() +"/" + s.getYVar();
                result.append(replaceSpaces(xVar) + " " + replaceSpaces(yVar) + " " + replaceSpaces(s.getSource()) + " "
                        + replaceSpaces(s.getSourceNature().toString()) );
            }
            result.append( " " + s.getName() + endl);
        }
        return result.toString();
    }

    //
    Plot pl = null;
    @Override
    public void start(String string)
    {
        pl = transformer.getProcessedObject();
        pl.removeAllSeries();
    }

    @Override
    public void addValue(String string)
    {
        StringTokenizer strtok = new StringTokenizer(string, " ");

        //  System.out.println("strtok.countTokens() = " + strtok.countTokens());
        if( strtok.countTokens() == 7 || strtok.countTokens() == 8 )
        {
            Series s = new Series();
            s.setPlotName(replaceSpacesBack(strtok.nextToken()));
            s.setSpec(Pen.createInstance(replaceSpacesBack(strtok.nextToken())));
            s.setLegend(replaceSpacesBack(strtok.nextToken()));
            
            String xPath = replaceSpacesBack(strtok.nextToken());//TODO: beter way
            if( xPath.contains( "/" ) )
            {
                String[] pathComponents = Util.getMainPathComponents( xPath );
                s.setXPath( pathComponents[0] );
                s.setXVar( pathComponents[1] );
            }
            else
                s.setXVar( xPath );
            
            String yPath = replaceSpacesBack(strtok.nextToken());
            if( yPath.contains( "/" ) )
            {
                String[] pathComponents = Util.getMainPathComponents( yPath );
                s.setYPath( pathComponents[0] );
                s.setYVar( pathComponents[1] );
            }
            else
                s.setYVar( yPath );

            s.setSource( replaceSpacesBack( strtok.nextToken() ) );
            String r = replaceSpacesBack(strtok.nextToken());
            s.setSourceNature(Series.SourceNature.valueOf(r));

            try
            {
                String name = strtok.nextToken();
                s.setName(name);
            }
            catch( Exception e1 )
            {
            }
            
            pl.addSeries(s);
        }
        else if( strtok.countTokens() == 6 )
        {
            DataGeneratorSeries s = new DataGeneratorSeries();
            s.setPlotName(replaceSpacesBack(strtok.nextToken()));
            s.setSpec(Pen.createInstance(replaceSpacesBack(strtok.nextToken())));
            s.setLegend(replaceSpacesBack(strtok.nextToken()));
            //TODO: support other types of data generators
            SimulationDataGenerator sdgX = new ScriptDataGenerator(replaceSpacesBack(strtok.nextToken()));
            SimulationDataGenerator sdgY = new ScriptDataGenerator(replaceSpacesBack(strtok.nextToken()));
            s.setXGenerator(sdgX);
            s.setYGenerator(sdgY);
            s.setName(replaceSpacesBack(strtok.nextToken()));
            pl.addSeries(s);
        }
        else
        {
            log.log(Level.SEVERE, "Could not parse the string: " + string);
            return;
        }
    }

    private String replaceSpaces(String str)
    {
        if( str == null )
            return str;
        String result = str.replaceAll(" ", TOKEN);
        if( result.equals("") )
        {
            result = TOKEN;
        }
        return result;
    }

    private String replaceSpacesBack(String str)
    {
        String result = str;
        if( result.equals(TOKEN) )
        {
            result = "";
        }
        result = result.replaceAll(TOKEN, " ");
        return result;
    }
}
