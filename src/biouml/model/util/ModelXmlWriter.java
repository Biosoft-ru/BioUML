package biouml.model.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.State;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.util.ColorUtils;

public class ModelXmlWriter extends DiagramXmlSupport
{
    protected Document doc;
    protected Map<String, String> newPaths = new HashMap<>();
    
    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }   

    public Element createModel(Diagram diagram, Document document)
    {
        double time = System.currentTimeMillis();
        doc = document;
        this.diagram = diagram;
        
        Element element = null;

        Role role = null;

        if( diagram != null )
        {
            role = diagram.getRole();

            // fix for diagram role bug
            if( role == null && diagram.getType() instanceof PathwaySimulationDiagramType )
            {
                try
                {
                    role = new EModel( diagram );
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE,  "Cannot create new model", t );
                }
            }
        }

        if( role instanceof EModel )
        {
            EModel model = (EModel)role;

            element = doc.createElement( EXECUTABLE_MODEL_ELEMENT );
            element.setAttribute( MODEL_CLASS_ATTR, role.getClass().getName() );

            // write parameters
            for(Variable var: model.getParameters())
            {
                if( !var.getName().equals( TIME_VARIABLE ) )
                    element.appendChild( createParameter( var ) );
            }

            // write variables
            for(VariableRole var: model.getVariableRoles())
                element.appendChild( createVariable( var ) );

            createElementsList( element, diagram );
            createBusList( element, diagram );
            setComment( element, model.getComment() );
            
            Variable timeVar = model.getVariable( TIME_VARIABLE );
            if( !timeVar.getUnits().isEmpty() )
                element.setAttribute( TIME_UNITS_ATTR, timeVar.getUnits() );
        }
        return element;
    }
    
    private Element createParameter(Variable parameter)
    {
        Element element = doc.createElement( PARAMETER_ELEMENT );
        element.setAttribute( NAME_ATTR, validate( parameter.getName() ) );
        element.setAttribute( TITLE_ATTR, validate( parameter.getTitle() ) );
        element.setAttribute( VALUE_ATTR, Double.toString( parameter.getInitialValue() ) );

        if( parameter.isConstant() )
            element.setAttribute( CONST_ATTR, Boolean.toString( parameter.isConstant() ) );

        setOptionalAttribute(element, UNITS_ATTR, parameter.getUnits());
        setComment(element,  parameter.getComment());

        DynamicPropertySet dps = parameter.getAttributes();
        if( !dps.isEmpty() )        
            serializeDPS( doc, element, dps, null, false );

        return element;
    }

    private Element createVariable(VariableRole variable)
    {
        Element element = doc.createElement( VARIABLE_ELEMENT );
        writeDiagramElement( element, variable.getAssociatedElements() );

        element.setAttribute( INITIAL_VALUE_ATTR, Double.toString( variable.getInitialValue() ) );

        setOptionalAttribute(element, UNITS_ATTR, variable.getUnits());

        if( variable.isBoundaryCondition() )
            element.setAttribute( BOUNDARY_CONDITION_ATTR, "true" );

        if( variable.isConstant() )
            element.setAttribute( CONST_ATTR, "true" );

        setComment(element, variable.getComment());
        return element;
    }

    private void createElementsList(Element node, Diagram diagram)
    {
        for( Entry<?, List<Role>> e : diagram.recursiveStream().map( de -> de.getRole() ).nonNull().groupingBy( r -> r.getClass() )
                .entrySet() )
        {
            for( Role role : e.getValue() )
            {
                Element el = createElement( role, diagram );
                if( el != null )
                    node.appendChild( el );
            }
        }
    }

    private Element createElement(Role role, Diagram diagram)
    {
        if( role instanceof Function )
            return createFunction( (Function)role );
        else if( role instanceof Equation )
            return createEquation( (Equation)role );
        else if( role instanceof Constraint )
            return createConstraint( (Constraint)role );
        else if( role instanceof SimpleTableElement )
            return createSimpleTableElement( (SimpleTableElement)role );
        else if( role instanceof TableElement )
            return createTableElement( (TableElement)role );
        else if( role instanceof Event )
            return createEvent( (Event)role, diagram );
        else if( role instanceof State )
            return createState( (State)role );
        else if( role instanceof Transition )
            return createTransition( (Transition)role );
        return null;
    }

    private void createBusList(Element node, Diagram diagram)
    {
        diagram.recursiveStream().map( de -> de.getRole() ).select( Bus.class ).distinct()
                .forEach( equation -> node.appendChild( createBus( equation ) ) );
    }

    private Element createState(State state)
    {
        Element element = doc.createElement( STATE_ELEMENT );
        writeDiagramElement( element, state.getDiagramElement() );

        if( state.isStart() )
            element.setAttribute( STATE_START_ATTR, Boolean.TRUE.toString() );

        Element entryElement = doc.createElement( STATE_ENTRY_ELEMENT );
        Assignment[] assign = state.getOnEntryAssignment();
        for( Assignment element2 : assign )
        {
            Element assignElement = doc.createElement( ASSIGNMENT_ELEMENT );
            String variable = validate( element2.getVariable() );
            assignElement.setAttribute( VARIABLE_ATTR, variable );
            String math = validate( element2.getMath() );
            assignElement.setAttribute( FORMULA_ATTR, math );
            entryElement.appendChild( assignElement );
        }
        element.appendChild( entryElement );

        Element exitElement = doc.createElement( STATE_EXIT_ELEMENT );
        assign = state.getOnExitAssignment();
        for( Assignment element2 : assign )
        {
            Element assignElement = doc.createElement( ASSIGNMENT_ELEMENT );
            String variable = validate( element2.getVariable() );
            assignElement.setAttribute( VARIABLE_ATTR, variable );
            String math = validate( element2.getMath() );
            assignElement.setAttribute( FORMULA_ATTR, math );
            exitElement.appendChild( assignElement );
        }
        element.appendChild( exitElement );

        return element;
    }

    private Element createTransition(Transition transition)
    {
        Element element = doc.createElement( TRANSITION_ELEMENT );
        writeDiagramElement( element, transition.getDiagramElement() );

        String when = transition.getWhen();
        String after = transition.getAfter();

        /**
         * @todo do something with "on_event" !!
         */

        if( after != null && after.length() > 0 )
        {
            Element afterElement = doc.createElement( AFTER_ELEMENT );
            afterElement.setAttribute( AFTER_ATTR, after );
            element.appendChild( afterElement );
        }
        else if( when != null && when.length() > 0 )
        {
            Element whenElement = doc.createElement( WHEN_ELEMENT );
            whenElement.setAttribute( TRIGGER_ATTR, when );
            element.appendChild( whenElement );
        }

        Assignment[] assign = transition.getAssignments();
        for( Assignment element2 : assign )
        {
            Element assignElement = doc.createElement( ASSIGNMENT_ELEMENT );
            assignElement.setAttribute( VARIABLE_ATTR, element2.getVariable() );
            assignElement.setAttribute( FORMULA_ATTR, element2.getMath() );
            element.appendChild( assignElement );
        }
        return element;
    }

    private Element createFunction(Function function)
    {
        Element element = doc.createElement( FUNCTION_ELEMENT );
        writeDiagramElement( element, function.getDiagramElement() );

        String formula = function.getFormula();
        if( formula == null || formula.isEmpty() )
        {
            formula = "";
            warn( "WARN_EQUATION_FORMULA_ABSENTS", new String[] {diagram.getName(), function.getDiagramElement().getName()} );
        }
        element.setAttribute( FORMULA_ATTR, formula );

        return element;
    }

    private Element createSimpleTableElement(SimpleTableElement table)
    {
        Element element = doc.createElement( SIMPLE_TABLE_ELEMENT );
        String path = table.getTablePath().toString();
        if (newPaths.containsKey( path ))
            path = newPaths.get( path );
        writeDiagramElement( element, table.getDiagramElement() );
        element.setAttribute(TABLE_PATH_ATTR, path);
        Element argColumnElement = doc.createElement(ARGCOLUMN_ELEMENT);           
        argColumnElement.setAttribute(TABLE_VARIABLE_ATTR, table.getArgColumn().getVariable());
        argColumnElement.setAttribute(TABLE_COLUMN_ATTR, table.getArgColumn().getColumn());
        for (int i=0; i< table.getColumns().length; i++)
        {
            Element varColumnElement = doc.createElement(VARCOLUMN_ELEMENT);
            varColumnElement.setAttribute(TABLE_VARIABLE_ATTR, table.getColumns()[i].getVariable());
            varColumnElement.setAttribute(TABLE_COLUMN_ATTR, table.getColumns()[i].getColumn());
            element.appendChild( varColumnElement );
        }                
        element.appendChild( argColumnElement );
        return element;
    }
    
    private Element createTableElement(TableElement table)
    {
        Element element = doc.createElement( TABLE_ELEMENT );
        writeDiagramElement( element, table.getDiagramElement() );
        if( table.getVariables() != null )
        {
            for (TableElement.Variable var: table.getVariables())
            {
                Element varElement = doc.createElement( VARIABLE_ATTR );
                varElement.setAttribute( TABLE_COLUMN_ATTR, var.getColumnName() );
                varElement.setAttribute( "name", var.getName() );
                element.appendChild( varElement );
            }
        }
        else
            warn( "ERROR_TABLE_VARIABLE_ABSENTS", new String[] {diagram.getName(), table.getDiagramElement().getName()} );

        if( table.getTablePath() != null )
            element.setAttribute( TABLE_ATTR, table.getTablePath().toString() );

        if( table.getFormula() != null && !table.getFormula().isEmpty() )
            element.setAttribute( FORMULA_ATTR, table.getFormula() );
        else
            warn( "ERROR_TABLE_DATA_ABSENTS", new String[] {diagram.getName(), table.getDiagramElement().getName()} );

        if( !table.getSplineType().isEmpty() )
            element.setAttribute( SPLINE_TYPE_ATTR, table.getSplineType() );

        if( table.isCycled() )
            element.setAttribute( CYCLED_ATTR, String.valueOf( table.isCycled()) );
        return element;
    }

    private Element createEquation(Equation equation)
    {
        Element element = doc.createElement( EQUATION_ELEMENT );
        writeDiagramElement( element, equation.getDiagramElement() );

        // skip algebraic equations
        if( equation.getVariable() != null )
            element.setAttribute( VARIABLE_ATTR, equation.getVariable() );

        String formula = equation.getFormula();
        if( formula == null || formula.length() == 0 )
        {
            formula = "";
            warn( "WARN_EQUATION_FORMULA_ABSENTS", new String[] {diagram.getName(), equation.getDiagramElement().getName()} );
        }
        element.setAttribute( FORMULA_ATTR, formula );

        if( !Equation.TYPE_RATE.equals( equation.getType() ) )
            element.setAttribute( EQUATION_TYPE_ATTR, equation.getType() );

        setOptionalAttribute(element, UNITS_ATTR, equation.getUnits());
        setComment(element, equation.getComment());
        return element;
    }

    private Element createConstraint(Constraint constraint)
    {
        Element element = doc.createElement( CONSTRAINT_ELEMENT );
        writeDiagramElement( element, constraint.getDiagramElement() );
        element.setAttribute( FORMULA_ATTR, constraint.getFormula() );
        setComment( element, constraint.getComment() );
        return element;
    }

    private Element createEvent(Event event, Diagram diagram)
    {
        Element element = doc.createElement( EVENT_ELEMENT );
        Assignment[] actions = event.getEventAssignment();

        writeDiagramElement( element, event.getDiagramElement() );
        element.setAttribute( TRIGGER_ATTR, event.getTrigger() );

        if( event.isTriggerInitialValue() == false )
            element.setAttribute( TRIGGER_INITIAL_VALUE_ATTR, String.valueOf( event.isTriggerInitialValue() ) );
        if( event.isTriggerPersistent() == false )
            element.setAttribute( TRIGGER_PERSISTENT_ATTR, String.valueOf( event.isTriggerPersistent() ) );
        if( event.isUseValuesFromTriggerTime() == false )
            element.setAttribute( USE_VALUES_FROM_TRIGGER_TIME_ATTR, String.valueOf( event.isUseValuesFromTriggerTime() ) );

        setOptionalAttribute(element, DELAY_ATTR, event.getDelay());
        setOptionalAttribute(element, PRIORITY_ATTR, event.getPriority());
        setComment(element, event.getComment());

        for( Assignment action : actions )
            element.appendChild( createAssignment( action, diagram ) );

        return element;
    }

    private Element createAssignment(Assignment action, Diagram diagram)
    {
        Element element = doc.createElement( ASSIGNMENT_ELEMENT );
        element.setAttribute( VARIABLE_ATTR, validate( action.getVariable() ) );
        element.setAttribute( FORMULA_ATTR, validate( action.getMath() ) );
        return element;
    }
    
    private Element createUnit(Unit unit)
    {
        Element element = doc.createElement( UNIT_DEFINITION_ELEMENT );
        String name = unit.getName();
        String title = unit.getTitle();
        BaseUnit[] baseUnits = unit.getBaseUnits();
        element.setAttribute( "name", name );
        element.setAttribute( "title", title );
        
        for (BaseUnit baseUnit: baseUnits)
        {
            Element baseElement = doc.createElement( BASE_UNIT_ELEMENT );
            baseElement.setAttribute( BASE_UNIT_TYPE_ATTR, String.valueOf( baseUnit.getType()) );
            baseElement.setAttribute( MULTIPLIER_ATTR, String.valueOf( baseUnit.getMultiplier()) );
            baseElement.setAttribute( SCALE_ATTR, String.valueOf( baseUnit.getScale()) );
            baseElement.setAttribute( EXPONENT_ATTR, String.valueOf( baseUnit.getExponent()) );
            
            element.appendChild( baseElement );
        }        
        return element;
    }
    
    private Element createBus(Bus bus)
    {
        Element element = doc.createElement( BUS_ELEMENT );
        writeDiagramElement( element, bus.getNodes().toArray( new Node[bus.getNodes().size()] ) );
        element.setAttribute( COLOR_ATTR, ColorUtils.paintToString( bus.getColor() ) );
        element.setAttribute( BUS_NAME_ATTR, bus.getName() );
        return element;
    }

    protected void writeDiagramElement(Element element, DiagramElement de)
    {
        String name = CollectionFactory.getRelativeName( de, diagram );
        element.setAttribute( DIAGRAM_ELEMENT_ATTR, validate( name ) );
    }

    private void writeDiagramElement(Element element, DiagramElement[] des)
    {
        String diagramElement = StreamEx.of( des ).map( de -> CollectionFactory.getRelativeName( de, diagram ) )
                .map( DiagramXmlWriter::validate ).joining( ";" );
        element.setAttribute( DIAGRAM_ELEMENT_ATTR, diagramElement );
    }
}