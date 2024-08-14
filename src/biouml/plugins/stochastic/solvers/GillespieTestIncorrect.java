package biouml.plugins.stochastic.solvers;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.stochastic.StochasticSimulator;
import biouml.plugins.stochastic.Util;

public class GillespieTestIncorrect extends StochasticSimulator
{
    protected int nextReactionIndex = -1;
    protected double timeBeforeNextReaction;
    protected double[] propensities;

    @Override
    public boolean doStep()
    {
        try
        {
            while( nextSpanIndex < span.getLength() )
            {
                fireSolutionUpdate(span.getTime(nextSpanIndex), x);
                nextSpanIndex++;
                
                nextReaction();
                double nextReactionTime = time + timeBeforeNextReaction;

                while( nextReactionTime < span.getTime(nextSpanIndex) )
                {
                    if( nextReactionIndex != -1 && nextReactionTime < span.getTimeFinal() )
                    {
                        model.doReaction(nextReactionIndex, x, 1); //processing chosen reaction
                        time = nextReactionTime;

                        if( x[1] > 30 )
                        {
                            x[0] = 100;
                            x[1] = 0;
                        }
                    }
                    nextReaction();
                    nextReactionTime = time + timeBeforeNextReaction;
                }
            }
            return false;

        }
        catch( Exception ex )
        {
            if( jobControl != null )
                jobControl.terminate();
            profile.setErrorMessage(ex.getLocalizedMessage());
            profile.setUnstable(true);
            log.info(ex.getMessage());
            return false;
        }
    }

    protected boolean eventOccured;

    protected boolean equals(double t1, double t2, double error)
    {
        return Math.abs(t1 - t2) < error;
    }

    /**
     * Method for choosing next reaction to perform
     * @param r
     * @param propensities
     * @return
     */
    protected void nextReaction() throws Exception
    {
        double[] propensities = model.getPropensities(x);
        double propensitiesSum = Util.sum(propensities); //sum of propensities

        timeBeforeNextReaction = calculateTau(propensitiesSum);

        if( timeBeforeNextReaction == Double.POSITIVE_INFINITY )
        {
            nextReactionIndex = -1;
            return;
        }

        propensitiesSum *= stochastic.getUniform();

        double sum = 0;

        for( int j = 0; j < propensities.length; j++ )
        {
            sum += propensities[j];
            if( propensitiesSum <= sum )
            {
                nextReactionIndex = j;
                return;
            }
        }
        nextReactionIndex = -1;
    }

    protected double calculateTau(double a)
    {
        return ( a <= 0 ) ? Double.POSITIVE_INFINITY : -Math.log(stochastic.getUniform()) / a;
    }

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Gillespie test incorrect";
        info.eventsSupport = false;
        info.delaySupport = false;
        info.boundaryConditionSupport = false;
        return info;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new GillespieOptions();
    }

    @Override
    public void setOptions(Options options)
    {
        if( ! ( options instanceof GillespieOptions ) )
            throw new IllegalArgumentException("Only GillespieOptions are compatible with Gillespie solver");
        this.options = options;
    }

    //Options class
    public static class GillespieOptions extends Options
    {
    }

    public static class GillespieOptionsBeanInfo extends BeanInfoEx2<GillespieOptions>
    {
        public GillespieOptionsBeanInfo()
        {
            super(GillespieOptions.class);
        }
    }



}
