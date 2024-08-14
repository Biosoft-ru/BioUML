package biouml.model.dynamics;

import one.util.streamex.StreamEx;

import biouml.model.DiagramElement;
import biouml.model.Role;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
@PropertyName("Event")
@PropertyDescription("Event describe the time and form of explicit instantaneous <br>"
        + "discontinuous state changes in the model. For example, <br>"
        + "an event may describe that one species concentration <br>"
        + "is halved when another species concentration exceeds " + "a given threshold value.")
public class Event extends EModelRoleSupport implements ExpressionOwner
{
    /**
     * Delay property defines the length of time after the event has fired that the event is executed.
     * This String should be evaluated when the rule is fired. The value of the delay
     * String should always be positive.
     *
     * This is optional property, its default value is 0.
     */
    protected String delay = "0";
    
    /**
     * Trigger property. Event is firing when trigger makes the transition from false to true.
     * The event will fire at any further time points when the trigger make this transition.
     */
    protected String trigger = "false";
    
    /**
     * Determines the units of time that apply to the delay.
     * If not set, the units are taken from the defaults defined by the built-in "time".
     */
    protected String timeUnits = "time";
    
    /**
     * Determines what values should be used while performing assignments:<br>
     * if <b>true</b> then will use values calculated at the time when trigger became true<br>
     * if <b>false</b> then will use values calculated at the time when assignments should be executed<br>
     * <b>Note:</b> it matters only if event has delay!<br>
     * Default value in BioUML is true (as in SBML l2v4)<br>
     */
    protected boolean useValuesFromTriggerTime = true;
    
    private String triggerMessage = "";
    
    public Event(DiagramElement de)
    {
        super(de);
        addEventAssignment(new Assignment("unknown", "0"), false);
    }

    public Event(DiagramElement de, String trigger, String delay, Assignment[] actions)
    {
        super(de);

        this.trigger = transformSingleEquals(trigger);
        if( delay != null && !delay.isEmpty() )
            this.delay = delay;

        if( actions != null )
        {
            this.eventAssignment = actions;
            for (Assignment element : eventAssignment)
                element.setParent(this);
        }

    }

    @PropertyName("Trigger")
    @PropertyDescription("Trigger defines condition for event occuring.")
    public String getTrigger()
    {
        return trigger;
    }
    public void setTrigger(String trigger)
    {
        trigger = transformSingleEquals(trigger);
        String oldValue = this.trigger;
        this.trigger = trigger;
        firePropertyChange("trigger", oldValue, trigger);
    }
     
    @PropertyName("Trigger message")
    @PropertyDescription("Message displaying when event is triggered.")
    public String getTriggerMessage()
    {
        return triggerMessage;
    }
    public void setTriggerMessage(String message)
    {
        this.triggerMessage = message;
    }

    /**
     * transforms single "=" into "==",
     * e.g.: "x=y || t==6 " => "x==y || t==6"
     * Important: it should leave next sequences unchanged: !=, <=, >=, =>. 
     * TODO: find better way to parse triggers, for now Parser allows expressions like "x = 5 || y == 6" (but not "x == 5 || y = 6")
     * @param trigger
     * @return transformed trigger
     */
    private String transformSingleEquals(String trigger)
    {
        return trigger == null ? "false" : trigger.replaceAll("(?<![!<>=])=(?!=|>)", "==");
    }

    @PropertyName("Delay")
    @PropertyDescription("Delay is optional field defining time delay after <br>"+
             "which event action (assignments) will be executed if event has occurred.")
    public String getDelay()
    {
        return delay;
    }
    public void setDelay(String delay)
    {
        if( delay == null || delay.isEmpty() )
            delay = "0";
        String oldValue = this.delay;
        this.delay = delay;
        firePropertyChange("delay", oldValue, delay);
    }


