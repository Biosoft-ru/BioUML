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
    private final static double EVENT_LOCATION_TOLERANCE = 1E-10;

    public SimpleEventDetector(OdeModel odeModel, SimulatorSupport simulator)
    {
        this.odeModel = odeModel;
        this.simulator = simulator;
        this.eventDetected = false;
    }

    /*
     * Detects events with an error of O(h)
     * by checking that the event was not executed at the previous point, 
     * but is executed at the new one.
     */
    protected boolean detectEvent(double[] xOld, double tOld, double[] xNew, double tNew) throws Exception
    {
        xEvent = xNew.clone();

        final double[] eventsOld = odeModel.checkEvent(tOld, xOld);
        final double[] eventsNew = odeModel.checkEvent(tNew, xEvent);

        events = new int[eventsOld.length];
        HashMap<Double, double[]> thetaToX = new HashMap<>();

        thetaEvent = 1.1;

        eventDetected = false;
        for(int i = 0; i < eventsOld.length; i++)
        {
            if(eventsOld[i] == -1 && eventsNew[i] == 1)
            {
            	double theta = 1.0;
            	thetaToX.put(theta, xEvent);
                eventDetected = true;
                
                if(theta < thetaEvent)
                {
                    for(int j = 0; j < i; j++)
                    {
                        events[j] = 0;
                    }
                    thetaEvent = theta;
                }

                if(theta <= thetaEvent)
                {
                    events[i] = 1;
                }
            }
        }

        if(eventDetected)
        {
        	tEvent = tNew;
            xEvent = thetaToX.get(thetaEvent);
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
