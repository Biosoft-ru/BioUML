package biouml.plugins.simulation;

/**
 * Simple uniform infinite span from 0 to INFINITY
 * Its only parameter is a time step
 * @author Damag
 */
public class InfiniteSpan implements Span
{
    private double step;

    public InfiniteSpan(double step)
    {
        this.step = step;
    }

    @Override
    public double getTimeStart()
    {
        return 0;
    }

    @Override
    public double getTimeFinal()
    {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public int getLength()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isProper()
    {
        return true;
    }

    @Override
    public Span getRestrictedSpan(double a, double b)
    {
        return new InfiniteSpan( step );
    }

    @Override
    public double getTime(int i)
    {
        return i * step;
    }

    @Override
    public void addPoints(double[] points)
    {
    }

    @Override
    public InfiniteSpan clone()
    {
        return new InfiniteSpan( step );
    }
}