    @PropertyName("Delay time units")
    @PropertyDescription("Delay time units is optional field that defines <br>" +
              "the units of time that apply to the delay field.")
    public String getTimeUnits()
    {
        return timeUnits;
    }
    public void setTimeUnits(String units)
    {
        String oldValue = this.timeUnits;
        this.timeUnits = units;
        firePropertyChange("timeUnits", oldValue, timeUnits);
    }


    @PropertyName("Use trigger time values")
    @PropertyDescription("Defines what values should be used during event assignments execution: <br>"
                                        +"If true then trigger time values <br>"
                                        +"If false then assignment execution time values.")
    public boolean isUseValuesFromTriggerTime()
    {
        return this.useValuesFromTriggerTime;
    }
    public void setUseValuesFromTriggerTime(boolean useValuesFromTriggerTime)
    {
        boolean oldValue = this.useValuesFromTriggerTime;
        this.useValuesFromTriggerTime = useValuesFromTriggerTime;
        firePropertyChange("useValuesFromTriggerTime", oldValue, useValuesFromTriggerTime);
    }


    /**
     * Expression, which value defines the order of event execution:<br>
     * Higher priority means earlier execution.<br>
     * Expression evaluating should be performed at the event execution moment.<br>
     * This is optional property, default value in BioUML is "", SBML does not specify events order in the case of undefined priority
     * Added in SBML l.3 v.1
     */
    protected String priority = "";
    @PropertyName("Priority")
    @PropertyDescription("Optional field, defines events execution order.")
    public String getPriority()
    {
        return this.priority;
    }
    public void setPriority(String priority)
    {
        String oldValue = this.priority;
        this.priority = priority;
        firePropertyChange("priority", oldValue, priority);
    }
  
   
    /**
     * Defines trigger value BEFORE simulation started.
     * Therefore, "false" value means that if trigger at the start of simulation
     * is calculated as "true" then event should be triggered at the start of simulation (as it passed from "false" to "true").<br>
     * <b>Example</b>:<br>
     * if trigger = "time = 0" and simulation starts with time=0, and triggerInitialValue = "false" then event will be triggered at the start of simulation<br>
     * Default value in BioUML is true, in SBML it should be stated implicitly.<br>
     * Added in SBML l.3 v.1
     */
    protected boolean triggerInitialValue = true;
    @PropertyName( "Trigger initial value")
    @PropertyDescription ( "Defines event condition before model initial time, if false then event may occur at the initial time point." )
    public boolean isTriggerInitialValue()
    {
        return this.triggerInitialValue;
    }
    public void setTriggerInitialValue(boolean triggerInitialValue)
    {
        boolean oldValue = this.triggerInitialValue;
        this.triggerInitialValue = triggerInitialValue;
        firePropertyChange("triggerInitialValue", oldValue, triggerInitialValue);
    }


    
    /**
     * If "false" then if in the time interval between event triggers and it is executed, event trigger passes from "true" to "false" then event should not be executed
     * It may happens because of time delay or other events interference<br>
     * If "true" then once event trigger became "true" it should not be re-checked before event is executed.
     * Default value in BioUML is true, in SBML it should be stated implicitly.<br>
     * Added in SBML l.3 v.1
     */
    protected boolean triggerPersistent = true;
    @PropertyName("Persistent trigger")
    @PropertyDescription ( "If true then event trigger should not be rechecked after it triggers and before assignments are executed." )
    public boolean isTriggerPersistent()
    {
        return this.triggerPersistent;
    }
    public void setTriggerPersistent(boolean triggerPersistent)
    {
        boolean oldValue = this.triggerPersistent;
        this.triggerPersistent = triggerPersistent;
        firePropertyChange("triggerPersistent", oldValue, triggerPersistent);
    }

    protected Assignment[] eventAssignment = new Assignment[0];
    @PropertyName("Assignments")
    @PropertyDescription("Event action is specified as a set of assignemnts for model variables or parameters.")
    public Assignment[] getEventAssignment()
    {
        return eventAssignment;
    }
    public Assignment getEventAssignment(int i)
    {
        return eventAssignment[i];
    }

