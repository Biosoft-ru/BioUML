package biouml.model.dynamics;

import java.beans.PropertyChangeListener;

import ru.biosoft.math.model.AstStart;
import ru.biosoft.util.WeakPropertyChangeForwarder;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Equation")
@PropertyDescription("Mathematical equation.")
public class Equation extends EModelRoleSupport implements ExpressionOwner
{
    /** Algebraic equation of the form "f(x) = 0" */
    public static final String TYPE_ALGEBRAIC = "algebraic";

    /** Differential equation of the form "dx/dt = f(z)" */
    public static final String TYPE_RATE = "rate";

    /**Scalar equation of the form "x = f(z)" */
    public static final String TYPE_SCALAR = "scalar";

    /** Type of equation considered only at the zero point of the timeline.*/
    public static final String TYPE_INITIAL_ASSIGNMENT = "initial assignment";

    /**
     * Type of initial assignment specially created to set initial value to species with initial quantity type = "concentration"
     * Have a form " species = <initial_concentration> * compartment"
     */
    public static final String TYPE_INITIAL_VALUE = "initial value assignment";

    /**Constant variable.*/
    public static final String TYPE_CONST = "const";

    /** Type of equation/variables that are changed by scalar assignments in events and transitions, but not in equations. */
    public static final String TYPE_SCALAR_CONDITIONAL = "conditional_scalar";

    /**Equations generated for reaction rates, i.e. equations like "$$rate_x = f(y)".*/
    public static final String TYPE_SCALAR_INTERNAL = "scalar_internal";

    /**
     * Auxiliary equation (and variable) created to support expressions in "delay" function. So, expressions "delay(f(x), 10)" is converted
     * into two "delay(x, 10)" and "z = f(x)".
     */
    public static final String TYPE_SCALAR_DELAYED = "scalar_delayed";

    /**Rate equation type that produced by external rule, not by chemical reaction.*/
    public static final String TYPE_RATE_BY_RULE = "rate_by_rule";

    private Variable var = null;
    protected boolean linkedToKernel = true;
    protected String type = TYPE_RATE;
    protected String variable;
    private String formula = "0";
    protected String units;
    private AstStart math;
    protected boolean fast = false;
    private PropertyChangeListener listener;
    
    public Equation(DiagramElement de)
    {
        this(de, TYPE_SCALAR, "unknown", "0");
    }

    public Equation(DiagramElement de, String type, String variable)
    {
        this(de, type, variable, "0");
    }

    public Equation(DiagramElement de, String type, String variable, String formula)
    {
        super(de);

        this.type = type;
        this.variable = variable;
        this.formula = formula;
        initListener(de);
    }

    @Override
    public String toString()
    {
        if (isAlgebraic())
            return "Equation: 0  = " + getFormula() + ", type=" + type + ".\n";
        else if (isInitial())
            return "Equation: " + variable + "(0) = " + getFormula() + ", type=" + type + ".\n";
        return "Equation: " + variable + " = " + getFormula() + ", type=" + type + ".\n";
    }

    /** Creates equation copy and associates it with specified diagram element. */
    @Override
    public Role clone(DiagramElement de)
    {
        Equation eq = new Equation(de, getType(), getVariable(), getFormula());
        eq.fast = fast;
        eq.units = units;
        eq.comment = comment;
        return eq;
    }

    ////////////////////////////////////////////////////////////////////////////
    // ExpressionOwner interface
    //
    
    @Override
    public boolean isExpression(String propertyName)
    {
        return "formula".equals(propertyName) || (! isAlgebraic() &&"variable".equals(propertyName));
    }

    @Override
    public String[] getExpressions()
    {
        return isAlgebraic() ? new String[] {getFormula()} : new String[] {variable, getFormula()};
    }

    @Override
    public void setExpressions(String[] exps)
    {
        if(  isAlgebraic() )
            setFormula(exps[0]);
        else
        {
            String oldVal = this.variable;
            this.variable = exps[0];
            firePropertyChange( "variable", oldVal, this.variable );
            setFormula(exps[1]);
        }
    }

    @Override
    public Role getRole()
    {
        return this;
    }

