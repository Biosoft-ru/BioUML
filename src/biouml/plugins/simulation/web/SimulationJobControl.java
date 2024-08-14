package biouml.plugins.simulation.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Series;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.CycledResultListener;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngine.Var;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.plot.PlotPane;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;
import biouml.standard.simulation.plot.Plot;
import one.util.streamex.StreamEx;
import ru.biosoft.analysis.Util;
import ru.biosoft.graphics.Pen;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.util.LimitedTextBuffer;

public class SimulationJobControl extends AbstractJobControl implements CycledResultListener
{
    protected static final Logger log = Logger.getLogger(SimulationJobControl.class.getName());

    protected SimulationEngine engine;
    protected WebResultPlot plots[];
    protected String errorMessage = null;
    private LimitedTextBuffer message = new LimitedTextBuffer( 100 );
    protected boolean isInit = false;
    
    private SimulationResult simulationResult;

    public SimulationJobControl(SimulationEngine simulationEngine)
    {
        super(null);
        this.engine = simulationEngine;
    }

    @Override
    protected void doRun() throws JobControlException
    {
        if( engine.needToShowPlot && !engine.hasVariablesToPlot() )
        {
            errorMessage = "There are no variables to plot<br>Set up and adjust variables in the \"Plot\" view part.";
            return;
        }
        List<String> incorrect = new ArrayList<>();
        try
        {
            incorrect = engine.getIncorrectPlotVariables();
        }
        catch( Exception ex )
        {
            errorMessage = ex.getMessage();
            return;
        }
        
        if( incorrect.size() > 0 )
        {
            errorMessage = "Some plot variables are missing or incorrect. Please, change plot settings for "
                    + String.join( ", ", incorrect ) + " in the \"Plot\" view part.";
            return;
        }
        String outputDir = engine.getOutputDir();
        
        if( outputDir != null ) //some simulation engines do not need output dir
        {
            File dir = new File( outputDir );
            if( !dir.exists() )
            {
                dir.mkdirs();
            }
        }
        
        Model model = null;
        try
        {
            model = engine.createModel();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not generate model", e);
            errorMessage = e.getMessage();
        }

        if( model != null )
        {
            try
            {
                simulationResult = engine.generateSimulationResult();
                ResultWriter resultWriter = new ResultWriter( simulationResult ); 

                int type = engine.getSimulationType();
                if( type == SimulationEngine.STOCHASTIC_TYPE )
                    plots = StreamEx.of( engine.getPlots() ).map( p -> new StochasticWebResultPlot(p) ).toArray( WebResultPlot[]::new );
                else
                    plots = StreamEx.of( engine.getPlots() ).map( p -> new WebResultPlot(p) ).toArray( WebResultPlot[]::new );
                
                for( WebResultPlot plot : plots )
                    plot.init(engine);
                
                isInit = true;
                errorMessage = engine.simulate( model, new ResultListener[] {this, resultWriter});
                if( errorMessage != null )
                    setTerminated(AbstractJobControl.TERMINATED_BY_ERROR);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not simulate model", t);
                if( errorMessage == null )
                    errorMessage = "Can not simulate model: " + t.getMessage();
                setTerminated( AbstractJobControl.TERMINATED_BY_ERROR );
            }
        }
        else
        {
            if( errorMessage == null )
                errorMessage = this.message.toString();
            setTerminated( AbstractJobControl.TERMINATED_BY_ERROR );
        }
    }
    public String getErrorMessage()
    {
        return errorMessage;
    }
    
    public static class WebResultPlot
    {
        protected JFreeChart chart;
        protected Map<SimulationEngine.Var, List<Series>> variableIndeces;
        protected AbstractXYDataset dataset;
        protected Var xVariable;
        protected PlotInfo plotInfo; 
        
        public WebResultPlot(PlotInfo plotInfo)
        {
            this.plotInfo = plotInfo;
        }
        
