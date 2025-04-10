package biouml.model.dynamics;

import one.util.streamex.StreamEx;

import ru.biosoft.util.TextUtil2;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * The Assignment structures represent variable assignments which have effect
 * when the event is executed.
 */
@PropertyName("Assignment")
@PropertyDescription("Model variable or parameter assignment.")
public class Assignment extends Option implements ExpressionOwner
{
    
    /** Name of variable whose value should be changed. */
    protected String variable;
    
    /** Expression that used to calculate new value of the variable. */
    protected String math;
    
    public Assignment()
    {
        this.variable = "unknown";
        this.math = "0";
    }

    public Assignment(String variable, String math, Option parent)
    {
        super(parent);
        this.variable = variable;
        this.math = math;
    }

    public Assignment(String variable, String math)
    {
        this.variable = variable;
        this.math = math;
    }

    @PropertyName("Variable")
    @PropertyDescription("Name of variable or parameter.")
    public String getVariable()
    {
        return variable;
    }
    public void setVariable(String variable)
    {
        if( getParent() instanceof Role )
        {
            variable = validateVariableName(variable, ( (Role)getParent() ).getDiagramElement());
            if( variable == null )
                return;
        }
        String oldValue = this.variable;
        this.variable = variable;
        firePropertyChange("variable", oldValue, variable);
    }

    @PropertyName("Expression")
    @PropertyDescription("Expression (formula) which result will be assigned to variable.")
    public String getMath()
    {
        return math;
    }
    public void setMath(String math)
    {
        String oldValue = this.math;
        this.math = math;
        firePropertyChange("math", oldValue, math);
    }

    public Assignment clone(Role parent)
    {
        Assignment assignment = new Assignment(variable, math);

        if( parent != null )
            assignment.setParent((Option)parent);

        return assignment;
    }

    public static Assignment[] clone(Assignment[] assignments, Role parent)
    {
        if( assignments == null )
            return new Assignment[0];

        return StreamEx.of(assignments).map(assignment -> assignment.clone(parent)).toArray(Assignment[]::new);
    }

    ////////////////////////////////////////////////////////////////////////////
    // ExpressionOwner interface
    //

    @Override
    public boolean isExpression(String propertyName)
    {
        return "math".equals(propertyName) || "variable".equals(propertyName);
    }

    @Override
    public String[] getExpressions()
    {
        return new String[] {variable, math};
    }

    @Override
    public void setExpressions(String[] exps)
    {
        setVariable(exps[0]);
        setMath(exps[1]);
    }

    @Override
    public Role getRole()
    {
        return (Role)getParent();
    }

    @Override
    public String toString()
    {
        return "Assignment: " + variable + " = " + math;
    }

    public Assignment(String str)
    {
        str = str.replace("Assignment:", "");
        String[] strings = TextUtil2.split( str, '=' );
        this.variable = strings[0].trim();
        this.math = strings[1].trim();
    }

    static String validateVariableName(String var, DiagramElement de)
    {
        //check syntactic correctness
        if( de == null || var.length() == 0
                || ! ( var.matches("[$]{0,2}[_a-zA-Z](\\w)*([\\x2E][_a-zA-Z](\\w)*)*") || var.matches("[$]\"(.)+\"") ) )
            return null;

        Diagram diagram = Diagram.optDiagram(de);

        if( diagram == null || diagram.getRole() == null || ! ( diagram.getRole() instanceof EModel ) )
            return null;

        EModel emodel = diagram.getRole(EModel.class);

        try
        {
            String qualifiedName = emodel.getQualifiedName(var, de);
            Variable variable = emodel.getVariable(qualifiedName);
            if( variable != null )
                return variable.getName();
        }
        catch( IllegalArgumentException ex )
        {

        }
        if( var.startsWith("$") ) //if variable does not exist in model then we can create only parameter (without "$")
            return null;

        return var;
    }
}
