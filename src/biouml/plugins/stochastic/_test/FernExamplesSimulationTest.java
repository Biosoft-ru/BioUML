package biouml.plugins.stochastic._test;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.stochastic.StochasticModel;
import biouml.plugins.stochastic.StochasticSimulationEngine;
import biouml.plugins.stochastic.StochasticSimulator;
import biouml.plugins.stochastic.solvers.GibsonBruckSolver;
import biouml.standard.simulation.ResultListener;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FernExamplesSimulationTest extends TestCase implements ResultListener
{
    double[][] recentData;
    double[][] data;
    //    double[] times;
    Map<String, Integer> variables;
    int repeationsTime = 1;
    Span span = new ArraySpan(0, 400, 1);
    int index = 0;
    int varNumber;
    int spanSize;

    private String[] fileList = {"mapk_sbml.xml"};

    static String filePath = "C:/projects/Java/workspace/BioUML/out/biouml/plugins/stochastic/_test/examples";

    public FernExamplesSimulationTest(String name)
    {
        super(name);
    }

    public static void main(String ... args)
    {

    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(FernExamplesSimulationTest.class.getName());

        suite.addTest(new FernExamplesSimulationTest("test"));

        return suite;
    }

    public void test()
    {
        try
        {
            //            test(new GillespieSolver());
            //            test(new GillespieEfficientSolver());
            test(new GibsonBruckSolver());
            //            test(new TauLeapingSolver());
            //            testFernExamples(new MaxTSSolver());
            //            testFernExamples(new MaxTSSolverUsingGillespie());
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }



    public void test(StochasticSimulator simulator) throws Exception
    {
        File examplesDirectory = new File(filePath);

        assertNotNull("Can not find exaples directory", examplesDirectory);
        if( examplesDirectory.isDirectory() )
        {
            for( String fileName : fileList )
            {
                File file = new File(filePath, fileName);
                if( !file.exists() )
                {
                    System.out.println("Can not find file " + fileName);
                    continue;
                    
                }
                Diagram diagram = SbmlModelFactory.readDiagram(file, null, file.getName());
                assertNotNull("Can not read diagram " + file.getName(), diagram);
                testDiagram(diagram, simulator);
                outputResult("C:/StochasticResults/Solver_" + simulator.getInfo().name + "_Diagram_" + diagram.getName() + ".txt");
            }
        }
        System.out.println("Testing of " + simulator.getInfo().name + " finished");
        System.out.println();
    }

    public void outputResult(String fileName) throws IOException
    {
        try(BufferedWriter bw1 = ApplicationUtils.utfWriter( fileName ))
        {
            for( int i = 0; i < variables.size(); i++ )
            {
                //            bw1.write(formatDouble(times[i], 2) + "     ");
                for( int j = 1; j < data[i].length - 2; j++ )
                {
                    bw1.write(formatDouble(data[i][j], 5) + "\t");
                }
                bw1.write("\n");
            }
            bw1.write(variables.toString());
        }
    }

    protected void testDiagram(Diagram diagram, StochasticSimulator simulator) throws Exception
    {
        StochasticSimulationEngine engine = new StochasticSimulationEngine();
        engine.setDiagram(diagram);
        engine.setOutputDir("./out");

        StochasticModel model = (StochasticModel)engine.createModel();

        assertNotNull("Can not load model class", model);

        EModel emodel = ( diagram.getRole( EModel.class ) );
        varNumber = emodel.getVariables().getSize();

        variables = new HashMap<>();
        Iterator<Variable> iter = emodel.getVariables().iterator();
        while( iter.hasNext() )
        {
            Variable variable = iter.next();
            variables.put(variable.getName(), engine.getVarIndex(variable.getName()));
        }



        spanSize = span.getLength();
        System.out.println("");
        System.out.println("Simulating model " + diagram.getName() + " with solver " + simulator.getInfo().name);

        engine = initEngine(diagram, simulator, span);

        double elapsedTime = 0;
        for( int i = 0; i < repeationsTime; i++ )
        {
            double time = System.currentTimeMillis();
            this.start(emodel);
            simulator.start(model, span, new ResultListener[] {this}, null);
            //            engine.simulate(model, new ResultListener[] {this});
            elapsedTime += ( System.currentTimeMillis() - time );

            if( i == 0 )
                data = recentData.clone();
            else
                addMatrix(data, recentData);
        }
        System.out.println("Total time: " + elapsedTime + "ms, Average time: " + elapsedTime / repeationsTime + " ms");
        divideByScalar(data, repeationsTime);
        
        showgraphics("");
        System.out.println("done");
    }
    
    public StochasticSimulationEngine initEngine(Diagram diagram, Simulator simulator, Span span) throws Exception
    {
        StochasticSimulationEngine engine = new StochasticSimulationEngine();
        engine.setDiagram(diagram);
        engine.setSpan(span);
        engine.setSolver(simulator);
        return engine;
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        for( int i = 0; i < varNumber; i++ )
        {
            {
                try
                {
                    recentData[i][index] = y[i];
                    //                    times[index] = t;
                }
                catch( Exception ex )
                {
                }
            }
        }
        index++;
    }

    @Override
    public void start(Object model)
    {
        index = 0;
        //        times = new double[spanSize];
        recentData = new double[varNumber][spanSize];

    }

    public void addMatrix(double[][] target, double[][] arr)
    {
        for( int i = 0; i < target.length; i++ )
        {
            for( int j = 0; j < target[i].length; j++ )
            {
                target[i][j] += arr[i][j];
            }
        }
    }

    public void divideByScalar(double[][] matrix, double scalar)
    {
        for( int i = 0; i < matrix.length; i++ )
        {
            for( int j = 0; j < matrix[i].length; j++ )
            {
                matrix[i][j] /= scalar;
            }
        }
    }

    String formatDouble(double d, int dz)
    {
        double dd = Math.pow(10, dz);
        String str = String.valueOf(Math.round(d * dd) / dd);
        return str.replaceAll("\\.", ",");
    }



    public void showgraphics(String name)
    {
        XYSeriesCollection collection = new XYSeriesCollection();

        for( int i = 0; i < varNumber; i++ )
        {
            XYSeries series = new XYSeries("v" + i);

            for( int j = 0; j < spanSize; j++ )
            {
                series.add(j, data[i][j]);
            }
            collection.addSeries(series);
        }

        JFrame frame = new JFrame("");
        Container content = frame.getContentPane();


        JFreeChart chart = ChartFactory.createXYLineChart(name, "Time", "", collection, PlotOrientation.VERTICAL, true, // legend
                true, // tool tips
                false // URLs
                );

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
