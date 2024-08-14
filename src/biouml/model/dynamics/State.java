package biouml.model.dynamics;

import one.util.streamex.StreamEx;
import ru.biosoft.util.TextUtil;
import biouml.model.DiagramElement;
import biouml.model.Role;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
@PropertyName("State")
@PropertyDescription("State. Model may transit between states during simulation.")
public class State extends EModelRoleSupport implements ExpressionOwner
{
    public State(DiagramElement de)
    {
        super(de);
    }
  
    @Override
    public String toString()
    {
        String endl = System.getProperty("line.separator");

        StringBuffer buf = new StringBuffer();
        buf.append("State " + getDiagramElement().getName() + endl);

        if( onEntryAssignment.length > 0 )
        {
            buf.append("  on entry: " + endl);
            for( Assignment assignment : onEntryAssignment )
                buf.append("    " + assignment.variable + " = " + assignment.math);
        }

        if( onEntryAssignment.length > 0 )
        {
            buf.append("  on exit: " + endl);
            for( Assignment assignment : onExitAssignment )
                buf.append("    " + assignment.variable + " = " + assignment.math);
        }

        return buf.toString();
    }

    /** Creates state copy and associate it with specified diagram element. */
    @Override
    public Role clone(DiagramElement de)
    {
        State state = new State(de);

        state.onEntryAssignment = Assignment.clone(onEntryAssignment, state);
        state.onExitAssignment = Assignment.clone(onExitAssignment, state);
        state.comment = comment;
        state.start = start;
        return state;
    }

    protected boolean start;
    @PropertyName("Is start")
    @PropertyDescription("Indicates if state is start.")
    public boolean isStart()
    {
        return start;
    }

    public void setStart(boolean start)
    {
        this.start = start;
    }

    protected Assignment[] onEntryAssignment = new Assignment[0];
    @PropertyName("On entry")
    @PropertyDescription("On entry assignment.")
    public Assignment[] getOnEntryAssignment()
    {
        return onEntryAssignment;
    }
    public Assignment getOnEntryAssignment(int i)
    {
        return onEntryAssignment[i];
    }

    public void setOnEntryAssignment(Assignment[] onEntryAssignment)
    {
        Assignment[] oldValue = this.onEntryAssignment;
        if( oldValue != null )
        {
            for( Assignment assignment : oldValue )
                assignment.setParent(null);
        }

        this.onEntryAssignment = onEntryAssignment;
        if( onEntryAssignment == null )
            onEntryAssignment = new Assignment[0];
        else
        {
            for( Assignment assignment : onEntryAssignment )
                assignment.setParent(this);
        }

        firePropertyChange("onEntryAssignment", oldValue, onEntryAssignment);
    }

    public void setOnEntryAssignment(int i, Assignment onEntryAssignment)
    {
        Assignment oldValue = this.onEntryAssignment[i];
        oldValue.setParent(null);

        this.onEntryAssignment[i] = onEntryAssignment;
        onEntryAssignment.setParent(this);

        firePropertyChange("onEntryAssignment", oldValue, onEntryAssignment);
    }

    public void addOnEntryAssignment(Assignment ea, boolean fireEvent)
    {
        Assignment[] oldValue = this.onEntryAssignment;

        onEntryAssignment = new Assignment[oldValue.length + 1];
        if( oldValue.length > 0 )
            System.arraycopy(oldValue, 0, onEntryAssignment, 0, oldValue.length);

        onEntryAssignment[oldValue.length] = ea;
        ea.setParent(this);

        if( fireEvent )
            firePropertyChange("onEntryAssignment", oldValue, onEntryAssignment);
    }

    protected Assignment[] onExitAssignment = new Assignment[0];
    @PropertyName("On exit")
    @PropertyDescription("On exit assignment.")
    public Assignment[] getOnExitAssignment()
    {
        return onExitAssignment;
    }
    public Assignment getOnExitAssignment(int i)
    {
        return onExitAssignment[i];
    }

    public void setOnExitAssignment(Assignment[] onExitAssignment)
    {
        Assignment[] oldValue = this.onExitAssignment;
        if( oldValue != null )
        {
            for( Assignment assignment : oldValue )
                assignment.setParent(null);
        }

        this.onExitAssignment = onExitAssignment;
        if( onExitAssignment == null )
            onExitAssignment = new Assignment[0];
        else
        {
            for( Assignment assignment : onExitAssignment )
                assignment.setParent(this);
        }

        firePropertyChange("onExitAssignment", oldValue, onExitAssignment);
    }

    public void setOnExitAssignment(int i, Assignment onExitAssignment)
    {
        Assignment oldValue = this.onExitAssignment[i];
        oldValue.setParent(null);

        this.onExitAssignment[i] = onExitAssignment;
        onExitAssignment.setParent(this);

        firePropertyChange("onExitAssignment", oldValue, onExitAssignment);
    }

    public void addOnExitAssignment(Assignment ea, boolean fireEvent)
    {
        Assignment[] oldValue = this.onExitAssignment;

        onExitAssignment = new Assignment[oldValue.length + 1];
        if( oldValue.length > 0 )
            System.arraycopy(oldValue, 0, onExitAssignment, 0, oldValue.length);

        onExitAssignment[oldValue.length] = ea;
        ea.setParent(this);

        if( fireEvent )
            firePropertyChange("onExitAssignment", oldValue, onExitAssignment);
    }

    ////////////////////////////////////////////////////////////////////////////
    // ExpressionOwner interface
    //

    @Override
    public boolean isExpression(String propertyName)
    {
        return "onEntryAssignment".equals(propertyName) || "onExitAssignment".equals(propertyName);
    }

    @Override
    public String[] getExpressions()
    {
        return StreamEx.of( onEntryAssignment ).append( onExitAssignment )
                .map( assignment -> assignment.variable + '=' + assignment.math )
                .toArray( String[]::new );
    }

    @Override
    public void setExpressions(String[] exps)
    {
        int entryLen = onEntryAssignment.length;
        int exitLen = onExitAssignment.length;

        for( int i = 0; i < entryLen; i++ )
        {
            String[] maths = TextUtil.split( exps[i], '=' );
            Assignment assignment = onEntryAssignment[i];
            assignment.setVariable(maths[0]);
            assignment.setMath(maths[1]);
        }

        for( int i = 0; i < exitLen; i++ )
        {
            String[] maths = TextUtil.split( exps[i + entryLen], '=' );
            Assignment assignment = onExitAssignment[i];
            assignment.setVariable(maths[0]);
            assignment.setMath(maths[1]);
        }
    }

    @Override
    public Role getRole()
    {
        return this;
    }

    public String getName()
    {
        return getDiagramElement().getName();
    }

}
