package biouml.plugins.agentmodeling.covid19.old._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
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
import biouml.model.dynamics.EModel;
import biouml.plugins.agentmodeling.Classification;
import biouml.plugins.agentmodeling._test.AgentTestingUtils;
import biouml.plugins.agentmodeling.covid19.old.AgentPopulationModelOld;
import biouml.plugins.agentmodeling.covid19.old.HealthCare;
import biouml.plugins.agentmodeling.covid19.old.Scenario;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAgentPopulationModelOld extends AbstractBioUMLTest
{
    public TestAgentPopulationModelOld(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAgentPopulationModelOld.class);
        return suite;
    }
    
    public Diagram getDiagram(String collectionName, String diagramName) throws Exception
    {
        return AgentTestingUtils.loadDiagram(diagramName, collectionName);
    }
    
    public TableDataCollection getTableDataCollection(String collectionName, String tableName) throws Exception
    {
        return AgentTestingUtils.loadTable( tableName, collectionName);
    }
        
    public void test() throws Exception
    {
        Diagram agentDiagram = getDiagram( "data/Collaboration/Ilya/Data/Diagrams/covid19/Agent-based model/", "InfectiousAgent" );
        
        //set parameters
        agentDiagram.getRole(EModel.class).getVariable( "avgContacts" ).setInitialValue( 12 );
        agentDiagram.getRole(EModel.class).getVariable( "p_infect" ).setInitialValue( 0.28);
        
        Scenario scenario = new Scenario();
        scenario.times = new double[] {17, 25, 41, 59};
        scenario.mobilityLimit = new double[] {0.5, 0.5, 0.5, 0.5};
        scenario.testMode = new double[] {1, 2, 2, 2};
        scenario.testLimits = new double[] {10, 10, 55, 65} ;
        scenario.newBeds = new double[] {0,0,0,0};
        scenario.newICU = new double[] {0,0,0,0};
        scenario.limit_mass_gathering = new double[] {10,10,10,10};
        
        TableDataCollection statistics = getTableDataCollection("data/Collaboration/Ilya/Data/Diagrams/covid19/", "Population Data");
        AgentPopulationModelOld population = new AgentPopulationModelOld( agentDiagram, statistics, 0, false, 2700000 );
        population.setCompletionTime( 65 );
        HealthCare healthCare = population.addHealthCare();
//        healthCare.contactLevels = 1;
        healthCare.setScenario( scenario );
        population.addSharedVariable( healthCare, "available_beds", 2600 );
        population.addSharedVariable( healthCare, "available_ICU", 500 );
        population.addSharedVariable( healthCare, "mobility_limit", 1.0 );
        population.addSharedVariable( healthCare, "limit_mass_gathering", 100.0 );
        population.addVariable( healthCare, "testingMode", 1.0 );
        
//        Classification classification = population.createClassification("Seek_Testing");
//        classification.setTitle(1.0, "Seek testing");        population.addVariable( healthCare, "testingMode", 0 );
//        classification.setTitle(0.0, "Does not seek");        population.addVariable( healthCare, "testingMode", 0 );
        
        Classification classification = population.createClassification("Status");
        classification.setTitle(1.0, "Infected");
        classification.setTitle(2.0, "Ill");
        classification.setTitle(3.0, "Recovered");
        classification.setTitle(4.0, "Hospitalized");
        classification.setTitle(5.0, "On ICU");
        classification.setTitle(6.0, "Dead");
        classification.setTitle(7.0, "Detected recovered");
        
        classification = population.createClassification("Symptoms");
        classification.setTitle(-1.0, "Incubation");
        classification.setTitle(0.0, "Asymptomatic");
        classification.setTitle(1.0, "Mild");
        classification.setTitle(2.0, "Severe");
        classification.setTitle(3.0, "Critical");
        
        classification = population.createClassification("Detected");
        classification.setTitle(1.0, "Detected");

        double start = System.nanoTime();
        population.simulate(10);
        System.out.println( "Simulation ended "+ (System.nanoTime() - start)/1E9 );
        
        TableDataCollection experiment = this.getTableDataCollection( "data/Collaboration/Covid/Data/Files/", "Data_NSO_final_14_05" );
       double[] experiment_Detected = TableDataCollectionUtils.getColumn( experiment, "Registered_total_NSO" );
//       double[] experiment_Recovered = TableDataCollectionUtils.getColumn( experiment, "Recovered_total_NSO" );
//       double[] detected =  population.getDynamicDouble( "Detected" );
//       double[] detected_Recovered =  population.getDynamicDouble( "Detected recovered" );
       
       double[] time = population.getTimes();
       Map<String, double[]> simulatedData = new HashMap<>();
       simulatedData.put( "Зарегестрировано, симуляция", population.getDynamicDouble( "Detected" ) );
       Map<String, double[]> experimentData = new HashMap<>();
       experimentData.put( "Зарегестрировано" , experiment_Detected );
       
       generatePlot(time, simulatedData, experimentData);
       
//       System.out.println( DoubleStreamEx.of( detected ).joining( "," ) );
       
//        population.getDynamicDouble( value )
//        System.out.println( "" );
        Thread.sleep( 700000 );
    }
