package biouml.model.web;

import java.awt.Color;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.EquationBeanInfo.EquationTypeEditor;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.FormulaEditor;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.type.Unit;
import biouml.workbench.diagram.viewpart.ConnectionSimple;
import biouml.workbench.diagram.viewpart.ModelViewPart;
import biouml.workbench.diagram.viewpart.PortSimple;
import biouml.workbench.diagram.viewpart.ReactionSimple;
import biouml.workbench.diagram.viewpart.SubDiagramSimple;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramModelTableResolver extends TableResolver
{
    public static final String TABLE_TYPE_PARAMETER = "tabletype";
    
    protected String type;
    protected String subDiagram;
    public DiagramModelTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        this.type = arguments.getString(TABLE_TYPE_PARAMETER);
        this.subDiagram = arguments.get( "subDiagram" );
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        Diagram diagram = de.cast( Diagram.class );
        Object modelObj = diagram.getRole();
        if( subDiagram != null )
        {
            SubDiagram sub = Util.getSubDiagram( diagram, subDiagram );
            if( sub != null )
                modelObj = sub.getDiagram().getRole();
        }
        if( modelObj instanceof EModel )
        {

            EModel emodel = ( (EModel)modelObj );
            // Copying of collection is necessary as original collections are FilterDataCollection and
            // then columnModel is created it uses non-filtered original collection
            // (see ru.biosoft.table.TableDataCollectionUtils.getColumnModel)
            if( "variables".equals( type ) )
            {
                VectorDataCollection<Variable> result = new VectorDataCollection<>("Parameters", Variable.class, null);
                for(Variable parameter : ( (EModel)modelObj ).getParameters())
                {
                    result.put(parameter);
                }
                return result;
            }
            if( "variableroles".equals( type ) )
            {
                VectorDataCollection<VariableRole> result = new VectorDataCollection<>( "Parameters", VariableRole.class, null );
                for( VariableRole parameter : ( (EModel)modelObj ).getVariableRoles() )
                {
                    result.put( parameter );
                }
                return result;
            }
            else if( "entities".equals( type ) )
            {
                VectorDataCollection<VariableRole> result = new VectorDataCollection<>("Variables", VariableRole.class, null);
                for( VariableRole variable : ( (EModel)modelObj ).getEntityRoles() )
                {
                    result.put(variable);
                }
                return result;
            }
            else if( "units".equals( type ) )
            {
                VectorDataCollection<UnitWrapper> result = new VectorDataCollection<>( "Units", UnitWrapper.class, null );
                for( Unit unit : ( (EModel)modelObj ).getUnits().values() )
                {
                    result.put( new UnitWrapper( unit ) );
                }
                return result;
            }
            else if( "compartments".equals( type ) )
            {
                VectorDataCollection<VariableRole> result = new VectorDataCollection<>( "Compartments", VariableRole.class, null );
                for( VariableRole variable : ( (EModel)modelObj ).getCompartmentRoles() )
                {
                    result.put( variable );
                }
                return result;
            }
            else if( "equations".equals( type ) )
            {

                Filter<DiagramElement> filter = new ModelViewPart.RuleFilter();
                VectorDataCollection<EquationWrapper> result = new VectorDataCollection<>( "Equations", EquationWrapper.class, null );
                emodel.getChildrenRoles( emodel.getParent(), Equation.class, filter )
                        .map( e -> new EquationWrapper( e ) ).forEach( ew -> result.put( ew ));
                return result;
            }
            else if( "functions".equals( type ) )
            {
                VectorDataCollection<FunctionWrapper> result = new VectorDataCollection<>( "Functions", FunctionWrapper.class, null );
                emodel.getChildrenRoles( emodel.getParent(), Function.class ).map( e -> new FunctionWrapper( e ) )
                        .forEach( fw -> result.put( fw ) );
                return result;
            }
            else if( "events".equals( type ) )
            {
                VectorDataCollection<EventWrapper> result = new VectorDataCollection<>( "Events", EventWrapper.class, null );
                emodel.getChildrenRoles( emodel.getParent(), Event.class ).map( e -> new EventWrapper( e ) )
                        .forEach( ev -> result.put( ev ) );
                return result;
            }
            else if( "constraints".equals( type ) )
            {
                VectorDataCollection<ConstraintWrapper> result = new VectorDataCollection<>( "Constraints", ConstraintWrapper.class, null );
                emodel.getChildrenRoles( emodel.getParent(), Constraint.class )
                        .map( e -> new ConstraintWrapper( e ) ).forEach( cw -> result.put( cw ) );
                return result;
            }
            else if( "reactions".equals(type) )
            {
                VectorDataCollection<ReactionSimple> result = new VectorDataCollection<>("Reactions", ReactionSimple.class, null);
                StreamEx.of( DiagramUtility.getReactions( diagram ) ).map( r -> new ReactionSimple( r ) ).forEach( r -> result.put( r ) );
                return result;
            }
            else if( "ports".equals( type ) )
            {
                VectorDataCollection<PortSimple> result = new VectorDataCollection<>( "Ports", PortSimple.class, null );
                DiagramUtility.getTopLevelPorts( diagram ).map( r -> new PortSimple( r ) ).forEach( port -> result.put( port ) );
                return result;
            }
            else if( "connections".equals( type ) )
            {
                VectorDataCollection<ConnectionSimple> result = new VectorDataCollection<>( "Connections", ConnectionSimple.class, null );
                DiagramUtility.getConnections( diagram ).map( n -> new ConnectionSimple( n ) ).forEach( c -> result.put( c ) );
                return result;
            }
            else if( "subdiagrams".equals( type ) )
            {
                VectorDataCollection<SubDiagramSimple> result = new VectorDataCollection<>( "Subdiagrams", SubDiagramSimple.class, null );
                DiagramUtility.getSubDiagrams( diagram ).map( n -> new SubDiagramSimple( n ) ).forEach( c -> result.put( c ) );
                return result;
            }
            else if( "buses".equals( type ) )
            {
                VectorDataCollection<BusWrapper> result = new VectorDataCollection<>( "Buses", BusWrapper.class, null );
                DiagramUtility.getBuses( diagram ).map( b -> new BusWrapper( b.getRole( Bus.class ) ) ).forEach( c -> result.put( c ) );
                return result;
            }
        }
        return null;
    }

    public static class BusWrapper implements DataElement
    {
        private Bus bus;
        private Color color;
        public BusWrapper()
        {
            this.bus = new Bus( "", false );
        }

        public BusWrapper(Bus bus)
        {
            this.bus = bus;
        }

        public String getName()
        {
            return bus.getName();
        }

        public void setName(String name)
        {

        }

        @PropertyName ( "Color" )
        public Color getColor()
        {
            return color;
        }

        public void setColor(Color color)
        {
            this.color = color;
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public static class BusWrapperBeanInfo extends BeanInfoEx2<BusWrapper>
    {
        public BusWrapperBeanInfo()
        {
            super( BusWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            addReadOnly( "name" );
            add( "color" );
        }
    }

    public static class UnitWrapper implements DataElement
    {
        Unit unit;

        public UnitWrapper()
        {
            this.unit = new Unit();
        }

        public UnitWrapper(Unit unit)
        {
            this.unit = unit;
        }

        public String getName()
        {
            return unit.getName();
        }

        public void setName(String name)
        {
            unit.setName( name );
        }

        @PropertyName ( "Title" )
        @PropertyDescription ( "The object title (generally it is object brief name)." )
        public String getTitle()
        {
            return unit.getTitle();
        }
        public void setTitle(String title)
        {
            unit.setTitle( title );
        }

        @PropertyName ( "Comment" )
        public String getComment()
        {
            return unit.getComment();
        }
        public void setComment(String comment)
        {
            unit.setComment( comment );
        }

        @PropertyName ( "Formula" )
        public String getFormula()
        {
            return unit.getFormula();
        }

        public void setFormula(String formula)
        {
            //do nothing
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public static class UnitWrapperBeanInfo extends BeanInfoEx2<UnitWrapper>
    {
        public UnitWrapperBeanInfo()
        {
            super( UnitWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "name" ).htmlDisplayName( "ID" ).add();
            property( "title" ).htmlDisplayName( "TI" ).add();
            property( "comment" ).htmlDisplayName( "CC" ).add();
            addReadOnly( "formula" );
        }
    }

    public static class EquationWrapper implements DataElement
    {
        Equation equation;


        public EquationWrapper()
        {
            this.equation = new Equation( null, "x", "0" );
        }

        public EquationWrapper(Equation equation)
        {
            this.equation = equation;
        }

        @PropertyName ( "Equation" )
        public String getFormula()
        {
            return equation.getFormula();
        }
        public void setFormula(String formula)
        {
            equation.setFormula( formula );
        }

        @Override
        public String getName()
        {
            return equation.getParent().getName();
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @PropertyName ( "Variable" )
        public String getVariable()
        {
            return equation.getVariable();
        }
        public void setVariable(String variable)
        {
            equation.setVariable( variable );
        }

        public boolean isAlgebraic()
        {
            return equation.isAlgebraic();
        }

        @PropertyName ( "Type" )
        public String getType()
        {
            return equation.getType();
        }

        public void setType(String type)
        {
            equation.setType( type );
        }

        @PropertyName ( "Comment" )
        public String getComment()
        {
            return equation.getComment();
        }
        public void setComment(String comment)
        {
            equation.setComment( comment );
        }

    }

    public static class EquationWrapperBeanInfo extends BeanInfoEx2<EquationWrapper>
    {
        public EquationWrapperBeanInfo()
        {
            super( EquationWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            addHidden( "variable", "isAlgebraic" );
            add( "formula", FormulaEditor.class );
            add( "type", EquationTypeEditor.class );
            add( "comment" );
        }
    }


    public static class FunctionWrapper implements DataElement
    {
        Function function;

        public FunctionWrapper()
        {
            this.function = new Function( null, "function f() = 0" );
        }
        public FunctionWrapper(Function equation)
        {
            this.function = equation;
        }

        @PropertyName ( "Formula" )
        public String getFormula()
        {
            return function.getFormula();
        }
        public void setFormula(String formula)
        {
            function.setFormula( formula );
        }

        @PropertyName ( "Name" )
        public String getName()
        {
            return function.getName();
        }

        public void setName(String name)
        {
            function.setName( name );
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @PropertyName ( "Right Hand Side" )
        public String getRightHandSide()
        {
            return function.getRightHandSide();
        }
        public void setRightHandSide(String rhs)
        {
            function.setRightHandSide( rhs );
        }


        @PropertyName ( "Comment" )
        public String getComment()
        {
            return function.getComment();
        }
        public void setComment(String comment)
        {
            function.setComment( comment );
        }

    }

    public static class FunctionWrapperBeanInfo extends BeanInfoEx2<FunctionWrapper>
    {
        public FunctionWrapperBeanInfo()
        {
            super( FunctionWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "name" );
            add( "rightHandSide", FormulaEditor.class );
            addReadOnly( "formula" );
            add( "comment" );
        }
    }


    public static class EventWrapper implements DataElement
    {
        Event event;

        public EventWrapper()
        {
            this.event = new Event( null );
        }

        public EventWrapper(Event equation)
        {
            this.event = equation;
        }

        @PropertyName ( "Name" )
        public String getName()
        {
            return event.getParent().getName();
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @PropertyName ( "Trigger" )
        public String getTrigger()
        {
            return event.getTrigger();
        }
        public void setTrigger(String trigger)
        {
            event.setTrigger( trigger );
        }

        @PropertyName ( "Trigger message" )
        public String getTriggerMessage()
        {
            return event.getTriggerMessage();
        }
        public void setTriggerMessage(String message)
        {
            event.setTriggerMessage( message );
        }

        @PropertyName ( "Delay" )
        public String getDelay()
        {
            return event.getDelay();
        }
        public void setDelay(String delay)
        {
            event.setDelay( delay );
        }

        @PropertyName ( "Delay time units" )
        public String getTimeUnits()
        {
            return event.getTimeUnits();
        }
        public void setTimeUnits(String units)
        {
            event.setTimeUnits( units );
        }

        @PropertyName ( "Use trigger time values" )
        public boolean isUseValuesFromTriggerTime()
        {
            return event.isUseValuesFromTriggerTime();
        }
        public void setUseValuesFromTriggerTime(boolean useValuesFromTriggerTime)
        {
            event.setUseValuesFromTriggerTime( useValuesFromTriggerTime );
        }

        @PropertyName ( "Priority" )
        public String getPriority()
        {
            return event.getPriority();
        }
        public void setPriority(String priority)
        {
            event.setPriority( priority );
        }

        @PropertyName ( "Comment" )
        public String getComment()
        {
            return event.getComment();
        }
        public void setComment(String comment)
        {
            event.setComment( comment );
        }

        @PropertyName ( "Trigger initial value" )
        public boolean isTriggerInitialValue()
        {
            return event.isTriggerInitialValue();
        }
        public void setTriggerInitialValue(boolean triggerInitialValue)
        {
            event.setTriggerInitialValue( triggerInitialValue );
        }

        @PropertyName ( "Persistent trigger" )
        public boolean isTriggerPersistent()
        {
            return event.isTriggerPersistent();
        }
        public void setTriggerPersistent(boolean triggerPersistent)
        {
            event.setTriggerPersistent( triggerPersistent );
        }

        @PropertyName ( "Assignments" )
        public Assignment[] getEventAssignment()
        {
            return event.getEventAssignment();
        }
        public void setEventAssignment(Assignment[] eventAssignment)
        {
            event.setEventAssignment( eventAssignment );
        }

    }

    public static class EventWrapperBeanInfo extends BeanInfoEx2<EventWrapper>
    {
        public EventWrapperBeanInfo()
        {
            super( EventWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "trigger", FormulaEditor.class );
            add( "priority", FormulaEditor.class );
            add( "useValuesFromTriggerTime" );
            add( "triggerPersistent" );
            add( "triggerInitialValue" );
            add( "delay" );
            add( "timeUnits" );
            add( "triggerMessage" );
            add( "eventAssignment" );
            add( "comment" );
        }
    }


    public static class ConstraintWrapper implements DataElement
    {
        private Constraint constraint;

        public ConstraintWrapper()
        {
            this.constraint = new Constraint( null );
        }

        public ConstraintWrapper(Constraint constraint)
        {
            this.constraint = constraint;
        }

        @PropertyName ( "Name" )
        public String getName()
        {
            return constraint.getParent().getName();
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @PropertyName ( "Formula" )
        public String getFormula()
        {
            return constraint.getFormula();
        }
        public void setFormula(String formula)
        {
            constraint.setFormula( formula );
        }

        @PropertyName ( "Message" )
        public String getMessage()
        {
            return constraint.getMessage();
        }

        public void setMessage(String message)
        {
            constraint.setMessage( message );
        }

        @PropertyName ( "Comment" )
        public String getComment()
        {
            return constraint.getComment();
        }
        public void setComment(String comment)
        {
            constraint.setComment( comment );
        }
    }

    public static class ConstraintWrapperBeanInfo extends BeanInfoEx2<ConstraintWrapper>
    {
        public ConstraintWrapperBeanInfo()
        {
            super( ConstraintWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "formula", FormulaEditor.class );
            add( "message" );
            add( "comment" );
        }
    }
}