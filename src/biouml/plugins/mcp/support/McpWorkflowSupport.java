package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.plugins.mcp.McpConstants;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.WorkflowItem;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.standard.type.Species;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.ComponentFactory;

import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.log.JULLoggerAdapter;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.TextUtil2;

/**
 * Workflows — ready-made pipelines of analyses, the diagrams below {@value #ROOT} — for the analysis MCP tools:
 * they are listed beside the analyses, described by their parameters and steps, and run as background tasks,
 * as the web UI's workflow panel runs them.
 */
public final class McpWorkflowSupport
{
	private static final Logger log = Logger.getLogger( McpWorkflowSupport.class.getName() );

	/** The repository folder of the workflows the platform offers. */
	public static final String ROOT = "analyses/Workflows";

	private static final int MAX_DEPTH = 4;
	private static final int MAX_WORKFLOWS = 2000;

	private McpWorkflowSupport()
	{
	}

	/** A workflow the caller may see: its path and its path below {@link #ROOT} ("folder/name"). */
	public static final class Entry
	{
		public final DataElementPath path;
		public final String relativePath;

		Entry( DataElementPath path, String relativePath )
		{
			this.path = path;
			this.relativePath = relativePath;
		}

		public String folder()
		{
			int slash = relativePath.lastIndexOf( '/' );
			return slash < 0 ? "" : relativePath.substring( 0, slash );
		}
	}

	/** The workflows below {@link #ROOT}, found without loading them; empty when there is no such folder. */
	public static List<Entry> list()
	{
		List<Entry> entries = new ArrayList<Entry>();
		try
		{
			McpProviderSupport.inSession( () -> {
				DataElementPath root = DataElementPath.create( ROOT );
				if ( root.exists() )
					collect( root, "", entries, 0 );
				return null;
			} );
		}
		catch ( Exception e )
		{
			log.log( Level.FINE, "Cannot list workflows", e );
		}
		return entries;
	}

	private static void collect( DataElementPath folder, String prefix, List<Entry> out, int depth )
	{
		List<String> names;
		try
		{
			names = folder.getDataCollection().getNameList();
		}
		catch ( Exception e )
		{
			return; // not readable by this user
		}
		for ( String name : names )
		{
			if ( out.size() >= MAX_WORKFLOWS )
				return;
			DataElementPath child = folder.getChildPath( name );
			try
			{
				DataElementDescriptor descriptor = child.getDescriptor();
				if ( descriptor == null )
					continue;
				Class<?> type = descriptor.getType();
				if ( type != null && Diagram.class.isAssignableFrom( type ) )
					out.add( new Entry( child, prefix + name ) );
				else if ( !descriptor.isLeaf() && depth < MAX_DEPTH )
					collect( child, prefix + name + "/", out, depth + 1 );
			}
			catch ( Exception e )
			{
				// an element that cannot be described is not offered
			}
		}
	}

