package biouml.standard.type;

import java.util.Optional;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.FormulaDelegate;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.SerializableAsText;

/** SpecieReference specifies role of molecule involved in the {@link Reaction}. */
@PropertyName("Species reference")
@PropertyDescription("Reference to species involved into the reaction, its role and stoichiometry.")
public class SpecieReference extends GenericEntity implements Relation, FormulaDelegate, SerializableAsText
{   
    // possible role values
    public static final String REACTANT = "reactant";
    public static final String PRODUCT = "product";
    public static final String MODIFIER = "modifier";
    public static final String OTHER = "other";
    public static final String[] specieRoles = {SpecieReference.REACTANT, SpecieReference.PRODUCT, SpecieReference.MODIFIER};
    
    // possible modifier actions
    public static final String ACTION_CATALYSIS = "catalysis";
    public static final String ACTION_INHIBITION = "inhibition";
    public static final String ACTION_MODULATION = "modulation";
    public static final String ACTION_STIMULATION = "stimulation";
    public static final String ACTION_NECCESSARY_STIMULATION = "necessary stimulation";

    public static final String ACTION_CATALYST = "catalyst";
    public static final String ACTION_INHIBITOR = "inhibitor";
    public static final String ACTION_SWITCH_ON = "switch on";
    public static final String ACTION_SWITCH_OFF = "switch off";
    //    public static final String[] modifierActions = {SpecieReference.ACTION_CATALYST, SpecieReference.ACTION_INHIBITOR, SpecieReference.ACTION_SWITCH_ON,
    //            SpecieReference.ACTION_SWITCH_OFF};
    
    public static final String[] modifierActions = {ACTION_CATALYSIS, ACTION_INHIBITION, ACTION_MODULATION, ACTION_STIMULATION,
            ACTION_NECCESSARY_STIMULATION};
    protected static final Logger log = Logger.getLogger(SpecieReference.class.getName());

    protected String specie;
    private String role;
    
    /** Specifies the involved molecule stoichiometry. */
    protected String stoichiometry = "1";
    
    /**Indicates if SpecieReference is under creation and role can be changed by user*/
    boolean isInitialized = true;
    
    /**Specifies the modifier action. This value is needed only if role is MODIFIER, otherwise it should be empty. */
    protected String modifierAction;
    
    private String participation = PARTICIPATION_DIRECT;
    
    public SpecieReference(DataCollection<?> origin, String name)
    {
        this(origin, name, REACTANT);
    }

    public SpecieReference(DataCollection<?> origin, String name, String role)
    {
        super( origin, name );

        this.role = role;
        if( MODIFIER.equals(role) )
            modifierAction = ACTION_CATALYSIS;
    }
    
    public SpecieReference(DataCollection<?> origin, String reactionName, String speciesName, String role)
    {
        this( origin, generateSpecieReferenceName( reactionName, speciesName, role ), role);
    }
    
