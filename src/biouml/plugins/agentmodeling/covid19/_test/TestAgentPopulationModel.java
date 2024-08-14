package biouml.plugins.agentmodeling.covid19._test;

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
import biouml.plugins.agentmodeling.Classification;
import biouml.plugins.agentmodeling._test.AgentTestingUtils;
import biouml.plugins.agentmodeling.covid19.AgentPopulationModel;
import biouml.plugins.agentmodeling.covid19.HealthCare;
import biouml.plugins.agentmodeling.covid19.Scenario;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAgentPopulationModel extends AbstractBioUMLTest
{
    public TestAgentPopulationModel(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAgentPopulationModel.class);
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
        //TODO: rework scenario to be similar to "events"
//        Scenario scenario = new Scenario();
//        scenario.times = new double[] {17, 25, 41, 59};
//        scenario.mobilityLimit = new double[] {0.05, 0.05, 0.05, 0.05};
//        scenario.testMode = new double[] {1, 1, 1, 1};
//        scenario.testLimits = new double[] {10, 10, 55, 65} ;
//        scenario.newBeds = new double[] {0,0,0,0};
//        scenario.newICU = new double[] {0,0,0,0};
//        scenario.limit_mass_gathering = new double[] {10,10,10,10};
//        
//        double p_immune = 0.0;
//                
//        TableDataCollection statistics = getTableDataCollection("data/Collaboration/Ilya/Data/Diagrams/covid19/", "Population Data");
//        AgentPopulationModel population = new AgentPopulationModel( 111, statistics, p_immune, 1700000 );
//        population.getPrototypeAgent().avgContacts = 8; //average number of contacts
//        population.getPrototypeAgent().p_infect = 0.19; //probability to infect upon contact
//        
//        population.setInitialInfected( 1 );
//        population.setCompletionTime( 65 );
//        HealthCare healthCare = population.addHealthCare();
//        healthCare.setScenario( scenario );
//        
//        //initial data for health care
//        healthCare.available_beds = 1500;
//        healthCare.available_ICU = 250;
//        healthCare.mobilityLimit = 0.9;
//        healthCare.limit_mass_gathering = 100.0;
//        healthCare.testingMode = HealthCare.TEST_ALL_WITH_SYMPTOMS;
//        healthCare.contactLevels = 1;
//        
//        Classification classification;
//        
////        classification = population.createClassification("Seek_Testing", 3);
////        classification.setTitle(1.0, "Seek testing");
////        classification.setTitle(0.0, "Does not seek");
////        classification = population.createClassification("age", 8);
////        classification.setTitle(10.0, "10");
////        classification.setTitle(20.0, "20");
////        classification.setTitle(30.0, "30");
////        classification.setTitle(40.0, "40");
////        classification.setTitle(50.0, "50");
////        classification.setTitle(60.0, "60");
////        classification.setTitle(70.0, "70");
////        classification.setTitle(80.0, "80");
////        classification.setTitle(90.0, "90");
////        classification.setTitle(100.0, "100");
//        
//        classification = population.createClassification("Status", 0);
//        classification.setTitle(1.0, "Infected");
//        classification.setTitle(2.0, "Ill");
//        classification.setTitle(3.0, "Recovered");
//        classification.setTitle(4.0, "Hospitalized");
//        classification.setTitle(5.0, "On ICU");
//        classification.setTitle(6.0, "Dead");
//        classification.setTitle(7.0, "Detected recovered");
////        
//        classification = population.createClassification("Symptoms", 1);
////        classification.setTitle(-1.0, "Incubation");
//        classification.setTitle(0.0, "Asymptomatic");
//        classification.setTitle(1.0, "Mild");
//        classification.setTitle(2.0, "Severe");
//        classification.setTitle(3.0, "Critical");             
//        
//        String healthCareName = healthCare.getName();
////        population.addPlot( healthCareName, "Today registered", new String[] {"registered today", "dead registered today", "recovered registered today"});
////        population.addPlot( healthCareName, "Today real", new String[] {"recovered today", "dead today"});
////        population.addPlot( healthCareName, "Total registered", new String[] {"registered", "dead registered", "recovered registered"} );
//        population.addPlot( healthCareName, "Total real", new String[] {"dead", "recovered" } );
////        population.addPlot( healthCareName, "Clinical supplies", new String[]{"available beds", "available ICU"} );
//        population.addPlot( healthCareName, "Pandemic properties", new String[] {"Rt", "Rt Registered"} );
//        
////       names from HealthCare
////        "tests today", "tests total", "registered today", "registered", "Rt", "Rt Registered", "inQeue", , "dead",
////                "dead today", "recovered", "recovered today", "dead registered", "dead registered today", "recovered registered", "recovered registered today"    
//
//        double start = System.nanoTime();
//        population.simulate();
//       System.out.println( "Simulation ended "+ (System.nanoTime() - start)/1E9 );
//        
//        TableDataCollection experiment = this.getTableDataCollection( "data/Collaboration/Ilya/Data/Diagrams/covid19/June 1", "Data_NSO_29_05_2020_all" );
//        
//       double[] experiment_Detected = TableDataCollectionUtils.getColumn( experiment, "Registered_total_NSO" );
//       double[] experiment_Detected_Today = TableDataCollectionUtils.getColumn( experiment, "Registered_day_NSO" );
//       
//       double[] experiment_Dead = TableDataCollectionUtils.getColumn( experiment, "Dead_total_NSO" );
//       double[] experiment_Dead_Today = TableDataCollectionUtils.getColumn( experiment, "Dead_day_NSO" );
//       
//       double[] experiment_Recovered = TableDataCollectionUtils.getColumn( experiment, "Recovered_total_NSO" );
//       double[] experiment_Recovered_Today = TableDataCollectionUtils.getColumn( experiment, "Recovered_day_NSO" );
//       
////       double[] experiment_Recovered = TableDataCollectionUtils.getColumn( experiment, "Recovered_total_NSO" );
////       double[] detected =  population.getDynamicDouble( "Detected" );
////       double[] detected_Recovered =  population.getDynamicDouble( "Detected recovered" );
//       
//       double[] time = population.getTimes();
//       Map<String, double[]> simulatedData = new HashMap<>();
//       simulatedData.put( "Зарегестрировано, симуляция", population.getGlobalDynamic( healthCareName, "registered" ) );
////       simulatedData.put( "Умерло, симуляция", population.getGlobalDynamic( healthCareName, "dead registered" ) );
////       simulatedData.put( "Выздоровело, симуляция", population.getGlobalDynamic( healthCareName, "recovered registered" ) );
//       Map<String, double[]> experimentData = new HashMap<>();
//       experimentData.put( "Зарегестрировано" , experiment_Detected );
////       experimentData.put( "Умерло" , experiment_Dead );
////       experimentData.put( "Выздоровело" , experiment_Recovered );
//              
//       generatePlot("Comparison with experiment", time, simulatedData, experimentData);
//
//        Thread.sleep( 700000 );
    }    
    
    public void generatePlot(String name, double[] time, Map<String, double[]> simulated, Map<String, double[]> experiment)
    {
        JFrame frame = new JFrame(name);
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
            for (int i=0; i<simEntry.getValue().length && i< time.length; i++)
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