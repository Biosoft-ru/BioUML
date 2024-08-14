package biouml.plugins.agentmodeling.covid19._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
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
import biouml.plugins.agentmodeling.Classification;
import biouml.plugins.agentmodeling._test.AgentTestingUtils;
import biouml.plugins.agentmodeling.covid19.AgentPopulationSimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.StochasticSimulationResult;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAgentModel extends AbstractBioUMLTest
{
    public TestAgentModel(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestAgentModel.class );
        return suite;
    }
    private String collectionPath = "data/Collaboration/MyProj/Data/Diagrams/";
    private String diagramName = "Agent Novosibirsk";
    private String dataName = "Novosibirsk";

    //completion time for simulation
    private int completionTime = 500;

    /**
     * Main methods for test
     * @throws Exception
     */
    public void test() throws Exception
    {
    	CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository("../data_resources");
//        CollectionFactory.createRepository(repositoryPath);
        
    	 DataCollection collection1 = CollectionFactory.getDataCollection(collectionPath);
         DataElement de = collection1.get(diagramName);
         Diagram diagram = (Diagram)de;
//        Diagram diagram = getDiagram( collectionPath, diagramName ); //get diagram from repository
        AgentPopulationSimulationEngine engine = (AgentPopulationSimulationEngine)DiagramUtility.getPreferredEngine( diagram ); //get simulation engine  
        engine.setDiagram( diagram ); //set diagram into engine
        engine.setCompletionTime( completionTime ); //sets final time point for simulation
        StochasticSimulationResult result = (StochasticSimulationResult)engine.generateSimulationResult(); //stores simulation result    

        this.addClassifications( engine );//additional classifications to output
        engine.simulate( result ); //run simulation

        TableDataCollection data = getTableDataCollection( collectionPath, dataName ); //real data to compare with simulation result
        double[] registeredSimulated = result.getValues( "Agent Novosibirsk Core/registered" ); //total number of registered cases in simulation
        double[] registeredReal = TableDataCollectionUtils.getColumn( data, "total_cases" ); //total number of registered cases in data
        double[] time = result.getTimes(); // simulated time points (days)

        //create data for plot
        Map<String, double[]> simulated = new HashMap<>();
        simulated.put( "Registered simulated", registeredSimulated );
        Map<String, double[]> real = new HashMap<>();
        real.put( "Registered real", registeredReal );
        generatePlot( "Comparison with experiment", time, simulated, real ); //creates plot to display
        Thread.sleep( 700000 ); //time delay so test will not close plot immediately
    }

    /**
     * Adds additional classifications of simulated population
     * @param engine
     */
    private void addClassifications(AgentPopulationSimulationEngine engine)
    {
        //variant classification  	
    	 Classification variantClassification = engine.createClassification( "variant", 10 );
    	 variantClassification.setTitle( 0.0, "0" );
    	 variantClassification.setTitle( 1.0, "1" );
         
        //age groups classification
        Classification ageClassification = engine.createClassification( "age", 8 );
        ageClassification.setTitle( 10.0, "10" );
        ageClassification.setTitle( 20.0, "20" );
        ageClassification.setTitle( 30.0, "30" );
        ageClassification.setTitle( 40.0, "40" );
        ageClassification.setTitle( 50.0, "50" );
        ageClassification.setTitle( 60.0, "60" );
        ageClassification.setTitle( 70.0, "70" );
        ageClassification.setTitle( 80.0, "80" );
        ageClassification.setTitle( 90.0, "90" );
        ageClassification.setTitle( 100.0, "100" );

        // agent status classification
        Classification statusClassification = engine.createClassification( "Status", 0 );
        statusClassification.setTitle( 1.0, "Infected" );
        statusClassification.setTitle( 2.0, "Ill" );
        statusClassification.setTitle( 3.0, "Recovered" );
        statusClassification.setTitle( 4.0, "Hospitalized" );
        statusClassification.setTitle( 5.0, "On ICU" );
        statusClassification.setTitle( 6.0, "Dead" );
        statusClassification.setTitle( 7.0, "Detected recovered" );

        // agent symptoms classification
        Classification symptomClassification = engine.createClassification( "Symptoms", 1 );
//        symptomClassification.setTitle( -1.0, "Incubation" );
        symptomClassification.setTitle( 0.0, "Asymptomatic" );
        symptomClassification.setTitle( 1.0, "Mild" );
        symptomClassification.setTitle( 2.0, "Severe" );
        symptomClassification.setTitle( 3.0, "Critical" );
        
        Classification reinfectedClassification = engine.createClassification( "reinfected", 9 );
        reinfectedClassification.setTitle(1.0, "Reinfected");
    }

    /**
     * Displays plot
     */
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
    
    public Diagram getDiagram(String collectionName, String diagramName) throws Exception
    {
        return AgentTestingUtils.loadDiagram( diagramName, collectionName );
    }

    public TableDataCollection getTableDataCollection(String collectionName, String tableName) throws Exception
    {
        return AgentTestingUtils.loadTable( tableName, collectionName );
    }

}