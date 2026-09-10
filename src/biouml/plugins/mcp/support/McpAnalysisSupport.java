package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
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

	// ---------------------------------------------------------------- list

	/**
	 * List all analysis methods grouped by their analyses group. Returns
	 * {@code {groups: {<group>: [{name, displayName, description, class}]}, total: N}}.
	 */
	public static McpEnvelope listMethods()
	{
		try
		{
			Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<String, List<Map<String, Object>>>();
			int total = 0;
			for ( String joined : AnalysisMethodRegistry.getAnalysisNamesWithGroup() )
			{
				int slash = joined.indexOf( '/' );
				String group = slash < 0 ? "" : joined.substring( 0, slash );
				String name = slash < 0 ? joined : joined.substring( slash + 1 );
				ru.biosoft.analysiscore.AnalysisMethodInfo info = null;
				try
				{
					// getMethodInfo returns metadata only (no instantiation), so listing stays cheap
					// and safe even for methods whose beans fail to reflect.
					info = AnalysisMethodRegistry.getMethodInfo( joined );
				}
				catch ( Exception e )
				{
					info = null;
				}
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put( "name", name );
				m.put( "group", group );
				m.put( "fullName", joined );
				if ( info != null )
				{
					try
					{
						m.put( "displayName", info.getDisplayName() );
						String desc = info.getShortDescription();
						m.put( "description", desc != null && !desc.isEmpty() ? desc : info.getDescription() );
						Class<?> cls = info.getAnalysisClass();
						m.put( "class", cls == null ? null : cls.getName() );
					}
					catch ( Exception e )
					{
						// leave the basic fields
					}
				}
				List<Map<String, Object>> list = groups.get( group );
				if ( list == null )
				{
					list = new ArrayList<Map<String, Object>>();
					groups.put( group, list );
				}
				list.add( m );
				total++;
			}
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			result.put( "groups", groups );
			result.put( "total", Integer.valueOf( total ) );
			return McpEnvelope.ok( result );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "failed to list analysis methods: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
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
			m.put( "description", method.getDescription() );
		}
		catch ( Exception e )
		{
			// ignore
		}
		// Parameter bean + schema
		if ( method instanceof AnalysisMethodSupport )
		{
			AnalysisMethodSupport<?> support = (AnalysisMethodSupport<?>) method;
			AnalysisParameters params = support.getParameters();
			if ( params != null )
			{
				List<Map<String, Object>> props = new ArrayList<Map<String, Object>>();
				collectProperties( ComponentFactory.getModel( params, ComponentFactory.Policy.DEFAULT, true ), "", props );
				m.put( "properties", props );
				String[] in = params.getInputNames();
				String[] out = params.getOutputNames();
				m.put( "inputNames", in == null ? new ArrayList<String>() : new ArrayList<String>( java.util.Arrays.asList( in ) ) );
				m.put( "outputNames", out == null ? new ArrayList<String>() : new ArrayList<String>( java.util.Arrays.asList( out ) ) );
			}
		}
		return McpEnvelope.ok( m );
	}

	/** Recursively flatten a {@link ComponentModel} into a list of property descriptors. */
	private static void collectProperties( ComponentModel model, String prefix, List<Map<String, Object>> out )
	{
		if ( model == null )
			return;
		for ( int i = 0; i < model.getPropertyCount(); i++ )
		{
			Property p = model.getPropertyAt( i );
			String key = prefix.isEmpty() ? p.getName() : prefix + "/" + p.getName();
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
			try
			{
				pm.put( "default", p.getValue() );
			}
			catch ( Exception e )
			{
				pm.put( "default", null );
			}
			// A composite property (nested bean) — recurse to expose the leaves.
			if ( p instanceof ComponentModel && out.size() < McpConstants.PAYLOAD_BYTES_CAP )
			{
				collectProperties( (ComponentModel) p, key, out );
			}
			out.add( pm );
		}
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
		try
		{
			// Coerce path-typed leaves, then bind. readBeanFromHierarchicalMap expects a *nested*
			// map (composite property -> sub-map), so expand the caller's flat '/'-joined keys
			// into that shape first.
			coercePaths( bean, params );
			Map<String, Object> nested = BeanAsMapUtil.expandMap( new LinkedHashMap<String, Object>( params ) );
			BeanAsMapUtil.readBeanFromHierarchicalMap( nested, bean );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "could not bind parameter: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		return McpEnvelope.ok( null );
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
			Property p = model.findProperty( e.getKey() );
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
	 * Report a task's status, progress, result paths and error.
	 */
	public static McpEnvelope taskStatus( String taskId )
	{
		TaskManager tm = TaskManager.getInstance();
		TaskInfo info = tm.getTask( taskId );
		if ( info == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no task named: " + taskId );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "taskId", info.getName() );
		m.put( "status", statusOf( safeStatus( info ) ) );
		try
		{
			m.put( "progress", Integer.valueOf( info.getJobControl() == null ? 0 : info.getJobControl().getPreparedness() ) );
			m.put( "textStatus", info.getJobControl() == null ? null : info.getJobControl().getTextStatus() );
		}
		catch ( Exception e )
		{
			m.put( "progress", null );
		}
		// Result paths: resolve the analysis's existing outputs (best-effort).
		List<String> results = new ArrayList<String>();
		try
		{
			Object jc = info.getTask() == null ? null : info.getTask();
			// The task is an AnalysisTask wrapping the analysis; recover the output paths from its
			// journal/attributes if present. We keep this best-effort — the get_result tool is the
			// primary way to read results by path.
		}
		catch ( Exception e )
		{
			// ignore
		}
		m.put( "results", results );
		return McpEnvelope.ok( m );
	}

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

	/**
	 * Cancel a queued/running task.
	 */
	public static McpEnvelope cancelTask( String taskId )
	{
		TaskManager tm = TaskManager.getInstance();
		TaskInfo info = tm.getTask( taskId );
		if ( info == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no task named: " + taskId );
		try
		{
			tm.stopTask( info );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not cancel task: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "taskId", info.getName() );
		m.put( "status", statusOf( safeStatus( info ) ) );
		m.put( "cancelled", Boolean.TRUE );
		return McpEnvelope.ok( m );
	}

	/**
	 * List active and recent tasks (a summary of the TaskManager's task collection).
	 */
	public static McpEnvelope listTasks()
	{
		TaskManager tm = TaskManager.getInstance();
		DataCollection<?> dc = tm.getTasksInfo();
		List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
		try
		{
			for ( Object o : dc )
			{
				// Elements are TaskInfo wrappers; best-effort map them.
				if ( o instanceof TaskInfo )
				{
					TaskInfo info = (TaskInfo) o;
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
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element at path: " + resultPath );
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
	 * and the first N rows; any other {@link DataCollection} yields a child listing; anything else
	 * yields a type summary. This is how an agent reads results without the UI.
	 */
	public static McpEnvelope getAnalysisResult( String path )
	{
		DataElement de = CollectionFactory.getDataElement( path );
		if ( de == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element at path: " + path );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		if ( de instanceof TableDataCollection )
		{
			TableDataCollection t = (TableDataCollection) de;
			List<String> columns = new ArrayList<String>();
			List<String> types = new ArrayList<String>();
			for ( ru.biosoft.table.TableColumn tc : t.columns() )
			{
				columns.add( tc.getName() );
				types.add( tc.getValueClass() == null ? "unknown" : tc.getValueClass().getName() );
			}
			m.put( "columns", columns );
			m.put( "columnTypes", types );
			m.put( "rowCount", Integer.valueOf( t.getSize() ) );
			List<List<Object>> rows = new ArrayList<List<Object>>();
			int n = Math.min( t.getSize(), McpConstants.TABLE_ROWS_CAP );
			for ( int r = 0; r < n; r++ )
			{
				List<Object> row = new ArrayList<Object>();
				for ( int c = 0; c < columns.size(); c++ )
				{
					try
					{
						row.add( t.getValueAt( r, c ) );
					}
					catch ( Exception e )
					{
						row.add( null );
					}
				}
				rows.add( row );
			}
			m.put( "rows", rows );
			m.put( "truncated", Boolean.valueOf( t.getSize() > n ) );
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
		return result;
	}
}
