package biouml.model;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.undo.UndoManager;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.security.NetworkDataCollection;
import ru.biosoft.graph.Layouter;
import ru.biosoft.plugins.graph.GraphPlugin;
import ru.biosoft.plugins.graph.LayouterDescriptor;
import ru.biosoft.util.TextUtil;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.EModel;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.state.State;
import biouml.standard.state.StateChangeListener;
import biouml.standard.state.StateDiagramViewBuilder;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Referrer;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.undo.Transactable;
import com.developmentontheedge.beans.util.Beans;

@ClassIcon ( "resources/diagram.gif" )
@PropertyName ( "Diagram" )
public class Diagram extends Compartment
{
    protected static final Logger log = Logger.getLogger( Diagram.class.getName() );

    public static final String DIAGRAM_TYPE_PROPERTY = "diagramType";
    public static final String DIAGRAM_ROLE_PROPERTY = "diagramRole";
    public static final String COMPOSITE_DIAGRAM_PROPERTY = "compositeDiagram";

    /**Class name of default layouter property name. */
    public static final String DEFAULT_LAYOUTER = "defaultLayouter";
    public static final String NON_STATE = "non";

    private boolean hasSubDiagrams = false;
    private boolean hideInvisibleElements = true;
    protected Layouter pathLayouter = null;
    protected Layouter labelLayouter = null;
    protected DiagramType type;
    protected DiagramViewOptions viewOptions;
    protected String currentStateName = NON_STATE;
    protected State currentState = null;

    private Set<String> names = new HashSet<>();

    /**
     * Creates the diagram of the specified type.
     * @param type the diagram type.
     */
    public Diagram(DataCollection<?> origin, Base kernel, DiagramType type)
    {
        super( origin, kernel );
        setType( type );
        getInfo().getProperties().setProperty( NetworkDataCollection.CLONE_FOR_EDIT_PROPERTY, "true" );
    }

    /**
     * This constructor is required for DefaultSemanticController.checkType method
     */
    public Diagram(DataCollection<?> origin, String id) throws Exception
    {
        super( origin, id, null );
        getInfo().getProperties().setProperty( NetworkDataCollection.CLONE_FOR_EDIT_PROPERTY, "true" );
    }

/*
    public Diagram( DataCollection<?> origin, Properties properties ) throws Exception
    {
        super( origin, properties.getProperty( "name" ), null );
        Iterator<Object> iter = properties.keySet().iterator();
        while( iter.hasNext() )
        {
            String key = iter.next().toString();
            getInfo().getProperties().put( key, properties.getProperty( key ) );
        }
    } 
*/


    @Override
    public void setTitle(String title)
    {
        super.setTitle( title );
//        String realTitle = title != null ? title : name;
//        getInfo().setDisplayName( realTitle );
    }

    public static @Nonnull Diagram getDiagram(DiagramElement de)
    {
        Diagram d = optDiagram( de );
        if( d == null )
        {
            throw new InternalException( "No parent diagram found for " + de.getCompletePath() );
        }
        return d;
    }

    /**
     * Returns first diagram (not subdiagram!) in parent hierarchy
     * @param de
     * @return
     */
    public static @CheckForNull Diagram optDiagram(DiagramElement de)
    {
        if(de instanceof Diagram)
            return (Diagram)de;
        return de.parents().filter( Diagram.class::isInstance ).map( d -> (Diagram)d ).findFirst().orElse( null );
    }

    @Override
    public void setRole(Role role)
    {
        Role oldValue = this.role;
        if( oldValue != null )
        {
            if( oldValue instanceof PropertyChangeListener )
                removePropertyChangeListener( (PropertyChangeListener)oldValue );

            if( oldValue instanceof DataCollectionListener )
                removeDataCollectionListener( (DataCollectionListener)oldValue );
        }
        this.role = role;
        if( role == null )
            getInfo().getProperties().remove( DIAGRAM_ROLE_PROPERTY );
        else
            getInfo().getProperties().setProperty( DIAGRAM_ROLE_PROPERTY, role.getClass().getName() );
        this.firePropertyChange( "role", oldValue, role );
    }

    //////////////////////////////////////////////////////////////////
    // Properties
    //

    public boolean needsRelayout()
    {
        return stream( Node.class ).map( Node::getLocation ).allMatch( p -> p.x == 0 && p.y == 0 );
    }

