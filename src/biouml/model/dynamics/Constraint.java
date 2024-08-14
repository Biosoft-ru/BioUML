package biouml.model.dynamics;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;
import ru.biosoft.math.model.AstStart;

@PropertyName ( "Constraint" )
@PropertyDescription ( "Mathematical constraint." )
public class Constraint extends EModelRoleSupport implements ExpressionOwner
{
    private String message = "Set message!";
    private String formula = "false";
    private AstStart math;

    public Constraint(DiagramElement de)
    {
        this(de, "false", "");
    }

    public Constraint(DiagramElement de, String formula, String message)
    {
        super(de);
        this.formula = formula;
        this.message = message;
    }

    @Override
    public String toString()
    {
        return "Constraint: " + formula;
    }

    /** Creates equation copy and associates it with specified diagram element. */
    @Override
    public Role clone(DiagramElement de)
    {
        Constraint eq = new Constraint(de, getFormula(), getMessage());
        eq.comment = comment;
        return eq;
    }

    ////////////////////////////////////////////////////////////////////////////
    // ExpressionOwner interface
    //

    @Override
    public boolean isExpression(String propertyName)
    {
        return "formula".equals(propertyName);
    }

    @Override
    public String[] getExpressions()
    {
        return new String[] {getFormula()};
    }

    @Override
    public void setExpressions(String[] exps)
    {
        setFormula(exps[0]);
    }

    @Override
    public Role getRole()
    {
        return this;
    }

    @PropertyName ( "Formula" )
    @PropertyDescription ( "Formula." )
    public String getFormula()
    {
        return formula;
    }
    public void setFormula(String formula)
    {
        String oldValue = getFormula();
        this.formula = formula;
        math = null; //reset tree
        firePropertyChange("formula", oldValue, formula);
    }

    //Ast issues
    public AstStart getMath()
    {
        if( math == null )
            math = initMath();
        return math;
    }

    protected AstStart initMath()
    {
        Role role = Diagram.getDiagram(getDiagramElement()).getRole();
        if( role instanceof EModel )
            return ( (EModel)role ).readMath(getFormula(), this, EModel.VARIABLE_NAME_BY_ID);
        return null;
    }

    @PropertyName ( "Message" )
    @PropertyDescription ( "Message for constraint violation." )
    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        String oldValue = this.getMessage();
        this.message = message;
        firePropertyChange("message", oldValue, message);
    }
}
