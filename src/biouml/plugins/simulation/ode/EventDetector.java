
package biouml.plugins.simulation.ode;

import java.util.HashMap;

import biouml.plugins.simulation.SimulatorSupport;

public class EventDetector
{
    private int[] events;
    private SimulatorSupport simulator;
    private OdeModel odeModel;
    private boolean eventDetected;
    private double thetaEvent;
    private double tEvent;
    private double[] xEvent;
    private final static double EVENT_LOCATION_TOLERANCE = 1E-10;

    public EventDetector(OdeModel odeModel, SimulatorSupport simulator)
    {
        this.odeModel = odeModel;
        this.simulator = simulator;
        this.eventDetected = false;
    }

    protected boolean detectEvent(double[] xOld, double tOld, double step) throws Exception
    {
        xEvent = new double[xOld.length];
        simulator.integrationStep(xEvent, xOld, tOld, step, 1);

        final double[] eventsOld = odeModel.checkEvent(tOld, xOld);
        final double[] eventsNew = odeModel.checkEvent(tOld + step, xEvent);

        events = new int[eventsOld.length];
        HashMap<Double, double[]> thetaToX = new HashMap<>();

        thetaEvent = 1.1;

        eventDetected = false;
        for( int i = 0; i < eventsOld.length; i++ )
        {
            if( eventsOld[i] == -1 && eventsNew[i] == 1 )
            {
                double theta = simpleBisection(i, xOld, tOld, step);
                thetaToX.put(theta, xEvent);

                // consider event only if it is far enough from told
//                if( theta >= EVENT_LOCATION_TOLERANCE )
//                {
                    eventDetected = true;
                    if( theta < thetaEvent )
                    {
                        for( int j = 0; j < i; j++ )
                            events[j] = 0;
                        thetaEvent = theta;
                    }

                    if( theta <= thetaEvent )
                        events[i] = 1;
//                }
            }
        }

        if( eventDetected )
        {
            tEvent = tOld + step * thetaEvent;
            xEvent = thetaToX.get(thetaEvent);
        }
        return eventDetected;
    }

    public double simpleBisection(int i, double[] xOld, double tOld, double step) throws Exception
    {
        double thetaA = 0;
        double thetaB = 1;

        double thetaStarFinal = 1;
        double[] xTemp = new double[xOld.length];

        while( Math.abs(thetaB - thetaA) > EVENT_LOCATION_TOLERANCE )
        {
            double thetaStar = ( thetaB + thetaA ) / 2;

            simulator.integrationStep(xTemp, xOld, tOld, step, thetaStar);

            double event = odeModel.checkEvent(tOld + step * thetaStar, xTemp)[i];

            if( event >= 0 )
            {
                StdMet.copyArray(xEvent, xTemp);
                thetaStarFinal = thetaStar;
                thetaB = thetaStar;
            }
            else
                thetaA = thetaStar;
        }
        return thetaStarFinal;
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