    @PropertyName("Layouter")
    public String getLayouterName()
    {
        if( pathLayouter == null )
            return "Default layouter";

        List<LayouterDescriptor> layouters = GraphPlugin.loadLayouters();
        for( LayouterDescriptor layouterDescriptor : layouters )
            if( layouterDescriptor.getType() == pathLayouter.getClass() )
                return layouterDescriptor.getDescription();
        return "Default layouter";
    }

    public Layouter getPathLayouter()
    {
        return pathLayouter;
    }
    public void setPathLayouter(Layouter pathLayouter)
    {
        Layouter oldValue = this.pathLayouter;
        this.pathLayouter = pathLayouter;
        firePropertyChange( "layouter", oldValue, pathLayouter );
    }

    public Layouter getLabelLayouter()
    {
        return labelLayouter;
    }

    public void setLabelLayouter(Layouter labelLayouter)
    {
        this.labelLayouter = labelLayouter;
    }

    public DiagramType getType()
    {
        return type;
    }
    public void setType(DiagramType type)
    {
        DiagramType oldValue = this.type;
        this.type = type;
        if( type == null )
        {
            getInfo().getProperties().remove( DIAGRAM_TYPE_PROPERTY );
            getInfo().getProperties().remove( COMPOSITE_DIAGRAM_PROPERTY );
        }
        else
        {
            getInfo().getProperties().setProperty( DIAGRAM_TYPE_PROPERTY, type.getClass().getName() );
            getInfo().getProperties().setProperty( COMPOSITE_DIAGRAM_PROPERTY, String.valueOf( DiagramUtility.isComposite( this ) ) );
        }
        setNodeViewBuilders();
        firePropertyChange( "type", oldValue, type );
    }

    public void setNodeViewBuilders()
    {
        List<NodeViewBuilder> builders = NodeViewBuilderRegistry.getBuilders();

        if( builders == null )
            return;

        for( NodeViewBuilder builder : builders )
        {
            if( builder.isApplicable( this ) )
            {
                //don't duplicate listeners
                removeDataCollectionListener( builder );
                removePropertyChangeListener( builder );

                addDataCollectionListener( builder );
                addPropertyChangeListener( builder );

                builder.applyNodeViewBuilder( this );
            }
        }
    }

    @PropertyName("Diagram type")
    @PropertyDescription("Diagram type.")
    public String getDiagramType()
    {
        try
        {
            if( type != null )
            {
                BeanInfo info = Introspector.getBeanInfo( type.getClass() );
                return info.getBeanDescriptor().getDisplayName();
            }
        }
        catch( IntrospectionException e )
        {
            log.log(Level.SEVERE,  "Invalid type BeanInfo", e );
        }
        return "Undefined diagram type";
    }

    public DiagramViewOptions getViewOptions()
    {
        if( viewOptions == null && type != null && type.getDiagramViewBuilder() != null )
        {
            viewOptions = type.getDiagramViewBuilder().createDefaultDiagramViewOptions();
        }
        return viewOptions;
    }

    public void setViewOptions(DiagramViewOptions options)
    {
        if( viewOptions != null )
            viewOptions.setParent( null );

        viewOptions = options;
        viewOptions.setParent( this );
    }

    protected DiagramFilter filter;
    public DiagramFilter getFilter()
    {
        if( filter == null )
            setDiagramFilter( type.getDiagramFilter( this ) );

        return filter;
    }

    public void setDiagramFilter(DiagramFilter filter)
    {
        if( this.filter != null && this.filter instanceof Option )
            ( (Option)this.filter ).setParent( null );

        this.filter = filter;
        if( filter instanceof Option )
            ( (Option)filter ).setParent( this );
    }

    protected DiagramFilter[] filterList;
    public DiagramFilter[] getFilterList()
    {
        if( filterList == null )
        {
            filterList = new DiagramFilter[0];
        }
        return filterList;
    }
    public void setFilterList(DiagramFilter[] filters)
    {
        this.filterList = filters;
    }

    @Override
    public void setKernel(Base newKernel)
    {
        Base oldValue = this.kernel;
        this.kernel = newKernel;
        this.firePropertyChange( "kernel", oldValue, newKernel );
    }

    /**
     * Enable/disable events notification of two types:
     * 1) PropertyChangeEvent (inherited from Option)
     * 2) DataCollectionEvent (encapsulated ru.biosoft.access.core.DataCollection)
     */
    @Override
    public void setNotificationEnabled(boolean notificationEnabled)
    {
        super.setNotificationEnabled( notificationEnabled );
        if( hasSubDiagrams )
        {
            stream( Diagram.class ).forEach( d -> d.setNotificationEnabled( notificationEnabled ) );
        }
    }

    protected HashMap<String, State> states;

