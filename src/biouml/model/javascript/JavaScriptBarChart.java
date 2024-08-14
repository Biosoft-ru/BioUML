package biouml.model.javascript;

import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.axis.CategoryLabelPositions;
import org.mozilla.javascript.NativeObject;

import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;

/**
 * 
 * @author helenka
 *
 */
public class JavaScriptBarChart extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger( JavaScriptBarChart.class.getName() );

    public StatisticalBarChart createStatisticalBarChart(Object settings)
    {
        StatisticalBarChart bc = new StatisticalBarChart();
        applySettings( bc, settings );
        return bc;
    }

    public void applySettings(BarChart bc, Object settings)
    {
        if( ! ( settings instanceof NativeObject ) )
        {
            log.log( Level.SEVERE, "Incorrect array of the bar chart settings." );
            return;
        }

        NativeObject njo = (NativeObject)settings;

        Object titleObj = njo.get( "title", scope );
        if( titleObj instanceof String )
            bc.setTitle( (String)titleObj );

        Object xTitleObj = njo.get( "xTitle", scope );
        if( xTitleObj instanceof String )
            bc.setXTitle( (String)xTitleObj );

        Object yTitleObj = njo.get( "yTitle", scope );
        if( yTitleObj instanceof String )
            bc.setYTitle( (String)yTitleObj );

        Object yFromObj = njo.get( "yFrom", scope );
        if( yFromObj instanceof Double )
            bc.setYFrom( (double)yFromObj );

        Object yToObj = njo.get( "yTo", scope );
        if( yToObj instanceof Double )
            bc.setYTo( (double)yToObj );

        Object lablePositionObj = njo.get( "lablePosition" );
        if( lablePositionObj instanceof CategoryLabelPositions )
            bc.setLablePosition( (CategoryLabelPositions)lablePositionObj );

        Object lableFontObj = njo.get( "lableFont" );
        if( lableFontObj instanceof Font )
            bc.setLableFont( (Font)lableFontObj );
    }
}
