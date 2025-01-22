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
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
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
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.AbstractSeriesDataset;
import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.SimulationEngine.Var;
import biouml.plugins.simulation.plot.PlotPane;
import biouml.standard.simulation.plot.Plot;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.Pen;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@SuppressWarnings ( "serial" )
public class ResultPlotPane extends JPanel implements CycledResultListener
{
    private static final String DEFAULT_Y_AXIS_LABEL = "Quantity or concentration";

    public static final Paint[] POSSIBLE_COLORS = new Paint[] {new Color(0xFF, 0x55, 0x55), new Color(0x55, 0x55, 0xFF),
            new Color(0x00, 0xFF, 0x00), Color.pink, ChartColor.DARK_RED, ChartColor.DARK_BLUE, ChartColor.DARK_GREEN,
            ChartColor.DARK_MAGENTA, ChartColor.DARK_CYAN, Color.darkGray, ChartColor.VERY_DARK_RED,
            ChartColor.VERY_DARK_BLUE, ChartColor.VERY_DARK_GREEN, ChartColor.VERY_DARK_YELLOW, ChartColor.VERY_DARK_MAGENTA,
            ChartColor.VERY_DARK_CYAN};

    protected XYSeriesCollection dataset;
    protected JFreeChart chart;

    protected List<Double> times;
    protected List<double[]> values;
    protected ChartPanel chartPanel;
    protected boolean doRepeats = false;
    protected PlotInfo plotInfo = null;
    protected SimulationEngine.Var xVariable;
    protected Map<SimulationEngine.Var, List<Series>> variableIndeces;
    protected FunctionJobControl jobControl;
    protected SimulationEngine simulationEngine;
    protected java.awt.Dimension paneSize = new java.awt.Dimension(500, 270);
    protected boolean notify = false;
    protected int cyclesNumber;
    protected int spanIndex;
    
    public ResultPlotPane(SimulationEngine simulationEngine, FunctionJobControl jobControl)
    {
        this.simulationEngine = simulationEngine;
        this.jobControl = jobControl;
    }
    
    public ResultPlotPane(SimulationEngine simulationEngine, FunctionJobControl jobControl, PlotInfo plotInfo)
    {
        this( simulationEngine, jobControl);
        this.plotInfo = plotInfo;
    }


    public SimulationEngine getSimulationEngine()
    {
        return simulationEngine;
    }

    ///////////////////////////////////////////////////////////////////
    // Chart issues
    //
    protected void createChartPanel()
    {
        xVariable = simulationEngine.getXVariable(plotInfo);
        variableIndeces = getVariables();
        
        String yAxisLabel = variableIndeces.size() == 1? StreamEx.of(variableIndeces.keySet()).findFirst().map(v->v.title).get(): DEFAULT_Y_AXIS_LABEL;

        chart = ChartFactory.createXYLineChart(plotInfo != null? plotInfo.getTitle(): "Simulation result", xVariable.title, yAxisLabel, null, //dataset,
                PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );
        chart.setBackgroundPaint(Color.white);
        chart.getLegend().setBorder( 1, 1, 1, 1 );
        chartPanel = new ChartPanel(chart);

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

        plot.setDrawingSupplier(new DefaultDrawingSupplier(POSSIBLE_COLORS, DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE, DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));

        dataset = new XYSeriesCollection();

        initSeries( renderer, dataset );
        if( plotInfo.getExperiments() != null )
            addExperiments( plotInfo.getExperiments(), renderer, dataset, this.simulationEngine.getCompletionTime()  );
        
        chart.getXYPlot().getRangeAxis().setLabel( plotInfo.getYAxisInfo().getTitle() );
        chart.getXYPlot().getDomainAxis().setLabel( plotInfo.getXAxisInfo().getTitle() );
        //default options
        chart.getXYPlot().getRangeAxis().setLabelFont( new Font("Arial", Font.BOLD, 18 ) );
        chart.getXYPlot().getRangeAxis().setTickLabelFont( new Font("Arial", Font.PLAIN, 16 ) );
        chart.getXYPlot().getDomainAxis().setLabelFont( new Font("Arial", Font.BOLD, 18 ) );
        chart.getXYPlot().getDomainAxis().setTickLabelFont( new Font("Arial", Font.PLAIN, 16 ) );
        chart.getXYPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setRenderer(renderer);
        chart.getXYPlot().setDataset(dataset);
    }
    