    public boolean containState(State state)
    {
        return states != null && states.containsKey( state.getName() ) && states.get( state.getName() ).equals( state );
    }

    public void addState(State state)
    {
        Object oldValue = states;
        if( states == null )
            states = new HashMap<>();
        states.put( state.getName(), state );
        firePropertyChange( "states", oldValue, states );
    }

    public void removeAllStates()
    {
        if( states == null )
            return;
        Object oldValue = states;
        if( currentState != null  )
            restore();

        states = null;
        firePropertyChange( "states", oldValue, states );
    }

    public void removeState(State state)
    {
        Object oldValue = states;
        if( states == null )
            return;

        if( currentState != null && currentState.equals( state ) )
            restore();

        State stateWithSameName = states.get( state.getName() );
        if( stateWithSameName.equals( state ) )
        {
            states.remove( state.getName() );
        }
        firePropertyChange( "states", oldValue, states );
    }

    /**
     * @return StreamEx of all Diagram states
     */
    public @Nonnull StreamEx<State> states()
    {
        return states == null ? StreamEx.empty() : StreamEx.ofValues( states );
    }

    public State getCurrentState()
    {
        return currentState;
    }

    protected StateChangeListener undoRedoListener = null;
    protected Transactable transactable;
    public void setStateEditingMode(State state)
    {
        setStateEditingMode( state, null );
    }

    public void setStateEditingMode(State state, Transactable transactable)
    {
        restore();
        this.transactable = transactable;
        currentState = state;
        String oldValue = currentStateName;
        currentStateName = currentState.getName();

        getViewOptions();
        DiagramViewBuilder viewBuilder = type.getDiagramViewBuilder();
        type.setDiagramViewBuilder( new StateDiagramViewBuilder( viewBuilder, state, this ) );

        UndoManager undoManager = state.getStateUndoManager();
        while( undoManager.canRedo() )
        {
            undoManager.redo();
        }
        firePropertyChange( "currentStateName", oldValue, currentStateName );

        undoRedoListener = new StateChangeListener( state );
        addDataCollectionListener( undoRedoListener );
        addPropertyChangeListener( undoRedoListener );
        if( this.transactable != null )
            this.transactable.addTransactionListener( undoRedoListener );
    }

    /**
     * Sets default state
     */
    public void restore()
    {
        DiagramViewBuilder viewBuilder = getType().getDiagramViewBuilder();
        if( viewBuilder instanceof StateDiagramViewBuilder )
        {
            getType().setDiagramViewBuilder( ( (StateDiagramViewBuilder)viewBuilder ).getBaseViewBuilder() );
        }
        if( currentState != null )
        {
            if( undoRedoListener != null )
            {
                removeDataCollectionListener( undoRedoListener );
                removePropertyChangeListener( undoRedoListener );
                undoRedoListener = null;
            }
            UndoManager undoManager = currentState.getStateUndoManager();
            //boolean notificationEnabled = isNotificationEnabled();
            //setNotificationEnabled( false );
            while( undoManager.canUndo() )
            {
                undoManager.undo();
            }
            //setNotificationEnabled( notificationEnabled );
            if( currentState != null )
            {
                currentState = null;
                String oldValue = currentStateName;
                currentStateName = NON_STATE;
                firePropertyChange( "currentStateName", oldValue, currentStateName );
            }
            if( this.transactable != null )
            {
                this.transactable.removeTransactionListener( undoRedoListener );
                this.transactable = null;
            }
        }
    }

    @PropertyName("State")
    @PropertyDescription("Current state.")
    public String getCurrentStateName()
    {
        return currentStateName;
    }
    public void setCurrentStateName(String currentStateName)
    {
        //        if( !this.currentStateName.equals( NON_STATE ) )
        //            restore();

        boolean restored = false;
        if( states != null )
        {
            State curState = states.get( currentStateName );
            if( curState != null )
            {
                setStateEditingMode( curState );
                restored = true;
            }
            //            StreamEx.ofValues( states ).findFirst( state -> state.getName().equals( currentStateName ) )
            //                    .ifPresent( this::setStateEditingMode );
        }
        if( !restored && !this.currentStateName.equals( NON_STATE ) )
            restore();
    }

    public List<String> getStateNames()
    {
        List<String> names = new ArrayList<>();
        names.add( NON_STATE );
        if( states != null )
            names.addAll( states.keySet() );
        return names;
    }

    public State getState(String name)
    {
        if( states == null )
            return null;

        return states.get( name );
    }

    //////////////////////////////////////////////////////////////////
    // clone issues
    //

