package biouml.model.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.EModel.DiagramVariableResolver;
import biouml.model.dynamics.EModelRoleSupport;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.State;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.Bus;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.Brush;
import ru.biosoft.math.model.UndeclaredFunction;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlUtil;

public class ModelXmlReader extends DiagramXmlSupport
{
    /**
     * Mapping between initial paths and paths in xml document. In some cases they may be different, e.g. when xml document is a part of omex archive.
     */
    protected Map<String, String> newPaths = new HashMap<>();

    public ModelXmlReader(Diagram diagram)
    {
        this.diagram = diagram;
    }

    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }

    public EModelRoleSupport readModel(Element element)
    {
        EModel model = null;
        try
        {
            String className = element.getAttribute( MODEL_CLASS_ATTR );
            if( className == null || className.indexOf( "MatlabOdeModel" ) >= 0 )
                model = new EModel( diagram );
            else
            {
                Class<? extends EModel> clazz = ClassLoading.loadSubClass( className, EModel.class );
                model = clazz.getConstructor( DiagramElement.class ).newInstance( diagram );
            }
            readParameters( element, model );
            readVariables( element, model );
            DiagramVariableResolver resolver = model.getVariableResolver( EModel.VARIABLE_NAME_BY_ID );
            readFunctions( element, model, resolver );
            readEquations( element, model, resolver );
            readConstraints( element, model, resolver );
            readEvents( element );
            readTables( element );
            readSimpleTables( element );
            readStates( element );
            readTransitions( element );
            readUnits( element, model );
            readBuses( element, diagram );

            if( element.hasAttribute( TIME_UNITS_ATTR ) )
                model.getVariable( TIME_VARIABLE ).setUnits( element.getAttribute( TIME_UNITS_ATTR ) );
            if( element.hasAttribute( COMMENT_ATTR ) )
                model.setComment( element.getAttribute( COMMENT_ATTR ) );
        }
        catch( Throwable t )
        {
            error( "ERROR_EXECUTABLE_MODEL", new String[] {diagram.getName(), t.getMessage()}, t );
        }
        if( model != null )
            model.setPropagationEnabled( true );
        return model;
    }

    protected void readParameters(Element modelElement, EModel model)
    {
        NodeList list = modelElement.getElementsByTagName( PARAMETER_ELEMENT );
        for( Element element : XmlUtil.elements( list ) )
        {
            String name = element.getAttribute( NAME_ATTR );
            try
            {
                model.put( readParameter( element, name, model ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_CONSTANT_PROCESSING", new String[] {diagram.getName(), name, t.getMessage()} );
            }
        }
    }

    protected Variable readParameter(Element element, String name, EModel model)
    {
        Variable parameter = new Variable( name, model, null );
        String title = element.getAttribute( TITLE_ATTR );
        if( !title.isEmpty() )
            parameter.setTitle( title );
        String value = element.getAttribute( VALUE_ATTR );
        try
        {
            if( value.isEmpty() )
                warn( "WARN_PARAMETER_VALUE_ABSENTS", new String[] {diagram.getName(), name} );
            else
                parameter.setInitialValue( Double.parseDouble( value ) );
        }
        catch( Throwable t )
        {
            error( "ERROR_PARAMETER_VALUE", new String[] {diagram.getName(), name, value, t.getMessage()} );
        }

        if( element.hasAttribute( UNITS_ATTR ) )
            parameter.setUnits( element.getAttribute( UNITS_ATTR ) );
        if( element.hasAttribute( CONST_ATTR ) )
            parameter.setConstant( Boolean.parseBoolean( element.getAttribute( CONST_ATTR ) ) );
        if( element.hasAttribute( COMMENT_ATTR ) )
            parameter.setComment( element.getAttribute( COMMENT_ATTR ) );
        if( element.hasChildNodes() )
            DiagramXmlReader.readDPS( element, null ).forEach( dp -> parameter.getAttributes().add( dp ) );
        return parameter;
    }

    protected void readVariables(Element modelElement, EModel model)
    {
        NodeList list = modelElement.getElementsByTagName( VARIABLE_ELEMENT );
        for( Element element : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = element.getAttribute( DIAGRAM_ELEMENT_ATTR );
                VariableRole var = readVariable( element, diagramElement );
                if( var != null )
                    model.put( var );
            }
            catch( Throwable t )
            {
                error( "ERROR_VARIABLE_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()}, t );
            }
        }
    }

    /**
     * Transform old style buses into new style (with Bus role instead of VariableRole)
     * @param diagramElements - node names corresponding to single bus 
     */
    private void fixBuses(String[] diagramElements)
    {
        String name = diagramElements[0];
        Bus bus = new Bus( name, false );
        bus.setColor( null );
        for( String s : diagramElements )
        {
            DiagramElement de = getDiagramElement( s );
            if( de == null || ! ( de instanceof Node ) )
                continue;
            de.setRole( bus );
            bus.addNode( (Node)de );
            Brush b = (Brush)de.getAttributes().getValue( COLOR_ATTR );
            de.getAttributes().remove( COLOR_ATTR );
            bus.setColor( b.getColor() );
        }
    }

    protected VariableRole readVariable(Element element, String diagramElement)
    {
        String[] diagramElements = TextUtil2.split( diagramElement, ';' );
        DiagramElement de = getDiagramElement( diagramElements[0] );
        if( de == null )
            return null;
        else if( de.getKernel() instanceof Stub.Bus )
        {
            fixBuses( diagramElements );
            return null;
        }
        VariableRole variable = new VariableRole( de, 0.0 );
        de.setRole( variable );

        for( int i = 1; i < diagramElements.length; i++ )
        {
            DiagramElement associatedDE = getDiagramElement( diagramElements[i] );
            if( associatedDE != null )
            {
                variable.addAssociatedElement( associatedDE );
                associatedDE.setRole( variable );
            }
        }
        String initialValue = element.getAttribute( INITIAL_VALUE_ATTR );
        try
        {
            if( initialValue.isEmpty() )
                warn( "WARN_VARIABLE_VALUE_ABSENTS", new String[] {diagram.getName(), diagramElement} );
            else
                variable.setInitialValue( Double.parseDouble( initialValue ) );
        }
        catch( Throwable t )
        {
            error( "ERROR_VARIABLE_VALUE", new String[] {diagram.getName(), diagramElement, initialValue, t.getMessage()} );
        }
        if( element.hasAttribute( UNITS_ATTR ) )
            variable.setUnits( element.getAttribute( UNITS_ATTR ) );
        if( element.hasAttribute( BOUNDARY_CONDITION_ATTR ) && "true".equals( element.getAttribute( BOUNDARY_CONDITION_ATTR ) ) )
            variable.setBoundaryCondition( true );
        if( element.hasAttribute( CONST_ATTR ) && "true".equals( element.getAttribute( CONST_ATTR ) ) )
            variable.setConstant( true );
        if( element.hasAttribute( COMMENT_ATTR ) )
            variable.setComment( element.getAttribute( COMMENT_ATTR ) );
        return variable;
    }

    protected void readFunctions(Element modelElement, EModel model, DiagramVariableResolver resolver)
    {
        NodeList list = modelElement.getElementsByTagName( FUNCTION_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String formula = child.getAttribute( FORMULA_ATTR );
            if( formula.isEmpty() )
                continue;
            try
            {
                String name = formula.substring( formula.indexOf( ' ' ) + 1, formula.indexOf( '(' ) );
                if( model.getFunction( name ) == null )
                    model.declareFunction( new UndeclaredFunction( name, ru.biosoft.math.model.Function.FUNCTION_PRIORITY ) );
            }
            catch( Throwable t )
            {
                continue;
            }
        }
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readFunction( child, diagramElement, model, resolver );
            }
            catch( Throwable t )
            {
                error( "ERROR_EQUATION_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readEquations(Element modelElement, EModel model, DiagramVariableResolver resolver)
    {
        NodeList list = modelElement.getElementsByTagName( EQUATION_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readEquation( child, diagramElement, model, resolver );
            }
            catch( Throwable t )
            {
                error( "ERROR_EQUATION_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readConstraints(Element modelElement, EModel model, DiagramVariableResolver resolver)
    {
        NodeList list = modelElement.getElementsByTagName( CONSTRAINT_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readConstraint( child, diagramElement, model, resolver );
            }
            catch( Throwable t )
            {
                error( "ERROR_CONSTRAINT_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readSimpleTables(Element modelElement)
    {
        NodeList list = modelElement.getElementsByTagName( SIMPLE_TABLE_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readSimpleTable( child, diagramElement );
            }
            catch( Throwable t )
            {
                error( "ERROR_TABLE_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readTables(Element modelElement)
    {
        NodeList list = modelElement.getElementsByTagName( TABLE_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readTable( child, diagramElement );
            }
            catch( Throwable t )
            {
                error( "ERROR_TABLE_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readSimpleTable(Element element, String diagramElement)
    {
        Node de = getDiagramElement( diagramElement, Node.class );
        if( de == null )
            return;
        SimpleTableElement table = new SimpleTableElement( de );
        String path = element.getAttribute( TABLE_PATH_ATTR );
        if( newPaths != null && newPaths.containsKey( path ) )
            path = newPaths.get( path );

        table.setTablePath( DataElementPath.create( path ) );
        Element argElement = XmlUtil.getChildElement( element, ARGCOLUMN_ELEMENT );
        table.getArgColumn().setColumn( argElement.getAttribute( TABLE_COLUMN_ATTR ) );
        table.getArgColumn().setVariable( argElement.getAttribute( TABLE_VARIABLE_ATTR ) );
        List<VarColumn> columns = new ArrayList<>();
        for( Element child : XmlUtil.elements( element, VARCOLUMN_ELEMENT ) )
        {
            VarColumn column = new VarColumn();
            column.setColumn( child.getAttribute( TABLE_COLUMN_ATTR ) );
            column.setVariable( child.getAttribute( TABLE_VARIABLE_ATTR ) );
            columns.add( column );
        }
        table.setColumns( StreamEx.of( columns ).toArray( VarColumn[]::new ) );
        de.setRole( table );
    }

    protected void readTable(Element element, String diagramElement)
    {
        Node de = getDiagramElement( diagramElement, Node.class );
        if( de == null )
            return;
        TableElement tableElement = new TableElement( de );
        String path = getRequiredAttribute( element, TABLE_ATTR, diagram.getName() );
        DataElementPath tablePath = DataElementPath.create( path );
        DataElement tdc = null;
        try
        {
            tdc = tablePath.getDataElement();
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, ExceptionRegistry.log( ex ) );
        }
        if( tdc != null && tdc instanceof TableDataCollection )
        {
            tableElement.setTablePath( tablePath );
            tableElement.setFormula( element.getAttribute( FORMULA_ATTR ) );
            tableElement.setSplineType( element.getAttribute( SPLINE_TYPE_ATTR ) );
            NodeList list = element.getChildNodes();
            for( Element child : XmlUtil.elements( list ) )
            {
                if( VARIABLE_ATTR.equals( child.getNodeName() ) )
                {
                    String name = getRequiredAttribute( child, "name", diagram.getName() );
                    String columnName = getRequiredAttribute( child, "column", diagram.getName() );
                    for( TableElement.Variable var : tableElement.getVariables() )
                    {
                        if( var.getColumnName().equals( columnName ) )
                            var.setName( name );
                    }
                }
            }
        }
        if( "true".equals( element.getAttribute( CYCLED_ATTR ) ) )
            tableElement.setCycled( true );
        de.setRole( tableElement );
    }

    protected void readEvents(Element modelElement)
    {
        NodeList list = modelElement.getElementsByTagName( EVENT_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readEvent( child, diagramElement );
            }
            catch( Throwable t )
            {
                error( "ERROR_EVENT_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readEvent(Element element, String diagramElement)
    {
        Node de = getDiagramElement( diagramElement, Node.class );
        if( de == null )
            return;
        String trigger = getRequiredAttribute( element, TRIGGER_ATTR, diagram.getName() );
        if( trigger == null )
            return;
        String delay = element.getAttribute( DELAY_ATTR );
        readComment( de, element );
        NodeList list = element.getElementsByTagName( ASSIGNMENT_ELEMENT );
        List<Assignment> assign = new ArrayList<>();
        for( Element child : XmlUtil.elements( list ) )
        {
            try
            {
                assign.add( readAssignment( child ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_ASSIGNMENT_PROCESSING", new String[] {diagram.getName(), de.getName(), t.getMessage()} );
            }
        }
        Event event = new Event( de, trigger, delay, assign.toArray( new Assignment[assign.size()] ) );
        String persistent = element.getAttribute( TRIGGER_PERSISTENT_ATTR );
        if( !persistent.isEmpty() )
            event.setTriggerPersistent( Boolean.parseBoolean( persistent ) );
        String initial = element.getAttribute( TRIGGER_INITIAL_VALUE_ATTR );
        if( !initial.isEmpty() )
            event.setTriggerInitialValue( Boolean.parseBoolean( initial ) );
        String triggerTime = element.getAttribute( USE_VALUES_FROM_TRIGGER_TIME_ATTR );
        if( !triggerTime.isEmpty() )
            event.setUseValuesFromTriggerTime( Boolean.parseBoolean( triggerTime ) );
        String priority = element.getAttribute( PRIORITY_ATTR );
        if( !priority.isEmpty() )
            event.setPriority( priority );
        de.setRole( event );
    }

    protected void readStates(Element element)
    {
        NodeList list = element.getElementsByTagName( STATE_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readState( child, diagramElement );
            }
            catch( Throwable t )
            {
                error( "ERROR_STATE_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readState(Element element, String diagramElement)
    {
        DiagramElement de = getDiagramElement( diagramElement );
        if( de == null )
            return;
        State state = new State( de );
        //read start flag
        String isStart = element.getAttribute( STATE_START_ATTR );
        state.setStart( Boolean.parseBoolean( isStart ) );
        NodeList entryList = element.getElementsByTagName( STATE_ENTRY_ELEMENT );
        // we want only one entry element
        if( entryList.getLength() > 0 )
        {
            Element entryElement = (Element)entryList.item( 0 );
            NodeList assignList = entryElement.getElementsByTagName( ASSIGNMENT_ELEMENT );
            for( Element child : XmlUtil.elements( assignList ) )
            {
                try
                {
                    Assignment assign = readAssignment( child );
                    state.addOnEntryAssignment( assign, false );
                }
                catch( Throwable t )
                {
                    error( "ERROR_STATE_ENTRY_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()}, t );
                }
            }
        }
        NodeList exitList = element.getElementsByTagName( STATE_EXIT_ELEMENT );
        // we want only one exit element
        if( exitList.getLength() > 0 )
        {
            Element exitElement = (Element)exitList.item( 0 );
            NodeList assignList = exitElement.getElementsByTagName( ASSIGNMENT_ELEMENT );
            for( Element child : XmlUtil.elements( assignList ) )
            {
                try
                {
                    state.addOnExitAssignment( readAssignment( child ), false );
                }
                catch( Throwable t )
                {
                    error( "ERROR_STATE_EXIT_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()}, t );
                }
            }
        }
        NodeList onEventList = element.getElementsByTagName( STATE_ON_EVENT_ELEMENT );
        // we want only one "onEvent" element
        if( onEventList.getLength() > 0 )
        {
            Element onEventElement = (Element)onEventList.item( 0 );
            NodeList assignList = onEventElement.getElementsByTagName( ASSIGNMENT_ELEMENT );
            for( Element child : XmlUtil.elements( assignList ) )
            {
                try
                {
                    // TODO:
                    // Assignment assign = readAssignment(entryElement, model);
                    //     state.addOnExitAssignment(assign, false);
                }
                catch( Throwable t )
                {
                    error( "ERROR_STATE_ON_EVENT_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()}, t );
                }
            }
        }
        de.setRole( state );
    }

    protected void readTransitions(Element element)
    {
        NodeList list = element.getElementsByTagName( TRANSITION_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String diagramElement = null;
            try
            {
                diagramElement = child.getAttribute( DIAGRAM_ELEMENT_ATTR );
                readTransition( child, diagramElement );
            }
            catch( Throwable t )
            {
                error( "ERROR_TRANSITION_PROCESSING", new String[] {diagram.getName(), diagramElement, t.getMessage()} );
            }
        }
    }

    protected void readTransition(Element element, String diagramElement)
    {
        DiagramElement de = getDiagramElement( diagramElement );
        if( de == null )
            return;
        Element whenElement = XmlUtil.findElementByTagName( element, WHEN_ELEMENT );
        Element afterElement = XmlUtil.findElementByTagName( element, AFTER_ELEMENT );
        if( whenElement != null && afterElement != null )
        {
            error( "ERROR_HAS_WHEN_HAS_AFTER", new String[] {diagram.getName(), diagramElement} );
            return;
        }
        Transition transition = new Transition( de );
        if( whenElement != null )
            transition.setWhen( whenElement.getAttribute( TRIGGER_ATTR ) );
        if( afterElement != null )
            transition.setAfter( afterElement.getAttribute( AFTER_ATTR ) );
        de.setRole( transition );
    }

    protected void readFunction(Element element, String diagramElement, EModel model, DiagramVariableResolver resolver)
    {
        DiagramElement de = getDiagramElement( diagramElement );
        if( de == null )
            return;
        Function function = new Function( de );
        de.setRole( function );
        String formula = element.getAttribute( FORMULA_ATTR );
        if( formula.isEmpty() )
        {
            warn( "WARN_EQUATION_FORMULA_ABSENTS", new String[] {diagram.getName(), diagramElement} );
            formula = "0";
        }
        function.setFormula( formula );// validate the expression
        model.readMath( function.getFormula(), function, resolver );
    }

    protected void readConstraint(Element element, String diagramElement, EModel model, DiagramVariableResolver resolver)
    {
        DiagramElement de = getDiagramElement( diagramElement );
        if( de == null )
            return;
        Constraint constraint = new Constraint( de );
        de.setRole( constraint );

        String formula = element.getAttribute( FORMULA_ATTR );
        if( formula.isEmpty() )
            warn( "WARN_CONSTRAINT_FORMULA_ABSENTS", new String[] {diagram.getName(), diagramElement} );
        else
        {
            constraint.setFormula( formula );
            model.readMath( constraint.getFormula(), constraint, resolver );
        }
        if( element.hasAttribute( COMMENT_ATTR ) )
            constraint.setComment( element.getAttribute( COMMENT_ATTR ) );
    }

    protected void readEquation(Element element, String diagramElement, EModel model, DiagramVariableResolver resolver)
    {
        DiagramElement de = getDiagramElement( diagramElement );
        if( de == null )
            return;
        String variableName = element.getAttribute( VARIABLE_ATTR );
        //dirty hack for name normalization ($"compartment.node" => $compartment.node )
        Variable variable = model.getVariable( variableName );
        if( variable != null )
            variableName = variable.getName();

        Reaction reaction = null;

        // check variable
        if( de instanceof Edge )
        {
            Edge edge = (Edge)de;
            boolean resolved = edge.nodes().map( Node::getRole ).select( VariableRole.class ).map( VariableRole::getName )
                    .has( variableName );

            reaction = edge.nodes().map( Node::getKernel ).select( Reaction.class ).findFirst().orElse( null );
            if( !resolved )
            {
                warn( "WARN_EQUATION_VARIABLE_UNRESOLVED", new String[] {diagram.getName(), diagramElement, variableName} );
                // if some variable is null - try to create it
                if( edge.nodes().map( Node::getRole ).has( null ) )
                {
                    DiagramElement bad = edge.getInput().getRole() == null ? edge.getInput() : edge.getOutput();
                    VariableRole var = new VariableRole( bad, 0 );
                    if( var.getName().equals( variableName ) )
                    {
                        bad.setRole( var );
                        model.put( var );
                        resolver.addVariable( var );
                        warn( "WARN_MISSED_VARIABLE_DECLARATION", new String[] {diagram.getName(), bad.getName(), variableName} );
                    }
                }
            }
        }
        else if( de.getKernel() instanceof Reaction )
        {
            reaction = (Reaction)de.getKernel();
            model.put( new Variable( variableName, model, null ) ); //auxiliary variables $$reaction_rate are not explicitly declared in dml, need to add them here
        }
        String type = Equation.TYPE_RATE;
        if( element.hasAttribute( EQUATION_TYPE_ATTR ) )
            type = element.getAttribute( EQUATION_TYPE_ATTR );
        Equation equation = new Equation( de, type, variableName );
        de.setRole( equation );

        String formula = element.getAttribute( FORMULA_ATTR );
        if( formula.isEmpty() )
            warn( "WARN_EQUATION_FORMULA_ABSENTS", new String[] {diagram.getName(), diagramElement} );
        else if( !equation.hasDelegate() )
        {
            //if equation has delegate then its formula will be derived from it and we should not set it from here!
            //for example if we change equation (delegate) formula in diagram and then save it, it will read old diagram at first
            //and reset delegate formula here, which will reset changes made by user!
            equation.setFormula( formula );
            // validate the expression
            model.readMath( equation.getFormula(), equation, resolver );
        }
        if( element.hasAttribute( UNITS_ATTR ) )
            equation.setUnits( element.getAttribute( UNITS_ATTR ) );

        if( element.hasAttribute( COMMENT_ATTR ) )
            equation.setComment( element.getAttribute( COMMENT_ATTR ) );

        if( reaction != null )
            equation.setFast( reaction.isFast() );
    }

    protected @Nonnull Assignment readAssignment(Element element)
    {
        return new Assignment( element.getAttribute( VARIABLE_ATTR ), element.getAttribute( FORMULA_ATTR ) );
    }

    protected void readUnits(Element element, EModel model)
    {
        NodeList list = element.getElementsByTagName( UNIT_DEFINITION_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            try
            {
                String name = child.getAttribute( NAME_ATTR );
                String title = child.getAttribute( TITLE_ATTR );
                Unit unit = new Unit( null, name );
                unit.setTitle( title );
                List<BaseUnit> baseUnits = new ArrayList<>();
                NodeList baseUnitList = child.getElementsByTagName( BASE_UNIT_ELEMENT );
                for( Element baseUnitElement : XmlUtil.elements( baseUnitList ) )
                {
                    BaseUnit baseUnit = new BaseUnit();
                    baseUnit.setType( baseUnitElement.getAttribute( BASE_UNIT_TYPE_ATTR ) );
                    baseUnit.setMultiplier( Double.parseDouble( baseUnitElement.getAttribute( MULTIPLIER_ATTR ) ) );
                    baseUnit.setExponent( Integer.parseInt( baseUnitElement.getAttribute( EXPONENT_ATTR ) ) );
                    baseUnit.setScale( Integer.parseInt( baseUnitElement.getAttribute( SCALE_ATTR ) ) );
                    baseUnits.add( baseUnit );
                }
                unit.setBaseUnits( baseUnits.toArray( new BaseUnit[baseUnits.size()] ) );
                model.addUnit( unit );
            }
            catch( Throwable t )
            {
                //TODO: throw error 
            }
        }
    }

    protected void readBuses(Element modelElement, Diagram diagram)
    {
        NodeList list = modelElement.getElementsByTagName( BUS_ELEMENT );
        for( Element child : XmlUtil.elements( list ) )
        {
            String name = child.getAttribute( BUS_NAME_ATTR );
            try
            {
                readBus( child, name, diagram );
            }
            catch( Throwable t )
            {
                error( "ERROR_BUS_PROCESSING", new String[] {diagram.getName(), name, t.getMessage()} );
            }
        }
    }

    protected void readBus(Element element, String name, Diagram diagram)
    {
        String deNamesAttr = element.getAttribute( DIAGRAM_ELEMENT_ATTR );
        Bus bus = new Bus( name, false );
        bus.setColor( (Color)DiagramXmlReader.stringToColor( element.getAttribute( COLOR_ATTR ), diagram.getName(), name ) );
        for( String nodeName : deNamesAttr.trim().split( ";" ) )
        {
            Node node = getDiagramElement( nodeName, Node.class );
            if( node == null )
                continue;
            node.setRole( bus );
            bus.addNode( node );
        }
    }
}