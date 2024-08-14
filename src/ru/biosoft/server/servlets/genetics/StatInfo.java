package ru.biosoft.server.servlets.genetics;

/**
 * Table column statistic bean
 */
public class StatInfo
{
    public int number;
    public double average;
    public double dispersion;
    public double deviation;
    public double median;
    public double min;
    public double max;

    public StatInfo()
    {
        this.number = 0;
        this.average = 0.0;
        this.min = Double.MAX_VALUE;
        this.max = Double.MIN_VALUE;
    }

    public int getNumber()
    {
        return number;
    }
    
    public double getAverage()
    {
        return average;
    }
    public double getDispersion()
    {
        return dispersion;
    }
    public double getDeviation()
    {
        return deviation;
    }
    public double getMedian()
    {
        return median;
    }
    public double getMin()
    {
        return min;
    }
    public double getMax()
    {
        return max;
    }
}