    public @Nonnull Diagram clone(DataCollection<?> origin, String newName) throws Exception
    {
        boolean notif = this.isNotificationEnabled();
        this.setNotificationEnabled( false );
        Diagram diagram = getType().clone().createDiagram( origin, newName, null );
        doClone( diagram );
        diagram.getType().postProcessClone( this, diagram );
        this.setNotificationEnabled( notif );
        return diagram;
    }

    /**
     * Reimplement {@link Compartment} clone method
     */
    @Override
    public @Nonnull Diagram clone(Compartment newParent, String newName)
    {
        try
        {
            Diagram diagram = getType().clone().createDiagram( newParent, newName, null );
            doClone( diagram );
            return diagram;
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    /**
     * @pending implement
     */
    protected void doClone(Diagram diagram) throws Exception
    {
        diagram.restore();
        boolean notificationEnabled = false;
        if( diagram.isNotificationEnabled() )
        {
            notificationEnabled = true;
            diagram.setNotificationEnabled( false );
        }

        if (diagram.getRole() instanceof EModel)
            diagram.getRole(EModel.class).setAutodetectTypes(false);
        
        Beans.copyBean( (DiagramViewOptions)getViewOptions(), diagram.getViewOptions() );

        super.doClone( diagram );

        if( getKernel() instanceof DiagramInfo )
        {
            DiagramInfo newInfo = ( (DiagramInfo)kernel ).clone( diagram.getName() );
            newInfo.setParent( diagram );
            diagram.kernel = newInfo;
        }

        if( filterList != null )
        {
            List<DiagramFilter> newFilters = new ArrayList<>();
            for( DiagramFilter element : filterList )
            {
                if( element == null )
                    continue;
                DiagramFilter newFilter = element.clone();
                if( filter == element )
                    diagram.setDiagramFilter( newFilter );
                newFilters.add( newFilter );
            }
            diagram.setFilterList( newFilters.toArray( new DiagramFilter[newFilters.size()] ) );
        }

        if( states != null )
        {
            for( State state : states.values() )
            {
                state.clone( null, diagram, state.getName() );
            }
        }
        diagram.setCurrentStateName( getCurrentStateName() );

        if( pathLayouter != null )
        {
            diagram.setPathLayouter( pathLayouter );
        }
        if( labelLayouter != null )
        {
            diagram.setLabelLayouter( labelLayouter );
        }

        Properties newProperties = diagram.getInfo().getProperties();
        Properties oldProperties = getInfo().getProperties();
        Enumeration<?> oldPropertyNames = oldProperties.propertyNames();
        while( oldPropertyNames.hasMoreElements() )
        {
            String name = oldPropertyNames.nextElement().toString();
            try
            {
                newProperties.setProperty( name, oldProperties.getProperty( name ) );
            }
            catch( Exception e )
            {
            }
        }

        if( notificationEnabled )
            diagram.setNotificationEnabled( true );
        
        if (diagram.getRole() instanceof EModel)
            diagram.getRole(EModel.class).setAutodetectTypes(true);
    }

    public String getDescription()
    {
        return ( (Referrer)getKernel() ).getDescription();
    }

    public void setDescription(String description)
    {
        ( (Referrer)getKernel() ).setDescription( TextUtil.nullToEmpty( description ) );
        try
        {
            getCompletePath().save( this );
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
    }

    public boolean containsRecursively(String name)
    {
        return names.contains( name );
    }

    @Override
    public DiagramElement put(DiagramElement obj)
    {
        DiagramElement oldElement = super.put( obj );
        if( obj instanceof SubDiagram )
            hasSubDiagrams = true;
        return oldElement;
    }

    protected void deregister(String name)
    {
        this.names.remove( name );
    }

    protected void deregister(Collection<String> names)
    {
        this.names.removeAll( names );
    }

    protected void register(String name)
    {
        this.names.add( name );
    }

    protected void register(Collection<String> names)
    {
        this.names.addAll( names );
    }

    public Set<String> getNames()
    {
        return names;
    }

    @PropertyName("Hide invisible elements")
    @PropertyDescription("Hide invisible elements.")
    public boolean isHideInvisibleElements()
    {
        return hideInvisibleElements;
    }
    public void setHideInvisibleElements(boolean hideInvisibleElements)
    {
        this.hideInvisibleElements = hideInvisibleElements;
    }

    public void removeStates()
    {
        if( !this.currentStateName.equals( NON_STATE ) )
            restore();
        states = null;
    }

    @Override
    public String getCompleteNameInDiagram()
    {
        return "";
    }
}
