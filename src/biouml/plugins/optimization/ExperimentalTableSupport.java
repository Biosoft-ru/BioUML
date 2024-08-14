package biouml.plugins.optimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Option;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class ExperimentalTableSupport extends Option
{
    protected static final Logger log = Logger.getLogger(ExperimentalTableSupport.class.getName());

    public ExperimentalTableSupport()
    {
    }

    public ExperimentalTableSupport(DataElementPath filePath)
    {
        this.filePath = filePath;
        int columnCount = getTable().getColumnModel().getColumnCount();
        weights = new double[columnCount];
    }

    public ExperimentalTableSupport(TableDataCollection tdc)
    {
        this.tdc = tdc;
        int columnCount = tdc.getColumnModel().getColumnCount();
        weights = new double[columnCount];
    }

    private TableDataCollection tdc;
    public TableDataCollection getTable()
    {
        if( tdc != null )
            return tdc;

        try
        {
            if( filePath != null )
            {
                tdc = filePath.getDataElement(TableDataCollection.class);
                return tdc;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_EXPERIMENT_TABLE_GETTING"), e);
        }
        return null;
    }

    public double[][] getTableMatrix()
    {
        return TableDataCollectionUtils.getMatrix(getTable());
    }

    private DataElementPath filePath = null;
    public void setFilePath(DataElementPath newPath)
    {
        this.filePath = newPath;
        int columnCount = getTable().getColumnModel().getColumnCount();
        weights = new double[columnCount];
    }
    public DataElementPath getFilePath()
    {
        return filePath;
    }

    @Override
    public String toString()
    {
        if( filePath == null )
            return "";
        return filePath.toString() + ";";
    }

    private String weightMethod = "";
    public String getWeightMethod()
    {
        return this.weightMethod;
    }
    public void setWeightMethod(String method)
    {
        this.weightMethod = method;
    }

    private double[] weights;
    public double[] getWeights()
    {
        return this.weights;
    }

    public double[] calculateWeights(boolean isTimeCourse, List<Integer> exactDataColumns)
    {
        TableDataCollection expTable = getTable();

        if( expTable == null )
            return null;

        int columnCount = expTable.getColumnModel().getColumnCount();

        int methodIndex = WeightMethod.getWeightMethods().indexOf(weightMethod);
        WeightMethod method = WeightMethod.values()[methodIndex];

        switch( method )
        {
            case MEAN:
            case MEAN_SQUARE:
            case STANDARD_DEVIATION:
                weights = calculateWeights(expTable, method, exactDataColumns);
                break;

            case SMOOTHING_SPLINE:
                weights = new double[columnCount];
                Arrays.fill( weights, Double.NaN );
                break;

            default:
                weights = new double[columnCount];
                Arrays.fill( weights, 1.0 );
        }

        return weights;
    }

    private double[] calculateWeights(TableDataCollection expTable, WeightMethod method, List<Integer> exactDataColumns)
    {
        int rowCount = expTable.getSize();
        int columnCount = expTable.getColumnModel().getColumnCount();

        double[] weights = new double[columnCount];
        double[][] tableMatrix = TableDataCollectionUtils.getMatrix(expTable);

        double minWeight = Double.MAX_VALUE;

        for( int j = 0; j < columnCount; j++ )
        {
            double norm = 1;
            if( exactDataColumns != null && exactDataColumns.contains(j) )
                norm = 100 / getMaximumValue(tableMatrix, j);

            double mean = 0;
            double meanSquare = 0;

            int isNaN = 0;
            for( int i = 0; i < rowCount; i++ )
            {
                if( !Double.isNaN(tableMatrix[i][j]) )
                {
                    mean += tableMatrix[i][j] * norm;
                    meanSquare += Math.pow(tableMatrix[i][j] * norm, 2);
                }
                else
                    isNaN++;
            }

            mean /= ( rowCount - isNaN );
            meanSquare /= ( rowCount - isNaN );

            switch( method )
            {
                case MEAN:
                    weights[j] = Math.abs(mean);
                    break;

                case MEAN_SQUARE:
                    weights[j] = Math.sqrt(meanSquare);
                    break;

                case STANDARD_DEVIATION:
                    weights[j] = Math.sqrt(meanSquare - mean * mean);
                    break;

                default:
                    break;
            }

            if( minWeight > weights[j] )
                minWeight = weights[j];
        }

        for( int j = 0; j < columnCount; j++ )
            weights[j] = ( minWeight + Math.sqrt(Double.MIN_NORMAL) ) / ( weights[j] + Math.sqrt(Double.MIN_NORMAL) );

        return weights;
    }

    private double getMaximumValue(double[][] tableMatrix, int column)
    {
        return StreamEx.of( tableMatrix ).mapToDouble( row -> row[column] ).remove( Double::isNaN ).max().orElse( Double.MIN_VALUE );
    }

    public static enum WeightMethod
    {
        MEAN, MEAN_SQUARE, STANDARD_DEVIATION, ABSOLUTE, EDITED, SMOOTHING_SPLINE, EXPERIMENTAL;

        public static String toString(WeightMethod weightMethod)
        {
            switch( weightMethod )
            {
                case MEAN:
                    return "Mean";

                case MEAN_SQUARE:
                    return "Mean square";

                case STANDARD_DEVIATION:
                    return "Standard deviation";

                case ABSOLUTE:
                    return "Absolute";

                case EDITED:
                    return "Edited";

                case SMOOTHING_SPLINE:
                    return "Smoothing spline";

                case EXPERIMENTAL:
                    return "Experimental";

                default:
                    return "";
            }
        }

        public static List<String> getWeightMethods()
        {
            List<String> list = new ArrayList<>();
            for( WeightMethod method : values() )
            {
                list.add(toString(method));
            }
            return list;
        }
    }

    public double getDistance(double[] values, String tdcVariable, int relativeTo, List<Integer> exactDataColumns, int limit)
    {
        int tdcIndex = getTable().getColumnModel().getColumnIndex(tdcVariable);
        double[][] tdcMatrix = getTableMatrix();

        double result = 0;
        for( int i = 0; i < limit; ++i )
        {
            double weight = weights[tdcIndex];
            if( weightMethod.equals(WeightMethod.toString(WeightMethod.SMOOTHING_SPLINE)) )
            {
                weight = calculateWeight(tdcMatrix, values, i, tdcIndex, relativeTo, exactDataColumns);
            }

            double difference = getDifference( tdcMatrix, values, i, tdcIndex, relativeTo, exactDataColumns );

            if( Double.isNaN( difference ) )
                result = Double.POSITIVE_INFINITY;
            else
            if( difference > 1e-10 )
            {
                result += weight * Math.pow(difference, 2);
            }
        }
        return result;
    }

    private double getDifference(double[][] tdcMatrix, double[] values, int i, int j, int relativeTo, List<Integer> exactDataColumns)
    {
        if( !Double.isNaN(tdcMatrix[i][j]) )
        {
            if( relativeTo == -1 )
            {
                double norm = 1;
                if( exactDataColumns != null && exactDataColumns.contains(j) )
                    norm = getMaximumValue(tdcMatrix, j);

                if( getWeightMethod().equals(WeightMethod.toString(WeightMethod.EXPERIMENTAL)) && tdcMatrix[i][j] > 0
                        && tdcMatrix[i][j] != 1 )
                    return Math.abs( ( tdcMatrix[i][j] - values[i] ) / ( Math.log10(tdcMatrix[i][j]) * norm ));

                return Math.abs( ( tdcMatrix[i][j] - values[i] ) / norm);
            }
            else
                return Math.abs(values[i] / values[relativeTo] - tdcMatrix[i][j] / 100);
        }
        return -1;
    }

    private static final int h = 2;
    private double calculateWeight(double[][] tdcMatrix, double[] values, int i, int j, int relativeTo, List<Integer> exactDataColumns)
    {
        double weight = 0;

        int k = i - h;

        int step = 0;
        while( step < 5 )
        {
            if( k >= 0 && k < values.length )
            {
                double difference = getDifference(tdcMatrix, values, k, j, relativeTo, exactDataColumns);
                if( ! ( difference == -1 ) )
                {
                    weight += Math.pow(difference, 2);
                    step++;
                }
            }
            else
            {
                step++;
            }
            k++;
        }

        weight /= 2 * h;
        return 1 / weight;
    }
}
