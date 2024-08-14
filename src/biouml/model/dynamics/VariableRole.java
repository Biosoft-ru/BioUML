package biouml.model.dynamics;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.model.dynamics.util.EModelHelper;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;

/**
 * Variable corresponding to entity on diagram (species in SBML)
 */
@PropertyName ( "Variable" )
@PropertyDescription ( "Variable with associated diagram element." )
public class VariableRole extends Variable implements Role
{
    public static final String PREFIX = "$";
    public static final String SCOPE = ".";

    public static final Pattern IDENTIFIER = Pattern.compile("[_a-zA-Z][_0-9a-zA-Z]*([\\x2E][_a-zA-Z][_0-9a-zA-Z]*)*");

    public static final int AMOUNT_TYPE = 0;
    public static final int CONCENTRATION_TYPE = 1;
    public static final int UNDEFINED_TYPE = -1;

    private String shortName;
    private String compartment;

    /**
     * It indicates what means species (variable) identifier when it appears in some model formula: species concentration or amount<br>
     * It also defines initialQuantityType if it is not defined directly<br>
     * Corresponds to SBML <b>hasOnlySubstanceUnits</b> flag.<br>
     * <b>hasOnlySubstanceUnits</b> = true => quantityType = <b>AMOUNT_TYPE</b> (0)<br>
     * <b>hasOnlySubstanceUnits</b> = false (default) => quantityType = <b>CONCENTRATION_TYPE</b> (1)<br>
     */
    protected int quantityType = CONCENTRATION_TYPE;
    protected DiagramElement diagramElement;
    protected boolean boundaryCondition;

    /**
     * Variable that will be used as conversion factor for variable units in rate equations
     * See SBML specification for more details
     */
    private Variable conversionFactor = null;

    /**one VariableRole may be associated with the set of diagram elements (like bus node, for example)*/
    protected DiagramElement[] associatedElements;

    /**Quantity type for output of simulation result*/
    protected int outputQuantityType = AMOUNT_TYPE;

    /**Defines initialValue unit types: concentration or amount
     * Connection with SBML:
     * If SBML species has <b>initialConcentration</b> field then initialQuantityType = CONCENTRATION_TYPE
     * If SBML species has <b>initialAmount field</b> then intiialQuantityType = AMOUNT_TYPE
     * Else initialQuantityType is defined by quanityType*/
    protected int initialQuantityType = AMOUNT_TYPE;

    @Override
    @PropertyName ( "Name" )
    @PropertyDescription ( "Variable name." )
    public String getName()
    {
        return super.getName();
    }

    /** Warning: for internal usage only. */
    public void setShortName(String name)
    {
        if( name.equals(this.shortName) )
            return;

        String oldName = this.name;
        try
        {
            Diagram diagram = Diagram.getDiagram(getDiagramElement());
            EModel emodel = diagram.getRole(EModel.class);
            EModelHelper helper = new EModelHelper(emodel);
            helper.renameVariableRole(oldName, name);
            ( (EModel)getParent() ).getVariableRoles().remove(oldName);
        }
        catch( Exception ex )
        {
        }
    }

    @PropertyName ( "Name" )
    public String getShortName()
    {
        return shortName;
    }

    @PropertyName ( "Compartment" )
    public String getCompartment()
    {
        return compartment;
    }