//    
//    public void testDiscrete() throws Exception
//    {
//        Diagram agentDiagram = getDiagram( "data/Collaboration/Ilya/Data/Diagrams/covid19/Agent-based model/", "InfectiousAgent_discrete" );
//        AgentPopulation3 population = new AgentPopulation3( agentDiagram, true, 1000, 1 );
//        HealthCare healthCare = population.addHealthCare();
//        population.addVariable( healthCare, "available_beds", 10 );
//        population.addVariable( healthCare, "available_ICU", 10 );
//        
//        Classification classification = population.createClassification("Symptoms");
//        classification.setTitle(1.0, "Infected");
//        classification.setTitle(2.0, "Ill");
//        classification.setTitle(3.0, "Recovered");
//        classification.setTitle(4.0, "Hospitalized");
//        classification.setTitle(5.0, "On ICU");
//        classification.setTitle(6.0, "Dead");
//
//        classification = population.createClassification("Symptoms");
//        classification.setTitle(-1.0, "Incubation");
//        classification.setTitle(0.0, "Asymptomatic");
//        classification.setTitle(1.0, "Mild");
//        classification.setTitle(2.0, "Severe");
//        classification.setTitle(3.0, "Critical");
//
//        classification = population.createClassification("Detected");
//        classification.setTitle(1.0, "Detected");
//        
//        population.simulate();
//    }
    
    
    public void generatePlot(double[] time, Map<String, double[]> simulated, Map<String, double[]> experiment)
    {
        JFrame frame = new JFrame("");
        Container content = frame.getContentPane();
        XYSeriesCollection collection = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        int index=0;
        
        for( Entry<String, double[]> simEntry: simulated.entrySet() )
        {
            XYSeries series = new XYSeries( simEntry.getKey() );
            collection.addSeries( series );
            renderer.setSeriesShapesVisible( index, true );
            index++;
            
            for (int i=0; i<simEntry.getValue().length; i++)
                series.add( time[i], simEntry.getValue()[i] );
        }
        
        for( Entry<String, double[]> simEntry: experiment.entrySet() )
        {
            XYSeries series = new XYSeries( simEntry.getKey() );
            collection.addSeries( series );
            
            renderer.setSeriesLinesVisible(index, false);
//            renderer.setSeriesShape(index, new Ellipse2D.Float(0, 0, 5, 5));
            renderer.setSeriesShapesVisible( index, true );
            index++;
            for (int i=0; i<simEntry.getValue().length; i++)
                series.add( time[i], simEntry.getValue()[i] );
        }

        JFreeChart chart = ChartFactory.createXYLineChart( "", "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
        );

        chart.getXYPlot().setDomainGridlinePaint(Color.black);
        chart.getXYPlot().setRangeGridlinePaint(Color.black);
        
        chart.getXYPlot().setRenderer(renderer);
        chart.getXYPlot().setBackgroundPaint(Color.white);
        chart.setBackgroundPaint(Color.white);
        content.add(new ChartPanel(chart));
        frame.setSize(800, 600);
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
    
   
}