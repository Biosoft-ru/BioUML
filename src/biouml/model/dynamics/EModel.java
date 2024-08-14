package biouml.model.dynamics;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.resources.MessageBundle;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.type.Unit;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.ParserContext;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.math.parser.Parser;
import ru.biosoft.util.DataCollectionDynamicPropertySet;
import ru.biosoft.util.HtmlUtil;
import ru.biosoft.util.LazyValue;

/**
 * Generally this role should be associated with {@link Diagram}.
 *
 * @pending high - parameters update
 */
@SuppressWarnings ( "serial" )
@PropertyName ( "Executable model" )
@PropertyDescription ( "Executable model (system of ordinary differential equations)" )
public class EModel extends EModelRoleSupport implements DataCollectionListener, PropertyChangeListener, ParserContext
{
    private static final Logger log = Logger.getLogger(EModel.class.getName());

    //variable name modes
    public static final int VARIABLE_NAME_BY_ID = 0;
    public static final int VARIABLE_NAME_BY_TITLE = 1;
    public static final int VARIABLE_NAME_BY_TITLE_BRIEF = 2;
    public static final int VARIABLE_NAME_BY_ID_BRIEF = 3;//special type for reaction formulas (not available in preferences)

    public static final int VARIABLE_NAME_BY_SHORT_ID = 4;

    //EModel types
    public static final int STATIC_TYPE = 0;
    public static final int STATIC_EQUATIONS_TYPE = 1;
    public static final int ODE_TYPE = 2;
    public static final int ODE_DELAY_TYPE = 4;
    public static final int ALGEBRAIC_TYPE = 8;
    public static final int EVENT_TYPE = 16;
    public static final int STATE_TRANSITION_TYPE = 32;

    public final static String CONVERSION_FACTOR_UNDEFINED = "";

    protected DataCollection<Variable> variables;
    DiagramVariableResolver resolver;

    private Variable conversionFactor = null;

    protected DataCollectionDynamicPropertySet varsSet;
    protected HashMap<String, Object> constantsMap = new HashMap<>();
    protected HashMap<String, ru.biosoft.math.model.Function> functionsMap = new HashMap<>();
    private Map<String, Unit> units;

    protected Role parsedRole;
    protected Parser parser;

    private State initialState;

    protected DataElement elementToRemove = null;

    private boolean autodetectTypes = true;

    public EModel(DiagramElement diagramElement)
    {
        super(diagramElement);

        DefaultParserContext.declareStandardConstants(this);
        DefaultParserContext.declareStandardOperators(this);

        Diagram diagram = (Diagram)diagramElement;
        diagram.addPropertyChangeListener(this);
        diagram.addDataCollectionListener(this);

        try
        {
            variables = new VariablesDataCollection(null, diagram);
            varsSet = new DataCollectionDynamicPropertySet(variables);
            varsSet.setParent(this);
            units = new HashMap<>();
            variables.addDataCollectionListener(new DataCollectionListenerSupport()
            {
                @Override
                public void elementAdded(DataCollectionEvent e) throws Exception
                {
                    if( resolver != null && e.getDataElement() instanceof Variable )
                        resolver.addVariable((Variable)e.getDataElement());
                }

                @Override
                public void elementChanged(DataCollectionEvent e) throws Exception
                {
                    resolver = null;
                }

                @Override
                public void elementRemoved(DataCollectionEvent e) throws Exception
                {
                    resolver = null;
                }
            });

        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Error during variables parsing: " + t.getMessage(), t);
        }
        declareFunction(new PredefinedFunction("delay", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2));
        declareFunction(new PredefinedFunction("rateOf", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1));
        declareFunction(new PredefinedFunction("random", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 0));
        declareFunction(new PredefinedFunction("binomial", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2));
        declareFunction(new PredefinedFunction("uniform", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2));
        declareFunction(new PredefinedFunction("normal", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2));
        declareFunction( new PredefinedFunction( "round", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1 ) );
        declareFunction(new PredefinedFunction("logNormal", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2));
        declareVariable("time", 0.0);
        resolver = new DiagramVariableResolver(VARIABLE_NAME_BY_ID);
    }

    @Override
    public String toString()
    {
        return "Executable model";
    }

    /**
     * Creates EModel copy and associate it with specified diagram element.
     * <br/>
     *
     * @pending - initialState
     * @pending - events
     * @pending - transitions
     */
    @Override
    public Role clone(DiagramElement de)
    {
        EModel emodel = new EModel(de);
        doClone(emodel);
        return emodel;
    }

    protected void doClone(EModel emodel)
    {
        emodel.comment = comment;
        emodel.conversionFactor = conversionFactor;

        Diagram diagram = emodel.getDiagramElement();

        adjustBuses(diagram);

        DataCollection<Variable> destVariables = emodel.getVariables();
        for( Variable var : getVariables() )
        {
            try
            {
                if( var instanceof VariableRole && diagram != null )
                {
                    VariableRole oldVar = (VariableRole)var;
                    DiagramElement mainDe = oldVar.getDiagramElement();
                    DiagramElement newMainDe = diagram.findNode(mainDe.getCompleteNameInDiagram());
                    VariableRole mainVariable = newMainDe.getRole(VariableRole.class);

                    /** If several de's have the same variable as a role - after cloning new roles will be automatically created for them.
                     We should restore it according to the state in initial diagram (which we are cloning)
                     */
                    StreamEx.of(oldVar.getAssociatedElements()).map(de -> diagram.findNode(de.getCompleteNameInDiagram())).nonNull()
                            .forEach(de -> {
                                de.setRole(mainVariable);
                                mainVariable.addAssociatedElement(de);
                            });

                    newMainDe.setRole(mainVariable);
                    emodel.put(mainVariable);
                }
                else
                {
                    Variable cloned = (Variable)var.clone();
                    cloned.setParent(emodel);
                    destVariables.put(cloned);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,
                        "Can not clone variable " + var.getName() + ", model=" + getDiagramElement().getName() + ", error: " + e, e);
            }
        }

        for( Unit unit : units.values() )
            emodel.addUnit(unit.clone(unit.getOrigin(), unit.getName()));

        // variableRoles will be added automatically when DiagramElements will be added to diagram

        EntryStream.of(functionsMap).forEach(entry -> emodel.functionsMap.putIfAbsent(entry.getKey(), entry.getValue()));
    }