    /** Utility method used in preprocessors right before Java code generation. it makes diagram unsuitable for further use */
    public void unlinkKernel()
    {
        String formula = getFormula();
        linkedToKernel = false;
        setFormula(formula);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    public boolean isFast()
    {
        return fast;
    }
    public void setFast(boolean fast)
    {
        boolean oldValue = this.fast;
        this.fast = fast;
        firePropertyChange("fast", oldValue, fast);
    }

    @PropertyName("Type")
    @PropertyDescription("Equation type.")
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        String oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    @PropertyName("Variable")
    @PropertyDescription("Variable to be assigned (left-hand side).")
    public String getVariable()
    {
        return variable;
    }
    public void setVariable(String variable)
    {
        variable = validateVariableName(variable);
        if( variable == null )
            return;
        String oldValue = this.variable;
        this.variable = variable;
        firePropertyChange("variable", oldValue, variable);
    }

    public boolean hasDelegate()
    {
        return linkedToKernel && getDiagramElement() != null && getDiagramElement().getKernel() instanceof FormulaDelegate;
    }

    @PropertyName("Equation")
    @PropertyDescription("Equation formula.")
    public String getFormula()
    {
        if( hasDelegate() )
            return ( (FormulaDelegate)getDiagramElement().getKernel() ).getFormula();

        return formula;
    }
    public void setFormula(String formula)
    {
        String oldValue = getFormula();
        if( oldValue != null && oldValue.equals( formula ) )
            return;
        if( hasDelegate() )
            ( (FormulaDelegate)getDiagramElement().getKernel() ).setFormula(formula);
        this.formula = formula;
        math = null; //reset tree
        firePropertyChange("formula", oldValue, formula);
    }

    public String getUnits()
    {
        return units;
    }
    public void setUnits(String units)
    {
        String oldValue = this.units;
        this.units = units;
        firePropertyChange("units", oldValue, units);
    }

    private String validateVariableName(String var)
    {
        return Assignment.validateVariableName(var, getDiagramElement());
    }

    //Ast issues
    public AstStart getMath()
    {
        if (math == null)
            math = initMath();
        return math;
    }

    protected AstStart initMath()
    {
        Role role = Diagram.getDiagram( getDiagramElement()).getRole();
        if( role instanceof EModel )
            return ( (EModel)role ).readMath( getFormula(), this, EModel.VARIABLE_NAME_BY_ID );
        return null;
    }
    
    public boolean isODE()
    {
        return TYPE_RATE.equals(type);
    }
    
    public boolean isAlgebraic()
    {
        return TYPE_ALGEBRAIC.equals(type);
    }
    
    public boolean isAssignment()
    {
        return TYPE_SCALAR.equals(type);
    }
    
    public final boolean isInitial()
    {
        return TYPE_INITIAL_ASSIGNMENT.equals(type);
    }
    
    private void initListener(DiagramElement de)
    {
        if( de != null )
        {
            if( de.getKernel() instanceof FormulaDelegate )
            {
                FormulaDelegate delegate = (FormulaDelegate)de.getKernel();
                listener = pce -> {
                    if( pce.getPropertyName().equals("fast") )
                    {
                        boolean isFast = (Boolean)pce.getNewValue();
                        setFast(isFast);
                        if( getParent() instanceof Node )
                        {
                            Node node = (Node)getParent();
                            node.edges().map(Edge::getRole).select(Equation.class).forEach(eq -> eq.setFast(isFast));
                        }
                    }
                    else if ( pce.getPropertyName().equals("formula"))
                    {
                        math = null;
                    }
                };
                new WeakPropertyChangeForwarder(listener, delegate);
            }
        }
    }
    
    /**
     * Means it is algebraic but in simple form: x_1 = f(x_2,...)
     */
    public static final boolean isScalar(String type)
    {
        return TYPE_SCALAR.equals(type) || TYPE_SCALAR_CONDITIONAL.equals(type) || TYPE_SCALAR_INTERNAL.equals(type)
                || TYPE_SCALAR_DELAYED.equals(type) || TYPE_INITIAL_ASSIGNMENT.equals(type);
    }

    /**
     * Means it is scalar equation which defines reaction kinetik law
     */
    public static final boolean isInternal(String type)
    {
        return TYPE_SCALAR_INTERNAL.equals(type);
    }

    /**
     * Means it is differential equation - either defined as rule or reaction
     */
    public static final boolean isRate(String type)
    {
        return TYPE_RATE.equals(type) || TYPE_RATE_BY_RULE.equals(type);
    }

    
}
