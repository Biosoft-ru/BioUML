package biouml.plugins.simulation.web;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Series;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.SimulationEngine.Var;
import biouml.plugins.simulation.plot.PlotPane;
import biouml.standard.simulation.StochasticSimulationResult;
import biouml.standard.simulation.plot.Plot;
import one.util.streamex.StreamEx;
import ru.biosoft.analysis.Util;
import ru.biosoft.graphics.Pen;

public class StochasticWebResultPlot extends WebResultPlot
{
    private List<double[][]> values;
    private Map<Integer, Integer> varIndexInPlot;
    private int currentCycle;
    private int spanIndex;
    private int spanSize;

    public StochasticWebResultPlot(PlotInfo plotInfo)
    {
        super(plotInfo);
    }
    
    @Override
    public void init(SimulationEngine engine)
    {
        spanSize = new UniformSpan(engine.getInitialTime(), engine.getCompletionTime(), engine.getTimeIncrement()).getLength();
        dataset = new YIntervalSeriesCollection();
        xVariable = engine.getXVariable(plotInfo);
        variableIndeces = engine.getVariablesToPlot(plotInfo);
        String yAxisLabel = variableIndeces.size() == 1? StreamEx.of(variableIndeces.keySet()).findFirst().map(v->v.title).get(): "Quantity or concentration";
        
        chart = ChartFactory.createXYLineChart(plotInfo.getTitle(), xVariable.title, yAxisLabel, null, //dataset,
                PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );
        chart.setBackgroundPaint(Color.white);

        XYLineAndShapeRenderer renderer = new DeviationRenderer();
        renderer.setDrawSeriesLineAsPath(true);
        // plot properties
        XYPlot plot = chart.getXYPlot();
        //X axis
        ValueAxis axis = PlotPane.generateAxis( Plot.AxisType.getAxisType( plotInfo.getXAxisType() ) );
        if( axis != null )
            plot.setDomainAxis( axis );

        if( plotInfo.getXFrom() != plotInfo.getXTo() )
        {
            plot.getDomainAxis().setLowerBound( plotInfo.getXFrom() );
            plot.getDomainAxis().setUpperBound( plotInfo.getXTo() );
        }
        else
        {
            plot.getDomainAxis().setLowerBound( engine.getInitialTime() );
            plot.getDomainAxis().setUpperBound( engine.getCompletionTime() );
        }
        plot.getDomainAxis().setAutoRange( plotInfo.isXAutoRange() );

        //Y axis
        axis = PlotPane.generateAxis( Plot.AxisType.getAxisType( plotInfo.getYAxisType() ) );
        if( axis != null )
            plot.setRangeAxis( axis );
        if( plotInfo.getYFrom() != plotInfo.getYTo() )
        {
            plot.getRangeAxis().setLowerBound( plotInfo.getYFrom() );
            plot.getRangeAxis().setUpperBound( plotInfo.getYTo() );
        }
        plot.getRangeAxis().setAutoRange( plotInfo.isYAutoRange() );

        int counter = 0;
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            SimulationEngine.Var var = e.getKey();
            YIntervalSeries series = (YIntervalSeries)e.getValue().get(0);
            if( series != null )
            {
                Pen spec = var.pen;
                if( spec != null )
                {
                    renderer.setSeriesShapesVisible( counter, false );
                    renderer.setSeriesPaint( counter, spec.getColor() );
                    renderer.setSeriesFillPaint( counter, spec.getColor() );
                    renderer.setSeriesStroke( counter, spec.getStroke());
                }
                counter++;
                ((YIntervalSeriesCollection)dataset).addSeries(series);
            }
        }
        
        if( plotInfo.getExperiments() != null )
            ResultPlotPane.addExperiments( plotInfo.getExperiments(), renderer, dataset, engine.getCompletionTime()  );

        chart.getXYPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setRenderer(renderer);
        chart.getXYPlot().setDataset(dataset);
    
        varIndexInPlot = new HashMap<Integer, Integer>();
        int i = 0;
        for( Entry<Var, List<Series>> e : variableIndeces.entrySet() )
        {
            varIndexInPlot.put( e.getKey().index, i );
            i++;
        }
    }
    
    @Override
    public void add(double t, double[] y)
    {
        double[][] vals = values.get( currentCycle );
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            int varIndex = e.getKey().index;
            int xIndex = xVariable.index;
            int index = varIndexInPlot.get( varIndex );
            vals[index][spanIndex] = y[varIndex];
            Series s = e.getValue().get( 0 );

            if( currentCycle >= 2 )
            {
                double[] variableValues = new double[currentCycle + 1];
                for( int k = 0; k <= currentCycle; k++ )
                    variableValues[k] = values.get( k )[index][spanIndex];
                Util.sort( variableValues );
                double median = StochasticSimulationResult.median( variableValues );
                double q1 = StochasticSimulationResult.quartile1( variableValues );
                double q3 = StochasticSimulationResult.quartile3( variableValues );
                ( (YIntervalSeries)s ).add( y[xIndex], median, q1, q3 );
            }
            else
            {
                ( (YIntervalSeries)s ).add( y[xIndex], y[varIndex], y[varIndex], y[varIndex] );
            }
        }
        spanIndex++;
    }
    
    @Override
    public void start(Object model)
    {
        currentCycle = 0;
        spanIndex = 0;
        values = new ArrayList<>();        
        values.add( new double[variableIndeces.size()][spanSize] );
    }

    @Override
    public void startCycle()
    {
        currentCycle++;
        spanIndex = 0;

        for( List<Series> list : variableIndeces.values() )
        {
            for( Series s : list )
                ( (YIntervalSeries)s ).clear();
        }
        values.add( new double[variableIndeces.size()][spanSize] );
    }
}