    /**
     * After bus nodes ar cloned - each of them now have separate role
     * This method sets the same role for all associated bus nodes
     * @param diagram
     */
    private void adjustBuses(Diagram diagram)
    {
        //clone buses
        //TODO: do this somewhere else
        Map<Bus, List<Node>> clusters = DiagramUtility.getBuses(getDiagramElement()).filter(n -> n.getRole() instanceof Bus)
                .groupingBy(n -> (Bus)n.getRole());
        for( Entry<Bus, List<Node>> cluster : clusters.entrySet() )
        {
            Node selected = cluster.getValue().get(0);
            Node selectedCopy = diagram.findNode(selected.getCompleteNameInDiagram());
            if( selectedCopy == null )
                break;
            Bus newBus = (Bus)cluster.getKey().clone(selectedCopy);
            for( Node node : cluster.getValue() )
            {
                Node copy = diagram.findNode(node.getCompleteNameInDiagram());
                copy.setRole(newBus);
                newBus.addNode(copy);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // ParserContext implementation
    //

    @Override
    public boolean containsConstant(String name)
    {
        return constantsMap.containsKey(name);
    }

    @Override
    public Object getConstantValue(String name)
    {
        return constantsMap.get(name);
    }

    @Override
    public void declareConstant(String name, Object value)
    {
        constantsMap.put(name, value);
    }

    @Override
    public void removeConstant(String name)
    {
        constantsMap.remove(name);
    }
    //////////////////////////////////////

    @Override
    public ru.biosoft.math.model.Function getFunction(String name)
    {
        if( name.startsWith("\"") )
            name = name.substring(1, name.length() - 1);

        return functionsMap.get(name);
    }

    public void removeFunction(String name)
    {
        functionsMap.remove(name);
    }

    @Override
    public void declareFunction(ru.biosoft.math.model.Function function)
    {
        functionsMap.put(function.getName(), function);
    }

    public DataCollection<Variable> getVariables()
    {
        return variables;
    }

    public DynamicPropertySet getVars()
    {
        return varsSet;
    }

    @Override
    public boolean containsVariable(String name)
    {
        return getVariable(name) != null;
    }

    /**
     * Returns value of variable with the specified name or null
     * if variable is not declared.
     */
    @Override
    public Object getVariableValue(String name)
    {
        try
        {
            Variable variable = getVariable(name);
            if( variable != null )
                return variable.getInitialValue();
        }
        catch( Throwable t )
        {
            throw new RuntimeException(t);
        }

        return null;
    }

    /**
     * Declares the variable (parameter).
     *
     * @param name - parameter name
     * @param value - parameter value, should be instance of <code>Double</code>.
     */
    @Override
    public void declareVariable(String name, Object value)
    {
        try
        {
            if( name.startsWith("$") )
                return;

            Variable variable = new Variable(name, this, variables);
            if( "time".equals(name) )
                variable.setType(Variable.TYPE_TIME);

            variable.setInitialValue( ( (Double)value ).doubleValue());
            variables.put(variable);
        }
        catch( Throwable t )
        {
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean canDeclare(String name)
    {
        return !name.startsWith("$");
    }
    /** Adds variable or parameter to the context. */
    public void put(Variable variable)
    {
        try
        {
            variables.put(variable);
        }
        catch( Throwable t )
        {
            throw new RuntimeException(t);
        }
    }

    /**
     * All direct access to variables by name should go through this method
     * @param name
     * @return
     */
    public Variable getVariable(String name)
    {
        try
        {
            Variable result = variables.get(name);
            if( result == null )
                result = variables.get("$" + name);
            if( result == null )
                result = variables.get("$\"" + name + "\"");
            if( result == null && name.length() > 2 )
                result = variables.get("$" + name.substring(2, name.length() - 1));
            if( result == null )
                result = variables.get("$\"" + name.substring(1) + "\"");
            return result;
        }
        catch( Throwable t )
        {
            return null;
        }
    }
    ///////////////////////////////////////////////////////////////////
    // VariableRoles and Parameters as a FilteredDataCollections
    //

    protected final LazyValue<DataCollection<VariableRole>> variableRoles = new LazyValue<DataCollection<VariableRole>>("variableRoles")
    {
        @Override
        protected DataCollection<VariableRole> doGet() throws Exception
        {
            Properties props = new Properties();
            props.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, variables);
            props.put(DataCollectionConfigConstants.NAME_PROPERTY, variables.getName() + ": roles");
            props.put(DataCollectionConfigConstants.FILTER_PROPERTY, new VariableFilter(true));
            return new FilteredDataCollection<>(null, props);
        }
    };

    protected final LazyValue<DataCollection<VariableRole>> entityRoles = new LazyValue<DataCollection<VariableRole>>("entityRoles")
    {
        @Override
        protected DataCollection<VariableRole> doGet() throws Exception
        {
            Properties props = new Properties();
            props.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, variables);
            props.put(DataCollectionConfigConstants.NAME_PROPERTY, variables.getName() + ": roles");
            props.put(DataCollectionConfigConstants.FILTER_PROPERTY, new CompartmentFilter(false));
            return new FilteredDataCollection<>(null, props);
        }
    };

    protected final LazyValue<DataCollection<VariableRole>> compartmentRoles = new LazyValue<DataCollection<VariableRole>>(
            "compartmentRoles")
    {
        @Override
        protected DataCollection<VariableRole> doGet() throws Exception
        {
            Properties props = new Properties();
            props.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, variables);
            props.put(DataCollectionConfigConstants.NAME_PROPERTY, variables.getName() + ": roles");
            props.put(DataCollectionConfigConstants.FILTER_PROPERTY, new CompartmentFilter(true));
            return new FilteredDataCollection<>(null, props);
        }
    };


    //TODO: remove variableRoles
    public DataCollection<VariableRole> getVariableRoles()
    {
        return variableRoles.get();
    }

    public DataCollection<VariableRole> getCompartmentRoles()
    {
        return compartmentRoles.get();
    }

    public DataCollection<VariableRole> getEntityRoles()
    {
        return entityRoles.get();
    }

    protected final LazyValue<DataCollection<Variable>> parameters = new LazyValue<DataCollection<Variable>>("parameters")
    {
        @Override
        protected DataCollection<Variable> doGet() throws Exception
        {
            Properties props = new Properties();
            props.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, variables);
            props.put(DataCollectionConfigConstants.NAME_PROPERTY, variables.getName() + ": parameters");
            props.put(DataCollectionConfigConstants.FILTER_PROPERTY, new VariableFilter(false));
            return new FilteredDataCollection<>(null, props);
        }
    };
    public DataCollection<Variable> getParameters()
    {
        return parameters.get();
    }

    public static class VariableFilter implements Filter<DataElement>
    {
        private final boolean isVariableRole;

        public VariableFilter(boolean isVariableRole)
        {
            this.isVariableRole = isVariableRole;
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }

        @Override
        public boolean isAcceptable(DataElement de)
        {
            // hide internal variables
            if( de.getName().startsWith("$$") )
                return false;

            //hide variables from parameter list
            if( !isVariableRole && de.getName().startsWith("$") )
                return false;

            return isVariableRole == ( de instanceof VariableRole );
        }
    }

    public static class CompartmentFilter implements Filter<DataElement>
    {
        private boolean findCompartments;

        public CompartmentFilter(boolean findCompartments)
        {
            this.findCompartments = findCompartments;
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }

        @Override
        public boolean isAcceptable(DataElement de)
        {
            if( de instanceof VariableRole )
            {
                boolean isCompartment = ( (VariableRole)de ).getDiagramElement().getKernel() instanceof biouml.standard.type.Compartment; //hack, should do something more clever                
                return isCompartment == findCompartments;
            }
            return false;
        }
    }

    public void addUnit(Unit unit)
    {
        Map<String, Unit> oldValue = new HashMap<>(units);
        units.put(unit.getName(), unit);
        unit.setParent(this);
        this.firePropertyChange("units", oldValue, units);
    }

    public void removeUnit(String name)
    {
        Map<String, Unit> oldValue = new HashMap<>(units);
        units.remove(name);
        this.firePropertyChange("units", oldValue, units);
        for( Variable var : variables )
        {
            if( name.equals(var.getUnits()) )
                var.setUnits(Unit.UNDEFINED);
        }
    }
    public Map<String, Unit> getUnits()
    {
        return units;
    }

    public @Nonnull StreamEx<Equation> getEquations(Filter<DiagramElement> filter)
    {
        return getChildrenRoles(getDiagramElement(), Equation.class, filter);
    }

    public @Nonnull StreamEx<Equation> getEquations()
    {
        return getChildrenRoles(getDiagramElement(), Equation.class);
    }

    public @Nonnull StreamEx<Equation> getODE()
    {
        return getEquations(new ODEFilter());
    }

    public @Nonnull StreamEx<Equation> getInitialAssignments()
    {
        return getEquations(new InitialAssignmentsFilter());
    }

    public @Nonnull StreamEx<Equation> getAlgebraic()
    {
        return getEquations(new AlgebraicFilter());
    }

    public @Nonnull StreamEx<Equation> getAssignments()
    {
        return getEquations(new AssignmentsFilter());
    }

    /**
     * @return list of assignments which should be executed at the very start of model simulation
     * includes initial assignments and scalar equations
     */
    public @Nonnull List<Equation> getOrderedInitialEquations() throws Exception
    {
        if( orderedInitialAssignments == null )
        {
            List<Equation> cycled = new ArrayList<>();
            orderInitialAssignments(cycled);
            if( !cycled.isEmpty() )
            {
                log.info("There is cyclic dependency between initial assignments for variables:");
                log.info(StreamEx.of(cycled).map(e -> e.getVariable()).joining(", "));
                log.info("Initial values may be calculated incorrectly!");
            }
        }
        return orderedInitialAssignments;
    }

    public void orderInitialAssignments(List<Equation> cycledEquations) throws Exception
    {
        orderedInitialAssignments = new ArrayList<>();
        DAEModelUtilities.reorderAssignmentRules(getEquations(new InitialEquationsFilter(this)).toSet(), orderedInitialAssignments,
                cycledEquations);
    }

    public void orderScalarEquations(List<Equation> cycledEquations) throws Exception
    {
        orderedScalarEquations = new ArrayList<>();
        DAEModelUtilities.reorderAssignmentRules(getEquations(new ExtendedAssignmentsFilter(this)).toSet(), orderedScalarEquations,
                cycledEquations);
    }

    protected List<Equation> orderedScalarEquations;
    protected List<Equation> orderedInitialAssignments;
    public List<Equation> getOrderedScalarEquations() throws Exception
    {
        if( orderedScalarEquations == null )
            orderScalarEquations(null);
        return orderedScalarEquations;
    }
    ///////////////////////////////////////////////////////////////////
    // Event, State and Transition functions
    //

    public Event[] getEvents()
    {
        return getChildrenRoles(getDiagramElement(), Event.class).toArray(Event[]::new);
    }

    public Constraint[] getConstraints()
    {
        return getChildrenRoles(getDiagramElement(), Constraint.class).toArray(Constraint[]::new);
    }

    public State[] getStates()
    {
        return getChildrenRoles(getDiagramElement(), State.class).toArray(State[]::new);
    }

    public Transition[] getTransitions()
    {
        return getChildrenRoles(getDiagramElement(), Transition.class).toArray(Transition[]::new);
    }

    public Function[] getFunctions()
    {
        return getChildrenRoles(getDiagramElement(), Function.class).toArray(Function[]::new);
    }

    public <T extends Role> StreamEx<T> getChildrenRoles(Compartment compartment, Class<T> role)
    {
        return compartment.recursiveStream().map(DiagramElement::getRole).select(role);
    }

    public <T extends Role> StreamEx<T> getChildrenRoles(Compartment compartment, Class<T> role, Filter<DiagramElement> filter)
    {
        return compartment.recursiveStream().filter(de -> role.isInstance(de.getRole())).filter(filter::isAcceptable)
                .map(DiagramElement::getRole).select(role);
    }

    public static boolean isSubdiagram(DiagramElement de)
    {
        return de.getRole() != null && de.getRole() instanceof EModel;
    }

    // /////////////////////////////////////////////////////////////////
    // Model type issues
    //

    public static final String ODE_EMODEL_TYPE_STRING = "ODE EModel";

    /**
     * Returns emodel formalism type (ode, ode etc)
     * @return
     */
    public String getType()
    {
        return ODE_EMODEL_TYPE_STRING;
    }

    /**
     * returns model
     * @return
     */
    public int getModelType()
    {
        int compoundType = STATIC_TYPE;
        for( Equation eq : getEquations().filter(eq -> !Equation.TYPE_INITIAL_ASSIGNMENT.equals(eq.getType())) )
        {
            if( eq.getType().equals(Equation.TYPE_RATE) && !eq.isFast() )
            {
                Variable variable = getVariable(eq.getVariable());
                // skip constants and boundaryConditions
                if( variable != null && ! ( variable.isConstant()
                        || ( variable instanceof VariableRole && ( (VariableRole)variable ).isBoundaryCondition() ) ) )
                {
                    compoundType |= ODE_TYPE;
                }
            }
            else if( !eq.isAlgebraic() && !eq.isFast() )
            {
                Variable variable = getVariable(eq.getVariable());
                // skip internal variables
                if( variable != null )
                {
                    String varName = variable.getName();
                    if( ! ( varName.length() > 2 && varName.charAt(0) == '$' && varName.charAt(1) == '$' ) )
                    {
                        compoundType |= STATIC_EQUATIONS_TYPE;
                    }
                }
            }
            else
                compoundType |= ALGEBRAIC_TYPE;
        }


        Event[] events = getEvents();

        if( events != null && events.length > 0 )
            compoundType |= EVENT_TYPE;

        if( hasDelayedVariables(getDiagramElement()) )
            compoundType |= ODE_DELAY_TYPE;

        State[] states = getStates();
        Transition[] transitions = getTransitions();
        if( states != null && states.length > 0 && transitions != null && transitions.length > 0 )
        {
            compoundType |= STATE_TRANSITION_TYPE;
        }

        Diagram parentDiag = this.getParent();
        if( parentDiag.getType() instanceof CompositeDiagramType )
        {
            for( DiagramElement de : parentDiag )
            {
                if( de instanceof SubDiagram )
                    compoundType |= ( (SubDiagram)de ).getDiagram().getRole(EModel.class).getModelType();
            }
        }

        return compoundType;
    }

    public String getModelTypeName()
    {
        int modelType = getModelType();
        if( modelType == STATIC_TYPE )
            return "Static model";

        String str = "";

        if( isOfType(modelType, ODE_TYPE) )
        {
            if( isOfType(modelType, STATIC_EQUATIONS_TYPE) )
                str = "ODE model with static equations";
            else
                str = "ODE model";
        }
        else
        {
            if( isOfType(modelType, STATIC_EQUATIONS_TYPE) )
                str = "Model with static equations";
        }

        if( ( modelType & ODE_DELAY_TYPE ) != 0 )
            str += " with delays";

        if( ( modelType & ALGEBRAIC_TYPE ) != 0 )
            str += ", with algebraic rules";

        if( ( modelType & EVENT_TYPE ) != 0 )
            str += ", with events";

        if( ( modelType & STATE_TRANSITION_TYPE ) != 0 )
            str += ", with states and transitions";

        return str;
    }

    public static boolean isOfType(int type, int suggestedType)
    {
        return ( type & suggestedType ) != 0;
    }

    public boolean hasDelayedVariables(DiagramElement de)
    {
        if( de instanceof Node || de instanceof Edge )
        {
            Role role = de.getRole();
            if( role instanceof ExpressionOwner )
            {
                for( String expression : ( (ExpressionOwner)role ).getExpressions() )
                {
                    if( expression != null && expression.indexOf("delay") != -1 )
                        return true;
                }
            }

            if( de instanceof Compartment )
            {
                for( DiagramElement next : (Compartment)de )
                {
                    //We do not check the elements of subdiagrams of composite diagrams.
                    if( ! ( next instanceof Diagram ) && hasDelayedVariables(next) )
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * @author puz
     */
    public static class DelayedExpression
    {
        public Role role;
        public AstVarNode node;

        public DelayedExpression(AstVarNode node, Role role)
        {
            this.node = node;
            this.role = role;
        }
    }

    public List<DelayedExpression> getDelayedExpressionsList()
    {
        List<DelayedExpression> varList = new ArrayList<>();
        fillDelayedVariablesList(getDiagramElement(), varList);
        return varList;
    }

    public void fillDelayedVariablesList(DiagramElement de, List<DelayedExpression> list)
    {
        if( de instanceof Node || de instanceof Edge )
        {
            Role role = de.getRole();
            if( role instanceof ExpressionOwner )
            {
                for( String expression : ( (ExpressionOwner)role ).getExpressions() )
                {
                    if( expression != null && expression.indexOf("delay") != -1 )
                        fillAllDelayedVariablesForTree(readMath(expression, role), list, role);
                }
            }

            //Delayed variables of subdiagrams are not included into composite diagrams
            if( de instanceof Compartment )
            {
                for( DiagramElement innerDe : (Compartment)de )
                    fillDelayedVariablesList(innerDe, list);
            }
        }
    }

    private void fillAllDelayedVariablesForTree(ru.biosoft.math.model.Node node, List<DelayedExpression> list, Role role)
    {
        if( node instanceof AstFunNode )
        {
            AstFunNode funcNode = (AstFunNode)node;
            if( "delay".equals(funcNode.getFunction().getName()) )
                list.add(new DelayedExpression((AstVarNode)funcNode.jjtGetChild(0), role));
        }

        Utils.children(node).forEach(child -> fillAllDelayedVariablesForTree(child, list, role));
    }

    ///////////////////////////////////////////////////////////////////
    // Utility functions
    //

    /**
     * Get variable resolver specific for diagrams
     */
    public DiagramVariableResolver getVariableResolver(int varNameMode)
    {
        if( resolver == null || resolver.varNameMode != varNameMode )
            resolver = new DiagramVariableResolver(varNameMode);
        return resolver;
    }

    public class ViewVariableResolver extends DiagramVariableResolver
    {

        public ViewVariableResolver(int varNameMode)
        {
            super( varNameMode );
        }

        @Override
        public String resolveVariable(String variableName)
        {
            DiagramElement de = null;
            if( parsedRole != null )
                de = parsedRole.getDiagramElement();
            return getNamePresentation( variableName, de );
        }

        public String getNamePresentation(String name, DiagramElement de) throws IllegalArgumentException
        {
            if( name == null || name.length() == 0 )
                return name;

            if( name.length() > 1 && name.charAt( 1 ) == '$' )
                return name;

            String varName = name;
            Variable variable = getVariable( varName );

            if( de != null )
            {
                if( variable == null )
                {
                    StreamEx<Edge> edges = de instanceof Edge ? StreamEx.of( (Edge)de ) : ( (Node)de ).edges();
                    // search variable for related edges
                    variable = edges.map( edge -> getEdgeQualifiedVariable( varName, edge ) ).nonNull().findFirst().orElse( null );
                }

                if( variable == null && de.getOrigin() instanceof Compartment ) // search in the same compartment
                {
                    variable = (VariableRole) ( de.getCompartment().stream().map( DiagramElement::getRole )
                            .filter( role -> role instanceof VariableRole ).filter( role -> isVariableName( varName, (VariableRole)role ) )
                            .findFirst().orElse( null ) );
                }
            }

            if( variable == null )
            {
                // search suitable variable in variables data collection
                List<Variable> varList = getVariables().stream().filter( var -> isVariableName( varName, var ) )
                        .collect( Collectors.toList() );
                // check whether name is ambiguous
                if( varList.size() == 1 )
                    variable = varList.get( 0 );
                else if( varList.size() > 1 )
                    throw new IllegalArgumentException( "Ambiguous variable name, name=$" + varName + ", suitable variables: "
                            + StreamEx.of( varList ).map( Variable::getName ).joining( ", " ) );
            }

            //get qualified name for variable
            if( variable != null )
            {
                if( varNameMode == VARIABLE_NAME_BY_SHORT_ID )
                {
                    if( variable instanceof VariableRole )
                        return wrap( variable, ( (VariableRole)variable ).getShortName() );
                    else
                        return variable.getName();
                }

                if( varNameMode == VARIABLE_NAME_BY_ID )
                    return wrap( variable, variable.getName() );

                if( variable instanceof VariableRole )
                {
                    DiagramElement varDe = ( (VariableRole)variable ).getDiagramElement();
                    String result = varNameMode == VARIABLE_NAME_BY_ID_BRIEF ? VariableRole.createName( varDe, true )
                            : getTitle( varDe, ( varNameMode == VARIABLE_NAME_BY_TITLE ) );
                    return wrap( variable, result );
                }
                else
                {
                    return variable.getTitle();
                }
            }
            return null;
        }

        private String wrap(Variable var, String name)
        {
            String result = name;
            if( var instanceof VariableRole )
            {
                if( ( (VariableRole)var ).getQuantityType() == VariableRole.CONCENTRATION_TYPE
                        && ! ( ( (VariableRole)var ).getDiagramElement().getKernel() instanceof biouml.standard.type.Compartment ) )
                    result = "[" + result + "]";
            }
            return result;
        }

    }

    public class DiagramVariableResolver implements VariableResolver
    {
        protected int varNameMode;

        private final Map<String, String> index = new HashMap<>();

        public DiagramVariableResolver(int varNameMode)
        {
            this.varNameMode = varNameMode;

            for( Variable var : variables )
                addVariable(var);
        }

        public void addVariable(Variable v)
        {
            if( v instanceof VariableRole )
            {
                DiagramElement varDe = ( (VariableRole)v ).getDiagramElement();
                String title = getTitle(varDe, false);
                String completeTitle = getTitle(varDe, true);
                //if variable titles coincide, index should store only one, probably with shortest name
                if( !index.containsKey(title) || index.get(title).length() > v.getName().length() )
                    index.put(title, v.getName());
                if( !index.containsKey(title) || index.get(title).length() > v.getName().length() )
                    index.put(completeTitle, v.getName());
            }
        }

        @Override
        public String getVariableName(String variableTitle)
        {
            String vName = index.get(variableTitle);
            if( vName != null )
                return vName;
            Variable var = getVariable(variableTitle);
            if( var != null )
                return var.getName();

            DiagramElement de = null;
            if( parsedRole != null )
                de = parsedRole.getDiagramElement();
            return getQualifiedName(variableTitle, de, VARIABLE_NAME_BY_ID);
        }

        @Override
        public String resolveVariable(String variableName)
        {
            DiagramElement de = null;
            if( parsedRole != null )
                de = parsedRole.getDiagramElement();
            return getQualifiedName(variableName, de, varNameMode);
        }
    }

    /**
     * Resolves brief variable name to fully qualified variable name,
     * using information from corresponding DiagramElement.
     *
     * @returns fully qualified variable name. This name corresponds to variable name in variables
     * data collection. Returns null if variable can not be resolved.
     *
     * @throws IllegalArgumentException if variable name is ambiguous.
     *
     * Variables beginning from '$$' are considered as local.
     * @todo high
     */
    public String getQualifiedName(String name, DiagramElement de) throws IllegalArgumentException
    {
        return getQualifiedName(name, de, VARIABLE_NAME_BY_ID);
    }

    public String getQualifiedName(String name, DiagramElement de, int varNameMode) throws IllegalArgumentException
    {
        if( name == null || name.length() == 0 )
            return name;

        if( name.length() > 1 && name.charAt(1) == '$' )
            return name;

        String varName = name;
        Variable variable = getVariable(varName);

        if( de != null )
        {
            if( variable == null )
            {
                StreamEx<Edge> edges = de instanceof Edge ? StreamEx.of((Edge)de) : ( (Node)de ).edges();
                // search variable for related edges
                variable = edges.map(edge -> getEdgeQualifiedVariable(varName, edge)).nonNull().findFirst().orElse(null);
            }

            if( variable == null && de.getOrigin() instanceof Compartment ) // search in the same compartment
            {
                variable = (VariableRole) ( de.getCompartment().stream().map(DiagramElement::getRole)
                        .filter(role -> role instanceof VariableRole).filter(role -> isVariableName(varName, (VariableRole)role))
                        .findFirst().orElse(null) );
            }
        }

        if( variable == null )
        {
            // search suitable variable in variables data collection
            List<Variable> varList = getVariables().stream().filter(var -> isVariableName(varName, var)).collect(Collectors.toList());
            // check whether name is ambiguous
            if( varList.size() == 1 )
                variable = varList.get(0);
            else if( varList.size() > 1 )
                throw new IllegalArgumentException("Ambiguous variable name, name=$" + varName + ", suitable variables: "
                        + StreamEx.of(varList).map(Variable::getName).joining(", "));
        }

        //get qualified name for variable
        if( variable != null )
        {
            if( varNameMode == VARIABLE_NAME_BY_SHORT_ID )
            {
                if( variable instanceof VariableRole )
                    return ( (VariableRole)variable ).getShortName();
                else
                    return variable.getName();
            }

            if( varNameMode == VARIABLE_NAME_BY_ID )
                return variable.getName();

            if( variable instanceof VariableRole )
            {
                DiagramElement varDe = ( (VariableRole)variable ).getDiagramElement();
                String result = varNameMode == VARIABLE_NAME_BY_ID_BRIEF ? VariableRole.createName( varDe, true )
                        : getTitle(varDe, ( varNameMode == VARIABLE_NAME_BY_TITLE ));
                return result;
            }
            else
            {
                return variable.getTitle();
            }
        }
        return null;
    }

    private static String getTitle(DiagramElement de, boolean complete)
    {
        String title = HtmlUtil.stripHtml(de.getTitle());
        if( complete )
        {
            DataElement origin = de.getOrigin();
            while( ( origin instanceof DiagramElement ) && ! ( origin instanceof Diagram ) )
            {
                title = ( (DiagramElement)origin ).getTitle() + "." + title;
                origin = origin.getOrigin();
            }
        }

        if( !checkVariableName(title) )
            title = "\"" + title + "\"";
        return "$" + title;
    }

    protected Variable getEdgeQualifiedVariable(String name, Edge edge)
    {
        String str = name.startsWith("$") ? name.substring(1) : name;
        return edge.nodes().map(Node::getRole).select(VariableRole.class).findFirst(role -> isVariableName(str, role)).orElse(null);
    }

    /**
     * Check if variable has current name
     */
    private boolean isVariableName(String name, Variable variable)
    {
        String str = variable.getName().startsWith("$") ? variable.getName().substring(1) : variable.getName();
        if( str.length() > 1 && str.startsWith("\"") && str.endsWith("\"") )
            str = str.substring(1, str.length() - 1);
        if( str.endsWith("." + name) || str.equals(name) || str.endsWith("." + name.substring(1)) )
            return true;
        return false;
    }

    private static final int MAX_CHAR = 128;
    private static final boolean[] firstVariableChar = new boolean[MAX_CHAR];
    private static final boolean[] followingVariableChars = new boolean[MAX_CHAR];

    static
    {
        for( char c = 0; c < MAX_CHAR; c++ )
        {
            if( Character.isLetter(c) )
            {
                firstVariableChar[c] = true;
                followingVariableChars[c] = true;
            }
            else if( Character.isDigit(c) || c == '_' )
            {
                followingVariableChars[c] = true;
            }
        }
    }

    /**
     * Check if variable name can be used without quotes
     */
    public static boolean checkVariableName(String name)
    {
        if( name.isEmpty() )
            return false;
        char[] chars = name.toCharArray();
        if( chars[0] > MAX_CHAR || !firstVariableChar[chars[0]] )
            return false;
        for( int i = 1; i < chars.length; i++ )
            if( chars[i] > MAX_CHAR || !followingVariableChars[chars[i]] )
                return false;
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Variables and constant update issues
    //

    /**
     * @pending role update
     */
    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if( pce.getSource() instanceof ExpressionOwner )
        {
            ExpressionOwner owner = (ExpressionOwner)pce.getSource();
            if( owner.isExpression(pce.getPropertyName()) || pce.getPropertyName().equals("type") )
            {
                readMath(owner.getExpressions(), owner.getRole());
                if( isAutodetectTypes() )
                    detectVariableTypes();
            }
        }
        else if( pce.getPropertyName().equals("role") )
        {
            if( pce.getNewValue() instanceof Role && pce.getNewValue() instanceof ExpressionOwner )
                readMath( ( (ExpressionOwner)(Role)pce.getNewValue() ).getExpressions(), (Role)pce.getNewValue());
            if( pce.getOldValue() instanceof ExpressionOwner || pce.getNewValue() instanceof ExpressionOwner )
                if( isAutodetectTypes() )
                    detectVariableTypes();
        }
        else if( pce.getPropertyName().equals("formula") && pce.getNewValue() instanceof String )
        {
            readMath((String)pce.getNewValue(), null);
            if( isAutodetectTypes() )
                detectVariableTypes();
        }
    }

    protected void initParser()
    {
        if( parser == null )
        {
            parser = new Parser();
            parser.setContext(this);
            parser.setDeclareUndefinedVariables(true);
        }
    }

    public Parser getParser()
    {
        if( parser == null )
            initParser();
        return parser;
    }

    public void setParser(Parser parser)
    {
        this.parser = parser;
    }

    public AstStart readMath2(String math, Role role)
    {
        AstStart start = null;

        if( math != null && math.length() > 0 )
        {
            parsedRole = role;
            Parser parser = new Parser();
            parser.setContext( this );
            parser.setDeclareUndefinedVariables( false );

            try
            {
                synchronized( parser )
                {
                    int type = getDiagramElement().getViewOptions().getVarNameCode();
                    parser.setVariableResolver( new ViewVariableResolver( type ) );
                    int status = parser.parse( math );

                    if( status > ru.biosoft.math.model.Parser.STATUS_OK )
                    {
                        MessageBundle.error( log, "ERROR_MATH_PARSING",
                                new String[] {getDiagramElement() == null ? "(no element)" : getDiagramElement().getName(),
                                        role == null || role.getDiagramElement() == null ? "-" : role.getDiagramElement().getName(), math,
                                        Utils.formatErrors( parser )} );
                    }

                    if( status < ru.biosoft.math.model.Parser.STATUS_FATAL_ERROR )
                        start = parser.getStartNode();
                }
            }
            catch( Throwable t )
            {
                MessageBundle.error( log, "ERROR_MATH_PARSING",
                        new String[] {getDiagramElement() == null ? "(no element)" : getDiagramElement().getName(),
                                role == null || role.getDiagramElement() == null ? "-" : role.getDiagramElement().getName(), math,
                                ExceptionRegistry.log( t )} );
            }
        }

        return start;
    }

    public List<AstStart> readMath(String[] math, Role role)
    {
        return readMath(math, role, getVariableResolver(VARIABLE_NAME_BY_ID));
    }

    public List<AstStart> readMath(String[] math, Role role, VariableResolver resolver)
    {
        List<AstStart> starts = new ArrayList<>();
        if( math != null )
        {
            for( String m : math )
            {
                if( m != null )
                    starts.add(readMath(m, role, resolver));
            }
        }
        return starts;
    }

    public AstStart readMath(String math, Role role)
    {
        if( role instanceof Equation && math.equals( ( (Equation)role ).getFormula() ) )
            return ( (Equation)role ).getMath();
        return readMath(math, role, VARIABLE_NAME_BY_ID);
    }

    public AstStart readMath(String math, Role role, int varNameMode)
    {
        return readMath(math, role, getVariableResolver(varNameMode));
    }

    public AstStart readMath(String math, Role role, VariableResolver resolver)
    {
        return readMath(math, role, resolver, false);
    }

    public AstStart readMath(String math, Role role, VariableResolver resolver, boolean silent)
    {
        AstStart start = null;

        if( math != null && math.length() > 0 )
        {
            initParser();
            parsedRole = role;
            try
            {
                synchronized( parser )
                {
                    parser.setVariableResolver(resolver);
                    int status = parser.parse(math);

                    if( status > ru.biosoft.math.model.Parser.STATUS_OK && !silent )
                    {
                        MessageBundle.error(log, "ERROR_MATH_PARSING",
                                new String[] {getDiagramElement() == null ? "(no element)" : getDiagramElement().getName(),
                                        role == null || role.getDiagramElement() == null ? "-" : role.getDiagramElement().getName(), math,
                                        Utils.formatErrors(parser)});
                    }

                    if( status < ru.biosoft.math.model.Parser.STATUS_FATAL_ERROR )
                        start = parser.getStartNode();
                }
            }
            catch( Throwable t )
            {
                MessageBundle.error(log, "ERROR_MATH_PARSING",
                        new String[] {getDiagramElement() == null ? "(no element)" : getDiagramElement().getName(),
                                role == null || role.getDiagramElement() == null ? "-" : role.getDiagramElement().getName(), math,
                                ExceptionRegistry.log(t)});
            }
        }

        return start;
    }

    public void removeNotUsedParameters()
    {
        try
        {
            detectVariableTypes();
            for( String name : getParameters().stream().filter(v -> Variable.TYPE_UNUSED.equals(v.getType())).map(v -> v.getName())
                    .collect(Collectors.toList()) )
            {
                if( log.isLoggable(Level.INFO) )
                    log.info("Remove not used parameter: " + name);
                getVariables().remove(name);
            }
        }
        catch( Throwable t )
        {
            MessageBundle.error(log, "ERROR_REMOVE_NOT_USED_PARAMS", new String[] {getDiagramElement().getName(), t.toString()});
        }
        return;
    }

    private Set<String> detectUnusedVariables()
    {
        Set<String> notUsedParams = getVariables().stream().map(Variable::getName).filter(varname -> !varname.equals("time"))
                .collect(Collectors.toSet());
        if( conversionFactor != null )
            notUsedParams.remove(conversionFactor.getName());

        VariableResolver resolver = getVariableResolver(VARIABLE_NAME_BY_ID);
        Diagram diagram = getParent();

        for( DiagramElement de : diagram.recursiveStream() )
        {
            Role role = de.getRole();

            if( role instanceof ExpressionOwner && ! ( role instanceof Function ) )
            {
                for( AstStart start : readMath( ( (ExpressionOwner)role ).getExpressions(), role, resolver) )
                    completeUsedParametersList(start, notUsedParams);
            }
            else if( role instanceof VariableRole )
            {
                notUsedParams.remove( ( (VariableRole)role ).getConversionFactor());
            }
            else if( Util.isPort(de) && !Util.isModulePort(de) )
            {
                notUsedParams.remove(Util.getPortVariable(de));
            }
        }
        return notUsedParams;
    }

    public void detectVariableTypes()
    {
        if( DiagramUtility.isComposite(getParent()) )
            SubDiagram.diagrams(getParent()).without(getParent()).map(d -> d.getRole()).select(EModel.class)
                    .forEach(e -> e.detectVariableTypes());

        Set<String> constants = getVariables().stream().map(Variable::getName).filter(varname -> !varname.equals("time"))
                .collect(Collectors.toSet());

        getParser().setDeclareUndefinedVariables(false);
        //0. find all unused parameters
        Set<String> notUsedParams = detectUnusedVariables();
        notUsedParams.forEach(name -> getVariable(name).setType(Variable.TYPE_UNUSED));
        constants.removeAll(notUsedParams);

        //1. All non-constant variables assigned in events are discrete (unless they have ODE or algebraic as well)
        for( Event event : getEvents() )
        {
            for( Assignment assignment : event.getEventAssignment() )
            {
                String variableName = assignment.getVariable();
                Variable var = getVariable(variableName);
                if( !var.isConstant() )
                {
                    var.setType(Variable.TYPE_DISCRETE);
                    constants.remove(variableName);
                }
            }
        }

        //1.5. Variables calculated by initial assignments are considered "calculated" unless they also participate in ODE
        for( Equation eq : this.getInitialAssignments() )
        {
            String variableName = eq.getVariable();
            Variable var = getVariable(variableName);

            if( var == null || var.isConstant() )
                continue;

            var.setType(Variable.TYPE_CALCULATED);
            constants.remove(variableName);
        }

        //2. Find ODE and calculated by assignments variables
        for( Equation eq : getEquations() )
        {
            String variableName = eq.getVariable();
            Variable var = getVariable(variableName);

            if( var == null || var.isConstant() )
                continue;

            if( Util.isSpecieReference(eq.getDiagramElement()) && var instanceof VariableRole )
            {
                if( ( (VariableRole)var ).isBoundaryCondition() )
                    continue;
                var.setType(Variable.TYPE_DIFFERENTIAL);
                constants.remove(variableName);
            }
            else if( eq.getType().equals(Equation.TYPE_SCALAR) || eq.getType().equals(Equation.TYPE_SCALAR_INTERNAL) )
            {
                var.setType(Variable.TYPE_CALCULATED);
                constants.remove(variableName);
            }
            else if( Equation.isRate(eq.getType()) )
            {
                var.setType(Variable.TYPE_DIFFERENTIAL);
                constants.remove(variableName);
            }
        }

        //3. Set variables mentioned in algebraic equation and nod mentioned anywhere else as algebraic
        getAlgebraic().flatMap(eq -> Utils.getVariables(eq.getMath()).stream()).filter(var -> constants.contains(var)).forEach(var -> {
            Variable variable = getVariable(var);
            if( !variable.isConstant() )
            {
                variable.setType(Variable.TYPE_ALGEBRAIC);
                constants.remove(var);
            }
        });

        //4. The rest are categorized as parameters
        constants.forEach(name -> getVariable(name).setType(Variable.TYPE_PARAMETER));

        getParser().setDeclareUndefinedVariables(true);

    }

    private void completeUsedParametersList(ru.biosoft.math.model.Node node, Set<String> modelParameters)
    {
        if( node == null )
            return;
        if( node instanceof AstVarNode )
            modelParameters.remove( ( (AstVarNode)node ).getName());
        Utils.children(node).forEach(child -> completeUsedParametersList(child, modelParameters));
    }

    ////////////////////////////////////////////////////////////////////////////
    // DataCollectionListener implementation
    //

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }
    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        if( !notificationEnabled )
            return;

        DataElement de = e.getDataElement();
        if( de instanceof DiagramElement )
        {
            Role role = ( (DiagramElement)de ).getRole();

            if( role instanceof Variable )
            {
                Variable newVar = (Variable)role;
                getVariables().put(newVar);
                firePropertyChange("variables", null, null);
            }
            else if( role instanceof ExpressionOwner )
            {
                readMath( ( (ExpressionOwner)role ).getExpressions(), role);
                //                detectVariableTypes();
            }
        }
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        elementToRemove = e.getDataElement();
    }
    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        if( !notificationEnabled )
            return;

        if( elementToRemove instanceof Node )
        {
            Node node = (Node)elementToRemove;
            Role role = node.getRole();
            if( role instanceof VariableRole )
            {
                //remove Variable role if this node was the last associated with node
                if( ( (VariableRole)role ).removeAssociatedElement(node) )
                    getVariables().remove( ( (Variable)role ).getName());
            }
            else if( role instanceof Variable )
            {
                getVariables().remove( ( (Variable)role ).getName());
            }
        }
        firePropertyChange("variables", null, null);
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        if( e.getType() == DataCollectionEvent.ELEMENT_WILL_REMOVE )
            elementWillRemove(e);
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        if( !notificationEnabled )
            return;

        while( e.getPrimaryEvent() != null )
            e = e.getPrimaryEvent();

        if( e.getType() == DataCollectionEvent.ELEMENT_ADDED )
            elementAdded(e);
        else if( e.getType() == DataCollectionEvent.ELEMENT_REMOVED )
            elementRemoved(e);
    }

    public State getInitialState()
    {
        return initialState;
    }
    public void setInitialState(State initialState)
    {
        this.initialState = initialState;
    }

    public void setConversionFactor(String variableName)
    {
        if( CONVERSION_FACTOR_UNDEFINED.equals(variableName) )
        {
            this.conversionFactor = null;
            return;
        }
        Variable variable = getVariable(variableName);
        if( variable == null )
            throw new IllegalArgumentException("Can not found parameter " + variableName + " in model");
        if( variable instanceof VariableRole )
            throw new IllegalArgumentException("Incorrect conversion factor, it should be model parameter with set constant = true");
        conversionFactor = variable;
    }

    @PropertyName ( "Conversion factor" )
    @PropertyDescription ( "Conversion factor." )
    public String getConversionFactor()
    {
        if( conversionFactor == null )
            return CONVERSION_FACTOR_UNDEFINED;
        return conversionFactor.getName();
    }

    public static abstract class NodeFilter implements Filter<DiagramElement>
    {
        @Override
        public boolean isEnabled()
        {
            return true;
        }

        @Override
        public boolean isAcceptable(DiagramElement de)
        {
            return de instanceof Node && isNodeAcceptable((Node)de);
        }

        protected abstract boolean isNodeAcceptable(Node de);
    }

    public static class AssignmentsFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            Equation role = de.getRole(Equation.class);
            return role.getType().equals(Equation.TYPE_SCALAR) || role.getType().equals(Equation.TYPE_SCALAR_INTERNAL);
        }
    }

