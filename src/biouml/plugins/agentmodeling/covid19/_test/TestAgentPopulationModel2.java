package biouml.plugins.agentmodeling.covid19._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.table.TableDataCollection;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.Stepper;
import biouml.plugins.agentmodeling._test.AgentTestingUtils;
import biouml.plugins.agentmodeling.covid19.AgentPopulationModel;
import biouml.plugins.agentmodeling.covid19.HealthCare;
import biouml.plugins.agentmodeling.covid19.Scenario;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAgentPopulationModel2 extends AbstractBioUMLTest
{
    public TestAgentPopulationModel2(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestAgentPopulationModel.class );
        return suite;
    }

    public Diagram getDiagram(String collectionName, String diagramName) throws Exception
    {
        return AgentTestingUtils.loadDiagram( diagramName, collectionName );
    }

    public TableDataCollection getTableDataCollection(String collectionName, String tableName) throws Exception
    {
        return AgentTestingUtils.loadTable( tableName, collectionName );
    }

    public void test() throws Exception
    {
        //TODO: rework scenario to be similar to "events"
        Scenario scenario = new Scenario();
        scenario.times = new double[] {17, 25, 41, 59};
        scenario.mobilityLimit = new double[] {0.05, 0.05, 0.05, 0.05};
        scenario.testMode = new double[] {1, 1, 1, 1};
        scenario.testLimits = new double[] {10, 10, 55, 65};
        scenario.newBeds = new double[] {0, 0, 0, 0};
        scenario.newICU = new double[] {0, 0, 0, 0};
        scenario.limit_mass_gathering = new double[] {10, 10, 10, 10};

        //parameters
        long seed = 111;
        double size = 1700000;
        double p_immune = 0.0;
        double available_beds = 1500;
        double available_ICU = 250;
        double mobilityLimit = 0.9;
        double limit_mass_gathering = 100.0;
        double testingMode = HealthCare.TEST_ALL_WITH_SYMPTOMS;
        double contactLevels = 1;
        double avgContacts = 8;
        double pInfect = 0.19;
        double initial = 1;

        TableDataCollection statistics = getTableDataCollection( "data/Collaboration/Ilya/Data/Diagrams/covid19/", "Population Data" );
//        AgentPopulationModel population = new AgentPopulationModel( size, avgContacts, pInfect, initial, available_beds, available_ICU,
//                mobilityLimit, limit_mass_gathering, testingMode, contactLevels, p_immune );
//
//        population.setSeed( seed );
//        population.setPopulationStatistics( statistics );
//        population.setScenario( scenario );
        
        Span span = new UniformSpan( 0, 65, 1 );

        double start = System.nanoTime();
        Stepper stepper = new Stepper();
//        stepper.start( population, span, null, null );

        System.out.println( "Simulation ended " + ( System.nanoTime() - start ) / 1E9 );

        Thread.sleep( 700000 );
    }

    public void generatePlot(String name, double[] time, Map<String, double[]> simulated, Map<String, double[]> experiment)
    {
        JFrame frame = new JFrame( name );
        Container content = frame.getContentPane();
        XYSeriesCollection collection = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        int index = 0;

        for( Entry<String, double[]> simEntry : simulated.entrySet() )
        {
            XYSeries series = new XYSeries( simEntry.getKey() );
            collection.addSeries( series );
            renderer.setSeriesShapesVisible( index, true );
            index++;

            for( int i = 0; i < simEntry.getValue().length; i++ )
                series.add( time[i], simEntry.getValue()[i] );
        }

        for( Entry<String, double[]> simEntry : experiment.entrySet() )
        {
            XYSeries series = new XYSeries( simEntry.getKey() );
            collection.addSeries( series );

            renderer.setSeriesLinesVisible( index, false );
            //            renderer.setSeriesShape(index, new Ellipse2D.Float(0, 0, 5, 5));
            renderer.setSeriesShapesVisible( index, true );
            index++;
            for( int i = 0; i < simEntry.getValue().length && i < time.length; i++ )
                series.add( time[i], simEntry.getValue()[i] );
        }

        JFreeChart chart = ChartFactory.createXYLineChart( "", "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );

        chart.getXYPlot().setDomainGridlinePaint( Color.black );
        chart.getXYPlot().setRangeGridlinePaint( Color.black );

        chart.getXYPlot().setRenderer( renderer );
        chart.getXYPlot().setBackgroundPaint( Color.white );
        chart.setBackgroundPaint( Color.white );
        content.add( new ChartPanel( chart ) );
        frame.setSize( 800, 600 );
        frame.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                System.exit( 0 );
            }
        } );
        frame.setVisible( true );
    }
}