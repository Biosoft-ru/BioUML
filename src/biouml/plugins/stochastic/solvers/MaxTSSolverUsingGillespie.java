package biouml.plugins.stochastic.solvers;

import biouml.plugins.stochastic.Util;

public class MaxTSSolverUsingGillespie extends MaxTSSolver
{
    double sumSlow;
    @Override
    protected void nextReaction(double timeLimit) throws Exception
    {
        propensities = model.getPropensities(x);
        fastSlowReactionsPartition(propensities, x);
        sumSlow = Util.sum(propensities, isFastReaction); //sum of propensities
        timeBeforeSlowReaction = stochastic.getExponential(sumSlow);

        tau = numberOfSlowReactions == propensities.length ? timeBeforeSlowReaction : Math.min(maxStep, timeBeforeSlowReaction);

        if( tau == Double.POSITIVE_INFINITY )
            return;
       
        else if( tau > timeLimit - time )
        {
            tau = timeLimit - time;
        }
        else if( timeBeforeSlowReaction <= tau )
        {
            nextSlowReactionIndex = getNextSlowReactionIndex();
            if( nextSlowReactionIndex == -1 )
                return;
            model.doReaction(nextSlowReactionIndex, x, 1);
        }

    }



    protected int getNextSlowReactionIndex()
    {
        double r2 = stochastic.getUniform();
        double test = r2 * sumSlow;

        double sum = 0;
        for( int i = 0; i < propensities.length; i++ )
        {
            if( !isFastReaction[i] )
            {
                sum += propensities[i];
                if( sum >= test )
                    return i;
            }
        }

        return -1;
    }
}