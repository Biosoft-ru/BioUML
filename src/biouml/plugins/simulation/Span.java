package biouml.plugins.simulation;

/**
 * Interface for span, implementations should store and retrieves values representing the span of time for an
 * integration interval
 */
public interface Span
{
    /**
     *  Retrieves the staring point of the span
     */
    public double getTimeStart();

    /**
     *  Retrieves the stopping point of the span
     */
    public double getTimeFinal();

    /**
     * Retrieves the length of span
     */
    public int getLength();


    /**
     * Retrieves whether the span is out of order or not
     */
    public boolean isProper();

    /**
     * Method recalculates span according to borders a and b
     */
    public Span getRestrictedSpan(double a, double b);

    /**
     *Retrieves time at ith position
     */
    public double getTime(int i);

    /**
     * Adds new points to array
     * Note that point that are already in span will be ignored
     */
    public void addPoints(double[] points);


    public Span clone();
}