package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;

import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.server.servlets.webservices.providers.WebProvider;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanAsMapUtil;

/**
 * Shared, test-friendly helpers for the analysis MCP tools (phase 3).
 *
 * <p>All methods take/return plain JSON-serializable values (Maps, Lists, Strings, Numbers) and
 * never throw checked exceptions — failures are returned as {@link McpEnvelope} error envelopes so
 * the tool layer can convert them directly into MCP tool errors.</p>
 *
 * <p>The design is deliberately registry-driven (via {@link AnalysisMethodRegistry}) rather than
 * hardcoded to any one set of analysis classes, so an external analysis registry (e.g. a
 * geneXplain-style configured registry) works unmodified when its bundle is present and absent
 * when it is not (the geneXplain-compatibility constraint).</p>
 */
public final class McpAnalysisSupport
{
	private McpAnalysisSupport()
	{
	}

	/** Map a {@link JobControl} status int to a stable string. */
	private static String statusOf( int status )
	{
		if ( status == JobControl.COMPLETED )
			return "done";
		if ( status == JobControl.TERMINATED_BY_REQUEST )
			return "cancelled";
		if ( status == JobControl.TERMINATED_BY_ERROR )
			return "error";
		if ( status == JobControl.PAUSED )
			return "paused";
		if ( status == JobControl.RUNNING )
			return "running";
		if ( status == JobControl.CREATED )
			return "queued";
		return "unknown";
	}

	/** Best-effort {@link JobControl} status of a task (defaults to COMPLETED if unavailable). */
	private static int safeStatus( TaskInfo info )
	{
		try
		{
			JobControl jc = info.getJobControl();
			return jc == null ? JobControl.COMPLETED : jc.getStatus();
		}
		catch ( Exception e )
		{
			return JobControl.COMPLETED;
		}
	}

	// ---------------------------------------------------------------- list

	/**
	 * List all analysis methods grouped by their analyses group. This delegates to the platform's
	 * own {@code analysis} provider ({@code list} action), which returns the flat
	 * {@code group/name} strings; the result is re-shaped into
	 * {@code {groups: {<group>: [{name, fullName}]}, total: N}} for discoverability.
	 */
	public static McpEnvelope listMethods()
	{
		McpEnvelope env = McpProviderSupport.invoke( "analysis", "list", new LinkedHashMap<String, Object>() );
		if ( !env.isOk() )
			return env;
		// The provider returns values as a flat JSON array of "group/name" strings.
		@SuppressWarnings( "unchecked" )
		List<String> joined = (List<String>) env.getData();
		Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<String, List<Map<String, Object>>>();
		int total = 0;
		if ( joined != null )
		{
			for ( String entry : joined )
			{
				int slash = entry.indexOf( '/' );
				String group = slash < 0 ? "" : entry.substring( 0, slash );
				String name = slash < 0 ? entry : entry.substring( slash + 1 );
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put( "name", name );
				m.put( "group", group );
				m.put( "fullName", entry );
				List<Map<String, Object>> list = groups.get( group );
				if ( list == null )
				{
					list = new ArrayList<Map<String, Object>>();
					groups.put( group, list );
				}
				list.add( m );
				total++;
			}
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "groups", groups );
		result.put( "total", Integer.valueOf( total ) );
		return McpEnvelope.ok( result );
	}

	// ---------------------------------------------------------------- describe

	/**
	 * Describe one analysis method: its parameter input schema (property name, type, description,
	 * default) plus input/output names. The schema is derived from the method's parameters bean
	 * via {@link ComponentFactory} (the same introspection the GUI uses), so nested/composite
	 * parameters appear with {@code /}-separated keys.
	 */
	public static McpEnvelope describeMethod( String name )
	{
		return describeMethod( name, null, null );
	}

	/** At most this many characters of an analysis' help page are returned. */
	private static final int HELP_CHARS = 4000;

	/**
	 * Like {@link #describeMethod(String)}, after binding {@code params} as {@link #runAnalysis} would: the choices
	 * of some parameters depend on others (e.g. the conditions of Genome Enhancer are the categories of the chosen
	 * annotation diagram), and array items appear once the array is set.
	 *
	 * @param filter when set, the allowed values of choice parameters are only those containing it (case-insensitive)
	 */
	public static McpEnvelope describeMethod( String name, Map<String, Object> params, String filter )
	{
		return describeMethod( name, params, filter, null );
	}

