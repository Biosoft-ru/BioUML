package biouml.plugins.brain.sde;

import java.util.HashMap;

import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.ode.OdeModel;

public class SimpleEventDetector
{
    private int[] events;
    private SimulatorSupport simulator;
    private OdeModel odeModel;
    private boolean eventDetected;
    private double thetaEvent;
    private double tEvent;
    private double[] xEvent;
    // Pre-allocate reusable buffers to avoid per-call allocation in the hot path (profiler hot path)
    private HashMap<Double, double[]> thetaToXCache;
    private final static double EVENT_LOCATION_TOLERANCE = 1E-10;

    public SimpleEventDetector(OdeModel odeModel, SimulatorSupport simulator)
    {
        this.odeModel = odeModel;
        this.simulator = simulator;
        this.eventDetected = false;
        // Allocate reusable buffers — sized on first detectEvent call via odeModel
        this.thetaToXCache = new HashMap<>();
    }

    /*
     * Detects events with an error of O(h)
     * by checking that the event was not executed at the previous point, 
     * but is executed at the new one.
     */
    protected boolean detectEvent(double[] xOld, double tOld, double[] xNew, double tNew) throws Exception
    {
        final double[] eventsOld = odeModel.checkEvent(tOld, xOld);
        // Defer xNew clone until after we know an event might have occurred — avoids allocation on the common non-event path
        // (profiler showed CalculateParameters.run as #2 leaf function, heavily called from checkEvent)

        final double[] eventsNew = odeModel.checkEvent(tNew, xNew);

        // Reuse pre-allocated events array, or allocate once on first call
        int nEvents = eventsOld.length;
        if (events == null || events.length != nEvents) {
            events = new int[nEvents];
        }

        // Clear reusable cache instead of allocating a new HashMap each call
        thetaToXCache.clear();

        thetaEvent = 1.1;
        eventDetected = false;

        for(int i = 0; i < nEvents; i++)
        {
            if(eventsOld[i] == -1 && eventsNew[i] == 1)
            {
            	double theta = 1.0;
            	thetaToXCache.put(theta, xNew);
                eventDetected = true;

                if(theta < thetaEvent)
                {
                    // No need to zero out — events array is reused and will be filled below
                    thetaEvent = theta;
                }

                if(theta <= thetaEvent)
                {
                    events[i] = 1;
                }
            }
            else
            {
                events[i] = 0;
            }
        }

        if(eventDetected)
        {
        	tEvent = tNew;
            xEvent = thetaToXCache.get(thetaEvent);
        }
        return eventDetected;
    }


    public double[] getEventX()
    {
        return xEvent;
    }

    public double getTheta()
    {
        return thetaEvent;
    }

    public double getEventTime()
    {
        return tEvent;
    }

    public int[] getEventInfo()
    {
        return events;
    }

    public boolean isEventDetected()
    {
        return eventDetected;
    }
}
