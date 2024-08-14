package ru.biosoft.analysis.optimization;

import java.util.List;

import ru.biosoft.access.core.DataCollection;

import ru.biosoft.jobcontrol.JobControl;

public interface OptimizationProblem
{
    /**
     * Returns the list of fitting parameters
     */
    public List<Parameter> getParameters();

    /**
     * Tests the goodness of fit for series of parameter values.
     */
    public double[][] testGoodnessOfFit(double[][] values, JobControl jobControl) throws Exception;

    /**
     * Tests the goodness of fit for the single set of parameter values.
     */
    public double[] testGoodnessOfFit(double[] values, JobControl jobControl) throws Exception;

    /**
     * Returns the current number of performed simulations.
     */
    public int getEvaluationsNumber();

    /**
     * Returns the array of the optimization results
     */
    public Object[] getResults(double[] values, DataCollection<?> origin) throws Exception;

    /**
     * Correctly stops calculations
     */
    public void stop();
}