        public void init(SimulationEngine engine)
        {
            dataset = new XYSeriesCollection();
            xVariable = engine.getXVariable(plotInfo);
            variableIndeces = engine.getVariablesToPlot(plotInfo);
            String yAxisLabel = variableIndeces.size() == 1? StreamEx.of(variableIndeces.keySet()).findFirst().map(v->v.title).get(): "Quantity or concentration";
            
            chart = ChartFactory.createXYLineChart(plotInfo.getTitle(), xVariable.title, yAxisLabel, null, //dataset,
                    PlotOrientation.VERTICAL, true, // legend
                    true, // tool tips
                    false // URLs
            );
            chart.setBackgroundPaint(Color.white);

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
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

            //Possible to adjust
            //          plot.getDomainAxis().setLabel(plotInfo.getXTitle());
            //            plot.getRangeAxis().setLabel(plotInfo.getYTitle());
            //
            //            plot.getDomainAxis().setLabelFont( plotInfo.getXTitleFont().getFont() );
            //            plot.getRangeAxis().setLabelFont( plotInfo.getYTitleFont().getFont() );
            //
            //            plot.getDomainAxis().setLabelPaint( plotInfo.getXTitleFont().getColor() );
            //            plot.getRangeAxis().setLabelPaint( plotInfo.getYTitleFont().getColor() );
            //
            //            plot.getDomainAxis().setTickLabelFont( plotInfo.getXTickLabelFont().getFont() );
            //            plot.getRangeAxis().setTickLabelFont( plotInfo.getYTickLabelFont().getFont() );
            //
            //            plot.getDomainAxis().setTickLabelPaint( plotInfo.getXTickLabelFont().getColor() );
            //            plot.getRangeAxis().setTickLabelPaint( plotInfo.getYTickLabelFont().getColor() );


            int counter = 0;
            for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
            {
                SimulationEngine.Var var = e.getKey();
                XYSeries series = (XYSeries)e.getValue().get(0);
                if( series != null )
                {
                    Pen spec = var.pen;
                    if( spec != null )
                    {
                        renderer.setSeriesShapesVisible(counter, false);
                        renderer.setSeriesPaint(counter, spec.getColor());
                        renderer.setSeriesStroke(counter, spec.getStroke());
                    }
                    counter++;
                    ((XYSeriesCollection)dataset).addSeries(series);
                }
            }
            
            if( plotInfo.getExperiments() != null )
                ResultPlotPane.addExperiments( plotInfo.getExperiments(), renderer, dataset, engine.getCompletionTime()  );

            chart.getXYPlot().setBackgroundPaint(Color.white);
            chart.getXYPlot().setRenderer(renderer);
            chart.getXYPlot().setDataset(dataset);
        }
        
        public BufferedImage getImage()
        {
            return chart.createBufferedImage(550, 350);
        }
        
        public void add(double t, double[] y)
        {
            int size = variableIndeces.size();
            int count = 0;
            for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
            {
                SimulationEngine.Var var = e.getKey();
                if (var.index == null)
                    continue;
                
                XYSeries series = (XYSeries)e.getValue().get(0);
                double yV =  y[var.index];
                double xV =  y[xVariable.index];
                series.add(xV, yV, count == size - 1);
                count++;
            }
        }
        
        public void start(Object model)
        {
        }

        public void startCycle()
        {
        }
    }
    
    @Override
    public void start(Object model)
    {
        for( WebResultPlot plot : plots )
            plot.start(model);
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {        
        for( WebResultPlot plot : plots )
            plot.add(t, y);
        
        double percent = ( ( t - engine.getInitialTime() ) / ( engine.getCompletionTime() - engine
                .getInitialTime() ) ) * 100.0;
        setPreparedness((int)percent);
    }

    public BufferedImage[] generateResultImage()
    {
        return isInit ? StreamEx.of(plots).map(p -> p.getImage()).toArray(BufferedImage[]::new) : null;
    }
    
    public SimulationResult getSimulationResult()
    {
        return simulationResult;
    }
    
    @Override
    public void terminate()
    {
        engine.stopSimulation();
        super.terminate();
    }

    public SimulationEngine getSimulationEngine()
    {
        return engine;
    }

    @Override
    public void finish()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addAsFirst(double t, double[] y)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(double t, double[] y)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void startCycle()
    {
        for( WebResultPlot plot : plots )
            plot.startCycle();
    }    
    
    public static class StochasticWebResultPlot extends WebResultPlot
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

    public String getJobMessage()
    {
        return message.toString();

    }

    public void addJobMessage(String message)
    {
        this.message.add( message );
    }

}
