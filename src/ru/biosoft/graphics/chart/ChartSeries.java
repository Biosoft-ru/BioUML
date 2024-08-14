package ru.biosoft.graphics.chart;

import java.awt.Color;
import java.awt.Shape;
import java.io.IOException;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import one.util.streamex.IntStreamEx;
import ru.biosoft.util.ColorUtils;

/**
 * Container object to hold data and view options for single chart series
 * @author lan
 */
public class ChartSeries
{
    double data[][] = null;
    String label = null;
    int xAxis = 1;
    int yAxis = 1;
    LinesOptions lines;
    BarsOptions bars;
    Color color = Color.RED;
    Shape shape = Chart.CIRCLE;

    public ChartSeries()
    {
        bars = new BarsOptions();
        lines = new LinesOptions();
    }

    public ChartSeries(JSONObject from)
    {
        if( from == null )
            return;
        JSONArray dataArray = from.optJSONArray("data");
        if( dataArray != null )
        {
            data = new double[dataArray.length()][2];
            for( int i = 0; i < dataArray.length(); i++ )
            {
                JSONArray dataPoint = dataArray.optJSONArray(i);
                if( dataPoint != null && dataPoint.length() >= 2 )
                {
                    data[i][0] = dataPoint.optDouble(0);
                    data[i][1] = dataPoint.optDouble(1);
                }
            }
        }
        xAxis = from.optInt("xaxis", 1);
        yAxis = from.optInt("yaxis", 1);
        if( from.has("label") && !from.isNull("label") )
            label = from.optString("label");
        if(from.has("color"))
        {
            color = ColorUtils.parseColor(from.optString("color"));
        }
        lines = new LinesOptions(from.optJSONObject("lines"));
        bars = new BarsOptions(from.optJSONObject("bars"));
    }

    public ChartSeries(double[][] data)
    {
        this();
        setData(data);
    }

    public ChartSeries(double[] x, double[] y)
    {
        this();
        if( x.length != y.length )
            throw new IllegalArgumentException("Arguments lengths must agree");
        double[][] data = IntStreamEx.ofIndices( x ).mapToObj( i -> new double[] {x[i], y[i]} ).toArray( double[][]::new );
        setData(data);
    }

    public double[][] getData()
    {
        return data;
    }

    public void setData(double[][] data)
    {
        this.data = data;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public int getXAxis()
    {
        return xAxis;
    }

    public void setXAxis(int xAxis)
    {
        this.xAxis = xAxis;
    }

    public int getYAxis()
    {
        return yAxis;
    }

    public void setYAxis(int yAxis)
    {
        this.yAxis = yAxis;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    public Color getColor()
    {
        return color;
    }

    public LinesOptions getLines()
    {
        return lines;
    }

    public void setLines(LinesOptions lines)
    {
        this.lines = lines;
    }

    public BarsOptions getBars()
    {
        return bars;
    }

    public void setBars(BarsOptions bars)
    {
        this.bars = bars;
    }

    public Shape getShape()
    {
        return shape;
    }

    public void setShape(Shape shape)
    {
        this.shape = shape;
    }

    public JSONObject toJSON() throws JSONException
    {
        if( data == null )
            return null;
        JSONArray dataArray = new JSONArray();
        for( double[] point : data )
        {
            if( point.length != 2 )
                continue;
            JSONArray dataPoint = new FormattedDoubleJSONArray();
            if (Double.isFinite( point[0]) && Double.isFinite( point[1] ) )
            {
            dataPoint.put(point[0]);
            dataPoint.put(point[1]);
            dataArray.put(dataPoint);
            }
        }
        JSONObject json = new JSONObject();
        json.put("data", dataArray);
        if( label != null )
            json.put("label", label);
        if( xAxis != 1 )
            json.put("xaxis", xAxis);
        if( yAxis != 1 )
            json.put("yaxis", yAxis);
        if(color.getAlpha()<255)
            json.put("color", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")");
        else
            json.put("color", "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue()+")");
        JSONObject linesJSON = lines.toJSON();
        if(linesJSON != null) json.put("lines", linesJSON);
        JSONObject barsJSON = bars.toJSON();
        if(barsJSON != null) json.put("bars", barsJSON);
        return json;
    }

    /**
     * Subclass of JSONArray containing doubles for custom number formats (to spare space)
     */
    private static class FormattedDoubleJSONArray extends JSONArray
    {
        private String formatNumber(double number)
        {
            try (Formatter formatter = new Formatter( Locale.US ))
            {
                formatter.format( "%.4g", number );
                String s = formatter.toString();
                if( s.indexOf( '.' ) > 0 && s.indexOf( 'e' ) < 0 && s.indexOf( 'E' ) < 0 )
                {
                    while( s.endsWith( "0" ) )
                    {
                        s = s.substring( 0, s.length() - 1 );
                    }
                    if( s.endsWith( "." ) )
                    {
                        s = s.substring( 0, s.length() - 1 );
                    }
                }
                return s;
            }
        }

        @Override
        public String join(String separator) throws JSONException
        {
            int len = length();
            StringBuffer sb = new StringBuffer();

            for( int i = 0; i < len; i += 1 )
            {
                if( i > 0 )
                {
                    sb.append(separator);
                }
                sb.append(formatNumber(optDouble(i)));
            }
            return sb.toString();
        }

        @Override
        public String toString()
        {
            try
            {
                return '[' + this.join(",") + ']';
            } catch (Exception e) {
                return super.toString();
            }
        }

        @Override
        public Writer write(Writer writer, int indentFactor, int indent) throws JSONException
        {
            try
            {
                writer.write( this.toString() );
            }
            catch( IOException e )
            {
                super.write( writer, indentFactor, indent );
            }
            return writer;
        }
    }
}
