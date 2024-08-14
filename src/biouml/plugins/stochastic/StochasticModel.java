package biouml.plugins.stochastic;

import java.util.Map;
import java.util.Set;

import biouml.plugins.simulation.java.JavaBaseModel;

/**
 * Base interface for stochastic models
 */
public abstract class StochasticModel extends JavaBaseModel
{
    public abstract double[] getPropensities(double[] x) throws Exception;

    public abstract void doReaction(int i, double[] x, double k);

    public abstract void updatePropensities(double[] propensities, int i, double[] x) throws Exception;

    public abstract Set<Integer> getIndexesOfSubstrate(int i);

    public abstract Map<Integer, Integer>[] getReactantStochiometry();

    public abstract Map<Integer, Integer>[] getProductStochiometry();

    public int[][] reactionDependencies;
    public void setReactionDependencies(int[][] reactionDependencies)
    {
        this.reactionDependencies = reactionDependencies;
    }
}