    /**
     * Returns name of ru.biosoft.access.core.DataElement including all compartment names.
     */
    public static String createName(DiagramElement de, boolean isBrief)
    {
        if( de == null )
            throw new NullPointerException("Diagram element is not defined.");

        String name = de.getName();
        if( !isBrief )
        {
            DiagramElement parent = (DiagramElement)de.getOrigin();
            while( parent != null && ! ( parent instanceof Diagram ) )
            {
                name = ( parent.getRole() != null ? parent.getRole().getDiagramElement().getName() : parent.getName() ) + SCOPE + name;
                parent = (DiagramElement)parent.getOrigin();
            }
        }

        // check, whether we should quote the string
        if( !IDENTIFIER.matcher(name).matches() )
            name = "\"" + name + "\"";

        name = PREFIX + name;
        return name;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public VariableRole(DataCollection<?> origin, DiagramElement diagramElement, double initialValue) // initialValue == -1 if it was not determine in diagram
    {
        super(createName(diagramElement, false), origin);
        this.initialValue = initialValue;
        this.diagramElement = diagramElement;
        setParent(diagramElement);
        generatePath(name);
    }

    private void generatePath(String name)
    {
        name = name.replace("$", "");

        int index = name.lastIndexOf(SCOPE);
        this.shortName = name.substring(index + 1);
        if( shortName.endsWith("\"") )
            shortName = "\"" + shortName;

        if( index == -1 )
            compartment = "";
        else
        {
            this.compartment = name.substring(0, index);
            if( compartment.startsWith("\"") )
                compartment = compartment + "\"";
        }
    }

    public VariableRole(DiagramElement diagramElement)
    {
        this(diagramElement, 0.0);
    }

    /**
     * @pending remove it. Just now it is used DynamicModelTest.
     */
    public VariableRole(DiagramElement diagramElement, double initialValue) // initialValue == -1 if it was not determine in diagram
    {
        this((DataCollection<?>)null, diagramElement, initialValue);
    }

    /** For internal usage. */
    public VariableRole(String name, DataCollection<?> origin, double initialValue) // initialValue == -1 if it was not determine in diagram
    {
        super(name, origin);
        this.initialValue = initialValue;
    }

    public VariableRole(String name, DiagramElement diagramElement) // initialValue == -1 if it was not determine in diagram
    {
        super(name, null);
        this.diagramElement = diagramElement;
    }

    public VariableRole(String name, DataCollection<?> origin)
    {
        super(name, origin);
    }

    /** Creates variable copy and associate it with specified diagram element.
     * Important: variable name is preserved (unlike the case when we create new variable role for given diagram element)
     * It is used widely while composite model preprocessing
     */
    @Override
    public Role clone(DiagramElement de)
    {
        return clone(de, getName());
    }

    /** Creates variable copy and associate it with specified diagram element. */
    public VariableRole clone(DiagramElement de, String newName)
    {
        VariableRole var = (VariableRole)super.clone(newName);
        var.setParent(de);
        var.generatePath(newName);
        var.diagramElement = de;
        var.associatedElements = new DiagramElement[] {var.diagramElement};
        return var;
    }

    @Override
    public VariableRole clone(String newName)
    {
        VariableRole clone = (VariableRole)super.clone(newName);
        clone.associatedElements = new DiagramElement[] {clone.diagramElement};
        generatePath(newName);
        return clone;
    }

    /**
     * Get name in specific variable name mode
     * Is used by {@link VariableRoleBeanInfo}
     */
    public String getTitledName()
    {
        String result = getName();
        if( diagramElement != null )
        {
            Diagram diagram = Diagram.optDiagram(diagramElement);
            if( ( diagram != null ) && ( diagram.getRole() instanceof EModel ) )
            {
                int mode = diagram.getViewOptions().getVarNameCode();
                return diagram.getRole( EModel.class ).getQualifiedName( result, diagramElement, mode );
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        String str = "VariableRole: " + name + "=" + initialValue + " " + units;
        if( boundaryCondition )
            str += "(b)";

        if( comment != null )
            str += str + "; " + comment;

        str += "; de=" + diagramElement;
        return str;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    @Override
    public DiagramElement getDiagramElement()
    {
        return diagramElement;
    }

    @Override
    @PropertyName ( "Title" )
    public String getTitle()
    {
        if( diagramElement == null )
            return "";
        return diagramElement.getTitle();
    }
    public void setTitle(String title)
    {
        if( diagramElement != null )
            diagramElement.setTitle(title);
    }

    public @Nonnull DiagramElement[] getAssociatedElements()
    {
        if( associatedElements == null )
            associatedElements = new DiagramElement[] {diagramElement};
        return associatedElements;
    }

    public void addAssociatedElement(DiagramElement de)
    {
        DiagramElement[] old = getAssociatedElements();
        if( !StreamEx.of(old).has(de) )
            associatedElements = StreamEx.of(old).append(de).toArray(DiagramElement[]::new);
    }

    public boolean removeAssociatedElement(DiagramElement de)
    {
        DiagramElement[] old = getAssociatedElements();
        if( ( old.length == 1 ) && ( old[0] ) == de )
        {
            return true;
        }
        else
        {
            associatedElements = StreamEx.of(old).without(de).toArray(DiagramElement[]::new);
            //if remove diagramElement
            if( getDiagramElement() == de )
                diagramElement = associatedElements[0];
            return false;
        }
    }

    @PropertyName ( "Boundary condition" )
    @PropertyDescription ( "Boundary condition determines whether the variable value is fixed"
            + "<br>or variable over the course of a simulation" )
    public boolean isBoundaryCondition()
    {
        return boundaryCondition;
    }
    public void setBoundaryCondition(boolean boundaryCondition)
    {
        boolean oldValue = this.boundaryCondition;
        this.boundaryCondition = boundaryCondition;
        firePropertyChange("boundaryCondition", oldValue, boundaryCondition);
    }

    @PropertyName ( "Initial units type" )
    @PropertyDescription ( "Initial quantity type." )
    public int getInitialQuantityType()
    {
        return initialQuantityType;
    }
    public void setInitialQuantityType(int quantityType)
    {
        int oldValue = this.initialQuantityType;
        this.initialQuantityType = quantityType;
        firePropertyChange("initialQuantityType", oldValue, quantityType);
    }

    @PropertyName ( "Output units type" )
    @PropertyDescription ( "Units type in simulation result." )
    public int getOutputQuantityType()
    {
        return outputQuantityType;
    }
    public void setOutputQuantityType(int quantityType)
    {
        int oldValue = this.outputQuantityType;
        this.outputQuantityType = quantityType;
        firePropertyChange("outputQuantityType", oldValue, quantityType);
    }

    @PropertyName ( "Units type" )
    @PropertyDescription ( "Units type." )
    public int getQuantityType()
    {
        return quantityType;
    }
    public void setQuantityType(int quantityType)
    {
        int oldValue = this.quantityType;
        this.quantityType = quantityType;
        firePropertyChange("quantityType", oldValue, quantityType);
    }

    public String getConversionFactor()
    {
        if( conversionFactor == null )
            return null;
        return conversionFactor.getName();
    }
    public void setConversionFactor(String variableName)
    {
        if( variableName == null )
        {
            conversionFactor = null;
            return;
        }
        Diagram diagram = Diagram.getDiagram(this.getDiagramElement());
        EModel role = diagram.getRole(EModel.class);
        Variable variable = role.getVariable(variableName);
        if( variable == null )
            throw new IllegalArgumentException("Can not found parameter " + variableName + " in model");
        if( variable instanceof VariableRole || !variable.isConstant() )
            throw new IllegalArgumentException("Incorrect conversion factor, it should be model parameter with set constant = true");
        conversionFactor = variable;
    }

    public boolean isCompartment()
    {
        return getDiagramElement() != null && getDiagramElement().getKernel() instanceof biouml.standard.type.Compartment;
    }

}