	/**
	 * The workflow a caller names: by its repository path, by its path below {@link #ROOT}, or by its name when
	 * only one workflow has it.
	 * @return its path, or null when {@code name} is no workflow
	 */
	public static DataElementPath resolve( String name )
	{
		if ( name == null || name.trim().isEmpty() )
			return null;
		try
		{
			return McpProviderSupport.inSession( () -> {
				for ( String candidate : new String[] { name, ROOT + "/" + name } )
				{
					if ( !candidate.contains( "/" ) )
						continue;
					DataElementPath path = DataElementPath.create( candidate );
					if ( isWorkflow( path ) )
						return path;
				}
				DataElementPath found = null;
				for ( Entry entry : list() )
					if ( entry.path.getName().equals( name ) )
					{
						if ( found != null )
							return null; // ambiguous: the caller has to give the path
						found = entry.path;
					}
				return found != null && isWorkflow( found ) ? found : null;
			} );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	private static boolean isWorkflow( DataElementPath path )
	{
		try
		{
			if ( !path.exists() )
				return false;
			DataElementDescriptor descriptor = path.getDescriptor();
			if ( descriptor == null || descriptor.getType() == null || !Diagram.class.isAssignableFrom( descriptor.getType() ) )
				return false;
			return path.getDataElement( Diagram.class ).getType() instanceof WorkflowDiagramType;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	/** The path of a workflow below {@link #ROOT}, or its name for a workflow kept elsewhere (e.g. in a project). */
	public static String relativePath( DataElementPath path )
	{
		String text = path.toString();
		return text.startsWith( ROOT + "/" ) ? text.substring( ROOT.length() + 1 ) : path.getName();
	}

	// ---------------------------------------------------------------- describe

	/**
	 * Describe a workflow as {@link McpAnalysisSupport#describeMethod} describes an analysis: its guide, its
	 * parameters (each with its role: input, output or setting) and the analyses it runs.
	 * @param params parameter values to bind first: the defaults of other parameters (e.g. the result folder)
	 *        are computed from them
	 */
	public static McpEnvelope describe( DataElementPath path, Map<String, Object> params, String filter, List<String> only )
	{
		try
		{
			return McpProviderSupport.inSession( () -> {
				Diagram workflow = copy( path );
				bindProject( params );
				DynamicPropertySet parameters = WorkflowItemFactory.getWorkflowParameters( workflow );
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put( "name", path.getName() );
				m.put( "kind", "workflow" );
				m.put( "path", path.toString() );
				McpGuides.Guide guide = McpGuides.workflow( relativePath( path ) );
				String description = description( workflow );
				if ( description == null && guide != null )
					description = guide.getSummary();
				if ( description != null )
					m.put( "description", description );
				boolean brief = filter != null && !filter.isEmpty() || only != null && !only.isEmpty();
				if ( guide != null && !brief )
					m.put( "guide", guide.toMap() );
				if ( params != null && !params.isEmpty() )
				{
					McpEnvelope bound = bind( workflow, parameters, params );
					if ( !bound.isOk() )
						return bound;
					m.put( "boundParams", new ArrayList<String>( params.keySet() ) );
				}
				Map<String, String> roles = roles( workflow );
				List<Map<String, Object>> props = new ArrayList<Map<String, Object>>();
				String lowerFilter = filter == null || filter.isEmpty() ? null : filter.toLowerCase( java.util.Locale.ENGLISH );
				McpAnalysisSupport.collectProperties( ComponentFactory.getModel( parameters, ComponentFactory.Policy.DEFAULT, true ), "", props,
						lowerFilter, 0 );
				McpGuides.describeParameters( guide, props );
				List<String> inputs = new ArrayList<String>();
				List<String> outputs = new ArrayList<String>();
				for ( Map<String, Object> p : props )
				{
					String role = roles.get( p.get( "name" ) );
					if ( role == null )
						continue;
					p.put( "role", role );
					if ( "input".equals( role ) )
						inputs.add( (String) p.get( "name" ) );
					else if ( "output".equals( role ) )
						outputs.add( (String) p.get( "name" ) );
				}
				if ( only != null && !only.isEmpty() )
				{
					props.removeIf( p -> !only.contains( p.get( "name" ) ) );
					m.put( "only", new ArrayList<String>( only ) );
				}
				else if ( lowerFilter != null )
				{
					props.removeIf( p -> !( p.get( "matchingCount" ) instanceof Integer ) || (Integer) p.get( "matchingCount" ) == 0 );
					m.put( "filter", filter );
				}
				m.put( "properties", props );
				m.put( "inputNames", inputs );
				m.put( "outputNames", outputs );
				if ( !brief )
					m.put( "steps", steps( workflow ) );
				return McpEnvelope.ok( m );
			} );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not describe workflow " + path + ": " + message( e ) );
		}
	}

	/** A private copy: reading the parameters of a workflow resets their values and adds attributes to the diagram. */
	private static Diagram copy( DataElementPath path ) throws Exception
	{
		Diagram diagram = path.getDataElement( Diagram.class );
		return diagram.clone( diagram.getOrigin(), diagram.getName() );
	}

	private static String description( Diagram workflow )
	{
		try
		{
			String html = workflow.getDescription();
			if ( html == null || html.trim().isEmpty() )
				return null;
			String text = McpIntroPages.htmlText( html );
			return text.isEmpty() ? null : text;
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	/** Parameter name to "input", "output" or "setting". */
	private static Map<String, String> roles( Diagram workflow )
	{
		Map<String, String> roles = new LinkedHashMap<String, String>();
		for ( WorkflowParameter parameter : parametersOf( workflow ) )
		{
			String role = parameter.getRole();
			roles.put( parameter.getName(), WorkflowParameter.ROLE_INPUT.equals( role ) ? "input" : WorkflowParameter.ROLE_OUTPUT.equals( role ) ? "output" : "setting" );
		}
		return roles;
	}

	private static List<WorkflowParameter> parametersOf( Diagram workflow )
	{
		List<WorkflowParameter> parameters = new ArrayList<WorkflowParameter>();
		for ( DiagramElement element : workflow )
		{
			if ( !( element instanceof Node ) )
				continue;
			WorkflowItem item = WorkflowItemFactory.getWorkflowItem( (Node) element );
			if ( item instanceof WorkflowParameter )
				parameters.add( (WorkflowParameter) item );
		}
		return parameters;
	}

	/** The analyses a workflow runs, each once with the number of its steps: [{analysis, count}]. */
	private static List<Map<String, Object>> steps( Compartment workflow )
	{
		Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
		countSteps( workflow, counts, 0 );
		List<Map<String, Object>> steps = new ArrayList<Map<String, Object>>();
		for ( Map.Entry<String, Integer> e : counts.entrySet() )
		{
			Map<String, Object> step = new LinkedHashMap<String, Object>();
			step.put( "analysis", e.getKey() );
			if ( e.getValue().intValue() > 1 )
				step.put( "count", e.getValue() );
			steps.add( step );
		}
		return steps;
	}

	private static void countSteps( Compartment compartment, Map<String, Integer> counts, int depth )
	{
		for ( DiagramElement element : compartment )
		{
			if ( !( element instanceof Compartment ) || element.getKernel() == null )
				continue;
			if ( Type.ANALYSIS_METHOD.equals( element.getKernel().getType() ) )
			{
				Object analysis = element.getAttributes().getValue( "analysisName" );
				String name = analysis == null ? element.getTitle() : analysis.toString();
				counts.merge( name, Integer.valueOf( 1 ), Integer::sum );
			}
			else if ( depth < MAX_DEPTH )
				countSteps( (Compartment) element, counts, depth + 1 ); // a cycle holds the steps it repeats
		}
	}

	// ---------------------------------------------------------------- run

	/**
	 * Start a workflow as a background task, as the web UI's workflow panel does.
	 * @return an envelope whose data is {@code {taskId, status, workflow, outputs}}
	 */
	public static McpEnvelope run( DataElementPath path, Map<String, Object> params )
	{
		try
		{
			return McpProviderSupport.inSession( () -> start( path, params == null ? new LinkedHashMap<String, Object>() : params ) );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not start workflow " + path + ": " + message( e ) );
		}
	}

	private static McpEnvelope start( DataElementPath path, Map<String, Object> params ) throws Exception
	{
		final Diagram workflow = copy( path );
		bindProject( params );
		DynamicPropertySet parameters = WorkflowItemFactory.getWorkflowParameters( workflow );
		McpEnvelope bound = bind( workflow, parameters, params );
		if ( !bound.isOk() )
			return bound;
		Map<String, String> roles = roles( workflow );
		List<String> missing = new ArrayList<String>();
		Map<String, Object> outputs = new LinkedHashMap<String, Object>();
		for ( DynamicProperty property : parameters )
		{
			String role = roles.get( property.getName() );
			Object value = property.getValue();
			boolean empty = value == null || value.toString().isEmpty();
			if ( "input".equals( role ) && empty )
				missing.add( property.getName() );
			if ( "output".equals( role ) && !empty )
			{
				outputs.put( property.getName(), value.toString() );
				if ( value instanceof DataElementPath )
					McpProjectContext.useProjectOf( (DataElementPath) value );
			}
		}
		if ( !missing.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "workflow inputs not set: " + missing
					+ " (biouml_analysis_describe lists the parameters of the workflow)" );

		final WorkflowEngine engine = new WorkflowEngine();
		engine.setSkipCompleted( Boolean.TRUE.equals( parameters.getValue( WorkflowEngine.SKIP_COMPLETED_PROPERTY ) ) );
		engine.setIgnoreFail( Boolean.TRUE.equals( parameters.getValue( WorkflowEngine.IGNORE_FAIL_PROPERTY ) ) );
		// a logger of its own, so the task's log holds the lines of this run only
		Logger runLog = Logger.getLogger( McpWorkflowSupport.class.getName() + ".run." + java.util.UUID.randomUUID() );
		runLog.setLevel( Level.ALL );
		final FunctionJobControl jobControl = new FunctionJobControl( null )
		{
			@Override
			public void terminate()
			{
				super.terminate();
				engine.stop();
			}

			@Override
			public void pause()
			{
				super.pause();
				engine.pause();
			}

			@Override
			public void resume()
			{
				super.resume();
				engine.resume();
			}
		};
		TaskInfo task = TaskManager.getInstance().addTask( TaskInfo.WORKFLOW, path, jobControl, new JULLoggerAdapter( runLog ),
				JournalRegistry.getCurrentJournal(), parameters, false, null );
		if ( task == null )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not create a task for workflow " + path );
		engine.setWorkflow( workflow );
		engine.setParameters( parameters );
		engine.setLogger( runLog );
		engine.setJobControl( jobControl );
		jobControl.functionStarted();
		task.setTransient( "parameters", parameters );
		task.setTransient( "diagram", workflow );
		try
		{
			engine.initWorkflow();
		}
		catch ( Exception e )
		{
			jobControl.functionTerminatedByError( e );
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "workflow cannot be started: " + message( e ) );
		}
		new SessionThread( "mcp-workflow-" + task.getName() )
		{
			@Override
			public void doRun()
			{
				try
				{
					engine.start();
				}
				catch ( Throwable t )
				{
					runLog.log( Level.SEVERE, "Workflow failed: " + t, t );
					jobControl.functionTerminatedByError( t );
				}
			}
		}.start();

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "taskId", task.getName() );
		result.put( "status", "running" );
		result.put( "workflow", path.toString() );
		result.put( "outputs", outputs );
		return McpEnvelope.ok( result );
	}

	/** As if the user had opened the project of the inputs: "$project$" in the workflow's defaults is that project. */
	private static void bindProject( Map<String, Object> params )
	{
		if ( params == null )
			return;
		for ( Object value : params.values() )
		{
			Object first = value instanceof List && !( (List<?>) value ).isEmpty() ? ( (List<?>) value ).get( 0 ) : value;
			if ( first instanceof String && ( (String) first ).contains( "/" ) && McpProjectContext.useProjectOf( DataElementPath.create( (String) first ) ) )
				return;
		}
	}

	/** Set workflow parameters from the caller's values, by parameter name. */
	private static McpEnvelope bind( Diagram workflow, DynamicPropertySet parameters, Map<String, Object> params )
	{
		for ( Map.Entry<String, Object> e : params.entrySet() )
		{
			DynamicProperty property = parameters.getProperty( e.getKey() );
			if ( property == null )
			{
				List<String> valid = new ArrayList<String>();
				for ( DynamicProperty p : parameters )
					valid.add( p.getName() );
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "unknown workflow parameter '" + e.getKey() + "'. Valid keys: " + valid );
			}
			try
			{
				property.setValue( convert( property.getType(), e.getValue() ) );
			}
			catch ( Exception ex )
			{
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
						"could not set workflow parameter '" + e.getKey() + "' (" + property.getType().getSimpleName() + "): " + message( ex ) );
			}
		}
		return McpEnvelope.ok( null );
	}

	private static Object convert( Class<?> type, Object value ) throws Exception
	{
		if ( value == null )
			return null;
		if ( value instanceof List )
		{
			List<?> list = (List<?>) value;
			if ( DataElementPathSet.class.isAssignableFrom( type ) )
			{
				DataElementPathSet set = (DataElementPathSet) type.getDeclaredConstructor().newInstance();
				for ( Object item : list )
					set.add( DataElementPath.create( String.valueOf( item ) ) );
				return set;
			}
			if ( type == String[].class )
			{
				String[] strings = new String[list.size()];
				for ( int i = 0; i < strings.length; i++ )
					strings[ i ] = String.valueOf( list.get( i ) );
				return strings;
			}
			if ( list.size() == 1 )
				return convert( type, list.get( 0 ) );
			throw new IllegalArgumentException( "a single value is expected, not a list" );
		}
		String text = String.valueOf( value );
		if ( type == String.class )
			return text;
		if ( type == String[].class && !text.trim().startsWith( "[" ) )
			return new String[] { text };
		if ( DataElementPathSet.class.isAssignableFrom( type ) && !text.trim().startsWith( "[" ) )
		{
			DataElementPathSet set = (DataElementPathSet) type.getDeclaredConstructor().newInstance();
			set.add( DataElementPath.create( text ) );
			return set;
		}
		if ( Species.class.isAssignableFrom( type ) )
		{
			Species species = species( text );
			if ( species == null )
				throw new IllegalArgumentException( "unknown species '" + text + "' (use the latin name, e.g. Homo sapiens)" );
			return species;
		}
		Object converted = TextUtil2.fromString( type, text );
		if ( converted == null && !text.isEmpty() )
			throw new IllegalArgumentException( "'" + text + "' is not a value of this type" );
		return converted;
	}

	/** A species by latin name, by "Human (Homo sapiens)" or by its repository path. */
	private static Species species( String text )
	{
		String name = text.trim();
		int open = name.lastIndexOf( '(' );
		if ( open >= 0 && name.endsWith( ")" ) )
			name = name.substring( open + 1, name.length() - 1 ).trim();
		if ( name.contains( "/" ) )
			name = name.substring( name.lastIndexOf( '/' ) + 1 );
		return Species.getSpecies( name );
	}

	private static String message( Throwable e )
	{
		return e.getClass().getSimpleName() + ( e.getMessage() == null ? "" : ": " + e.getMessage() );
	}
}
