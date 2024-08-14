package biouml.standard.simulation.plot._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

import junit.framework.TestSuite;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.sql.SqlConnectionPool;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import biouml.standard.simulation.plot.access.PlotSqlTransformer;

public class PlotSqlTransformerTest extends AbstractBioUMLTest
{
    private SqlDataCollection<Plot> resultDC;
    private Plot plot;

    public PlotSqlTransformerTest(String name)
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

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(PlotSqlTransformerTest.class.getName());
        suite.addTest(new PlotSqlTransformerTest("testWritePlot"));
        suite.addTest(new PlotSqlTransformerTest("testReadPlot"));
        suite.addTest(new PlotSqlTransformerTest("testRemovePlot"));
        return suite;
    }

    @Override
    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.put(DataCollectionConfigConstants.NAME_PROPERTY, "Plot (test)");
        props.put("data-element-class", Plot.class.getName());
        props.put("transformerClass", PlotSqlTransformer.class.getName());
        props.put(SqlDataCollection.JDBC_URL_PROPERTY, "cyclonet");
        resultDC = new SqlDataCollection<>( null, props );

        createPlot();
    }

    @Override
    public void tearDown() throws Exception
    {
        resultDC.close();
        SqlConnectionPool.closeMyConnections();
    }

    private void createPlot()
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

        plot = new Plot(null, "plot0", series);

        plot.setTitle("plot_title");
        plot.setDescription("plot_description");
        plot.setXTitle("plot_X_title");
        plot.setXFrom(1000);
        plot.setXTo(2000);
        plot.setYTitle("plot_Y_title");
        plot.setYFrom(10000);
        plot.setYTo(20000);
    }

    public void testWritePlot() throws Exception
    {
        System.out.println("testWritePlot");
        resultDC.remove("plot0");
        resultDC.put(plot);
    }

    public void testRemovePlot() throws Exception
    {
        System.out.println("tesRemovePlot");
        resultDC.remove(plot.getName());
        assertFalse(resultDC.contains(plot.getName()));
    }

    public void testReadPlot() throws Exception
    {
        System.out.println("testReadPlot");

        Plot pl = resultDC.get(plot.getName());
        assertEquals(plot.getName(), pl.getName());
        assertEquals(plot.getTitle(), pl.getTitle());
        assertEquals(plot.getDescription(), pl.getDescription());
        assertEquals(plot.getXTitle(), pl.getXTitle());
        assertEquals(plot.getXTo(), pl.getXTo(), 1e-8);
        assertEquals(plot.getXFrom(), pl.getXFrom(), 1e-8);
        assertEquals(plot.getYTitle(), pl.getYTitle());
        assertEquals(plot.getYTo(), pl.getYTo(), 1e-8);
        assertEquals(plot.getYFrom(), pl.getYFrom(), 1e-8);

        List<Series> old_series = plot.getSeries();
        List<Series> new_series = pl.getSeries();

        Iterator<Series> new_iter = new_series.iterator();
        Iterator<Series> old_iter = old_series.iterator();

        while (new_iter.hasNext()) {
            Series new_s = new_iter.next();
            while (old_iter.hasNext()) {
                Series old_s = old_iter.next();
                if (new_s.getSpec().equals(old_s.getSpec())) {
                    assertEquals(new_s.getPlotName(), old_s.getPlotName());
                    assertEquals(new_s.getSpec(), old_s.getSpec());
                    assertEquals(new_s.getLegend(), old_s.getLegend());
                    assertEquals(new_s.getXVar(), old_s.getXVar());
                    assertEquals(new_s.getSource(), old_s.getSource());
                    assertEquals(new_s.getSourceNature(), old_s.getSourceNature());
                    assertEquals(new_s.getYVar(), old_s.getYVar());
                }
            }
        }
    }

}