    public static class ExtendedAssignmentsFilter extends NodeFilter
    {
        EModel emodel;
        public ExtendedAssignmentsFilter(EModel emodel)
        {
            this.emodel = emodel;
        }
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            Equation role = de.getRole(Equation.class);
            String type = role.getType();
            String varName = role.getVariable();
            if( varName == null ) //algebraic equation
                return false;
            Variable var = emodel.getVariable(varName);
            if( var == null || var.isConstant() )
                return false;
            return Equation.TYPE_SCALAR.equals(type) || Equation.TYPE_SCALAR_INTERNAL.equals(type)
                    || Equation.TYPE_SCALAR_DELAYED.equals(type);
        }
    }

    public static class InitialEquationsFilter extends NodeFilter
    {
        EModel emodel;
        public InitialEquationsFilter(EModel emodel)
        {
            this.emodel = emodel;
        }

        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            Equation role = de.getRole(Equation.class);
            String type = role.getType();

            if( !Equation.TYPE_INITIAL_ASSIGNMENT.equals(type) && !Equation.TYPE_INITIAL_VALUE.equals(type) )//initial assignment still should be applied even if variable is constant
            {
                String varName = role.getVariable();
                Variable var = emodel.getVariable(varName);
                if( var == null || var.isConstant() )
                    return false;
            }
            return Equation.TYPE_SCALAR.equals(type) || Equation.TYPE_INITIAL_ASSIGNMENT.equals(type)
                    || Equation.TYPE_SCALAR_INTERNAL.equals(type) || Equation.TYPE_SCALAR_DELAYED.equals(type)
                    || Equation.TYPE_INITIAL_VALUE.equals(type);
        }
    }

    public static class InitialAssignmentsFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            return de.getRole(Equation.class).getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT);
        }
    }

    public static class NotInitialAssignmentsFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            return !de.getRole(Equation.class).getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT);
        }
    }

    public static class ODEFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            return de.getRole(Equation.class).isODE();
        }
    }

    public static class AlgebraicFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            return de.getRole(Equation.class).isAlgebraic();
        }
    }

    public static class AssignmentFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            return de.getRole(Equation.class).isAssignment();
        }
    }

    @Override
    public Diagram getDiagramElement()
    {
        return (Diagram)super.getDiagramElement();
    }

    @Override
    public Diagram getParent()
    {
        return (Diagram)super.getParent();
    }

    public String getConversionFactor(Variable var)
    {
        return ! ( var instanceof VariableRole ) ? null : ( (VariableRole)var ).getConversionFactor();
    }

    public boolean isAutodetectTypes()
    {
        return autodetectTypes;
    }

    public void setAutodetectTypes(boolean autodetectTypes)
    {
        this.autodetectTypes = autodetectTypes;
    }


}