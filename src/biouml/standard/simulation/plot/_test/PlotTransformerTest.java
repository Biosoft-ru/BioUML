package biouml.standard.simulation.plot._test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class PlotTransformerTest extends AbstractBioUMLTest
{
    
    static Plot plot;
    static DataCollection plots;

    public PlotTransformerTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(PlotTransformerTest.class.getName());
        
        suite.addTest(new PlotTransformerTest("testCreatePlot"));
        suite.addTest(new PlotTransformerTest("testWritePlot"));
        suite.addTest(new PlotTransformerTest("testReadPlot"));
        suite.addTest(new PlotTransformerTest("testRemovePlot"));

        return suite;
    }

    public void testCreatePlot() throws Exception
    {
        ArrayList<Series> series = new ArrayList<>();

        Series s = new Series("s0");

        s.setPlotName("plot0");
        s.setXVar("time");
//        s.setSpec("Spec0");
        s.setLegend("Legend0");
        s.setSource("result");
        s.setSourceNature(Series.SourceNature.SIMULATION_RESULT);
        s.setYVar("var0");

        series.add(s);

        s = new Series("s1");

        s.setPlotName("plot1");
        s.setSource("result");
        s.setXVar("time");
//        s.setSpec("Spec1");
        s.setLegend("Legend1");
        s.setSourceNature(Series.SourceNature.SIMULATION_RESULT);
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
        // create FileDataCollection
        DataCollection rep = CollectionFactory.createRepository( "../data/test/biouml/standard" );
        plots = ( DataCollection ) rep.get ( "plot" );
        
        plots.put ( plot );
    }

    public void testReadPlot() throws Exception
    {
        Plot pl = (Plot) plots.get ( "plot0" );
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

        while (new_iter.hasNext())
        {
            while (old_iter.hasNext())
            {
                Series new_s = new_iter.next();
                Series old_s = old_iter.next();
                if (old_s.getName().equals(new_s.getName()))
                {
                    assertEquals(new_s.getPlotName(), old_s.getPlotName());
                    assertEquals(new_s.getXVar(), old_s.getXVar());
                    assertEquals(new_s.getSpec(), old_s.getSpec());
                    assertEquals(new_s.getLegend(), old_s.getLegend());
                    assertEquals(new_s.getYVar(), old_s.getYVar());
                    assertEquals(new_s.getSource(), old_s.getSource());
                    assertEquals(new_s.getSourceNature(), old_s.getSourceNature());
                }
            }
        }
    }
    
    public void testRemovePlot() throws Exception
    {
        plots.remove ( plot.getName ( ) );
    }
    
}