	/**
	 * Like {@link #describeMethod(String, Map, String)}, listing only some parameters: those named in {@code only}
	 * (with their fields and items), or with a {@code filter} and no {@code only}, the choice parameters with
	 * matching values. Either leaves the help text out: a caller looking up values has read it.
	 */
	public static McpEnvelope describeMethod( String name, Map<String, Object> params, String filter, List<String> only )
	{
		AnalysisMethod method;
		try
		{
			method = AnalysisMethodRegistry.getAnalysisMethod( name );
		}
		catch ( Exception e )
		{
			method = null;
		}
		if ( method == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no analysis method named: " + name );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "name", name );
		try
		{
			String description = method.getDescription();
			String help = help( name, description );
			if ( help != null )
			{
				// the description names a help page: give its text instead of the resource path
				m.put( "description", clipText( firstParagraph( help ), 300 ) );
				if ( ( filter == null || filter.isEmpty() ) && ( only == null || only.isEmpty() ) )
					m.put( "help", clipText( help, HELP_CHARS ) );
			}
			else
				m.put( "description", description );
		}
		catch ( Exception e )
		{
			// ignore
		}
		if ( filter != null && !filter.isEmpty() )
			m.put( "filter", filter );
		// Parameter bean + schema
		if ( method instanceof AnalysisMethodSupport )
		{
			AnalysisMethodSupport<?> support = (AnalysisMethodSupport<?>) method;
			AnalysisParameters bean = support.getParameters();
			if ( bean != null )
			{
				if ( params != null && !params.isEmpty() )
				{
					McpEnvelope bound = bindParameters( bean, params );
					if ( !bound.isOk() )
						return bound;
					m.put( "boundParams", new ArrayList<String>( params.keySet() ) );
				}
				List<Map<String, Object>> props = new ArrayList<Map<String, Object>>();
				String lowerFilter = filter == null || filter.isEmpty() ? null : filter.toLowerCase( java.util.Locale.ENGLISH );
				collectProperties( ComponentFactory.getModel( bean, ComponentFactory.Policy.DEFAULT, true ), "", props, lowerFilter, 0 );
				if ( only != null && !only.isEmpty() )
				{
					props.removeIf( p -> only.stream().noneMatch( n -> p.get( "name" ).equals( n ) || ( (String) p.get( "name" ) ).startsWith( n + "/" ) ) );
					m.put( "only", new ArrayList<String>( only ) );
				}
				else if ( lowerFilter != null )
					props.removeIf( p -> !( p.get( "matchingCount" ) instanceof Integer ) || (Integer) p.get( "matchingCount" ) == 0 );
				m.put( "properties", props );
				String[] in = bean.getInputNames();
				String[] out = bean.getOutputNames();
				m.put( "inputNames", in == null ? new ArrayList<String>() : new ArrayList<String>( java.util.Arrays.asList( in ) ) );
				m.put( "outputNames", out == null ? new ArrayList<String>() : new ArrayList<String>( java.util.Arrays.asList( out ) ) );
			}
		}
		return McpEnvelope.ok( m );
	}

	/** At most this many allowed values are listed per property (the total is reported beside them). */
	private static final int ALLOWED_VALUES_CAP = 200;

	/**
	 * The values a choice property accepts — the tags of its property editor, i.e. what the GUI shows in the
	 * drop-down — or null for a free-form property.
	 */
	private static String[] allowedValues( Property p )
	{
		try
		{
			Class<?> editorClass = p.getPropertyEditorClass();
			if ( editorClass == null )
				return null;
			java.beans.PropertyEditor editor = (java.beans.PropertyEditor) editorClass.getDeclaredConstructor().newInstance();
			if ( editor instanceof com.developmentontheedge.beans.editors.PropertyEditorEx )
			{
				com.developmentontheedge.beans.editors.PropertyEditorEx editorEx = (com.developmentontheedge.beans.editors.PropertyEditorEx) editor;
				editorEx.setBean( p.getOwner() );
				editorEx.setDescriptor( p.getDescriptor() );
			}
			try
			{
				editor.setValue( p.getValue() );
			}
			catch ( Exception e )
			{
				// some editors need no current value to list their tags
			}
			String[] tags = editor.getTags();
			return tags == null || tags.length == 0 ? null : tags;
		}
		catch ( Throwable e )
		{
			return null;
		}
	}

	/** At most this many parameters are described (nested beans and array items included). */
	private static final int PROPERTIES_CAP = 500;
	private static final int NESTING_CAP = 4;

	/**
	 * Recursively flatten the properties of a bean model into a list of property descriptors: a nested bean is
	 * listed with its fields ({@code expertSettings/cutoffFC}), an array of beans with the fields of each item
	 * ({@code conditions/0/condition}), so every key is one {@link #runAnalysis} takes.
	 *
	 * @param filter lower-cased text the listed allowed values must contain, or null for all of them
	 */
	private static void collectProperties( Property model, String prefix, List<Map<String, Object>> out, String filter, int depth )
	{
		if ( model == null )
			return;
		for ( int i = 0; i < model.getPropertyCount() && out.size() < PROPERTIES_CAP; i++ )
		{
			Property p = model.getPropertyAt( i );
			if ( p.getValueClass() == Class.class )
				continue; // getClass() of a bean without BeanInfo: not a parameter
			String key = prefix.isEmpty() ? p.getName() : prefix + "/" + ( model instanceof ArrayProperty ? String.valueOf( i ) : p.getName() );
			Map<String, Object> pm = new LinkedHashMap<String, Object>();
			pm.put( "name", key );
			Class<?> vc = p.getValueClass();
			pm.put( "type", vc == null ? "unknown" : vc.getName() );
			pm.put( "displayName", p.getDisplayName() );
			try
			{
				pm.put( "description", p.getShortDescription() );
			}
			catch ( Exception e )
			{
				// ignore
			}
			pm.put( "readOnly", Boolean.valueOf( p.isReadOnly() ) );
			String[] allowed = allowedValues( p );
			if ( allowed != null )
			{
				List<String> values = new ArrayList<String>();
				for ( String value : allowed )
					if ( filter == null || value.toLowerCase( java.util.Locale.ENGLISH ).contains( filter ) )
						values.add( value );
				pm.put( "allowedValues", new ArrayList<String>( values.subList( 0, Math.min( values.size(), ALLOWED_VALUES_CAP ) ) ) );
				if ( filter != null )
					pm.put( "matchingCount", Integer.valueOf( values.size() ) );
				if ( allowed.length > ALLOWED_VALUES_CAP || filter != null )
					pm.put( "allowedValuesCount", Integer.valueOf( allowed.length ) );
			}
			out.add( pm );
			// A nested bean or an array of beans: list its fields, which carry the defaults (the bean itself is
			// not JSON-friendly)
			if ( isNested( p ) && depth < NESTING_CAP )
				collectProperties( p, key, out, filter, depth + 1 );
			else if ( p instanceof ArrayProperty && hasBeanItems( (ArrayProperty) p ) && depth < NESTING_CAP )
			{
				pm.put( "itemCount", Integer.valueOf( p.getPropertyCount() ) );
				collectProperties( p, key, out, filter, depth + 1 );
			}
			else
			{
				try
				{
					pm.put( "default", McpJsonSafe.toJsonSafe( p.getValue() ) );
				}
				catch ( Exception e )
				{
					pm.put( "default", null );
				}
			}
		}
	}

	/**
	 * A bean whose fields are shown as parameters, as the web UI's property inspector shows them; one with its own
	 * editor (a repository path, a color, ...) is a single field.
	 */
	private static boolean isNested( Property p )
	{
		if ( !( p instanceof CompositeProperty ) || p.isHideChildren() || p.getPropertyEditorClass() != null )
			return false;
		Class<?> vc = p.getValueClass();
		return vc == null || !DataElementPath.class.isAssignableFrom( vc ) && ( vc.getPackage() == null || !vc.getPackage().getName().startsWith( "java." ) );
	}

	/** The first line of a help text that is not a heading. */
	private static String firstParagraph( String text )
	{
		for ( String line : text.split( "\n" ) )
			if ( !line.startsWith( "## " ) && !line.trim().isEmpty() )
				return line.trim();
		return text;
	}

	private static String clipText( String text, int max )
	{
		if ( text.length() <= max )
			return text;
		int cut = text.lastIndexOf( ' ', max );
		return text.substring( 0, cut > max / 2 ? cut : max ) + "…";
	}

	private static boolean hasBeanItems( ArrayProperty p )
	{
		Class<?> item = p.getItemClass();
		return item != null && !item.isPrimitive() && !item.isEnum() && ( item.getPackage() == null || !item.getPackage().getName().startsWith( "java." ) )
				&& !DataElementPath.class.isAssignableFrom( item );
	}

	/**
	 * The text of an analysis' help page, when its description names one (a ".html" resource, as most geneXplain
	 * analyses have it); null otherwise.
	 */
	private static String help( String name, String description )
	{
		if ( description == null || !description.trim().toLowerCase( java.util.Locale.ENGLISH ).matches( ".*\\.html?" ) )
			return null;
		try
		{
			ru.biosoft.analysiscore.AnalysisMethodInfo info = AnalysisMethodRegistry.getMethodInfo( name );
			String html = info == null ? null : info.getDescriptionHTML();
			if ( html == null || html.isEmpty() )
				return null;
			String text = McpIntroPages.htmlText( html );
			return text.isEmpty() ? null : text;
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	/**
	 * The parameters of an analysis as the web UI's property inspector sees them, or null when there is no such
	 * analysis. {@code analysis} is its name or repository path.
	 */
	public static ComponentModel parametersModel( String analysis )
	{
		try
		{
			AnalysisMethod method = AnalysisMethodRegistry.getAnalysisMethod( analysis );
			if ( method == null && analysis != null && analysis.contains( "/" ) )
				method = AnalysisMethodRegistry.getAnalysisMethod( DataElementPath.create( analysis ).getName() );
			if ( !( method instanceof AnalysisMethodSupport ) )
				return null;
			AnalysisParameters bean = ( (AnalysisMethodSupport<?>) method ).getParameters();
			return bean == null ? null : ComponentFactory.getModel( bean, ComponentFactory.Policy.DEFAULT, true );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	/**
	 * Whether the parameters have a top-level parameter and it accepts a value: a choice parameter one of its
	 * allowed values, a boolean or number parameter a value of that type.
	 * @param value the value, or null to check only that the parameter exists
	 */
	public static boolean accepts( ComponentModel model, String parameter, String value )
	{
		Property p = null;
		for ( int i = 0; i < model.getPropertyCount() && p == null; i++ )
			if ( model.getPropertyAt( i ).getName().equals( parameter ) )
				p = model.getPropertyAt( i );
		if ( p == null )
			return false;
		if ( value == null )
			return true;
		String[] allowed = allowedValues( p );
		if ( allowed != null && !p.getValueClass().isArray() )
			return java.util.Arrays.asList( allowed ).contains( value );
		Class<?> vc = p.getValueClass();
		if ( vc == Boolean.class || vc == boolean.class )
			return "true".equals( value ) || "false".equals( value );
		if ( vc != null && ( Number.class.isAssignableFrom( vc ) || vc == int.class || vc == double.class || vc == long.class || vc == float.class ) )
		{
			try
			{
				Double.parseDouble( value );
				return true;
			}
			catch ( NumberFormatException e )
			{
				return false;
			}
		}
		return true;
	}

	// ---------------------------------------------------------------- run

	/**
	 * Run an analysis. Parameters are bound by bean property name (flat map with {@code /}-joined
	 * keys for nested values; string values that are repository paths for path-typed properties are
	 * converted to {@link DataElementPath}). With {@code sync=true} the analysis runs inline and
	 * the result is returned directly; otherwise it is queued via {@link TaskManager} and a
	 * {@code {taskId, status}} is returned.
	 */
	public static McpEnvelope runAnalysis( String name, Map<String, Object> params, String originPath, boolean sync )
	{
		AnalysisMethodSupport<?> method;
		try
		{
			method = (AnalysisMethodSupport<?>) AnalysisMethodRegistry.getAnalysisMethod( name );
		}
		catch ( Exception e )
		{
			method = null;
		}
		if ( method == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no analysis method named: " + name );
		AnalysisParameters bean = method.getParameters();
		if ( bean == null )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "analysis method has no parameters bean: " + name );

		// Bind the user-supplied params onto the bean.
		McpEnvelope bound = bindParameters( bean, params == null ? new LinkedHashMap<String, Object>() : params );
		if ( !bound.isOk() )
			return bound;

		// Validation.
		try
		{
			method.validateParameters();
		}
		catch ( IllegalArgumentException e )
		{
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "parameter validation failed: " + e.getMessage() );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "parameter validation error: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}

		// as if the user had opened the output's project: the run is journalled there and sees it as the current project
		McpProjectContext.useProjectOf( bean );

		if ( sync )
		{
			try
			{
				Object result = method.justAnalyzeAndPut();
				// Persist the analysis metadata (analysisName + params) onto the produced result
				// collection so it can be re-run via repeatAnalysis. This mirrors what the job
				// control lifecycle (AnalysisJobControl) does after an async run; a direct sync
				// call must do it itself.
				persistResult( method, result );
				return McpEnvelope.ok( describeResult( result, name ) );
			}
			catch ( Exception e )
			{
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "analysis failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
			}
		}
		// Async: queue it.
		try
		{
			TaskManager tm = TaskManager.getInstance();
			method.setLogger( taskLogger() );
			TaskInfo info = tm.addAnalysisTask( method, true );
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			result.put( "taskId", info.getName() );
			result.put( "status", statusOf( safeStatus( info ) ) );
			return McpEnvelope.ok( result );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not queue analysis: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Queue an analysis (async) under a <em>named</em> (logged-in) session so the resulting task is
	 * owned by that user. This mirrors production, where the MCP servlet authenticates every request,
	 * and lets the user-gated task tools ({@link #taskStatus}/{@link #cancelTask}, which delegate to
	 * the {@code tasks} provider) resolve the task by its owner. Sync runs are unaffected (they don't
	 * create a tracked task).
	 */
	public static McpEnvelope runAnalysisAs( String user, String name, Map<String, Object> params, String originPath, boolean sync )
	{
		if ( sync )
			return runAnalysis( name, params, originPath, true );
		try
		{
			return McpProviderSupport.withUserSession( user,
					() -> runAnalysis( name, params, originPath, false ) );
		}
		catch ( RuntimeException e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not queue analysis: " + e.getMessage() );
		}
	}

	/**
	 * Bind a flat/hierarchical map of params onto a parameters bean. Uses the same introspection
	 * the GUI uses ({@link BeanAsMapUtil}) so nested composite params work. String values that look
	 * like repository paths are left as-is for {@code DataElementPath} properties (converted in
	 * {@link #coercePath}); unknown keys are reported with the list of valid keys.
	 */
	private static McpEnvelope bindParameters( AnalysisParameters bean, Map<String, Object> params )
	{
		if ( params.isEmpty() )
			return McpEnvelope.ok( null );
		// Discover the valid top-level property keys from the bean.
		ComponentModel model = ComponentFactory.getModel( bean, ComponentFactory.Policy.DEFAULT, true );
		List<String> validKeys = new ArrayList<String>();
		for ( int i = 0; i < model.getPropertyCount(); i++ )
			validKeys.add( model.getPropertyAt( i ).getName() );
		for ( String key : params.keySet() )
		{
			String top = key;
			int slash = key.indexOf( '/' );
			if ( slash >= 0 )
				top = key.substring( 0, slash );
			if ( !validKeys.contains( top ) )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
						"unknown parameter '" + key + "'. Valid keys: " + validKeys );
		}
		Map<String, Object> flat = normalizeIndexKeys( params );
		Map<String, Object> nested;
		// Coerce path-typed leaves, then bind. readBeanFromHierarchicalMap expects a *nested* map
		// (composite property -> sub-map, array property -> list), so expand the caller's flat
		// '/'-joined keys into that shape first.
		coercePaths( bean, flat );
		try
		{
			nested = BeanAsMapUtil.expandMap( flat );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "could not bind parameters " + flat.keySet()
					+ ": conflicting keys (e.g. both 'name' and 'name/...')" + causeOf( e ) );
		}
		// Bind one top-level property at a time, in the bean's own property order (a structure-changing
		// property may recreate its siblings), so a failure names the offending parameter.
		for ( int i = 0; i < model.getPropertyCount(); i++ )
		{
			Property p = model.getPropertyAt( i );
			if ( !nested.containsKey( p.getName() ) )
				continue;
			Map<String, Object> single = new LinkedHashMap<String, Object>();
			single.put( p.getName(), nested.get( p.getName() ) );
			try
			{
				BeanAsMapUtil.readBeanFromHierarchicalMap( single, bean );
			}
			catch ( Exception e )
			{
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
						"could not bind parameter '" + p.getName() + "'" + expectedShape( p ) + causeOf( e ) );
			}
		}
		return McpEnvelope.ok( null );
	}

	/**
	 * Accept bare numeric path segments as array indices: {@code conditions/0/condition} is rewritten
	 * to {@code conditions/[0]/condition}, the subscript form {@link BeanAsMapUtil#expandMap} expects
	 * (a bare {@code 0} would make {@code conditions} a map, which an array property rejects). The
	 * first segment is always a property name and is left as is.
	 */
	private static Map<String, Object> normalizeIndexKeys( Map<String, Object> params )
	{
		Map<String, Object> out = new LinkedHashMap<String, Object>();
		for ( Map.Entry<String, Object> e : params.entrySet() )
		{
			String[] parts = e.getKey().split( "/", -1 );
			for ( int i = 1; i < parts.length; i++ )
				if ( !parts[ i ].isEmpty() && parts[ i ].chars().allMatch( Character::isDigit ) )
					parts[ i ] = "[" + parts[ i ] + "]";
			out.put( String.join( "/", parts ), e.getValue() );
		}
		return out;
	}

	/** A hint on the value shape an array or nested-bean property expects, for binding errors. */
	private static String expectedShape( Property p )
	{
		if ( BeanAsMapUtil.isArrayProperty( p ) )
			return " (an array: pass a JSON list, e.g. \"" + p.getName() + "\": [{...}], or keys like '" + p.getName() + "/0/<field>')";
		if ( BeanAsMapUtil.isCompositeProperty( p ) )
			return " (a nested bean: pass a JSON object, e.g. \"" + p.getName() + "\": {...}, or keys like '" + p.getName() + "/<field>')";
		return "";
	}

	private static String causeOf( Exception e )
	{
		return e.getMessage() == null ? "" : ": " + e.getMessage();
	}

	/**
	 * Replace string values for {@code DataElementPath}-typed properties with resolved
	 * {@link DataElementPath} objects. Operates recursively over the model so nested composite
	 * params are reached.
	 */
	private static void coercePaths( Object bean, Map<String, Object> params )
	{
		if ( params.isEmpty() )
			return;
		ComponentModel model = ComponentFactory.getModel( bean, ComponentFactory.Policy.DEFAULT, true );
		for ( Map.Entry<String, Object> e : params.entrySet() )
		{
			Property p;
			try
			{
				p = model.findProperty( e.getKey() );
			}
			catch ( Exception ex )
			{
				p = null; // e.g. an array subscript the model cannot resolve yet; bound as text later
			}
			if ( p == null )
				continue;
			Object val = e.getValue();
			if ( val instanceof String )
			{
				Class<?> vc = p.getValueClass();
				if ( vc != null && DataElementPath.class.isAssignableFrom( vc ) )
				{
					e.setValue( DataElementPath.create( (String) val ) );
				}
			}
			else if ( val instanceof Map )
			{
				// Nested composite: recurse.
				Object nested = p.getValue();
				if ( nested != null )
					coercePaths( nested, (Map<String, Object>) val );
			}
		}
	}

	// ---------------------------------------------------------------- status / cancel / list tasks

	/**
	 * A logger of its own for a queued analysis. By default every analysis logs to one shared logger, and a task
	 * captures its log by listening to its method's logger, so concurrent tasks (of any user) would read each
	 * other's lines. The web UI gives each run its job's logger the same way; the records still reach the server log.
	 */
	private static java.util.logging.Logger taskLogger()
	{
		return java.util.logging.Logger.getLogger( "biouml.plugins.mcp.task." + java.util.UUID.randomUUID() );
	}

	/** Whether the task belongs to the session user (the {@code tasks} provider's rule). */
	static boolean ownsTask( TaskInfo info )
	{
		String user = ru.biosoft.access.security.SecurityManager.getSessionUser();
		return info != null && info.getUser() != null && user != null && info.getUser().equalsIgnoreCase( user );
	}

	/**
	 * Report a task's status. Delegates to the platform's {@code tasks} provider ({@code status}
	 * action), which returns {@code {status: <int>, percent: 0-100, values: [textStatus]}}. The
	 * response is re-shaped to {@code {taskId, status, progress, textStatus}}.
	 *
	 * <p>The provider gates the action on the session user, so the call runs under a named session
	 * (see {@link McpProviderSupport#withUserSession}).</p>
	 */
	public static McpEnvelope taskStatus( String taskId )
	{
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "taskID", taskId );
		WebProvider provider = McpProviderSupport.provider( "tasks" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: tasks" );
		Map<String, Object> resp;
		String log;
		try
		{
			// another user's task: the provider answers with nothing, and its log must not be read either
			TaskInfo owned = McpProviderSupport.withUserSession( McpConstants.TASK_USER, () -> {
				TaskInfo info = TaskManager.getInstance().getTask( taskId );
				return ownsTask( info ) ? info : null;
			} );
			if ( owned == null )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no task named: " + taskId );
			log = owned.getLogInfo();
			resp = McpProviderSupport.withUserSession( McpConstants.TASK_USER,
					() -> McpProviderSupport.responseMap( (WebJSONProviderSupport) provider, "tasks", "status", params ) );
		}
		catch ( RuntimeException e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not read task status: " + e.getMessage() );
		}
		if ( resp == null )
		{
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no task named: " + taskId );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "taskId", taskId );
		Object status = resp.get( JSONResponse.ATTR_STATUS );
		boolean known = status instanceof Number;
		m.put( "status", known ? Integer.valueOf( ( (Number) status ).intValue() ) : null );
		if ( known )
			m.put( "state", statusOf( ( (Number) status ).intValue() ) );
		Object percent = resp.get( JSONResponse.ATTR_PERCENT );
		// a workflow's last steps may not report progress: a completed task is at 100, whatever it last reported
		boolean completed = known && ( (Number) status ).intValue() == JobControl.COMPLETED;
		m.put( "progress", completed ? Integer.valueOf( 100 ) : percent instanceof Number ? Integer.valueOf( ( (Number) percent ).intValue() ) : null );
		Object values = resp.get( JSONResponse.ATTR_VALUES );
		m.put( "textStatus", values instanceof java.util.List && ! ( (java.util.List<?>) values ).isEmpty() ? ( (java.util.List<?>) values ).get( 0 ) : null );
		m.putAll( summarizeLog( log ) );
		return McpEnvelope.ok( m );
	}

	/** A level at the start of a log line: the task log's own ("INFO: "), or that of a tool's line relayed into it (Picard's "WARNING\t"). */
	private static final java.util.regex.Pattern LOG_LEVEL = java.util.regex.Pattern
			.compile( "^\\s*(INFO|WARN|WARNING|ERROR|SEVERE|DEBUG|TRACE|CONFIG|FINE|FINER|FINEST)\\b:?\\s*" );
	/** Whole words, so that a tool option such as WARN_ON_MISSING_CONTIG in an echoed command line does not count. */
	private static final java.util.regex.Pattern PROBLEM_WORD = java.util.regex.Pattern
			.compile( "\\b(ERROR|SEVERE|WARN|WARNING|failed)\\b|Exception\\b", java.util.regex.Pattern.CASE_INSENSITIVE );
	private static final java.util.regex.Pattern STACK_FRAME = java.util.regex.Pattern.compile( "^\\s+at\\s" );
	private static final int LOG_PROBLEMS_CAP = 20, LOG_TAIL_LINES = 10, LOG_LINE_CHARS = 300;
	/** A problem message that is the start of another one, at least this long, is taken for a cut-off copy of it. */
	private static final int LOG_PREFIX_MERGE_CHARS = 40;
	private static final java.util.regex.Pattern DIGITS = java.util.regex.Pattern.compile( "\\d+" );

	/** {@code problems}, {@code problemCount}, {@code problemKinds} and {@code logTail} of a task log: its warnings/errors ({@code problems}:
	 * distinct messages, latest last, each with its count; {@code problemCount}: all problem lines; {@code problemKinds}: all
	 * distinct messages) and its last lines ({@code logTail}, a run of lines differing only in numbers shown once with its
	 * count), so a caller can follow a long run (and see e.g. skipped workflow steps) without reading the log by script. */
	public static Map<String, Object> summarizeLog( String log )
	{
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		if ( log == null || log.isEmpty() )
			return m;
		String[] lines = log.split( "\\r?\\n" );
		// Group problem lines by message pattern (digits normalized, so 8,000 "gene ENSMUSG0000..." lines are one
		// entry), each with its count, latest example and where it was last seen.
		Map<String, LogGroup> byPattern = new java.util.HashMap<String, LogGroup>();
		int problemCount = 0;
		for ( int i = 0; i < lines.length; i++ )
		{
			if ( !isProblem( lines[ i ] ) )
				continue;
			problemCount++;
			String pattern = DIGITS.matcher( lines[ i ] ).replaceAll( "#" );
			LogGroup group = byPattern.get( pattern );
			if ( group == null )
				byPattern.put( pattern, group = new LogGroup( pattern ) );
			group.add( lines[ i ], i );
		}
		if ( problemCount > 0 )
		{
			List<LogGroup> groups = mergeCutOff( byPattern.values() );
			groups.sort( ( a, b ) -> Integer.compare( a.last, b.last ) );
			List<Map<String, Object>> problems = new ArrayList<Map<String, Object>>();
			for ( LogGroup group : groups.subList( Math.max( 0, groups.size() - LOG_PROBLEMS_CAP ), groups.size() ) )
			{
				Map<String, Object> p = new LinkedHashMap<String, Object>();
				p.put( "message", clip( group.example ) );
				p.put( "count", Integer.valueOf( group.count ) );
				problems.add( p );
			}
			m.put( "problems", problems );
			m.put( "problemCount", Integer.valueOf( problemCount ) );
			m.put( "problemKinds", Integer.valueOf( groups.size() ) );
		}
		m.put( "logTail", tail( lines ) );
		return m;
	}

	/** Problem lines of one message pattern. */
	private static final class LogGroup
	{
		final String pattern;
		String example;
		int count, last;

		LogGroup( String pattern )
		{
			this.pattern = pattern;
		}

		void add( String line, int index )
		{
			example = line;
			count++;
			last = index;
		}
	}

	/**
	 * Whether a log line reports a problem. A line's level decides: the level of a tool's line relayed into the log
	 * (Picard writes "INFO\t..." and "WARNING\t..." lines to stderr, whatever level they are logged at) over the
	 * log's own; an INFO line or one with no level counts only when it names a failure, e.g. "Skip failed analysis".
	 */
	private static boolean isProblem( String line )
	{
		java.util.regex.Matcher level = LOG_LEVEL.matcher( line );
		if ( !level.find() )
			return !STACK_FRAME.matcher( line ).find() && PROBLEM_WORD.matcher( line ).find();
		String rest = line.substring( level.end() );
		java.util.regex.Matcher toolLevel = LOG_LEVEL.matcher( rest );
		if ( toolLevel.find() )
			return isProblemLevel( toolLevel.group( 1 ) );
		return isProblemLevel( level.group( 1 ) ) || "INFO".equals( level.group( 1 ) ) && PROBLEM_WORD.matcher( rest ).find();
	}

	private static boolean isProblemLevel( String level )
	{
		return level.startsWith( "WARN" ) || "ERROR".equals( level ) || "SEVERE".equals( level );
	}

	/**
	 * Fold a group whose message is the start of a longer one into it: a tool line cut off in the log ("... that is
	 * not part of") is the same problem as the whole line. In sorted order a prefix comes just before a line it starts.
	 */
	private static List<LogGroup> mergeCutOff( java.util.Collection<LogGroup> groups )
	{
		List<LogGroup> sorted = new ArrayList<LogGroup>( groups );
		sorted.sort( ( a, b ) -> a.pattern.compareTo( b.pattern ) );
		List<LogGroup> out = new ArrayList<LogGroup>();
		for ( int i = 0; i < sorted.size(); i++ )
		{
			LogGroup group = sorted.get( i );
			LogGroup next = i + 1 < sorted.size() ? sorted.get( i + 1 ) : null;
			if ( next != null && group.pattern.length() >= LOG_PREFIX_MERGE_CHARS && next.pattern.startsWith( group.pattern ) )
			{
				next.count += group.count;
				next.last = Math.max( next.last, group.last );
				continue;
			}
			out.add( group );
		}
		return out;
	}

	/** The last lines of a log; consecutive lines that differ only in numbers (e.g. progress lines) are shown once, the latest, with their count. */
	private static List<String> tail( String[] lines )
	{
		java.util.LinkedList<String> tail = new java.util.LinkedList<String>();
		int i = lines.length - 1;
		while ( i >= 0 && tail.size() < LOG_TAIL_LINES )
		{
			String pattern = DIGITS.matcher( lines[ i ] ).replaceAll( "#" );
			int n = 1;
			while ( i - n >= 0 && pattern.equals( DIGITS.matcher( lines[ i - n ] ).replaceAll( "#" ) ) )
				n++;
			tail.addFirst( n > 1 ? clip( lines[ i ] ) + " [×" + n + "]" : clip( lines[ i ] ) );
			i -= n;
		}
		return new ArrayList<String>( tail );
	}

	private static String clip( String line )
	{
		return line.length() <= LOG_LINE_CHARS ? line : line.substring( 0, LOG_LINE_CHARS ) + "…";
	}

	/**
	 * Cancel a queued/running task. Delegates to the {@code tasks} provider ({@code stop_rows}
	 * action). The provider gates on the session user, so the call runs under a named session.
	 * Returns the (possibly in-progress) status of the task after the stop request.
	 */
	public static McpEnvelope cancelTask( String taskId )
	{
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "rows", taskId );
		try
		{
			McpProviderSupport.withUserSession( McpConstants.TASK_USER,
					() -> {
						McpProviderSupport.invoke( "tasks", "stop_rows", params );
						return null;
					} );
		}
		catch ( RuntimeException e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not cancel task: " + e.getMessage() );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "taskId", taskId );
		McpEnvelope st = taskStatus( taskId );
		m.put( "status", st.isOk() && st.getData() instanceof Map ? ( (Map<String, Object>) st.getData() ).get( "status" ) : null );
		m.put( "cancelled", Boolean.TRUE );
		return McpEnvelope.ok( m );
	}

	/**
	 * List the session user's active and recent tasks (a summary of the TaskManager's task collection). Without a
	 * tasks database that collection holds every user's tasks, so it is filtered by owner here.
	 */
	public static McpEnvelope listTasks()
	{
		try
		{
			return McpProviderSupport.withUserSession( McpConstants.TASK_USER, McpAnalysisSupport::listOwnTasks );
		}
		catch ( RuntimeException e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not list tasks: " + e.getMessage() );
		}
	}

	private static McpEnvelope listOwnTasks()
	{
		TaskManager tm = TaskManager.getInstance();
		DataCollection<?> dc = tm.getTasksInfo();
		List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
		try
		{
			for ( Object o : dc )
			{
				// the collection holds TaskInfo wrappers
				TaskInfo info = o instanceof ru.biosoft.tasks.TaskInfoWrapper ? ( (ru.biosoft.tasks.TaskInfoWrapper) o ).getTask()
						: o instanceof TaskInfo ? (TaskInfo) o : null;
				if ( ownsTask( info ) )
				{
					Map<String, Object> m = new LinkedHashMap<String, Object>();
					m.put( "taskId", info.getName() );
					m.put( "type", info.getType() );
					m.put( "status", statusOf( safeStatus( info ) ) );
					tasks.add( m );
				}
			}
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not list tasks: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "count", Integer.valueOf( tasks.size() ) );
		result.put( "tasks", tasks );
		return McpEnvelope.ok( result );
	}

	// ---------------------------------------------------------------- repeat

	/**
	 * Re-run a completed analysis from its stored output collection (mirrors
	 * {@code RepeatAnalysisAction}): read the stored {@code analysisName} + parameters and run again.
	 */
	public static McpEnvelope repeatAnalysis( String resultPath, boolean sync )
	{
		DataElement de = CollectionFactory.getDataElement( resultPath );
		if ( de == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element at path: " + resultPath + McpProjectSupport.staleHint( resultPath ) );
		AnalysisMethod method;
		try
		{
			method = AnalysisParametersFactory.readAnalysis( de );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "could not read stored analysis from " + resultPath + ": " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		if ( !( method instanceof AnalysisMethodSupport ) )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "stored analysis is not runnable: " + ( method == null ? "null" : method.getClass().getName() ) );
		AnalysisMethodSupport<?> support = (AnalysisMethodSupport<?>) method;
		McpProjectContext.useProjectOf( support.getParameters() );
		if ( sync )
		{
			try
			{
				Object result = support.justAnalyzeAndPut();
				persistResult( support, result );
				return McpEnvelope.ok( describeResult( result, resultPath ) );
			}
			catch ( Exception e )
			{
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "repeat failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
			}
		}
		try
		{
			support.setLogger( taskLogger() );
			TaskInfo info = TaskManager.getInstance().addAnalysisTask( support, true );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "taskId", info.getName() );
			m.put( "status", statusOf( safeStatus( info ) ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not queue repeat: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	// ---------------------------------------------------------------- get result

	/**
	 * Read an analysis output collection: a {@link TableDataCollection} yields column names/types
	 * and its first rows (see the overloads for paging and column selection); any other {@link DataCollection} yields a child listing; anything else
	 * yields a type summary. This is how an agent reads results without the UI.
	 */
	public static McpEnvelope getAnalysisResult( String path )
	{
		return getAnalysisResult( path, null, null );
	}

	/** At most this many table rows per call. */
	private static final int TABLE_ROWS_LIMIT_MAX = 500;

	/**
	 * Like {@link #getAnalysisResult(String)}, reading table rows from {@code offset} (default 0), at most
	 * {@code limit} of them (default {@link McpConstants#TABLE_ROWS_CAP}, at most {@value #TABLE_ROWS_LIMIT_MAX}).
	 */
	public static McpEnvelope getAnalysisResult( String path, Integer offset, Integer limit )
	{
		return getAnalysisResult( path, offset, limit, null );
	}

	/**
	 * Like {@link #getAnalysisResult(String, Integer, Integer)}, returning only the table columns named in
	 * {@code selected} (in that order; null or empty for all of them).
	 */
	public static McpEnvelope getAnalysisResult( String path, Integer offset, Integer limit, List<String> selected )
	{
		DataElement de = McpProviderSupport.resolveElement( path );
		if ( de == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element at path: " + path + McpProjectSupport.staleHint( path ) );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		if ( de instanceof TableDataCollection )
		{
			TableDataCollection t = (TableDataCollection) de;
			List<String> all = new ArrayList<String>();
			List<String> allTypes = new ArrayList<String>();
			for ( ru.biosoft.table.TableColumn tc : t.columns() )
			{
				all.add( tc.getName() );
				allTypes.add( tc.getValueClass() == null ? "unknown" : tc.getValueClass().getName() );
			}
			List<String> columns = new ArrayList<String>();
			List<String> types = new ArrayList<String>();
			List<Integer> indices = new ArrayList<Integer>();
			for ( String name : selected == null || selected.isEmpty() ? all : selected )
			{
				int index = all.indexOf( name );
				if ( index < 0 )
					return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "no column '" + name + "' in " + path + "; columns: " + all );
				columns.add( name );
				types.add( allTypes.get( index ) );
				indices.add( Integer.valueOf( index ) );
			}
			m.put( "columns", columns );
			m.put( "columnTypes", types );
			m.put( "rowCount", Integer.valueOf( t.getSize() ) );
			List<List<Object>> rows = new ArrayList<List<Object>>();
			List<String> rowIds = new ArrayList<String>();
			int from = offset == null ? 0 : Math.max( 0, Math.min( offset.intValue(), t.getSize() ) );
			int count = limit == null || limit.intValue() <= 0 ? McpConstants.TABLE_ROWS_CAP : Math.min( limit.intValue(), TABLE_ROWS_LIMIT_MAX );
			int to = Math.min( t.getSize(), from + count );
			for ( int r = from; r < to; r++ )
			{
				String rowId;
				try
				{
					rowId = t.getName( r );
				}
				catch ( Exception e )
				{
					rowId = null;
				}
				rowIds.add( rowId );
				List<Object> row = new ArrayList<Object>();
				for ( Integer c : indices )
				{
					try
					{
						row.add( McpJsonSafe.toJsonSafe( t.getValueAt( r, c.intValue() ) ) );
					}
					catch ( Exception e )
					{
						row.add( null );
					}
				}
				rows.add( row );
			}
			m.put( "rowIds", rowIds );
			m.put( "rows", rows );
			if ( from > 0 )
				m.put( "offset", Integer.valueOf( from ) );
			m.put( "truncated", Boolean.valueOf( t.getSize() > to ) );
		}
		else if ( de instanceof ru.biosoft.bsa.Track )
		{
			McpEnvelope error = putTrackSites( (ru.biosoft.bsa.Track) de, m, offset, limit, selected );
			if ( error != null )
				return error;
		}
		else if ( de instanceof SimulationResult )
		{
			// A simulation result (e.g. produced by the "Simulation analysis" method) carries a
			// variable→index map plus the time series. Return the same shape the simulation provider's
			// `result` action emits ({vars, times, values}) so callers get the numbers directly.
			SimulationResult sr = (SimulationResult) de;
			m.put( "type", "simulation_result" );
			try
			{
				Map<String, Integer> pathMap = sr.getVariablePathMap();
				Map<String, Object> vars = new LinkedHashMap<String, Object>();
				if ( pathMap != null )
					for ( Map.Entry<String, Integer> e : pathMap.entrySet() )
						vars.put( e.getKey(), Integer.valueOf( e.getValue() ) );
				m.put( "vars", vars );
				double[] times = sr.getTimes();
				int nPoints = times == null ? 0 : times.length;
				int nData = ( nPoints > 0 && sr.getValue( 0 ) != null ) ? sr.getValue( 0 ).length : 0;
				List<List<Object>> timesOut = new ArrayList<List<Object>>();
				for ( int t = 0; t < nPoints; t++ )
					timesOut.add( java.util.Collections.singletonList( Double.valueOf( sr.getTime( t ) ) ) );
				m.put( "times", timesOut );
				// values is stored [point][var]; transpose to [var][point] so each variable is a row
				// (the same layout the simulation provider's `result` action emits).
				List<List<Object>> values = new ArrayList<List<Object>>();
				for ( int i = 0; i < nData; i++ )
				{
					List<Object> row = new ArrayList<Object>();
					for ( int t = 0; t < nPoints; t++ )
					{
						double[] at = sr.getValue( t );
						if ( at != null && i < at.length )
							row.add( Double.valueOf( at[ i ] ) );
						else
							row.add( null );
					}
					values.add( row );
				}
				m.put( "values", values );
				// Stochastic results also carry the Q1/median/Q3 percentile series.
				if ( sr instanceof StochasticSimulationResult )
				{
					StochasticSimulationResult ssr = (StochasticSimulationResult) sr;
					m.put( "Q1", transposeForJson( ssr.getQ1() ) );
					m.put( "Q2", transposeForJson( ssr.getMedian() ) );
					m.put( "Q3", transposeForJson( ssr.getQ3() ) );
				}
			}
			catch ( Exception e )
			{
				m.put( "error", "could not read simulation result: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
			}
		}
		else if ( de instanceof DataCollection )
		{
			DataCollection<?> dc = (DataCollection<?>) de;
			List<String> children = new ArrayList<String>();
			try
			{
				int cap = McpConstants.LIST_CHILDREN_CAP;
				List<String> names = dc.getNameList();
				for ( int i = 0; i < names.size() && i < cap; i++ )
					children.add( names.get( i ) );
			}
			catch ( Exception e )
			{
				// ignore
			}
			m.put( "type", "collection" );
			m.put( "elementCount", Integer.valueOf( dc.getSize() ) );
			m.put( "children", children );
		}
		else
		{
			m.put( "type", de.getClass().getName() );
		}
		return McpEnvelope.ok( m );
	}

	/** Columns of a track's sites before their properties. */
	private static final String[] SITE_COLUMNS = { "chromosome", "from", "to", "strand", "type" };
	/** A track whose sites cannot be paged by the database is read from its start: at most this far. */
	private static final int SITE_SKIP_MAX = 100000;

	/**
	 * Put the sites of a track into {@code m} as table rows, paged like table rows: the site ids as rowIds, then its
	 * chromosome, from, to, strand, type and properties (e.g. a VCF's RefAllele, AltAllele and genotype fields).
	 * @return null, or an error when the sites cannot be read
	 */
	@SuppressWarnings( "unchecked" )
	private static McpEnvelope putTrackSites( ru.biosoft.bsa.Track track, Map<String, Object> m, Integer offset, Integer limit, List<String> selected )
	{
		DataCollection<ru.biosoft.bsa.Site> sites;
		int size;
		try
		{
			sites = track.getAllSites();
			size = sites.getSize();
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "the sites of this track can not be listed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		int from = offset == null ? 0 : Math.max( 0, Math.min( offset.intValue(), size ) );
		int count = limit == null || limit.intValue() <= 0 ? McpConstants.TABLE_ROWS_CAP : Math.min( limit.intValue(), TABLE_ROWS_LIMIT_MAX );
		int to = Math.min( size, from + count );
		List<ru.biosoft.bsa.Site> page = new ArrayList<ru.biosoft.bsa.Site>();
		try
		{
			java.util.Iterator<ru.biosoft.bsa.Site> it;
			if ( sites instanceof ru.biosoft.access.core.SortableDataCollection )
				// the database pages the sites (an SQL track: LIMIT from, count)
				it = ( (ru.biosoft.access.core.SortableDataCollection<ru.biosoft.bsa.Site>) sites ).getSortedIterator( "", true, from, to );
			else
			{
				if ( from > SITE_SKIP_MAX )
					return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "this track's sites are read from its start: offset must be at most " + SITE_SKIP_MAX );
				it = sites.iterator();
				for ( int i = 0; i < from && it.hasNext(); i++ )
					it.next();
			}
			while ( it.hasNext() && page.size() < to - from )
				page.add( it.next() );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not read the sites of this track: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		List<String> all = new ArrayList<String>( java.util.Arrays.asList( SITE_COLUMNS ) );
		for ( ru.biosoft.bsa.Site site : page )
			if ( site.getProperties() != null )
				for ( com.developmentontheedge.beans.DynamicProperty property : site.getProperties() )
					if ( !all.contains( property.getName() ) )
						all.add( property.getName() );
		List<String> columns = selected == null || selected.isEmpty() ? all : selected;
		for ( String column : columns )
			if ( !all.contains( column ) && !page.isEmpty() )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "no column '" + column + "' in the sites of this track; columns: " + all );
		List<String> rowIds = new ArrayList<String>();
		List<List<Object>> rows = new ArrayList<List<Object>>();
		for ( ru.biosoft.bsa.Site site : page )
		{
			rowIds.add( site.getName() );
			List<Object> row = new ArrayList<Object>();
			for ( String column : columns )
				row.add( siteValue( site, column ) );
			rows.add( row );
		}
		m.put( "type", "track" );
		m.put( "columns", columns );
		m.put( "rowCount", Integer.valueOf( size ) );
		m.put( "rowIds", rowIds );
		m.put( "rows", rows );
		if ( from > 0 )
			m.put( "offset", Integer.valueOf( from ) );
		m.put( "truncated", Boolean.valueOf( size > to ) );
		return null;
	}

	private static Object siteValue( ru.biosoft.bsa.Site site, String column )
	{
		switch ( column )
		{
			case "chromosome":
				return site.getOriginalSequence() == null ? null : site.getOriginalSequence().getName();
			case "from":
				return Integer.valueOf( site.getFrom() );
			case "to":
				return Integer.valueOf( site.getTo() );
			case "strand":
				return site.getStrand() == ru.biosoft.bsa.StrandType.STRAND_PLUS ? "+" : site.getStrand() == ru.biosoft.bsa.StrandType.STRAND_MINUS ? "-"
						: site.getStrand() == ru.biosoft.bsa.StrandType.STRAND_BOTH ? "both" : ".";
			case "type":
				return site.getType();
			default:
				com.developmentontheedge.beans.DynamicProperty property = site.getProperties() == null ? null : site.getProperties().getProperty( column );
				return property == null ? null : McpJsonSafe.toJsonSafe( property.getValue() );
		}
	}

	/**
	 * Transpose a {@code [var][point]} 2-D array (the layout of a stochastic percentile series, e.g.
	 * Q1/median/Q3) into the {@code [var][point]} list-of-rows JSON form — each variable is a row. A
	 * {@code null} row is passed through as a null so the shape stays rectangular.
	 */
	private static List<List<Object>> transposeForJson( double[][] array )
	{
		List<List<Object>> out = new ArrayList<List<Object>>();
		if ( array == null )
			return out;
		for ( double[] row : array )
		{
			if ( row == null )
			{
				out.add( null );
				continue;
			}
			List<Object> r = new ArrayList<Object>();
			for ( double v : row )
				r.add( Double.valueOf( v ) );
			out.add( r );
		}
		return out;
	}

	/**
	 * Persist analysis metadata (analysisName + parameters) onto the produced result collection so
	 * it can be re-run via {@link #repeatAnalysis}. Mirrors the job-control lifecycle that does this
	 * after an async run. No-op if the result is not a {@link DataCollection}.
	 */
	private static void persistResult( AnalysisMethodSupport<?> method, Object result )
	{
		try
		{
			Object target = result;
			if ( result instanceof Object[] )
			{
				Object[] arr = (Object[]) result;
				if ( arr.length == 0 )
					return;
				target = arr[ 0 ];
			}
			if ( target instanceof DataCollection )
				AnalysisParametersFactory.write( (DataCollection<?>) target, method );
		}
		catch ( Exception e )
		{
			// Persisting is best-effort; the result itself is still returned.
		}
	}

	/**
	 * Summarize a {@code justAnalyzeAndPut} return value (usually an output {@link DataCollection})
	 * as a JSON-serializable map pointing at the produced result path(s).
	 */
	private static Object describeResult( Object result, String originName )
	{
		if ( result == null )
			return new LinkedHashMap<String, Object>();
		if ( result instanceof DataElement )
		{
			DataElement de = (DataElement) result;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "resultType", de.getClass().getName() );
			try
			{
				m.put( "resultPath", DataElementPath.create( de ).toString() );
			}
			catch ( Exception e )
			{
				// ignore
			}
			return m;
		}
		if ( result instanceof Object[] )
		{
			List<Object> out = new ArrayList<Object>();
			for ( Object o : (Object[]) result )
				out.add( describeResult( o, originName ) );
			return out;
		}
		return McpJsonSafe.toJsonSafe( result );
	}
}
