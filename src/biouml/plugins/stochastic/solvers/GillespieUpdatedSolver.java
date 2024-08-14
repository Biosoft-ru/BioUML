package biouml.plugins.stochastic.solvers;

import ru.biosoft.jobcontrol.FunctionJobControl;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.stochastic.StochasticSimulator;
import biouml.plugins.stochastic.Util;

public class GillespieUpdatedSolver extends StochasticSimulator
{
    protected int nextReactionIndex = -1;
    protected double timeBeforeNextReaction;
    protected double[] propensities;

    @Override
    public boolean doStep()
    {
        try
        {
            if( fireInitialValues || eventAtSpanPoint )
                fireSolutionUpdate(span.getTimeStart(), x);
            
            if( terminated || jobControl != null && jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                return false;
            
            fireInitialValues = false;
            eventAtSpanPoint = false;
            //            if( debug )
            //                System.out.println("Step: " + time + " - > " + span.getTime(currentSpanIndex + 1) + " x[1]= " + x[1]);

            xOld = x.clone();

            nextReaction();

            double nextSpanPoint = span.getTime(nextSpanIndex);

            double nextReactionTime = time + timeBeforeNextReaction;
            
            if( nextReactionIndex != -1 )
                model.doReaction(nextReactionIndex, x, 1); //processing chosen reaction

            if( modelHasEvents && EVENT_OCCURED == checkEvents(nextReactionTime, nextSpanPoint))
                        return false;

            while( nextSpanIndex < spanLength && nextSpanPoint <= nextReactionTime )
            {
                //                if( debug )
                //                    System.out.println("Out time = " + span.getTime(currentSpanIndex + 1) + " x[1]= " + xOut[1]);
                fireSolutionUpdate(nextSpanPoint, ( nextSpanPoint < nextReactionTime ) ? xOld : x);
                nextSpanIndex++;
                if( nextSpanIndex < spanLength )
                    nextSpanPoint = span.getTime(nextSpanIndex);
            }

            //                                if( debug )
            //                                    System.out.println("finish time = " + nextReactionTime + " x[1]= " + x[1]);

            time = nextReactionTime;
            profile.setX(x);
            profile.setTime(time);

            return nextSpanIndex < spanLength;

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
    public static final int SPAN_POINT_BEFORE_EVENT = 2;
    public static final int EVENT_OCCURED = 1;
    public static final int NO_EVENT = 0;

    protected int checkEvents(double nextReactionTime, double timeLimit) throws Exception
    {
        double[] eventsOnStart = model.checkEvent(time, xOld);
        double[] eventsOnFinish = odeModel.checkEvent(nextReactionTime, x);

        if( !compare(eventsOnFinish, eventsOnStart) )
            return NO_EVENT;

        double[] eventsWithoutReaction = odeModel.checkEvent(nextReactionTime, xOld);

        double eventTime;
        if( !compare(eventsWithoutReaction, eventsOnStart) ) // event was triggered by reaction
            eventTime = nextReactionTime;
        else
            //event depends on time, need to find exact time point
            eventTime = locateEvent(time, nextReactionTime, xOld, x); //finding event time point between current time and time of next reaction

        eventAtSpanPoint = equals(eventTime, timeLimit, 1E-10);

        //                            if( debug )
        //                                System.out.println("Out time = " + timeLimit + " x[1]= " + xOut[1]);
        //
        while( timeLimit < eventTime && !eventAtSpanPoint )
        {
            //                if( debug )
            //                    System.out.println("Out time = " + timeLimit + " x= " + xOld[1]);

            fireSolutionUpdate(timeLimit, xOld);
            nextSpanIndex++;
            if( nextSpanIndex >= span.getLength() )
                break;
            timeLimit = span.getTime(nextSpanIndex);
            eventAtSpanPoint = equals(eventTime, timeLimit, 1E-10);
        }

        //                    if( debug )
        //                        System.out.println("EVENT time = " + nextReactionTime + " x[1]= " + x[1]);

        if( eventAtSpanPoint )
        {
            profile.setX(x);
            profile.setTime(timeLimit);
        }
        else
        {
            profile.setX(x);
            profile.setTime(eventTime);
        }
        for( int j = 0; j < eventsOnFinish.length; j++ )
        {
            eventInfo[j] = (int)eventsOnFinish[j];
        }
        return EVENT_OCCURED;
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
        info.name = "Gillespie";
        info.eventsSupport = true;
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
