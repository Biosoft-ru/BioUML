package biouml.model.dynamics;

import one.util.streamex.StreamEx;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Transition")
@PropertyDescription("Transition from one state to another.")
public class Transition extends EModelRoleSupport implements ExpressionOwner
{
    public Transition(DiagramElement de)
    {
        super(de);
    }

    @Override
    public String toString()
    {
        String endl = System.getProperty("line.separator");

        StringBuffer buf = new StringBuffer();
        buf.append("Transition " + getDiagramElement().getName() + endl);
        if( when != null )
            buf.append("  when:  " + when + endl);
        if( after != null )
            buf.append("  after: " + after + endl);

        buf.append("  assignments: " + endl);
        for( Assignment element : assignment )
            buf.append("    " + element.variable + " = " + element.math);

        return buf.toString();
    }

    /** Creates transition copy and associate it with specified diagram element. */
    @Override
    public Role clone(DiagramElement de)
    {
        Transition transition = new Transition(de);

        transition.when = when;
        transition.after = after;
        transition.assignment = Assignment.clone(assignment, transition);
        transition.comment = comment;

        return transition;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /**
     * Indicates condition when transition should occur.
     * Mutually exclusive with 'after'.
     */
    protected String when;
    @PropertyName("When")
    @PropertyDescription("Trigger for transition to be performed. <br> Alternative to After.")
    public String getWhen()
    {
        return when;
    }

    public void setWhen(String when)
    {
        String oldValue = this.when;
        this.when = when;
        firePropertyChange("when", oldValue, when);

        if( after != null )
        {
            oldValue = this.after;
            this.after = null;
            firePropertyChange("after", oldValue, after);
        }
    }

    /**
     * Indicates time delay for .
     * Mutually exclusive with 'when'.
     */
    protected String after;
    @PropertyName("After")
    @PropertyDescription("Time after transition should be perfomed. <br> Alternative to When.")
    public String getAfter()
    {
        return after;
    }

    public void setAfter(String after)
    {
        String oldValue = this.after;
        this.after = after;
        firePropertyChange("after", oldValue, after);

        if( when != null )
        {
            oldValue = this.when;
            this.when = null;
            firePropertyChange("when", oldValue, when);
        }
    }

    protected Assignment[] assignment = new Assignment[0];
    @PropertyName("Assignments")
    @PropertyDescription("Assignments while transition is performed.")
    public Assignment[] getAssignments()
    {
        return assignment;
    }
    public Assignment getAssignment(int i)
    {
        return assignment[i];
    }

    public void setAssignments(Assignment[] assignment)
    {
        Assignment[] oldValue = this.assignment;
        if( oldValue != null )
        {
            for( Assignment element : oldValue )
                element.setParent(null);
        }

        this.assignment = assignment;
        if( assignment == null )
            assignment = new Assignment[0];
        else
        {
            for( Assignment element : assignment )
                element.setParent(this);
        }

        firePropertyChange("assignment", oldValue, assignment);
    }

    public void setAssignment(int i, Assignment assignment)
    {
        Assignment oldValue = this.assignment[i];
        oldValue.setParent(null);

        this.assignment[i] = assignment;
        assignment.setParent(this);

        firePropertyChange("assignment", oldValue, assignment);
    }

    public void addAssignment(Assignment ea, boolean fire)
    {
        Assignment[] oldValue = this.assignment;

        assignment = new Assignment[oldValue.length + 1];
        if( oldValue.length > 0 )
            System.arraycopy(oldValue, 0, assignment, 0, oldValue.length);

        assignment[oldValue.length] = ea;
        ea.setParent(this);

        if( fire )
            firePropertyChange("assignment", oldValue, assignment);
    }

    ////////////////////////////////////////////////////////////////////////////
    // ExpressionOwner interface
    //

    @Override
    public boolean isExpression(String propertyName)
    {
        return "when".equals(propertyName) || "after".equals(propertyName) || "assignment".equals(propertyName);
    }

    @Override
    public String[] getExpressions()
    {
        return StreamEx.of( assignment ).map( a -> a.variable + '=' + a.math ).prepend( when == null ? after : when )
                .toArray( String[]::new );
    }
    
    @Override
    public void setExpressions(String[] exps)
    {
    }

    @Override
    public Role getRole()
    {
        return this;
    }
    
    public State getFrom()
    {
        Edge transitionEdge = (Edge)getDiagramElement();
        Role role = transitionEdge.getInput().getRole();
        return (State)role;
    }
    
    public State getTo()
    {
        Edge transitionEdge = (Edge)getDiagramElement();
        Role role = transitionEdge.getOutput().getRole();
        return (State)role;
    }

}
