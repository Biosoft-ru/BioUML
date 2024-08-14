package biouml.plugins.agentmodeling.covid19;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Scenario
{
    public List<Event> events = new ArrayList<Event>();
    public double[] times;
    
    public void addEvent(double time, String variable, double value)
    {
        events.add( new Event(time, variable ,value) );
        Collections.sort( events );
    }
    
    public List<Event> getEvents()
    {        
        return events;
    }
    
    public double[] mobilityLimit;
    public double[] massGatheringsLimit;
    public double[] newICU;
    public double[] newBeds;
    public double[] testMode;
    public double[] testLimits;
    public double[] limit_mass_gathering;
    public double[] newInfected;
    
    public static class Event implements Comparable<Event>
    {
        public double time;
        public double value;
        public String variable; 
        
        public Event(double time, String variable, double value)
        {
            this.time = time;
            this.variable = variable;
            this.value = value;
        }

        @Override
        public int compareTo(Event o)
        {            
            return Double.compare( this.time, o.time );
        }
    }
}