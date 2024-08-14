package biouml.plugins.sedml.analyses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.util.ColorUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class Plot2D extends GenerateReport<Plot2D.Parameters>
{
    public Plot2D(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        return generateChart();
    }

    private ChartDataElement generateChart()
    {
        Chart chart = new Chart();

        Curve[] curves = parameters.getCurves();
        for( Curve curve : curves )
        {
            List<ChartSeries> series = createSeriesForCurve( curve );
            for(ChartSeries s : series)
                chart.addSeries( s );
        }
        for(int i = 0; i < chart.getSeriesCount(); i++)
        {
            ChartSeries series = chart.getSeries( i );
            if(chart.getSeriesCount() > 10)
                series.setLabel( null );
            series.setColor( ColorUtils.getDefaultColor( i ) );
        }

        DataElementPath resultPath = parameters.getOutputChart();
        ChartDataElement result = new ChartDataElement( resultPath.getName(), resultPath.getParentCollection(), chart );
        resultPath.save( result );
        return result;
    }

    private List<ChartSeries> createSeriesForCurve(Curve curve)
    {
        Map<String, double[]> xValues = evaluateExpression( curve.getExpressionX() );
        if(curve.isLogX())
            StreamEx.ofValues( xValues ).forEach( this::logArray );
        Map<String, double[]> yValues = evaluateExpression( curve.getExpressionY() );
        if(curve.isLogY())
            StreamEx.ofValues( yValues ).forEach( this::logArray );

        //Special case for prefixed result when one of expressions contains only time variable
        if(xValues.containsKey( "" ) && !yValues.containsKey( "" ))
        {
            double[] x = xValues.get( "" );
            xValues = StreamEx.ofKeys( yValues ).mapToEntry( yPrefix->x ).toSortedMap();
        }else if(yValues.containsKey( "" ) && !xValues.containsKey( "" ))
        {
            double[] y = yValues.get( "" );
            yValues = StreamEx.ofKeys( xValues ).mapToEntry( yPrefix->y ).toSortedMap();
        }

        List<ChartSeries> result = new ArrayList<>();
        for( Map.Entry<String, double[]> xEntry : xValues.entrySet() )
        {
            String prefix = xEntry.getKey();
            double[] x = xEntry.getValue();
            double[] y = yValues.get( prefix );
            ChartSeries cs = new ChartSeries( x, y );
            cs.setLabel( prefix + curve.getTitle() );
            result.add( cs );
        }
        return result;
    }

    public void logArray(double[] x)
    {
        for(int i = 0; i < x.length; i++)
            x[i] = Math.log( x[i] );
    }

    public static class Parameters extends GenerateReportParameters
    {
        private Curve[] curves;
        private DataElementPath outputChart;

        public Parameters()
        {
            setCurves( new Curve[] {new Curve()} );
        }

        @PropertyName("Curves")
        @PropertyDescription("Curves.")
        public Curve[] getCurves()
        {
            return curves;
        }
        public void setCurves(Curve[] curves)
        {
            Object oldValue = this.curves;
            this.curves = curves;
            for(Curve curve : curves)
                curve.setParent( this );
            firePropertyChange( "curves", oldValue, curves );
        }

        @PropertyName("Output chart")
        @PropertyDescription("Output chart.")
        public DataElementPath getOutputChart()
        {
            return outputChart;
        }
        public void setOutputChart(DataElementPath output)
        {
            Object oldValue = this.outputChart;
            this.outputChart = output;
            firePropertyChange( "outputChart", oldValue, output );
        }

    }

    public static class ParametersBeanInfo extends GenerateReportParametersBeanInfo<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            add( "curves" );
            property( "outputChart" ).outputElement( ChartDataElement.class ).add();
        }
    }
}