    public void setEventAssignment(Assignment[] eventAssignment)
    {
        Assignment[] oldValue = this.eventAssignment;
        if( oldValue != null )
        {
            for (Assignment element : oldValue)
                element.setParent(null);
        }

        this.eventAssignment = eventAssignment;
        if( eventAssignment == null )
            eventAssignment = new Assignment[0];
        else
        {
            for (Assignment element : eventAssignment)
                element.setParent(this);
        }

        firePropertyChange("eventAssignment", oldValue, eventAssignment);
    }

    public void setEventAssignment(int i, Assignment eventAssignment)
    {
        Assignment oldValue = this.eventAssignment[i];
        oldValue.setParent(null);
        this.eventAssignment[i] = eventAssignment;
        eventAssignment.setParent(this);
        firePropertyChange("eventAssignment", oldValue, eventAssignment);
    }

    public void addEventAssignment(Assignment ea, boolean fireEvent)
    {
        Assignment[] oldValue = this.eventAssignment;
        eventAssignment = new Assignment[oldValue.length + 1];
        if( oldValue.length > 0 )
            System.arraycopy(oldValue, 0, eventAssignment, 0, oldValue.length);
        eventAssignment[oldValue.length] = ea;
        ea.setParent(this);
        if( fireEvent )
            firePropertyChange("eventAssignment", oldValue, eventAssignment);
    }

    public void clearAssignments(boolean fireEvent)
    {
        Assignment[] oldValue = this.eventAssignment;
        eventAssignment = new Assignment[0];
        if( fireEvent )
            firePropertyChange("eventAssignment", oldValue, eventAssignment);
    }

    @Override
    public String toString()
    {
        String endl = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        buf.append("Event " + endl);
        buf.append("  trigger: " + trigger + endl);
        buf.append("  delay  : " + delay + endl);
        buf.append("  assignments: " + endl);
        for (Assignment element : eventAssignment)
            buf.append("    " + element.variable + " = " + element.math);
        return buf.toString();
    }

    /** Creates event copy and associate it with specified diagram element. */
    @Override
    public Role clone(DiagramElement de)
    {
        Event event = new Event(de, trigger, delay, null);
        event.comment = comment;
        event.triggerInitialValue = triggerInitialValue;
        event.triggerPersistent = triggerPersistent;
        event.useValuesFromTriggerTime = useValuesFromTriggerTime;
        event.timeUnits = timeUnits;
        event.priority = priority;
        event.eventAssignment = Assignment.clone(eventAssignment, event);
        event.triggerMessage = triggerMessage;
        return event;
    }

    ////////////////////////////////////////////////////////////////////////////
    // ExpressionOwner interface
    //

    @Override
    public boolean isExpression(String propertyName)
    {
        return "trigger".equals(propertyName) || "delay".equals(propertyName) || "eventAssignment".equals(propertyName);
    }

    @Override
    public String[] getExpressions()
    {
        return StreamEx.of( eventAssignment ).map( assignment -> assignment.variable + '=' + assignment.math )
                .prepend( trigger, delay, priority ).toArray( String[]::new );
    }

    @Override
    public void setExpressions(String[] exps)
    {
        int len = eventAssignment.length;
        setTrigger(exps[0]);
        setDelay(exps[1]);
        setPriority(exps[2]);
        for( int i = 0; i < len; i++ )
        {
            String exp = exps[3+i];
            int index = exp.indexOf("=");
            String leftHandSide  = exp.substring(0, index);
            String rightHandSide  = exp.substring(index+1, exp.length());
            eventAssignment[i].setVariable(leftHandSide.trim());
            eventAssignment[i].setMath(rightHandSide.trim());
        }
    }

    @Override
    public Role getRole()
    {
        return this;
    }
}
