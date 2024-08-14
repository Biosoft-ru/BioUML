package biouml.standard.simulation;

public interface ResultListener
{
    void start(Object model);

    void add(double t, double[] y) throws Exception;
}
