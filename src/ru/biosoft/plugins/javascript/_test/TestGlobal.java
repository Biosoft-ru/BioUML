package ru.biosoft.plugins.javascript._test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access._test.TestEnvironment;
import ru.biosoft.access.script.NullScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.AxisOptions.Transform;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.util.LazyStringBuilder;
import ru.biosoft.util.LazySubSequence;

public class TestGlobal extends AbstractBioUMLTest
{
    public void testPlot()
    {
        TestEnvironment env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "plot([1,2,3,4,5],[5,4,3,2,1])", env, false );
        assertEquals(1, env.imageElements.size());
        Chart chart = ((ChartDataElement)env.imageElements.get( 0 )).getChart();
        assertEquals(1, chart.getSeriesCount());
        assertTrue(Arrays.deepEquals( new double[][] {{1,5},{2,4},{3,3},{4,2},{5,1}}, chart.getSeries( 0 ).getData()));
        
        env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "plot('x', 'y', [1,2,3], "
                + "{name: 'series1', values: [3,2,1]}, "
                + "{name: 'series2', type: 'constant', values: 5}, "
                + "{name: 'exp', type: 'experiment', values: {x: [1.1, 3.1], y: [2.9, 0.9]}})", env, false );
        assertEquals(1, env.imageElements.size());
        chart = ((ChartDataElement)env.imageElements.get( 0 )).getChart();
        assertEquals(3, chart.getSeriesCount());
        assertTrue(Arrays.deepEquals( new double[][] {{1,3},{2,2},{3,1}}, chart.getSeries( 0 ).getData()));
        assertTrue(Arrays.deepEquals( new double[][] {{1,5},{2,5},{3,5}}, chart.getSeries( 1 ).getData()));
        assertTrue(Arrays.deepEquals( new double[][] {{1.1,2.9},{3.1,0.9}}, chart.getSeries( 2 ).getData()));
        assertTrue(chart.getSeries( 0 ).getLines().isShow());
        assertTrue(chart.getSeries( 1 ).getLines().isShow());
        assertFalse(chart.getSeries( 2 ).getLines().isShow());
        
        env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "plot([1,5,1,5,1])", env, false );
        assertEquals(1, env.imageElements.size());
        chart = ((ChartDataElement)env.imageElements.get( 0 )).getChart();
        assertEquals(1, chart.getSeriesCount());
        assertTrue(Arrays.deepEquals( new double[][] {{1,1},{2,5},{3,1},{4,5},{5,1}}, chart.getSeries( 0 ).getData()));

        env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "plot([1,5,1,5,1])", env, false );
        assertEquals(1, env.imageElements.size());
        chart = ((ChartDataElement)env.imageElements.get( 0 )).getChart();
        assertEquals(1, chart.getSeriesCount());
        assertTrue(Arrays.deepEquals( new double[][] {{1,1},{2,5},{3,1},{4,5},{5,1}}, chart.getSeries( 0 ).getData()));
    }
    
    public void testPlotChart()
    {
        TestEnvironment env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "plot([{data: [[1,2],[2,3],[3,4]]}])", env, false );
        assertEquals(1, env.imageElements.size());
        Chart chart = ((ChartDataElement)env.imageElements.get( 0 )).getChart();
        assertEquals(1, chart.getSeriesCount());
        assertTrue(Arrays.deepEquals( new double[][] {{1,2},{2,3},{3,4}}, chart.getSeries( 0 ).getData()));

        env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "plot([[{data: [[1,2],[2,3],[3,4]]}], {xaxis: {transform: 'log'}, yaxis: {min: 1, max: 10, transform: 'log'}}])", env, false );
        assertEquals(1, env.imageElements.size());
        chart = ((ChartDataElement)env.imageElements.get( 0 )).getChart();
        assertEquals(1, chart.getSeriesCount());
        assertEquals(Transform.LOGARITHM, chart.getOptions().getXAxis().getTransform());
        assertEquals(10.0, chart.getOptions().getYAxis().getMax());
        assertEquals(1.0, chart.getOptions().getYAxis().getMin());
        assertEquals(Transform.LOGARITHM, chart.getOptions().getYAxis().getTransform());
        assertTrue(Arrays.deepEquals( new double[][] {{1,2},{2,3},{3,4}}, chart.getSeries( 0 ).getData()));
    }

    public void testPrint()
    {
        TestEnvironment env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "print(1,2,3)", env, false );
        assertEquals(1, env.print.size());
        assertEquals("1 2 3", env.print.get( 0 ));
    }
    
    public void testConcat()
    {
        Map<String, Object> scope = new HashMap<>();
        scope.put( "s1", "testString" );
        scope.put( "s2", new LazySubSequence( "1234567890", 2, 5 ) );
        Map<String, Object> outVars = new HashMap<>();
        outVars.put( "x", null );
        new JSElement( null, "test.js", "" ).execute( "$.x = concat(s1, s2)", new NullScriptEnvironment(), scope, outVars, false );
        Object output = outVars.get( "x" );
        assertTrue(output instanceof LazyStringBuilder);
        assertEquals("testString345", output.toString());
    }
    
    public void testHelp()
    {
        TestEnvironment env = new TestEnvironment();
        ScriptTypeRegistry.execute( "js", "help(help)", env, false );
        assertEquals(1, env.help.size());
        String helpString = env.help.get( 0 );
        assertTrue(helpString, helpString.trim().startsWith( "<h2 align=center>JavaScript function</h2>" ));
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        Plugins.getPlugins();
    }
}
