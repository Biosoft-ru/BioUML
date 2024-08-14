package biouml.plugins.simulation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Series;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import ru.biosoft.analysis.Stat;
import ru.biosoft.analysis.Util;
import ru.biosoft.graphics.Pen;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.SimulationEngine.Var;
import biouml.plugins.simulation.plot.PlotPane;
import biouml.standard.simulation.StochasticSimulationResult;
import biouml.standard.simulation.plot.Plot;
import one.util.streamex.StreamEx;

@SuppressWarnings ( "serial" )
public class StochasticResultPlotPane extends JPanel implements CycledResultListener
{
    private static final String DEFAULT_Y_AXIS_LABEL = "Quantity or concentration";

    public static final Paint[] POSSIBLE_COLORS = new Paint[] {new Color( 0xFF, 0x55, 0x55 ), new Color( 0x55, 0x55, 0xFF ),
            new Color( 0x00, 0xFF, 0x00 ), Color.pink, ChartColor.DARK_RED, ChartColor.DARK_BLUE, ChartColor.DARK_GREEN,
            ChartColor.DARK_MAGENTA, ChartColor.DARK_CYAN, Color.darkGray, ChartColor.VERY_DARK_RED, ChartColor.VERY_DARK_BLUE,
            ChartColor.VERY_DARK_GREEN, ChartColor.VERY_DARK_YELLOW, ChartColor.VERY_DARK_MAGENTA, ChartColor.VERY_DARK_CYAN};

    protected YIntervalSeriesCollection dataset;
    protected JFreeChart chart;
    protected List<double[][]> values;
    private Map<Integer, Integer> varIndexInPlot;
    protected ChartPanel chartPanel;
    protected PlotInfo plotInfo = null;
    protected SimulationEngine.Var xVariable;
    protected Map<SimulationEngine.Var, List<Series>> variableIndeces;
    protected FunctionJobControl jobControl;
    protected SimulationEngine simulationEngine;
    protected java.awt.Dimension paneSize = new java.awt.Dimension( 500, 270 );;
    protected int currentCycle;
    protected int spanIndex;
    private int spanSize;
    private DeviationRenderer renderer;
    private boolean averageRegime = false;

    public StochasticResultPlotPane(SimulationEngine simulationEngine, FunctionJobControl jobControl, PlotInfo plotInfo)
    {
        this.simulationEngine = simulationEngine;
        this.spanSize = new UniformSpan( simulationEngine.getInitialTime(), simulationEngine.getCompletionTime(),
                simulationEngine.getTimeIncrement() ).getLength();
        this.jobControl = jobControl;
        this.plotInfo = plotInfo;        
    }

    public SimulationEngine getSimulationEngine()
    {
        return simulationEngine;
    }

    public void setAverageRegime(boolean avgRegime)
    {
        this.averageRegime = avgRegime;
    }

    protected void createChartPanel()
    {        
        xVariable = simulationEngine.getXVariable( plotInfo );

        String yAxisLabel = variableIndeces.size() == 1 ? StreamEx.of( variableIndeces.keySet() ).findFirst().map( v -> v.title ).get()
                : DEFAULT_Y_AXIS_LABEL;

        chart = ChartFactory.createXYLineChart( plotInfo != null ? plotInfo.getTitle() : "Simulation result", xVariable.title, yAxisLabel,
                null, //dataset,
                PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );
        chart.setBackgroundPaint( Color.white );
        chart.getLegend().setBorder( 1, 1, 1, 1 );
        chartPanel = new ChartPanel( chart );

        renderer = new DeviationRenderer();
        renderer.setDrawSeriesLineAsPath( true );
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
            plot.getDomainAxis().setLowerBound( simulationEngine.getInitialTime() );
            plot.getDomainAxis().setUpperBound( simulationEngine.getCompletionTime() );
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

        plot.setDrawingSupplier( new DefaultDrawingSupplier( POSSIBLE_COLORS, DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE, DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE ) );

        dataset = new YIntervalSeriesCollection();

        initSeries( renderer, dataset );
        if( plotInfo.getExperiments() != null )
            ResultPlotPane.addExperiments( plotInfo.getExperiments(), renderer, dataset, this.simulationEngine.getCompletionTime() );

        chart.getXYPlot().getRangeAxis().setLabel( plotInfo.getYAxisInfo().getTitle() );
        chart.getXYPlot().getDomainAxis().setLabel( plotInfo.getXAxisInfo().getTitle() );
        chart.getXYPlot().getRangeAxis().setLabelFont( new Font( "Arial", Font.BOLD, 18 ) );
        chart.getXYPlot().getRangeAxis().setTickLabelFont( new Font( "Arial", Font.PLAIN, 16 ) );
        chart.getXYPlot().getDomainAxis().setLabelFont( new Font( "Arial", Font.BOLD, 18 ) );
        chart.getXYPlot().getDomainAxis().setTickLabelFont( new Font( "Arial", Font.PLAIN, 16 ) );
        chart.getXYPlot().setBackgroundPaint( Color.white );
        chart.getXYPlot().setRenderer( renderer );
        chart.getXYPlot().setDataset( dataset );
    }

