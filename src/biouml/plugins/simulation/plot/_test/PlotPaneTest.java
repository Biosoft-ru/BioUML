package biouml.plugins.simulation.plot._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import biouml.model.Module;
import biouml.model._test.ViewTestCase;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.plot.PlotDialog;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.access.SimulationResultSqlTransformer;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.sql.SqlConnectionPool;

public class PlotPaneTest extends ViewTestCase
{
    public PlotPaneTest(String name)
    {
        super(name);
        File configFile = new File( "./biouml/plugins/simulation/plot/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    private DataCollection<SimulationResult> initSqlDataCollection() throws Exception
    {
        Properties props = new Properties();
        props.put(DataCollectionConfigConstants.NAME_PROPERTY, "Simulatiob results (test)");
        props.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, SimulationResult.class.getName());
        props.put(SqlDataCollection.SQL_TRANSFORMER_CLASS, SimulationResultSqlTransformer.class.getName());
        props.put(SqlDataCollection.JDBC_URL_PROPERTY, "cyclonet");

        return new SqlDataCollection<>( null, props );
    }

    private SimulationResult createSimulationResult(DataCollection<SimulationResult> resultDC)
    {
        SimulationResult result = new SimulationResult( resultDC, "result" );
        double[] times = new double[] {1, 2, 3};
        double[][] values = new double[][] {{10, 20}, {30, 40}, {50, 60}};

        Map<String, Integer> var_map = new HashMap<>();
        var_map.put( "var0", 0 );
        var_map.put( "var1", 1 );

        Variable v0 = new Variable( "v0", null, null );
        v0.setInitialValue( 111 );
        v0.setUnits( "unit0" );

        Variable v1 = new Variable( "v1", null, null );
        v1.setInitialValue( 222 );
        v1.setUnits( "unit1" );

        Variable v2 = new Variable( "v2", null, null );
        v2.setInitialValue( 333 );
        v2.setUnits( "unit2" );

        result.addInitialValue( v0 );
        result.addInitialValue( v1 );
        result.addInitialValue( v2 );

        result.setDiagramName( "diagram_name" );
        result.setTitle( "title" );
        result.setInitialTime( 1 );
        result.setCompletionTime( 2 );

        result.setTimes( times );
        result.setValues( values );
        result.setVariableMap( var_map );

        return result;
    }

    private Plot createPlot()
    {
        ArrayList<Series> series = new ArrayList<>();

        Series s = new Series("s0");

        s.setPlotName("plot0");
//        s.setSpec("Spec0");
        s.setLegend("Legend0");
        s.setSource("result");
        s.setSourceNature(Series.SourceNature.SIMULATION_RESULT);
        s.setXVar("time");
        s.setYVar("var0");

        series.add(s);

        s = new Series("s1");

        s.setPlotName("plot0");
        s.setSource("result");
        s.setSourceNature(Series.SourceNature.SIMULATION_RESULT);
        s.setXVar("time");
//        s.setSpec("Spec1");
        s.setLegend("Legend1");
        s.setYVar("var1");

        series.add(s);

        Plot plot = new Plot(null, "plot3", series);

        plot.setTitle("plot_title");
        plot.setDescription("plot_description");
        plot.setXTitle("plot_X_title");
        plot.setXFrom(1000);
        plot.setXTo(2000);
        plot.setYTitle("plot_Y_title");
        plot.setYFrom(10000);
        plot.setYTo(20000);

        return plot;
    }

    public void testView() throws Exception
    {
        DataCollection<SimulationResult> resultDC = initSqlDataCollection();
        SimulationResult result = createSimulationResult( resultDC );
        resultDC.put(result);

        DataCollection<?> repository = CollectionFactory.createRepository( "../data_resources" );
        Module module = (Module) repository.get("SBML model repository");

        Plot plot = createPlot();
        PlotDialog pane = new PlotDialog(null, plot);

        pane.doModal();

        resultDC.close();
        SqlConnectionPool.closeMyConnections();
    }
}
