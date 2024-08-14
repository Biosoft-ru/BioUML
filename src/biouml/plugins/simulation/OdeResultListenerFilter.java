package biouml.plugins.simulation;

import biouml.standard.simulation.ResultListener;


public class OdeResultListenerFilter extends ResultListenerFilter
{

    public OdeResultListenerFilter(ResultListener[] outputListener)
    {
        super(outputListener);
    }

    private double minimalTimeStep = 1e-5;
    public double getMinimalTimeStep()
    {
        return minimalTimeStep;
    }
    public void setMinimalTimeStep(double minimalTimeStep)
    {
        this.minimalTimeStep = minimalTimeStep;
    }

    private double minimalSolutionDifference = Double.MIN_VALUE;
    public double getMinimalSolutionDifference()
    {
        return minimalSolutionDifference;
    }
    public void setMinimalSolutionDifference(double minimalSolutionDifference)
    {
        this.minimalSolutionDifference = minimalSolutionDifference;
    }

    double recentProcessedTime = 0;
    double[] recentProcessedSolution = null;
    @Override
    public void add(double t, double[] x) throws Exception
    {
        if (recentProcessedSolution == null)
        {
            recentProcessedSolution = x;
            recentProcessedTime = t;
            super.add(t, x);
            return;
        }

        int n = x.length;
        if ( n != recentProcessedSolution.length )
            throw new Exception("Solution dimensions do not agree: " + x.length + " != " + recentProcessedSolution.length);

        if (Math.abs(t - recentProcessedTime) < minimalTimeStep)
        {
            boolean soluitionChanged = false;
            for (int i = 0; i < n; i++)
            {
                if (Math.abs(x[i] - recentProcessedSolution[i]) > minimalSolutionDifference)
                {
                    soluitionChanged = true;
                }
            }

            if ( !soluitionChanged )
                return;
        }

        recentProcessedSolution = x;
        recentProcessedTime = t;
        super.add(t, x);
    }
}