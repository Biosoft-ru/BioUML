
package biouml.plugins.research.workflow.engine;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.Pair;
import ru.biosoft.util.TextUtil2;
import biouml.standard.simulation.SimulationDataGenerator;
import biouml.standard.simulation.plot.DataGeneratorSeries;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;

import com.developmentontheedge.beans.DynamicProperty;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;

/**
 * @author anna
 *
 */
public class PlotElement extends WorkflowElement implements PropertyChangeListener
{
    private DataElementPath plotPath = null;
    private boolean autoOpen = true;
    private Plot plot = null;
    private final Map<String, Pair<String, String>> genSeries;
    private final Map<String, Pair<SimulationDataGenerator, SimulationDataGenerator>> seriesGenerators;
    private final Map<String, Pair<String, String>> varSeries;
    public static final String Y_GENERATOR_PROPERTY = "yGenerator";
    public static final String X_GENERATOR_PROPERTY = "xGenerator";

    private JobControlListener listener;

    public static final String AUTO_OPEN = "autoOpen";
    public static final String PLOT_PATH = "plotPath";

    private boolean complete = false;
    private int generatorsCount = 0;

    public PlotElement(String name, DataElementPath plotPath, boolean autoOpen, List<String> xSeriesSources, List<String> ySeriesSources,
            List<String> xGenSources, List<String> yGenSources, DynamicProperty statusProperty)
    {
        super(statusProperty);
        this.plotPath = plotPath;
        plot = new Plot(plotPath.optParentCollection(), plotPath.getName());
        try
        {
            plotPath.save(plot);
        }
        catch( Exception e )
        {
        }
        this.autoOpen = autoOpen;
        varSeries = new HashMap<>();
        genSeries = new HashMap<>();
        seriesGenerators = new HashMap<>();

        //TODO: refactor, unify series addition
        //Simple series
        if( xSeriesSources != null )
            for( String source : xSeriesSources )
            {
                String[] seriesParams = TextUtil2.split( source, ';' ); //seriesName;xVar
                varSeries.computeIfAbsent( seriesParams[0], k -> new Pair<>() ).setFirst( seriesParams[1] );
            }

        if( ySeriesSources != null )
            for( String source : ySeriesSources )
            {
                String[] seriesParams = TextUtil2.split( source, ';' ); //seriesName;yVar
                varSeries.computeIfAbsent( seriesParams[0], k -> new Pair<>() ).setSecond( seriesParams[1] );
            }
        for( Map.Entry<String, Pair<String, String>> entry : varSeries.entrySet() )
        {
            Pair<String, String> sVars = entry.getValue();
            if( sVars.getFirst() != null && sVars.getSecond() != null )
            {
                Series s = new Series(entry.getKey());
                s.setXVar(sVars.getFirst());
                s.setYVar(sVars.getSecond());
                plot.addSeries(s);
            }
        }

        //DataGeneratorSeries
        if( xGenSources != null )
            for( int i = 0; i < xGenSources.size(); i++ )
            {
                String[] seriesParams = TextUtil2.split( xGenSources.get(i), ';' ); //seriesName;xGeneratorName
                if( !genSeries.containsKey(seriesParams[0]) )
                    genSeries.put(seriesParams[0], new Pair<String, String>());
                genSeries.get(seriesParams[0]).setFirst(seriesParams[1]);
            }

        if( yGenSources != null )
            for( int i = 0; i < yGenSources.size(); i++ )
            {
                String[] seriesParams = TextUtil2.split( yGenSources.get(i), ';' ); //seriesName;yGeneratorName
                if( !genSeries.containsKey(seriesParams[0]) )
                    genSeries.put(seriesParams[0], new Pair<String, String>());
                genSeries.get(seriesParams[0]).setSecond(seriesParams[1]);
            }
    }

    @Override
    public boolean isComplete()
    {
        return complete;
    }

    @Override
    public void startElementExecution(JobControlListener listener)
    {
        this.listener = listener;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( evt.getPropertyName().equals("start") )
        {
            if( evt.getSource() instanceof SimulationDataGenerator )
                addDataGenerator((SimulationDataGenerator)evt.getSource());
            generatorsCount++;
        }
        else if( evt.getPropertyName().equals("end") )
        {
            generatorsCount--;
            if( generatorsCount == 0 )
            {
                complete = true;
                try
                {
                    plot.setNeedUpdate(false);
                    plotPath.save(plot);
                }
                catch( Exception e )
                {
                }
            }
        }
    }

    private void addDataGenerator(SimulationDataGenerator gen)
    {
        String generatorName = gen.getName();

        for( Map.Entry<String, Pair<String, String>> entry : genSeries.entrySet() )
        {
            String seriesName = entry.getKey();
            Pair<SimulationDataGenerator, SimulationDataGenerator> genPair = seriesGenerators.get(seriesName);
            if( genPair == null )
                genPair = new Pair<>();

            boolean isUsed = false;
            Pair<String, String> sVars = entry.getValue();
            if( generatorName.equals(sVars.getFirst()) )
            {
                genPair.setFirst(gen);
                isUsed = true;
            }
            if( generatorName.equals(sVars.getSecond()) )
            {
                genPair.setSecond(gen);
                isUsed = true;
            }

            if( genPair.getFirst() != null && genPair.getSecond() != null )
            {
                DataGeneratorSeries s = new DataGeneratorSeries(seriesName);
                s.setXGenerator(genPair.getFirst());
                s.setYGenerator(genPair.getSecond());
                plot.addSeries(s);
                seriesGenerators.remove(seriesName);
            }
            else if( isUsed )
                seriesGenerators.put(seriesName, genPair);
        }

        if( seriesGenerators.size() == 0 )
        {
            if( autoOpen )
            {
                try
                {
                    plot.setNeedUpdate(true);
                    JobControlEvent event = new JobControlEvent(null, new Object[] {plot});
                    listener.resultsReady(event);
                }
                catch( Exception e )
                {
                    log.log( Level.SEVERE, "Error occured when creating plot " + plot.getName(), e );
                }
            }
        }
    }
}