    protected void initSeries(XYLineAndShapeRenderer renderer, YIntervalSeriesCollection dataset)
    {
        int counter = 0;
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            SimulationEngine.Var var = e.getKey();
            List<Series> seriesList = e.getValue();

            if( seriesList != null )
            {
                for( int i = 0; i < seriesList.size(); i++ )
                {
                    YIntervalSeries series = (YIntervalSeries)seriesList.get( i );
                    Pen spec = var.pen;
                    if( spec != null )
                    {
                        renderer.setSeriesPaint( counter, spec.getColor() );
                        renderer.setSeriesStroke( counter, spec.getStroke() );
                        renderer.setSeriesFillPaint( counter, spec.getColor() );
                        renderer.setSeriesVisibleInLegend( counter, i == 0 );
                    }

                    renderer.setSeriesShapesVisible( counter, false );

                    counter++;
                    dataset.addSeries( series );
                }
            }
        }
    }

    protected Map<Var, List<Series>> getVariables()
    {
        return simulationEngine.getVariablesToPlot( plotInfo );
    }

    protected void createUI()
    {
        createChartPanel();

        // layout issues
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints( this, gbc );
        setLayout( gbl );

        setBorder( new EmptyBorder( 10, 10, 10, 10 ) );

        add( chartPanel, new GridBagConstraints( 0, 0, 1, 5, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets( 2, 2, 2, 2 ), 0, 0 ) );

        if( jobControl != null )
        {
            JPanel buttonPanel = new JPanel( new BorderLayout() );
            // stop button
            add( buttonPanel, new GridBagConstraints( 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets( 2, 2, 2, 2 ), 0, 0 ) );

            JButton stopButton = new JButton( "Stop" );
            buttonPanel.add( stopButton, BorderLayout.EAST );
            buttonPanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
            stopButton.addActionListener( ae -> {
                simulationEngine.stopSimulation();
                jobControl.terminate();
            } );
        }

        JFrame frame = new JFrame( chartPanel.getChart().getTitle().getText() );
        frame.setSize( 700, 700 );

        frame.getContentPane().setLayout( new BorderLayout() );

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout( new BorderLayout() );
        frame.getContentPane().add( mainPanel, BorderLayout.CENTER );

        mainPanel.add( this, BorderLayout.CENTER );

        frame.pack();
        frame.setVisible( true );

        // finish computations correctly when user closes the window
        frame.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                simulationEngine.stopSimulation();
                if( jobControl != null )
                    jobControl.terminate();
            }
        } );
    }

    @Override
    public void add(double t, double[] y)
    {
        if( currentCycle == 0 )
            addAsFirst( t, y );
        else
            update( t, y );
        spanIndex++;
    }

    @Override
    public void start(Object model)
    {
        currentCycle = 0;
        spanIndex = 0;
        variableIndeces = getVariables();
        varIndexInPlot = new HashMap<Integer, Integer>();
        int i = 0;
        for( Entry<Var, List<Series>> e : variableIndeces.entrySet() )
        {
            varIndexInPlot.put( e.getKey().index, i );
            i++;
        }
        values = new ArrayList<>();//double[variableIndeces.size()][spanSize][cyclesNumber];        
        values.add( new double[variableIndeces.size()][spanSize] );
        createUI();
    }

    @Override
    public final void startCycle()
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


    @Override
    public void addAsFirst(double t, double[] y)
    {
        double[][] vals = values.get( 0 );

        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            int varIndex = e.getKey().index;
            int index = this.varIndexInPlot.get( varIndex );
            vals[index][spanIndex] = y[varIndex];
            Series s = e.getValue().get( 0 );
            ( (YIntervalSeries)s ).add( y[xVariable.index], y[varIndex], y[varIndex], y[varIndex] );
        }
    }

    @Override
    public void update(double t, double[] y)
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

                if( averageRegime )
                {
                    double avg = Stat.mean( variableValues );
                    double stdv = Math.sqrt( Stat.variance( variableValues ) );
                    ( (YIntervalSeries)s ).add( y[xIndex], avg, avg - stdv, avg + stdv );
                }
                else
                {
                    Util.sort( variableValues );
                    double median = StochasticSimulationResult.median( variableValues );
                    double q1 = StochasticSimulationResult.quartile1( variableValues );
                    double q3 = StochasticSimulationResult.quartile3( variableValues );
                    ( (YIntervalSeries)s ).add( y[xIndex], median, q1, q3 );
                }
            }
            else
            {
                ( (YIntervalSeries)s ).add( y[xIndex], y[varIndex], y[varIndex], y[varIndex] );
            }
        }
    }

    @Override
    public void finish()
    {
    }
}