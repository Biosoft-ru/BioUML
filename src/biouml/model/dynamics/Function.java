package biouml.model.dynamics;

import ru.biosoft.math.model.Utils;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.model.dynamics.util.EModelHelper;
import one.util.streamex.StreamEx;

import java.util.Set;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Wrapper for MathML lambda element. Also corresponds SBML level 2 function element.
 *
 * Function can be defined as MathML lambda element, for example:
 * <pre>
 * &lt;lambda&gt;
 *   &lt;bvar&gt;&lt;ci&gt; x &lt;/ci&gt;&lt;/bvar&gt;
 *   &lt;bvar&gt;&lt;ci&gt; y &lt;/ci&gt;&lt;/bvar&gt;
 *   &lt;apply&gt;
 *     &lt;sin/&gt;
 *     &lt;apply&gt;
 *       &lt;plus/&gt;
 *       &lt;ci&gt; x &lt;/ci&gt;
 *       &lt;cn&gt; y &lt;/cn&gt;
 *     &lt;/apply&gt;
 *   &lt;/apply&gt;
 * &lt;/lambda&gt;
 * </pre>
 *
 * or as text, using linear syntax, for example:
 * <pre> f(x,y) := sin(x+y) </pre>
 */
@PropertyName("Function")
@PropertyDescription("Mathematical function.")
public class Function extends EModelRoleSupport implements ExpressionOwner
{
    private String formula;
    private String name;
    private String rhs = "0";
    private String[] arguments = new String[] {};
    private static final String  DEFAULT_FORMULA = "function math_function() = 0";
    
    public Function(DiagramElement diagramElement, String formula)
    {
        super(diagramElement);
        setFormula( formula, true );
    }
    
    public Function(DiagramElement diagramElement, String name, String[] arguments, String rightHandSide)
    {
        super(diagramElement);
        this.name = name;
        this.rhs = rightHandSide;
        this.arguments = arguments;
        generateFormula();
    }
    
    public Function(DiagramElement diagramElement)
    {
        this(diagramElement, diagramElement == null || diagramElement.getName() == null ? DEFAULT_FORMULA : "function " + diagramElement.getName().replaceAll("-", "_") + "() = 0");
    }
    
    private void generateFormula()
    {
        this.formula = "function " + name + "(" + StreamEx.of(arguments).joining(",") + ")" + " = " + rhs;
    }

    @PropertyName("Name")
    @PropertyDescription("Function name.")
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.setName(name, true);
    }
    
    public void setName(String name, boolean updateModel)
    {
        String oldValue = this.name;
        if( name.equals( this.name ) || !isValidName( name ) )
            return;
        this.name = name;
        generateFormula();
        firePropertyChange( oldValue, null, null );

        if( updateModel )
            updateModel(oldValue, name);
    }
    
    private void updateModel(String oldValue, String newName)
    {
        if (oldValue == null || oldValue.equals( newName ))
            return;
        try
        {
            Diagram diagram = Diagram.getDiagram( getDiagramElement() );
            EModel emodel = diagram.getRole( EModel.class );
            emodel.removeFunction( oldValue );
            emodel.readMath( getFormula(), this );
            new EModelHelper( emodel ).replaceFunction( oldValue, newName );
        }
        catch( Exception ex )
        {

        }
    }

    @PropertyName("Right Hand Side")
    @PropertyDescription("Right Hand Side.")
    public String getRightHandSide()
    {
        return rhs;
    }
    public void setRightHandSide(String rhs)
    {
        updateArgList(rhs);
        this.rhs = rhs;
        generateFormula();        
        firePropertyChange("*", null, null);
    }
    
    @PropertyName("Formula")
    @PropertyDescription("Function formula.")
    public String getFormula()
    {
        return formula;
    }
    public void setFormula(String formula) //TODO: make readonly
    {
        this.setFormula( formula, false );
    }
    public void setFormula(String formula, boolean silent) //TODO: make readonly
    {
        if( formula.equals( this.formula ) )
            return;
        String oldValue = this.formula;
        String name = null;
        String rhs = null;
        String argNames = null;
        try
        {
            name = formula.substring( 9, formula.indexOf( "(" ) ).trim();
            rhs = formula.substring( formula.indexOf( "=" ) + 1 ).trim();
            argNames = formula.substring( formula.indexOf("(") + 1, formula.indexOf( ")" ) );
        }
        catch( Exception ex )
        {
            return;
        }
        String oldName = this.name;
        this.name = name;
        this.rhs = rhs;
        this.formula = formula;
        arguments = argNames.isEmpty()? new String[0]: StreamEx.of( argNames.split( "," )).map( s->s.trim() ).toArray( String[]::new );
        if( !silent )
        {
            updateModel( oldName, name );
            firePropertyChange( "formula", oldValue, formula );
        }
    }

    /** Creates function copy and associate it with specified diagram element. */
    @Override
    public Role clone(DiagramElement de)
    {
        Function f = new Function(de, formula);
        f.comment = comment;
        return f;
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
        return new String[]{ getFormula() };
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
    
    private void updateArgList(String source)
    {
        if (arguments.length == 0)
        {
            arguments = Utils.variables( source ).toArray(String[]::new);
            return;
        }
        Set<String> oldArguments = StreamEx.of( arguments ).toSet();
        String[] addArguments =  Utils.variables( source ).filter( arg->!(oldArguments.contains(arg)) ).toArray( String[]::new );
        String[] newArguments = new String[arguments.length+addArguments.length];
        System.arraycopy( arguments, 0, newArguments, 0, arguments.length );
        System.arraycopy( addArguments, 0, newArguments, arguments.length , addArguments.length );
        arguments = newArguments;
    }

    public String[] getArguments()
    {
        return arguments;
    }
    
    @Override
    public String toString()
    {
        return "Function: "+getFormula();
    }
    
    private boolean isValidName(String var)
    {
        return var.matches("[a-zA-Z][_a-zA-Z0-9]*");
    }
}