    public SpecieReference(String descr)
    {
        super(null, "no name yet");
        StringTokenizer tokens = new StringTokenizer(descr, "\t");

        try
        {
            name = tokens.nextToken();

            int index = name.indexOf(": ");
            if( index > 0 )
                title = name.substring(index + 2);
            else
                title = name;

            index = name.indexOf(" as ");
            if( index >= 0 )
                role = name.substring(index + 4);
            if( ( role != null ) && role.equals(MODIFIER) )
                modifierAction = tokens.nextToken();

            name = DiagramUtility.validateName( name );
            specie = tokens.nextToken();
            stoichiometry = tokens.nextToken();
            participation = tokens.nextToken();

            if( tokens.hasMoreTokens() )
                comment = tokens.nextToken();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not parse SpecieReference \"" + descr + "\". Error :" + t, t);
        }
    }

    @Override
    public String getAsText()
    {
        StringBuffer buffer = new StringBuffer(name);

        if( role.equals(MODIFIER) )
            buffer.append("\t" + modifierAction);

        buffer.append("\t" + specie);
        buffer.append("\t" + stoichiometry);
        buffer.append("\t" + participation);

        if( comment != null )
            buffer.append("\t" + comment);

        return buffer.toString();
    }

    @Override
    public SpecieReference clone(DataCollection<?> origin, String name)
    {
        SpecieReference role = new SpecieReference(origin, name);

        // we are not using set methods to avoid changeProperty events generation
        role.title = getTitle();
        role.comment = getComment();
        role.specie = getSpecie();
        role.role = getRole();
        role.stoichiometry = getStoichiometry();
        role.modifierAction = getModifierAction();
        role.participation = getParticipation();
        return role;
    }

    @Override
    public String getType()
    {
        return TYPE_CHEMICAL_ROLE;
    }

    /** Get name of specie element*/
    public String getSpecieName()
    {
        if( specie != null )
        {
            int offset = specie.lastIndexOf('/');
            return ( offset > 0 ) ? specie.substring( offset + 1 ) : specie;
        }
        return null;
    }

    /** Get name of specie element*/
    public Optional<String> optSpecieName()
    {
        if( specie != null )
        {
            int offset = specie.lastIndexOf( '/' );
            if( offset > 0 )
            {
                return Optional.of( specie.substring( offset + 1 ) );
            }
            else
                return Optional.of( specie );
        }
        return Optional.empty();
    }
    
    /** Complete name of specie relative the module. */
    @PropertyName("Variable")
    @PropertyDescription("Name of species involved into the reaction.")
    public String getSpecie()
    {
        return specie;
    }
    public void setSpecie(String specie)
    {
        String oldValue = this.specie;
        this.specie = specie;
        firePropertyChange("specie", oldValue, specie);
    }

    public VariableRole getSpecieVariableRole(Diagram diagram)
    {
        try
        {
            DiagramElement de = diagram.findDiagramElement(getSpecieName());
            return (VariableRole)de.getRole();
        }
        catch( Exception ex )
        {
            return null;
        }
    }

    /**
     * TODO: we should automatically associate SpecieReference to diagram.<br>
     * Is used by {@link SpecieReferenceBeanInfo}
     */
    @PropertyName("Variable")
    @PropertyDescription("Name of specie involved into the reaction.")
    public String getSpecieVariable()
    {
        if( getOrigin() instanceof Compartment )
        {
            Diagram diagram = Diagram.getDiagram((Compartment)getOrigin());

            VariableRole role = getSpecieVariableRole(diagram);
            if( role != null )
                return role.getName();
        }
        return "$" + getSpecieName();
    }

    /**
     * Get name of specie variable in specific variable name mode
     */
    public String getSpecieVariable(Diagram diagram)
    {
        if( ( diagram != null ) && ( diagram.getRole() instanceof EModel ) )
        {
            try
            {
                DiagramElement de = diagram.findDiagramElement(getSpecieName());
                if( ( de != null ) && ( de.getRole() instanceof VariableRole ) )
                {
                    String result = de.getRole(VariableRole.class).getName();
                    int mode = diagram.getViewOptions().getVarNameCode();
                    return diagram.getRole( EModel.class ).getQualifiedName( result, de, mode );
                }
            }
            catch( Exception e )
            {
            }
        }
        return "$" + getSpecieName();
    }

    @PropertyName("Role")
    @PropertyDescription("Role of specie in the reaction.")
    public String getRole()
    {
        return role;
    }
    public void setRole(String role)
    {
        String oldValue = this.role;
        this.role = role;
        if( !isInitialized() )
        {
            String newName = generateSpecieReferenceName( getOrigin().getName(), optSpecieName().orElse( name ), role );
            if( MODIFIER.equals( role ) )
                setModifierAction(ACTION_CATALYSIS);
            setTitle( newName );

        }
        firePropertyChange("role", oldValue, role);
    }

    /**
     * Used in BeanInfo to disable editing of modifierAction property for reactant or product specie.
     */
    public boolean isReactantOrProduct()
    {
        return !MODIFIER.equals(role);
    }

    /**
     * Used in stochastic model template
     */
    public boolean isReactant()
    {
        return REACTANT.equals(role);
    }

    /**
     * Used in stochastic model template
     */
    public boolean isProduct()
    {
        return PRODUCT.equals(role);
    }

    public void setInitialized(boolean initialized)
    {
        this.isInitialized = initialized;
    }
    public boolean isInitialized()
    {
        return isInitialized;
    }

    @PropertyName("Stoichiometry")
    @PropertyDescription("Stoichiometry of involved molecule")
    public String getStoichiometry()
    {
        return stoichiometry;
    }
    public void setStoichiometry(String stoichiometry)
    {
        String oldValue = this.stoichiometry;
        this.stoichiometry = stoichiometry;
        firePropertyChange("stoichiometry", oldValue, stoichiometry);
    }

    @PropertyName ( "Modifier action" )
    @PropertyDescription("Specifies the modifier action.<br>This value is needed only if role is MODIFIER, othervise it should be empty.")
    public String getModifierAction()
    {
        return modifierAction;
    }
    public void setModifierAction(String modifierAction)
    {
        String oldValue = this.modifierAction;
        this.modifierAction = modifierAction;
        firePropertyChange("modifierAction", oldValue, modifierAction);
    }

    @Override
    @PropertyName("Participation")
    @PropertyDescription("Specifies whether the element directly involved into relation.")
    public String getParticipation()
    {
        return participation;
    }
    public void setParticipation(String participation)
    {
        String oldValue = this.participation;
        this.participation = participation;
        firePropertyChange("participation", oldValue, participation);
    }

    // //////////////////////////////////////////////////////////////////////////

    @Override
    public String getFormula()
    {
        if( role.equals(MODIFIER) )
            return "0";

        // check whether rate is defined in reaction
        try
        {
            Reaction reaction = (Reaction)getOrigin();
            if( reaction == null )
                return null;
            String rate = reaction.getFormula();
            if( rate == null || rate.trim().length() == 0 )
                return null;
        }
        catch( Exception e )
        {
        }

        StringBuffer formula = new StringBuffer();

        if( role.equals(REACTANT) )
            formula.append("-");

        formula.append("$$rate_" + getOrigin().getName());

        if( stoichiometry != null && stoichiometry.length() > 0 && !stoichiometry.equals("1") )
            formula.append("*(" + stoichiometry + ")");

        return formula.toString();
    }

    @Override
    public void setFormula(String formula)
    {

    }

    /**
     * Method generates species reference name according to BioUML conventions. 
     * It was changed to avoid symbols not appropriate for SBML and antimony (only literals, numbers and "_" are allowed)
     * It is recommended to call DefaultSemanticController.generateUniqueName method after generating name just in case
     * However it is quite unlikely to create overlapping with names of other diagram elements.
     */
    public static String generateSpecieReferenceName(String reactionName, String specieName, String role)
    {
        return reactionName + "__" + specieName + "_as_" + role;
    }
}
