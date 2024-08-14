package biouml.model.javascript;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.mozilla.javascript.NativeObject;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.Connection.Port;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.BusProperties;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.SplitDiagramAction;
import biouml.standard.diagram.SplitDiagramAction.SplitDiagramActionParameters;
import biouml.standard.diagram.SubDiagramProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.DirectedConnection;
import biouml.standard.type.Stub.UndirectedConnection;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class JavaScriptModel extends JavaScriptHostObjectBase
{
    protected static final Logger log = Logger.getLogger(JavaScriptModel.class.getName());

    public JavaScriptModel()
    {

    }
    
    public Diagram createCompositeDiagram(DataCollection origin, String name) throws Exception
    {  
        Diagram result = new CompositeDiagramType().createDiagram( origin, name, new DiagramInfo( name ) );
        result.save();
        return result;
    }
    
    /**
     * Returns initial value of the diagram variable with the specified name.
     *
     * @param diagram the diagram to get initial value of the variable.
     * @param variableName the name of the variable to get initial value of.
     * @return the variable value represented by real number.
     */
    public double getInitialValue(Diagram diagram, String variableName)
    {
        return getVariable(diagram, variableName).getInitialValue();
    }

    /**
     * Sets initial value of the diagram variable with the specified name.
     *
     * @param diagram the diagram to set initial value of the variable.
     * @param variableName the name of the variable to set initial value of.
     * @param value initial value to be set.
     */
    public void setInitialValue(Diagram diagram, String variableName, double value)
    {
        getVariable(diagram, variableName).setInitialValue(value);
    }

    /**
     * Returns initial values of the diagram variables with the specified names.
     *
     * @param diagram the diagram to get initial values of variables.
     * @param variableNames an array of variables names to get initial values of.
     * @return an array of initial variable values.
     */
    public double[] getInitialValues(Diagram diagram, String[] variableNames)
    {
        StreamEx<Variable> varStream;
        if( variableNames == null )
            varStream = StreamEx.of( diagram.getRole( EModel.class ).getVariables().stream() );
        else
            varStream = StreamEx.of(variableNames).map(name -> getVariable(diagram, name));

        return varStream.mapToDouble(Variable::getInitialValue).toArray();
    }

    /**
     * Sets initial values of the diagram variables with the specified names.
     *
     * @param diagram the diagram to set initial values of variables.
     * @param variableNames an array of variables names to set initial values of.
     * @param values an array of initial values represented by real numbers.
     * The array length must be equal to the length of variableNames array.
     */
    public void setInitialValues(Diagram diagram, String[] variableNames, double[] values)
    {
        if( values == null )
        {
            throw new IllegalArgumentException("Values array is null.");
        }

        if( variableNames == null )
        {
            Variable[] variables = getVariables(diagram);
            if( values.length != variables.length )
            {
                throw new IllegalArgumentException("Values array length must be equal to the number of diagram variables.");
            }
            for( int i = 0; i < variables.length; i++ )
            {
                variables[i].setInitialValue(values[i]);
            }
            return;
        }

        if( variableNames.length != values.length )
        {
            throw new IllegalArgumentException("Lengths of variableNames and values arrays must be the same.");
        }
        for( int i = 0; i < variableNames.length; i++ )
        {
            getVariable(diagram, variableNames[i]).setInitialValue(values[i]);
        }
    }

    /**
     * Returns boundary condition for the diagram variable with name <b>variableName</b>
     * and "false" if this name corresponds to the diagram parameter.
     *
     * @param diagram the diagram to get boundary condition of the variable.
     * @param variableName the name of the diagram variable to get boundary condition of.
     * @return the boundary condition.
     */
    public boolean getBoundaryCondition(Diagram diagram, String variableName)
    {
        Variable var = getVariable(diagram, variableName);
        if( var instanceof VariableRole )
        {
            return ( (VariableRole)var ).isBoundaryCondition();
        }
        else
        {
            return false; //Boundary conditions can not be applied for the diagram parameters.
        }
    }

    /**
     * Sets boundary condition for the diagram variable with name <b>variableName</b>
     * and throws exception if this name corresponds to the diagram parameter.
     *
     * @param diagram the diagram to set boundary condition of the variable.
     * @param variableName the name of the variable to set boundary condition of.
     * @param value the boundary condition to be set.
     */
    public void setBoundaryCondition(Diagram diagram, String variableName, boolean value)
    {
        Variable var = getVariable(diagram, variableName);
        if( var instanceof VariableRole )
        {
            ( (VariableRole)var ).setBoundaryCondition(value);
        }
        else
        {
            log.log(Level.SEVERE, "Parameter " + variableName + " can not be set to boundary condition");
        }
    }
    /**
     * Returns boundary conditions for diagram variables and "false" for diagram parameters.
     *
     * @param diagram the diagram to get boundary conditions of variables.
     * @return an array of variables with boundary conditions.
     */
    public String[] getBoundaryConditions(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getVariableRoles().stream().filter(VariableRole::isBoundaryCondition)
                .map(VariableRole::getName).toArray(String[]::new);
    }

    /**
     * Sets boundary conditions for diagram <b>variables</b>.
     *
     * @param diagram the diagram to set boundary conditions of variables.
     * @param variables an array of variables identifiers to set boundary conditions.
     */
    public void setBoundaryConditions(Diagram diagram, String[] variables)
    {
        EModel role = diagram.getRole(EModel.class);
        for( String variableName : variables )
        {
            Variable var = role.getVariable(variableName);
            if( var == null )
            {
                log.log(Level.SEVERE, "Variable " + variableName + " was not found in diagram " + diagram.getName());
            }
            else if( ! ( var instanceof VariableRole ) )
            {
                log.log(Level.SEVERE, "Parameter " + variableName + " can not be set to boundary condition");
            }
            else
            {
                ( (VariableRole)var ).setBoundaryCondition(true);
            }
        }
    }

    /**
     * Returns "true" if diagram variable with name <b>variableName</b> is constant.
     *
     * @param diagram the diagram to specify if the variable is constant.
     * @param variableName the name of the variable to specify is it constant or not.
     * @return 'true' if the variable is constant or 'false' otherwise.
     */
    public boolean isConstant(Diagram diagram, String variableName)
    {
        return getVariable(diagram, variableName).isConstant();
    }

    /**
     * Sets constant property of the specified diagram variable equal to value.
     *
     * @param diagram the diagram to set constant property of the variable.
     * @param variableName the name of the variable to set constant property of.
     * @param value the constant property of the variable to be set.
     */
    public void setConstant(Diagram diagram, String variableName, boolean value)
    {
        getVariable(diagram, variableName).setConstant(value);
    }

    /**
     * Returns Gets an array of constant variables names.
     *
     * @param diagram the diagram to get constant variables of.
     * @return an array of constant variables identifiers.
     */
    public String[] getConstants(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getVariables().stream().filter(Variable::isConstant).map(Variable::getName)
                .toArray(String[]::new);
    }

    /**
     * Sets specified diagram variables to be constant.
     *
     * @param diagram the diagram to set variables to be constant.
     * @param variables an array of identifiers of diagram variables to be constant.
     */
    public void setConstants(Diagram diagram, String[] variables)
    {
        EModel role = diagram.getRole(EModel.class);
        for( String variableName : variables )
        {
            Variable var = role.getVariable(variableName);
            if( var == null )
            {
                log.log(Level.SEVERE, "Variable " + variableName + " was not found in diagram " + diagram.getName());
            }
            else
            {
                var.setConstant(true);
            }
        }
    }

    /**
     * Sets boundary conditions of all diagrams variables to be "false".
     *
     * @param diagram
     */
    public void clearAllBoundaryConditions(Diagram diagram)
    {
        diagram.getRole(EModel.class).getVariableRoles().stream().forEach(var -> var.setBoundaryCondition(false));
    }

    /**
     * Sets constant properties of all diagram variables equal to 'false'.
     *
     * @param diagram the diagram to clear variables constant properties.
     */
    public void clearAllConstants(Diagram diagram)
    {
        diagram.getRole(EModel.class).getVariables().stream().forEach(var -> var.setConstant(false));
    }

    /**
     * Gets diagram mathematical object (equation, function or event) by name <b>mathName</b>.
     *
     * @param diagram the diagram to get mathematical object of.
     * @param mathName the name of the mathematical object.
     * @return EModelRole class object.
     * @throws Exception
     */
    public Role getMath(Diagram diagram, String mathName) throws Exception
    {
        try
        {
            Role role = diagram.findNode(mathName).getRole();
            if( role instanceof Equation || role instanceof Function || role instanceof Event || role instanceof Assignment )
                return role;
        }
        catch( Exception ex )
        {
            throw new Exception("Can not get math with name " + mathName, ex);
        }
        throw new Exception("Can not get math with name " + mathName);
    }

    /**
     * Returns identifiers of all diagram variables.
     *
     * @param diagram the diagram to get variables names of.
     * @return an array of variables identifiers.
     */
    public String[] getVariableNames(Diagram diagram)
    {
        DataCollection<Variable> dc = diagram.getRole(EModel.class).getVariables();
        return dc.names().toArray(String[]::new);
    }

    /**
     * Gets diagram variable with name <b>variableName</b>.<br>
     * If there is no such variable in model, the method throws an IllegalArgumentException.
     *
     * @param variableName the name of the variable.
     * @return
     */
    public Variable getVariable(Diagram diagram, String variableName)
    {
//        if (variableName.contains("\\"))
        Variable var = Util.getVariable(diagram, variableName);
//        Variable var = diagram.getRole(EModel.class).getVariable(variableName);
        if( var == null )
            throw new IllegalArgumentException("Variable " + variableName + " not found");
        return var;
    }

    /**
     * Gets variables array (species and parameters) of the <b>diagram</b>.
     *
     * @param diagram the diagram to get variables of.
     * @return created array of diagram variables.
     */
    public Variable[] getVariables(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getVariables().stream().toArray(Variable[]::new);
    }

    public Variable[] getVariableRoles(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getVariableRoles().stream().toArray(Variable[]::new);
    }

    /**
     * Creates clone of <b>diagram</b> with name <b>name</b>.
     *
     * @param diagram the diagram to be cloned.
     * @param name new name for cloned diagram.
     * @return copy of <b>diagram</b> with name <b>name</b>.
     * @throws Exception
     */
    public Diagram clone(Diagram diagram, String name) throws Exception
    {
        return diagram.clone(diagram.getOrigin(), name);
    }

    private static Map<String, Object> convertNativeObject(NativeObject object)
    {
        HashMap<String, Object> map = new HashMap<>();
        for( Object id : object.getIds() )
        {
            map.put(id.toString(), object.getAssociatedValue(id));
        }
        return map;
    }

    private Object convertType(String type)
    {
        if( type.endsWith(".class") )
        {
            try
            {
                return ClassLoading.loadClass(type.substring(0, type.length() - ".class".length()));
            }
            catch( LoggedClassNotFoundException e )
            {
                e.log();
                return null;
            }
        }
        return type;
    }

    protected Diagram getDiagram(DiagramElement de)
    {
        Diagram diagram = Diagram.getDiagram(de);
        while( diagram.getOrigin() instanceof DiagramElement )
        {
            DiagramElement origin = (DiagramElement)diagram.getOrigin();
            diagram = Diagram.getDiagram(origin);
        }
        return diagram;
    }

    /**
     * This method was validated only for the case of SBML models.
     */
    protected DiagramElement add(@Nonnull Compartment compartment, Point pt, Object type, Map<String, Object> properties)
    {
        DiagramElement de = createElement(compartment, pt, type, properties);
        if( putElement(compartment, de) )
        {
            return de;
        }
        return null;
    }

    protected DiagramElement createElement(@Nonnull Compartment compartment, Point pt, Object type, Map<String, Object> properties)
    {
        DefaultSemanticController semanticController = (DefaultSemanticController)getDiagram(compartment).getType().getSemanticController();
        Object elementProperties = semanticController.getPropertiesByType(compartment, type, pt);
        if( elementProperties != null )
        {
            if( properties != null && !properties.isEmpty() )
            {
                ComponentModel model = ComponentFactory.getModel(elementProperties);
                for( Entry<Property, Object> entry : EntryStream.of(properties).mapKeys(model::findProperty).nonNullKeys() )
                {
                    try
                    {
                        entry.getKey().setValue(entry.getValue());
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Cannot set property " + entry.getKey().getName(), e);
                    }
                }
            }
            return semanticController.createInstance( compartment, type, pt, elementProperties ).getElement();
        }
        return semanticController.createInstance( compartment, type, pt, (Object)null ).getElement();
    }

    protected boolean putElement(Compartment compartment, DiagramElement de)
    {
        if( de == null )
        {
            log.log(Level.SEVERE, "putElement: No diagram element is provided");
            return false;
        }
        DefaultSemanticController semanticController = (DefaultSemanticController)getDiagram(compartment).getType().getSemanticController();
        if( semanticController.canAccept(compartment, de) )
        {
            try
            {
                compartment.put(de);
                return true;
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "The equation adding error", t);
            }
        }
        else
        {
            log.info("Can not accept equation '" + de.getName() + "' to diagram '" + compartment.getName() + "'");
        }
        return false;
    }

    public void add(Compartment compartment, int x, int y, String type, NativeObject properties)
    {
        if( compartment == null )
        {
            throw new IllegalArgumentException("compartment is null");
        }
        add(compartment, new Point(x, y), convertType(type), convertNativeObject(properties));
    }

    public void add(Compartment compartment, String type, NativeObject properties)
    {
        add(compartment, 0, 0, type, properties);
    }

    /**
     * Adds equation of <b>type</b> corresponding to <b>variable</b> with <b>formula</b> to <b>diagram</b>
     * @param diagram the diagram to add equation to.
     * @param type the type of the equation: 'algebraic', 'rate', 'scalar' or 'initial assignment'.
     * @param variable the identifier of the diagram parameter or variable which value will be calculated by formula.
     * @param formula the formula for calculation the variable value.
     */
    public void addEquation(Diagram diagram, String type, String variable, String formula)
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put("formula", formula);
        properties.put("variable", variable);
        properties.put("type", type);
        add(diagram, new Point(0, 0), Equation.class, properties);
    }
    
    public void addSubDiagram(Diagram diagram, String subDiagramPath, String name)
    {
        SubDiagramProperties properties = new SubDiagramProperties( diagram );
        properties.setDiagramPath( DataElementPath.create( subDiagramPath ) );
        properties.setName( name );
        DiagramElementGroup deg = diagram.getType().getSemanticController().createInstance( diagram, SubDiagram.class , new Point(), properties );
        deg.putToCompartment();//TODO do this in properties?
    }
    
    /**
     * Adds port of <b>type</b> corresponding to <b>variable</b> to <b>diagram</b>
     * @param diagram the diagram to add equation to.
     * @param type the type of the port: 'input', 'output', 'contact'
     */
    public void addPublicPort(Diagram diagram, String variable, String type)
    {
        if (diagram.getRole( EModel.class ).getVariable( variable ) == null)
            throw new IllegalArgumentException("Diagram " + diagram.getName() + " has no variable " + variable);
        
        PortProperties properties = new PortProperties( diagram, type );
        if( properties.alreadyHasPort( variable, type, ConnectionPort.PUBLIC ) )
            return;
        properties.setVarName( variable );
        properties.setAccessType( ConnectionPort.PUBLIC );
        diagram.getType().getSemanticController().createInstance( diagram, ConnectionPort.class , new Point(), properties );
    }
    
    /**
     * Adds private port of <b>type</b> corresponding to <b>variable</b> to <b>diagram</b> and connects it to 
     * @param diagram the diagram to add equation to.
     * @param type the type of the port: 'input', 'output', 'contact'
     */
    public void addPrivatePort(Diagram diagram, String subDiagraName, String variable)
    {
        SubDiagram subDiagram = Util.getSubDiagram( diagram, subDiagraName );
        
        if (subDiagram == null)
            throw new IllegalArgumentException("Diagram " + diagram.getName() + " has no subdiagram " + subDiagraName);
        
        Node subDiagramPort = subDiagram.stream().select( Node.class ).filter( n -> Util.isPort( n ) )
                .findAny( n -> variable.equals( Util.getPortVariable( n ) ) ).orElse( null );

        if (subDiagramPort == null)
            throw new IllegalArgumentException("SubDiagram " + subDiagraName + " has no port for variable " + variable);
        
        String type = Util.getPortType( subDiagramPort );
        String oppositeType = type == "input"? "output": type == "output"? "input": "contact";

        //add variable if necessary
        EModel emodel = diagram.getRole( EModel.class );
        if (!emodel.containsVariable( variable ))
        {
            double initialValue = subDiagram.getDiagram().getRole( EModel.class ).getVariable( variable ).getInitialValue(); 
            emodel.declareVariable( variable, initialValue );
        }
        
        //add port
        Point location = subDiagramPort.getLocation();
        location.x +=(subDiagram.getShapeSize().width+100);
        PortProperties properties = new PortProperties( diagram, oppositeType );
        properties.setVarName( variable );
        properties.setAccessType( ConnectionPort.PRIVATE );
        DiagramElementGroup deg = diagram.getType().getSemanticController().createInstance( diagram, ConnectionPort.class , location, properties );
        Node port = deg.nodesStream().findAny( n->Util.isPort( n ) ).orElse( null );
        Util.setPortOrientation( port, PortOrientation.LEFT );
        if (port == null)//something went wrong
            return;

        String name = DefaultSemanticController.generateUniqueName( diagram, "Connection" );
        //add connection
        Edge edge;
        if( type == "input" )
            edge = new Edge( new Stub.DirectedConnection( null, name ), port, subDiagramPort );
        else if( type == "output" )
            edge = new Edge( new Stub.DirectedConnection( null, name ), subDiagramPort, port);
        else
            edge = new Edge( new Stub.UndirectedConnection( null, name ), subDiagramPort, port  );

        Connection role = ( type=="contact" ) ?  new biouml.model.dynamics.UndirectedConnection(edge): new biouml.model.dynamics.DirectedConnection(edge);
        role.setInputPort(new Connection.Port(variable, variable));
        role.setOutputPort(new Connection.Port(variable, variable));
        edge.setRole(role);
        diagram.put( edge );
    }

    /**
     * Adds empty event to diagram <b>diagram</b>.
     */     
    public Event addEvent(Diagram diagram)
    {
        return add(diagram, new Point(0, 0), Event.class, null).getRole( Event.class );
    }
    
    /**
     * Adds event with trigger <b>trigger</b>, delay <b>delay</b> and assignments <b>actions</b> to diagram <b>diagram</b>.
     *
     * @param diagram the diagram to add an event to.
     * @param trigger boolean expression triggering the event.
     * @param delay double number representing time of the event delay.
     * @param actions array of assignments performed after the event triggering.
     */
    public void addEvent(Diagram diagram, String trigger, double delay, Assignment[] actions)
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put("trigger", trigger);
        properties.put("delay", Double.toString(delay));
        properties.put("eventAssignment", actions);
        add(diagram, new Point(0, 0), Event.class, properties);
    }

    /**
     * Creates assignment to perform after triggering of a diagram event.
     *
     * @param diagram the diagram for which the assignment is created.
     * @param variable the identifier of the diagram parameter or variable which value will be changed after triggering of the event.
     * @param math the formula for calculation the new value of the variable after the event triggering.
     * @return created Assignment on success or null otherwise.
     */
    public Assignment createAssignment(Diagram diagram, String variable, String math)
    {
        if( diagram.getRole(EModel.class).getVariable(variable) == null )
            throw new IllegalArgumentException("Diagram " + diagram.getName() + " has no variable " + variable);
        return new Assignment(variable, math);
    }

    /**
     * Creates a participant to produce a chemical reaction in a diagram.
     *
     * @param diagram the diagram containing the specie to create the reaction involving it.
     * @param variable the identifier of the specie in the diagram.
     * @param role the role of the specie in the reaction: 'reactant', 'product' or 'modifier'.
     * @param stoichiometry the stoichiometric coefficient representing the degree to which the specie participates in the reaction.
     * @return created SpecieReference on success or null otherwise.
     * @throws Exception
     */
    public SpecieReference createSpecieReference(Diagram diagram, String variable, String role, double stoichiometry)
    {
        try
        {
            Node node = diagram.findNode(variable);
            if( node == null )
            {
                log.log(Level.SEVERE, "Diagram '" + diagram.getName() + "' has no variable '" + variable + "'.");
                return null;
            }
            if( !role.equals(SpecieReference.PRODUCT) && !role.equals(SpecieReference.REACTANT) && !role.equals(SpecieReference.MODIFIER) )
            {
                log.log(Level.SEVERE, "Unknown role of the specie. Possible values are: " + SpecieReference.REACTANT + "', '" + SpecieReference.PRODUCT
                        + "', '" + SpecieReference.MODIFIER + "'.");
                return null;
            }
            SpecieReference specieReference = new SpecieReference(null, variable, role);
            specieReference.setStoichiometry(Double.toString(stoichiometry));
            specieReference.setSpecie(variable);
            return specieReference;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not create specie reference.", e);
        }
        return null;
    }

    /**
     * Copy all selected elements into separate diagram and creates appropriate ports
     * @return new diagram
     */
    public Diagram split(Diagram diagram, String[] names, String path, String name)
    {
        SplitDiagramAction action = new SplitDiagramAction();
        List<DiagramElement> found = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        for( String n : names )
        {
            DiagramElement de = diagram.findDiagramElement(n);
            if( de == null )
                notFound.add(n);
            else
                found.add(de);
        }

        if( found.isEmpty() )
            log.log(Level.SEVERE, "No elements found in the diagram");
        else if( !notFound.isEmpty() )
            log.log(Level.SEVERE, "Elements not found in diagram: " + StreamEx.of(notFound).joining(","));
        SplitDiagramActionParameters parameters = new SplitDiagramActionParameters();
        parameters.setAutoIncludeReactions(true);
        parameters.setAddModule(false);

        DataElementPath.create(path);
        DataElementPath resultPath = DataElementPath.create(path, name);
        parameters.setResultPath(resultPath);

        return action.doSplit(diagram, found, parameters);
    }

    public SpecieReference createSpecieReference(Diagram diagram, String variable, String role)
    {
        return this.createSpecieReference(diagram, variable, role, 1);
    }

    /**
     * Adds reaction to the diagram.
     *
     * @param diagram the diagram to add reaction to.
     * @param formula the formula of the reaction kinetic law.
     * @param fast whether the reaction is fast or not.
     * @param specieReferences an array of the reaction participants. For further details see 'createSpecieReference' function.
     */
    public void addReaction(Diagram diagram, String formula, boolean fast, SpecieReference[] specieReferences)
    {
        try
        {
            Map<String, Object> properties = new HashMap<>();
            KineticLaw kineticLaw = new KineticLaw();
            kineticLaw.setFormula(formula);
            properties.put("kineticLaw", kineticLaw);
            properties.put("fast", fast);
            properties.put("specieReferences", specieReferences);
            add(diagram, new Point(0, 0), Reaction.class, properties);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add the reaction.", e);
        }
    }

    public void addReaction(Diagram diagram, String formula, SpecieReference[] specieReferences)
    {
        this.addReaction(diagram, formula, false, specieReferences);
    }

    /**
     * Adds new specie to the diagram.
     *
     * @param diagram the diagram to add specie to.
     * @param dataElementPath the path to the data element corresponding to some specie in BioUML repository or the name of the specie.
     * @param initialValue initial value of the specie.
     */
    public void addSpecies(Diagram diagram, String dataElementPath, double initialValue)
    {
        try
        {
            if( diagram == null )
            {
                throw new IllegalArgumentException("Diagram is null.");
            }
            if( dataElementPath == null )
            {
                log.log(Level.SEVERE, "Incorrect data element path.");
            }

            DataElementPath path = DataElementPath.create(dataElementPath);
            DataCollection<?> origin = path.optParentCollection();
            Object type;
            if( origin != null )
            {
                type = origin.getDataElementType();
            }
            else
            {
                //SBML model case
                type = "entity";
            }

            DiagramElement node = createElement(diagram, new Point(0, 0), type, null);

            if( node != null )
            {
                String name = path.getName();
                DiagramElement newNode = node.clone((Compartment)node.getOrigin(), name);

                newNode.setTitle(newNode.getName());

                if( putElement(diagram, newNode) )
                {
                    setInitialValue(diagram, name, initialValue);
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add specie.", e);
        }
    }

    public static void addBuses(Diagram diagram) throws Exception
    {
        for( SubDiagram subDiagram : Util.getSubDiagrams(diagram) )
        {

            EModel emodel = diagram.getRole(EModel.class);
            for( Node port : subDiagram.getNodes() )
            {

                if (port.getEdges().length != 0)
                    continue;

                String variableName = Util.getPortVariable(port);
                if( variableName.contains(".") )
                    variableName = variableName.substring(variableName.lastIndexOf(".") + 1);

                if (variableName.startsWith("$"))
                    variableName = variableName.substring(1);

                BusProperties properties = new BusProperties(emodel);
                properties.setName(variableName);

                Point p;
                switch( Util.getPortOrientation(port) )
                {
                    case LEFT:
                    {
                        p = new Point(subDiagram.getLocation().x - 100, port.getLocation().y);
                        break;
                    }
                    case RIGHT:
                    {
                        p = new Point(subDiagram.getLocation().x + subDiagram.getShapeSize().width + 50, port.getLocation().y);
                        break;
                    }
                    case BOTTOM:
                    {
                        p = new Point(port.getLocation().x, subDiagram.getLocation().y + subDiagram.getShapeSize().height + 50);
                        break;
                    }
                    case TOP:
                    {
                        p = new Point(port.getLocation().x, subDiagram.getLocation().y - 100);
                        break;
                    }
                    default:
                    {
                        p = new Point();
                    }
                }

                String type = Util.getPortType(port);

                Node bus = (Node)properties.createElements( diagram, p, null ).getElement();
                bus.setLocation(p);
                diagram.put(bus);

                Edge e;
                biouml.model.dynamics.Connection role;
                if( type.equals("contact") )
                {
                    e = new Edge(new UndirectedConnection(diagram, DefaultSemanticController.generateUniqueNodeName(diagram, "connection")),
                            port, bus);
                    role = new biouml.model.dynamics.UndirectedConnection(e);
                }
                else if( type.equals("input") )
                {
                    e = new Edge(new DirectedConnection(diagram, DefaultSemanticController.generateUniqueNodeName(diagram, "connection")),
                            bus, port);
                    role = new biouml.model.dynamics.DirectedConnection(e);
                }
                else
                {
                    e = new Edge(new DirectedConnection(diagram, DefaultSemanticController.generateUniqueNodeName(diagram, "connection")),
                             port, bus);
                    role = new biouml.model.dynamics.DirectedConnection(e);
                }
                role.setInputPort(new Port(variableName));
                role.setOutputPort(new Port(variableName));
                diagram.put(e);
            }
        }
    }
        
    /**
     * Creates events based on tabular data.
     * Each row of table corresponds to new Event
     * Example: 
     *     ID  timeColumn     parameter1     parameter2
     * rowName     x              v1             v2
     * will be translated to event: When time >= x Do: parameter1 = v1, parameter2 = v2;
     */ 
    public static void generateEvents(TableDataCollection collection, String timeColumn, Diagram diagram)
    {
        SemanticController controller = diagram.getType().getSemanticController();        
        String[] names = TableDataCollectionUtils.getColumnNames( collection );
        int timeIndex = TableDataCollectionUtils.getColumnIndexes( collection, new String[] {timeColumn} )[0];
        Map<String, Double> assignments = new HashMap<String, Double>();
        for (RowDataElement row: collection)
        {
            assignments.clear();
            Object[] values = row.getValues();
            double time = 0;
            for(int i=0; i<row.size(); i++ )
            {
                Double value = (Double)values[i];
                if( value.isNaN() )
                    continue;
                
                if (i == timeIndex)
                {
                    time = value;
                    continue;
                }    
                assignments.put( names[i], value );
            }
            addEvent(diagram, controller, time, assignments);
        }
    }
    
    private static void addEvent(Diagram diagram, SemanticController controller, double time, Map<String, Double> assignments )
    {
        Assignment[] assignmentArray = new Assignment[assignments.size()];
        int index = 0;
        for (Entry<String, Double> assignment: assignments.entrySet())
        {
            double value = assignment.getValue();
            String key = assignment.getKey();
            assignmentArray[index] = new Assignment(key, String.valueOf( value ));
            index++;
        }
        Event event = new Event(null,  "time >= "+time, null, assignmentArray);
        DiagramElementGroup group = controller.createInstance( diagram, Event.class, new Point(), event );
        group.nodesStream().forEach( node->diagram.put( node ) );
    }
    
    public void setInitialValues(Diagram diagram, TableDataCollection table, String rowID)
    {
        DiagramUtility.setInitialValues(diagram, table, rowID);
    }
    
    public SubDiagram[] getSubDiagrams(Diagram diagram)
    {
        return diagram.recursiveStream().select( SubDiagram.class ).toArray( SubDiagram[]::new );
    }
    
    public Reaction[] getReactions(Diagram diagram)
    {
        return diagram.recursiveStream().map(de->de.getKernel()).select(Reaction.class).toArray(Reaction[]::new);
    }
    
    public Equation[] getODE(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getODE().toArray(Equation[]::new);
    }
    
    public Equation[] getAlgebraic(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getAlgebraic().toArray(Equation[]::new);
    }
    
    public Event[] getEvents(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getEvents();
    }

    public Reaction[] getFastReactions(Diagram diagram)
    {
        return diagram.recursiveStream().map(de -> de.getKernel()).select(Reaction.class).filter(r -> r.isFast()).toArray(Reaction[]::new);
    }
}