    protected void initSeries(XYLineAndShapeRenderer renderer, XYSeriesCollection dataset)
    {
        int counter = 0;
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            SimulationEngine.Var var = e.getKey();
            List<Series> seriesList = e.getValue();

            if( seriesList != null )
            {
                if( seriesList.size() > 1 )
                    doRepeats = true;
                for( int i = 0; i< seriesList.size(); i++ )
                {
                    XYSeries series = (XYSeries)seriesList.get(i);
                    Pen spec = var.pen;
                    if( spec != null )
                    {
                        renderer.setSeriesPaint(counter, spec.getColor());
                        renderer.setSeriesStroke(counter, spec.getStroke());
                        renderer.setSeriesVisibleInLegend(counter, i == 0);
                    }
                    renderer.setSeriesShapesVisible(counter, false);
                    
                    counter++;
                    dataset.addSeries(series);
                }
            }
        }
    }
    
    protected Map<Var, List<Series>> getVariables()
    {
        return simulationEngine.getVariablesToPlot(plotInfo);
    }

    protected void createUI()
    {
        createChartPanel();

        // layout issues
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(this, gbc);
        setLayout(gbl);

        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(chartPanel, new GridBagConstraints(0, 0, 1, 5, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 2), 0, 0));

        if( jobControl != null )
        {
            JPanel buttonPanel = new JPanel(new BorderLayout());
            // stop button
            add(buttonPanel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(2, 2, 2, 2), 0, 0));

            JButton stopButton = new JButton("Stop");
            buttonPanel.add(stopButton, BorderLayout.EAST);
            buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            stopButton.addActionListener(ae -> {
                    simulationEngine.stopSimulation();
                    jobControl.terminate();
            });
        }

        JFrame frame = new JFrame(chartPanel.getChart().getTitle().getText());
        frame.setSize(700, 700);

        frame.getContentPane().setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);

        mainPanel.add(this, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);

        // finish computations correctly when user closes the window
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                simulationEngine.stopSimulation();
                if( jobControl != null )
                    jobControl.terminate();
            }
        });
    }


    ///////////////////////////////////////////////////////////////////
    // ResultListener interface implementation
    //
    @Override
    public void add(double t, double[] y)
    {
        if( cyclesNumber == 1 || doRepeats )
            addAsFirst(t, y);
        else
            update(t, y);
        spanIndex++;
    }

    @Override
    public void start(Object model)
    {
        cyclesNumber = 1;
        spanIndex = 0;
        values = new ArrayList<>();
        createUI();
    }

    @Override
    public final void startCycle()
    {
        cyclesNumber++;
        spanIndex = 0;
    }


    @Override
    public void addAsFirst(double t, double[] y)
    {
        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
            ((XYSeries)e.getValue().get(cyclesNumber - 1)).add(y[xVariable.index], y[e.getKey().index], true);

        values.add(spanIndex, y);
    }

    @Override
    public void update(double t, double[] y)
    {
        double[] newY = values.get(spanIndex);

        for( Entry<SimulationEngine.Var, List<Series>> e : variableIndeces.entrySet() )
        {
            int index = e.getKey().index;
            newY[index] += y[index];
            double time = ((XYSeries)e.getValue().get(0)).getX(spanIndex).doubleValue();
            ((XYSeries)e.getValue().get(0)).update(time, newY[index] / cyclesNumber);
        }
    }

    @Override
    public void finish()
    {
    }

    /**
     * Adds series from data collections (experimental nymbers to compare with simulated  
     */
    public static void addExperiments(Experiment[] experiments, XYLineAndShapeRenderer renderer, AbstractSeriesDataset dataset, double timeLimit)
    {
        int counter = dataset.getSeriesCount();
        for( Experiment experiment : experiments )
        {
            TableDataCollection tdc = experiment.getPath().getDataElement( TableDataCollection.class );
            String columnNameX = experiment.getNameX();
            String columnNameY = experiment.getNameY();
            String title = experiment.getTitle();
            double[] valuesY = TableDataCollectionUtils.getColumn( tdc, columnNameY );
            double[] valuesX = TableDataCollectionUtils.getColumn( tdc, columnNameX );

            Pen spec = experiment.getPen();
            if( spec != null )
            {
                renderer.setSeriesPaint( counter, spec.getColor() );
                renderer.setSeriesStroke( counter, spec.getStroke() );
                renderer.setSeriesVisibleInLegend( counter, true );
            }
            renderer.setSeriesShapesVisible( counter, true );
            renderer.setSeriesLinesVisible( counter, false );
            renderer.setSeriesShape( counter, new Ellipse2D.Float( -3, -3, 6, 6 ) );

            counter++;

            if( dataset instanceof YIntervalSeriesCollection )
            {
                YIntervalSeries series = new YIntervalSeries( title, false, true );
                for( int i = 0; i < valuesX.length && valuesX[i] <= timeLimit; i++ )
                    series.add( valuesX[i], valuesY[i], 0, 0 );

                ( (YIntervalSeriesCollection)dataset ).addSeries( series );
            }
            else if( dataset instanceof XYSeriesCollection )
            {
                XYSeries series = new XYSeries( title, false, true );
                for( int i = 0; i < valuesX.length && valuesX[i] <= timeLimit; i++ )
                    series.add( valuesX[i], valuesY[i] );

                ( (XYSeriesCollection)dataset ).addSeries( series );
            }
        }
    }
}
