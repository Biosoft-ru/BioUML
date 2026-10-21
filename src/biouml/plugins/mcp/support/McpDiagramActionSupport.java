package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.mcp.McpConstants;
import biouml.workbench.diagram.DiagramDynamicActionProperties;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.perspective.Perspective;
import biouml.workbench.perspective.PerspectiveUI;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.access.subaction.DynamicAction;
import ru.biosoft.access.subaction.DynamicActionFactory;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.TextUtil2;

/**
 * The diagram's dynamic actions ({@code ru.biosoft.access.dynamicAction}) — the buttons the web UI shows in the
 * toolbar of an open diagram, e.g. the Genome Enhancer annotation diagram's "Experiment control template" or
 * "One category". Plugins add them for their own diagram types, so these tools give an agent every such
 * operation without a tool of its own: {@link #list} what applies to a diagram (in the current perspective)
 * with the properties each one asks for, {@link #run} one headlessly with the selection and the property
 * values a user would give in its dialog. The diagram is saved after a run, like after the other diagram edits.
 */
public final class McpDiagramActionSupport
{
	private McpDiagramActionSupport()
	{
	}

	/**
	 * The dynamic actions applicable to the diagram that the current perspective allows.
	 * @return {@code {diagram, actions:[{name, description, selection, properties:[{name, displayName, type, value, description}]}]}}
	 */
	public static McpEnvelope list( String diagramPath )
	{
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		List<Map<String, Object>> actions = new ArrayList<Map<String, Object>>();
		Perspective perspective = currentPerspective();
		for ( DynamicAction action : DynamicActionFactory.dynamicActions() )
		{
			if ( !allowed( perspective, action ) || !applicable( action, diagram ) )
				continue;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "name", action.getTitle() );
			Object description = action.getValue( javax.swing.Action.SHORT_DESCRIPTION );
			if ( description != null )
				m.put( "description", String.valueOf( description ) );
			m.put( "selection", selection( action.getNumSelected() ) );
			List<Map<String, Object>> properties = properties( action, diagram );
			if ( !properties.isEmpty() )
				m.put( "properties", properties );
			actions.add( m );
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "diagram", diagramPath );
		result.put( "actions", actions );
		return McpEnvelope.ok( result );
	}

	/**
	 * Run a dynamic action on the diagram and save it.
	 * @param name the action's name from {@link #list} (case is ignored)
	 * @param elements the selected elements: names or paths in the diagram (may be empty)
	 * @param properties the values of the action's properties, by property name; the others keep their defaults
	 * @return {@code {action, added[...], removed[...], saved}}
	 */
	public static McpEnvelope run( String diagramPath, String name, String[] elements, Map<String, String> properties )
	{
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "action must be a non-empty string (a name from op list)" );
		McpEnvelope d = diagram( diagramPath );
		if ( !d.isOk() )
			return d;
		Diagram diagram = (Diagram) d.getData();
		DynamicAction action = find( name );
		if ( action == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no diagram action named '" + name + "'; op list names the actions of the diagram" );
		if ( !allowed( currentPerspective(), action ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "the current perspective does not allow the action '" + action.getTitle() + "'" );
		if ( !applicable( action, diagram ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "the action '" + action.getTitle() + "' does not apply to this diagram: " + diagramPath );
		List<DataElement> selected = new ArrayList<DataElement>();
		for ( String id : elements == null ? new String[ 0 ] : elements )
		{
			DiagramElement de = diagram.findDiagramElement( id.replace( '/', '.' ) );
			if ( de == null )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element '" + id + "' in the diagram" );
			selected.add( de );
		}
		McpDiagramSupport.ensureView( diagram );
		Object bean;
		try
		{
			action.validateParameters( diagram, selected );
			bean = action.getProperties( diagram, selected );
			String wrong = setProperties( bean, properties );
			if ( wrong != null )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, wrong );
			if ( bean instanceof DiagramDynamicActionProperties )
				( (DiagramDynamicActionProperties) bean ).setHelper( new DiagramEditorHelper( diagram ) );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "the action '" + action.getTitle() + "' can not run: "
					+ McpDiagramSupport.describeError( e ) );
		}
		Set<DiagramElement> before = elements( diagram );
		String what = "action '" + action.getTitle() + "'";
		if ( action instanceof BackgroundDynamicAction )
		{
			JobControl jc;
			try
			{
				jc = ( (BackgroundDynamicAction) action ).getJobControl( diagram, selected, bean );
			}
			catch ( Exception e )
			{
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, what + " failed: " + McpDiagramSupport.describeError( e ) );
			}
			McpEnvelope failed = McpDiagramEditSupport.runEdit( diagramPath, diagram, jc, true, what );
			if ( failed != null )
				return failed;
		}
		else
		{
			try
			{
				action.performAction( diagram, selected, bean );
				ru.biosoft.access.CollectionFactoryUtils.save( diagram );
			}
			catch ( Exception e )
			{
				McpDiagramEditSupport.discard( diagramPath, diagram );
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, what + " failed, the diagram is re-read from its last saved version: "
						+ McpDiagramSupport.describeError( e ) );
			}
		}
		Set<DiagramElement> after = elements( diagram );
		List<Map<String, Object>> added = new ArrayList<Map<String, Object>>();
		for ( DiagramElement de : after )
			if ( !before.contains( de ) )
				added.add( McpDiagramSupport.summary( de ) );
		List<String> removed = new ArrayList<String>();
		for ( DiagramElement de : before )
			if ( !after.contains( de ) )
				removed.add( de.getName() );
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "action", action.getTitle() );
		result.put( "added", added );
		result.put( "removed", removed );
		result.put( "saved", Boolean.TRUE );
		return McpEnvelope.ok( result );
	}

	private static McpEnvelope diagram( String path )
	{
		if ( path == null || path.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "diagramPath must be a non-empty string" );
		DataElement de = McpProviderSupport.resolveElement( path );
		if ( !( de instanceof Diagram ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no diagram at path: " + path + McpProjectSupport.staleHint( path ) );
		return McpEnvelope.ok( de );
	}

	private static DynamicAction find( String name )
	{
		DynamicAction action = DynamicActionFactory.getDynamicAction( name );
		if ( action != null )
			return action;
		return DynamicActionFactory.dynamicActions().findFirst( a -> name.equalsIgnoreCase( a.getTitle() ) ).orElse( null );
	}

	/** The perspective of the session, or null where there is none (e.g. no preferences are loaded). */
	private static Perspective currentPerspective()
	{
		try
		{
			return PerspectiveUI.getCurrentPerspective();
		}
		catch ( RuntimeException e )
		{
			return null;
		}
	}

	private static boolean allowed( Perspective perspective, DynamicAction action )
	{
		return perspective == null || perspective.isActionAvailable( action.getTitle() );
	}

	private static boolean applicable( DynamicAction action, Diagram diagram )
	{
		try
		{
			return action.isApplicable( diagram );
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	/** What the action needs selected, as op run's elements. */
	private static String selection( Integer numSelected )
	{
		switch ( numSelected == null ? DynamicAction.SELECTED_UNDEFINED : numSelected.intValue() )
		{
			case DynamicAction.SELECTED_ZERO:
				return "none";
			case DynamicAction.SELECTED_ONE:
				return "one element";
			case DynamicAction.SELECTED_ANY:
				return "one or more elements";
			default:
				return "any";
		}
	}

	/** The properties the action asks for without a selection, as its dialog shows them; empty if it asks for none. */
	private static List<Map<String, Object>> properties( DynamicAction action, Diagram diagram )
	{
		List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		Object bean;
		try
		{
			bean = action.getProperties( diagram, Collections.<DataElement> emptyList() );
		}
		catch ( Exception e )
		{
			return out; // e.g. it needs a selection to build its properties
		}
		if ( bean == null || bean instanceof DiagramDynamicActionProperties )
			return out;
		ComponentModel model = ComponentFactory.getModel( bean, ComponentFactory.Policy.DEFAULT, true );
		for ( int i = 0; i < model.getPropertyCount(); i++ )
		{
			Property p = model.getPropertyAt( i );
			if ( p.getValueClass() == Class.class || !p.isVisible( Property.SHOW_USUAL ) )
				continue;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "name", p.getName() );
			m.put( "displayName", p.getDisplayName() );
			m.put( "type", p.getValueClass() == null ? "unknown" : p.getValueClass().getSimpleName() );
			Object value = p.getValue();
			if ( value != null )
				m.put( "value", value instanceof Number || value instanceof Boolean ? value : String.valueOf( value ) );
			String description = p.getShortDescription();
			if ( description != null && !description.isEmpty() && !description.equals( p.getDisplayName() ) )
				m.put( "description", description );
			out.add( m );
		}
		return out;
	}

	/**
	 * Set the given properties of the action's bean.
	 * @return null when all were set, else what is wrong
	 */
	private static String setProperties( Object bean, Map<String, String> values ) throws Exception
	{
		if ( values == null || values.isEmpty() )
			return null;
		if ( bean == null )
			return "the action takes no properties, got " + values.keySet();
		ComponentModel model = ComponentFactory.getModel( bean, ComponentFactory.Policy.DEFAULT, true );
		for ( Map.Entry<String, String> e : values.entrySet() )
		{
			Property p = model.findProperty( e.getKey() );
			if ( p == null )
			{
				for ( int i = 0; i < model.getPropertyCount() && p == null; i++ )
					if ( e.getKey().equalsIgnoreCase( model.getPropertyAt( i ).getDisplayName() ) )
						p = model.getPropertyAt( i );
			}
			if ( p == null )
				return "the action has no property '" + e.getKey() + "'; op list names its properties";
			Class<?> type = p.getValueClass();
			Object value;
			if ( type == null || type == String.class || e.getValue() == null )
				value = e.getValue();
			else if ( DataElementPath.class.isAssignableFrom( type ) )
				value = DataElementPath.create( e.getValue() );
			else
			{
				try
				{
					value = TextUtil2.fromString( type, e.getValue(), true );
				}
				catch ( Exception ex )
				{
					return "property '" + p.getName() + "' expects a " + type.getSimpleName().toLowerCase( Locale.ENGLISH ) + ", got: " + e.getValue();
				}
			}
			p.setValue( value );
		}
		return null;
	}

	private static Set<DiagramElement> elements( Diagram diagram )
	{
		Set<DiagramElement> set = Collections.newSetFromMap( new IdentityHashMap<DiagramElement, Boolean>() );
		diagram.recursiveStream().skip( 1 ).forEach( set::add );
		return set;
	}
}
