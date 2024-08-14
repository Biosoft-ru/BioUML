package ru.biosoft.graphics.chart;

import java.util.Collection;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * @author lan
 *
 */
public class HistogramBuilder
{
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;
    private TDoubleList points = new TDoubleArrayList();
    
    public void add(double point)
    {
        if(Double.isInfinite(point) || Double.isNaN(point)) return;
        points.add(point);
        minValue = Math.min(minValue, point);
        maxValue = Math.max(maxValue, point);
    }
    
    public void addAll(Collection<? extends Number> points)
    {
        for(Number point: points) add(point.doubleValue());
    }
    
    public void addAll(double[] points)
    {
        for(double point: points) add(point);
    }
    
    public ChartSeries createSeries()
    {
        return createSeries(20);
    }
    
    public ChartSeries createSeries(int minBars)
    {
        if(points.size() == 0) return new ChartSeries(new double[][] {});
        double step = maxValue == minValue ? 1 : snapStep((maxValue-minValue)/minBars);
        ChartSeries chartSeries = new ChartSeries(getHistogramValues(points.toArray(), minValue, maxValue, step, 0));
        chartSeries.getLines().setShow(false);
        chartSeries.getBars().setShow(true);
        chartSeries.getBars().setWidth(step*.8);
        return chartSeries;
    }

    public static double[][] getHistogramValues(double[] points, double minValue, double maxValue, double step, double delta)
    {
        int minBucket = (int)Math.floor(minValue/step);
        double[][] buckets = new double[(int) ( Math.ceil(maxValue/step)-minBucket )+1][];
        for(int i=0; i<buckets.length; i++)
        {
            buckets[i] = new double[2];
            buckets[i][0] = (i+minBucket)*step+delta*step;
        }
        for(double val: points)
        {
            buckets[(int) ( Math.floor(val/step+0.5)-minBucket )][1]+=100.0/points.length;
        }
        return buckets;
    }

    /**
     * Changes supplied value to the least value higher or equal to supplied,
     * which can be represented in the form of k*10^n where k is 1, 2 or 5 and n is arbitrary integer
     * @param step - number to snap
     * @return snapped value (greater or equal to supplied one)
     */
    public static double snapStep(double step)
    {
        if(step <= 0)
            throw new IllegalArgumentException("Snap step can only be applied to positive number");
        double magnitude = 1;
        while(step >= 10)
        {
            magnitude*=10;
            step/=10;
        }
        while(step < 1)
        {
            magnitude/=10;
            step*=10;
        }
        if(step>=5) step=5;
        else if(step>=2) step=2;
        else step=1;
        step*=magnitude;
        return step;
    }
}
