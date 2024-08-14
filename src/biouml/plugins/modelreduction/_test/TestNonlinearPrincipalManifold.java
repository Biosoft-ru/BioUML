package biouml.plugins.modelreduction._test;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.workbench.Framework;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.modelreduction.NonlinearPrincipalManifold;
import biouml.plugins.modelreduction.NonlinearPrincipalManifold.StrategyParameters;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.type.Base;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.RNA;

public class TestNonlinearPrincipalManifold extends TestCase implements ResultListener
{
    private final JavaSimulationEngine engine = new JavaSimulationEngine();

    private final List<double[]> dataPoints = new ArrayList<>();

    private double tStart;
    private int step = 0;
    private Map<String, Integer> varIndexMapping = new HashMap<>();

    private int speciesNumber = 0;
    private final int dimension = 3;

    public void getDataPoints(SimulationEngine engine) throws Exception
    {
        JavaBaseModel model = (JavaBaseModel)engine.createModel();
        engine.simulate(model, new ResultListener[] {this});
        //        getMetabolitNames(engine.getDiagram());
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {

        //        EModel emodel = engine.getExecutableModel();
        if( t > tStart )
        {
            double[] dataPoint = new double[speciesNumber];
            for( Map.Entry<String, Integer> entry : varIndexMapping.entrySet() )
            {
                int indexOf = metabolitNames.indexOf(entry.getKey());
                if( indexOf != -1 )
                    dataPoint[indexOf] = y[entry.getValue()];
            }
            dataPoints.add(dataPoint);
            System.out.print("data point ");
        }
        //        log.info(step++);
        System.out.println(step++);
    }

    @Override
    public void start(Object model)
    {
        varIndexMapping = engine.getVarIndexMapping();
    }

    List<String> metabolitNames = new ArrayList<>();
    private void getMetabolitNames(Compartment compartment)
    {
        for( Node node : compartment.recursiveStream().select( Node.class ) )
        {
            Base kernel = node.getKernel();
            if( ( kernel instanceof Protein ) || ( kernel instanceof RNA ) || ( kernel instanceof Gene ) )
            {
                String proteinName = node.getCompleteNameInDiagram();
                metabolitNames.add("$\"" + proteinName + "\"");
                speciesNumber++;
            }
        }
    }

    public void test1()
    {
        try
        {
            final String repositoryPath = "../data";
            Framework.initRepository(repositoryPath);
            Diagram d = DataElementPath.create("databases/Biopath/Diagrams/DGR0401L_Steady").getDataElement(Diagram.class);

            engine.setOutputDir("../out");
            engine.setDiagram(d);

            tStart = ( engine.getCompletionTime() + engine.getInitialTime() ) / 2;
            engine.setTimeIncrement( ( engine.getCompletionTime() - engine.getInitialTime() ) / 40);

            NonlinearPrincipalManifold npm = new NonlinearPrincipalManifold();

            getMetabolitNames(d);
            getDataPoints(engine);

            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter( new FileOutputStream( new File( "../out/dataPoints.txt" ) ), StandardCharsets.UTF_8 ) ))
            {
                for( int i = 0; i < dataPoints.size(); i++ )
                {
                    for( int j = 0; j < speciesNumber; j++ )
                    {
                        double val = dataPoints.get( i )[j];
                        bw.write( Double.toString( val ) + "   " );
                    }
                    bw.write( '\n' );
                }
            }

            double[] weights = new double[dataPoints.size()];
            for( int i = 0; i < weights.length; ++i )
            {
                weights[i] = 1;
            }

            npm.constructGridApproximation(dimension, dataPoints, weights, new StrategyParameters());
        }
        catch( Throwable t )
        {
            System.out.println("abra");
        }
    }

}
