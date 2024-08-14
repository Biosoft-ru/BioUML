package biouml.plugins.stochastic.solvers;

import biouml.plugins.simulation.SimulatorInfo;

/**
 * This version of Gillespie is incorrect as it may throw (<b>and may not</b>, it's stochastic) multiple solutions at one time point in the case when
 * event occurs after span point but before next reaction time point (SPAN_POINT_BEFORE_EVENT  is returned by checkEvents)<br>
 * However it passes DSMTS tests even better than GillespieSolver, probably additional analysis is needed here
 * @author axec
 *
 */
@Deprecated
public class GillespieIncorrectSolver extends GillespieSolver
{
 
    @Override
    protected int checkEvents(double nextReactionTime, double timeLimit) throws Exception
    {
        double[] eventsOnStart = model.checkEvent(time, xOld);
        double[] eventsOnFinish = odeModel.checkEvent(nextReactionTime, x);

        if( !compare(eventsOnFinish, eventsOnStart) )
            return NO_EVENT;

        double[] eventsWithoutReaction = odeModel.checkEvent(nextReactionTime, xOld);

        double eventTime;
        if( !compare(eventsWithoutReaction, eventsOnStart) ) // event was triggered by reaction
        {
            eventTime = nextReactionTime;
        }
        else
        //event depends on time, need to find exact time point
        {
            eventTime = locateEvent(time, nextReactionTime, xOld, x); //finding event time point between current time and time of next reaction
        }
        double nextSpanPoint = timeLimit;

        eventAtSpanPoint = equals(eventTime, nextSpanPoint, 1E-10);

        if( nextSpanPoint < eventTime && !eventAtSpanPoint )
        {
            //                            if( debug )
            //                                System.out.println("Out time = " + timeLimit + " x[1]= " + xOut[1]);
            //
            x = xOld.clone();
            profile.setX(xOld);
            profile.setTime(timeLimit);
            fireSolutionUpdate(timeLimit, xOld);
            nextSpanIndex++;
            return SPAN_POINT_BEFORE_EVENT;//currentSpanIndex < span.getLength() - 1;
        }

        //                    if( debug )
        //                        System.out.println("EVENT time = " + nextReactionTime + " x[1]= " + x[1]);

        if( eventAtSpanPoint )
        {
            profile.setX(x);
            profile.setTime(nextSpanPoint);
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

    @Override
    public SimulatorInfo getInfo()
    {
        SimulatorInfo info = new SimulatorInfo();
        info.name = "Gillespie incorrect";
        info.eventsSupport = true;
        info.delaySupport = false;
        info.boundaryConditionSupport = false;
        return info;
    }
}
