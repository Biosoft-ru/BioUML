package biouml.plugins.mcp.support;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;

/**
 * Shared, test-friendly helpers for the repository MCP tools.
 *
 * <p>All methods take/return plain JSON-serializable values (Maps, Lists, Strings) and never
 * throw checked exceptions — failures are returned as {@link McpEnvelope} error envelopes so the
 * tool layer can convert them directly into MCP tool errors.</p>
 */
public final class McpRepositorySupport
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger( McpRepositorySupport.class.getName() );

	private McpRepositorySupport()
	{
	}

	/**
	 * Resolve an absolute repository path string to a {@link DataElement}.
	 * @return an envelope whose data is the element on success, or a not_found/path_escape error.
	 */
	public static McpEnvelope resolve( String path )
	{
		if ( path == null || path.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "path must be a non-empty string" );
		DataElementPath dep = DataElementPath.create( path );
		// Guard against traversal escaping the registered roots.
		DataElementPath first = dep;
		while ( !first.getParentPath().isEmpty() )
			first = first.getParentPath();
		String rootName = first.getName();
		if ( !CollectionFactory.getRootNames().contains( rootName ) )
			return McpEnvelope.error( McpConstants.CODE_PATH_ESCAPE,
					"path does not start with a registered repository root: " + rootName );
		DataElement de = CollectionFactory.getDataElement( path );
		if ( de == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no element at path: " + path );
		return McpEnvelope.ok( de );
	}

	/**
	 * Describe a single element as a flat, JSON-serializable map.
	 */
	public static McpEnvelope describe( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "name", de.getName() );
		m.put( "path", path );
		m.put( "class", de.getClass().getName() );
		boolean isCollection = de instanceof DataCollection;
		m.put( "isCollection", Boolean.valueOf( isCollection ) );
		if ( isCollection )
		{
			DataCollection<?> dc = (DataCollection<?>) de;
			m.put( "elementCount", Integer.valueOf( dc.getSize() ) );
			m.put( "elementType", dc.getDataElementType().getName() );
			m.put( "mutable", Boolean.valueOf( dc.isMutable() ) );
			List<String> names = dc.getNameList();
			int cap = McpConstants.LIST_CHILDREN_CAP;
			m.put( "childCount", Integer.valueOf( names.size() ) );
			m.put( "children", new ArrayList<String>( names.subList( 0, Math.min( names.size(), cap ) ) ) );
			m.put( "truncated", Boolean.valueOf( names.size() > cap ) );
		}
		return McpEnvelope.ok( m );
	}

	/**
	 * List the children of a collection as name/path/type summaries.
	 */
	public static McpEnvelope list( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		if ( !( de instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a collection: " + path );
		DataCollection<?> dc = (DataCollection<?>) de;
		List<String> names = dc.getNameList();
		List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
		int cap = McpConstants.LIST_CHILDREN_CAP;
		for ( int i = 0; i < names.size() && i < cap; i++ )
		{
			String name = names.get( i );
			DataElement child = null;
			try
			{
				child = dc.get( name );
			}
			catch ( Exception e )
			{
				// ignore; show name only
			}
			Map<String, Object> cm = new LinkedHashMap<String, Object>();
			cm.put( "name", name );
			cm.put( "path", DataElementPath.create( dc, name ).toString() );
			cm.put( "type", child == null ? "unknown" : child.getClass().getName() );
			cm.put( "isCollection", Boolean.valueOf( child instanceof DataCollection ) );
			children.add( cm );
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "path", path );
		result.put( "count", Integer.valueOf( names.size() ) );
		result.put( "returned", Integer.valueOf( children.size() ) );
		result.put( "truncated", Boolean.valueOf( names.size() > cap ) );
		result.put( "children", children );
		return McpEnvelope.ok( result );
	}

	/**
	 * Search element names (case-insensitive substring) under a scope path. Defaults to all
	 * registered roots when scope is null/empty. Returns matching full paths (capped).
	 */
	public static McpEnvelope search( String query, String scope )
	{
		if ( query == null || query.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "query must be a non-empty string" );
		String q = query.toLowerCase();
		List<String> matches = new ArrayList<String>();
		List<String> scopes = new ArrayList<String>();
		if ( scope != null && !scope.isEmpty() )
			scopes.add( scope );
		else
			for ( String root : CollectionFactory.getRootNames() )
				scopes.add( root );
		for ( String s : scopes )
		{
			McpEnvelope r = resolve( s );
			if ( !r.isOk() )
				continue;
			DataElement de = (DataElement) r.getData();
			if ( !( de instanceof DataCollection ) )
				continue;
			collectMatches( (DataCollection<?>) de, q, matches, 3 ); // depth limit to keep it bounded
			if ( matches.size() >= McpConstants.LIST_CHILDREN_CAP )
				break;
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "query", query );
		result.put( "count", Integer.valueOf( matches.size() ) );
		result.put( "truncated", Boolean.valueOf( matches.size() >= McpConstants.LIST_CHILDREN_CAP ) );
		result.put( "matches", matches );
		return McpEnvelope.ok( result );
	}

	/**
	 * Queue an async repository search as a task in the {@link TaskManager} and return its id. The
	 * search runs off the request thread, so it cannot exceed the MCP client's (e.g. the Anthropic
	 * proxy's ~60 s) per-request timeout; the caller polls {@link #searchStatus(String)} until the
	 * task completes.
	 *
	 * <p>Each {@code NetworkRepository} child access resolves the caller's permission, which is a
	 * database round-trip. On a large repository (or while the database is briefly unavailable) an
	 * unscoped search can therefore take many minutes, so it must not be served synchronously.</p>
	 *
	 * @param query  case-insensitive substring
	 * @param scope  scope path, or null/empty for all registered roots
	 * @return an envelope whose data is {@code {taskId, status:"queued"}} on success, or an error.
	 */
	public static McpEnvelope searchAsync( String query, String scope )
	{
		if ( query == null || query.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "query must be a non-empty string" );

		// The worker thread is bound to the calling session automatically: TaskPool captures the
		// session at submit time (on this request thread) and re-binds it before task.run(), so the
		// permission lookups inside search() resolve the caller's grants. The job-control lifecycle
		// (begin/end) is driven by doRun() below so the task status actually transitions
		// queued -> running -> done (addTask stores the JobControl as the task's status holder but
		// never runs it itself).
		ru.biosoft.jobcontrol.FunctionJobControl jc = new ru.biosoft.jobcontrol.FunctionJobControl( log )
		{
			@Override
			protected void doRun() throws ru.biosoft.jobcontrol.JobControlException
			{
				try
				{
					search( query, scope );
				}
				catch ( RuntimeException e )
				{
					throw new ru.biosoft.jobcontrol.JobControlException( e );
				}
			}
		};
		ru.biosoft.access.task.RunnableTask task = new ru.biosoft.access.task.RunnableTask(
				"repo-search:" + query,
				jc::run );
		ru.biosoft.tasks.TaskInfo info = ru.biosoft.tasks.TaskManager.getInstance().addTask(
				"repo-search",
				ru.biosoft.access.core.DataElementPath.create( "mcp/repo-search" ),
				jc, null, null, null, true, task );
		if ( info == null )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not queue the search task" );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "taskId", info.getName() );
		m.put( "status", "queued" );
		return McpEnvelope.ok( m );
	}

	/**
	 * Report the status of a queued repo-search task. Reads the task's {@link JobControl} status, so
	 * it works for tasks created in a previous request (the status is task-derived, not stored).
	 */
	public static McpEnvelope searchStatus( String taskId )
	{
		if ( taskId == null || taskId.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "taskId must be a non-empty string" );
		ru.biosoft.tasks.TaskInfo info = ru.biosoft.tasks.TaskManager.getInstance().getTask( taskId );
		if ( info == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no task named: " + taskId );
		int status = info.getJobControl() == null ? -1 : info.getJobControl().getStatus();
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "taskId", info.getName() );
		m.put( "status", taskStatusString( status ) );
		m.put( "elapsedMs", Long.valueOf( info.getJobControl() == null ? 0 : info.getJobControl().getElapsedTime() ) );
		return McpEnvelope.ok( m );
	}

	private static String taskStatusString( int status )
	{
		if ( status == ru.biosoft.jobcontrol.JobControl.COMPLETED )
			return "done";
		if ( status == ru.biosoft.jobcontrol.JobControl.TERMINATED_BY_REQUEST )
			return "cancelled";
		if ( status == ru.biosoft.jobcontrol.JobControl.TERMINATED_BY_ERROR )
			return "error";
		if ( status == ru.biosoft.jobcontrol.JobControl.PAUSED )
			return "paused";
		if ( status == ru.biosoft.jobcontrol.JobControl.RUNNING )
			return "running";
		if ( status == ru.biosoft.jobcontrol.JobControl.CREATED )
			return "queued";
		return "unknown";
	}

	private static void collectMatches( DataCollection<?> dc, String q, List<String> out, int depth )
	{
		if ( depth <= 0 || out.size() >= McpConstants.LIST_CHILDREN_CAP )
			return;
		List<String> names;
		try
		{
			names = dc.getNameList();
		}
		catch ( Exception e )
		{
			return;
		}
		for ( String name : names )
		{
			if ( out.size() >= McpConstants.LIST_CHILDREN_CAP )
				return;
			String full = DataElementPath.create( dc, name ).toString();
			if ( name.toLowerCase().contains( q ) )
				out.add( full );
			try
			{
				if ( dc.get( name ) instanceof DataCollection )
					collectMatches( (DataCollection<?>) dc.get( name ), q, out, depth - 1 );
			}
			catch ( Exception e )
			{
				// skip
			}
		}
	}

	/**
	 * Headlessly create a folder (sub-collection) inside a folder-collection parent. Mirrors
	 * {@code CreateFolderAction} without the input dialog.
	 * @param parentPath path to the parent (must be a folder collection)
	 * @param name name of the new folder
	 */
	public static McpEnvelope createFolder( String parentPath, String name )
	{
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "folder name must be a non-empty string" );
		McpEnvelope resolved = resolve( parentPath );
		if ( !resolved.isOk() )
			return resolved;
		DataElement parent = (DataElement) resolved.getData();
		if ( !( parent instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "parent is not a collection: " + parentPath );
		DataElementPath path = DataElementPath.create( (DataCollection<?>) parent, name );
		if ( path.exists() )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "an element already exists at: " + path );
		try
		{
			ru.biosoft.access.DataCollectionUtils.createSubCollection( path, false );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not create folder '" + name + "': " + e.getMessage() );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "created", path.toString() );
		return McpEnvelope.ok( m );
	}

	/**
	 * Remove an element by path. With {@code dryRun=true} reports what would be removed without
	 * touching the repository.
	 */
	public static McpEnvelope remove( String path, boolean dryRun )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElementPath dep = DataElementPath.create( path );
		if ( !dep.exists() )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "element does not exist: " + path );
		if ( dryRun )
		{
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "dryRun", Boolean.TRUE );
			m.put( "wouldRemove", path );
			return McpEnvelope.ok( m );
		}
		try
		{
			dep.remove();
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not remove '" + path + "': " + e.getMessage() );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "removed", path );
		return McpEnvelope.ok( m );
	}

	/**
	 * Copy a single element (a file, table, diagram, or any cloneable data element) to a new location —
	 * the headless equivalent of the web UI's "Save a copy" menu item (web provider {@code doc/save}).
	 * Mirrors {@code DocumentProvider.saveAs}: resolves the source, clones it into the target parent,
	 * and saves the clone under the target name. The source is left untouched.
	 *
	 * @param srcPath  full repository path of the element to copy
	 * @param destPath full repository path of the new copy (parent must exist and be writable)
	 * @return an envelope whose data is {@code {copied: destPath}} on success.
	 */
	public static McpEnvelope copyElement( String srcPath, String destPath )
	{
		McpEnvelope src = resolve( srcPath );
		if ( !src.isOk() )
			return src;
		DataElement srcDe = (DataElement) src.getData();

		// The destination's parent collection must exist and be writable (mirrors DocumentProvider.saveAs,
		// which resolves the target parent, not the not-yet-existing target leaf).
		DataElementPath destDep = DataElementPath.create( destPath );
		DataElement parentDe = CollectionFactory.getDataElement( destDep.getParentPath().toString() );
		if ( parentDe == null || !( parentDe instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND,
					"target parent does not exist: " + destDep.getParentPath() );
		DataCollection<?> parent = (DataCollection<?>) parentDe;
		if ( !parent.isMutable() )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "target collection is read-only: " + destPath );

		// The primary element is the concrete, cloneable one (a wrapper like a SymbolicLink resolves
		// to its target) — same as DocumentProvider.saveAs.
		DataElement de = ru.biosoft.access.DataCollectionUtils.fetchPrimaryElement( srcDe,
				ru.biosoft.access.security.Permission.READ );
		String name = destDep.getName();
		try
		{
			if ( de instanceof TableDataCollection )
			{
				de = ( (TableDataCollection) de ).clone( parent, name );
			}
			else if ( de instanceof CloneableDataElement )
			{
				de = ( (CloneableDataElement) de ).clone( parent, name );
			}
			else
			{
				return McpEnvelope.error( McpConstants.CODE_INTERNAL,
						"element is not copyable (not a cloneable data element): " + srcPath );
			}
			if ( de == null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "copy produced no result: " + srcPath );
			DataElementPath.create( parent, name ).save( de );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not copy '" + srcPath + "' to '" + destPath + "': " + e.getMessage() );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "copied", destPath );
		return McpEnvelope.ok( m );
	}

	/**
	 * Queue a folder (subtree) copy — the headless equivalent of the web UI's "Copy folder" menu item.
	 * Delegates to the platform's {@code folder} provider ({@code copy} action → {@code CopyFolderAnalysis}),
	 * which runs the copy as an async job. Returns a {@code jobID} immediately; poll {@link #jobStatus(String)}
	 * (the {@code jobcontrol} provider) until it completes, since copying a large folder can exceed a client's
	 * request timeout.
	 *
	 * @param fromPath full repository path of the folder to copy
	 * @param toPath   full repository path of the destination (parent must exist and be writable)
	 * @return an envelope whose data is {@code {jobID}} on success.
	 */
	public static McpEnvelope copyFolder( String fromPath, String toPath )
	{
		McpEnvelope from = resolve( fromPath );
		if ( !from.isOk() )
			return from;
		// The destination does not exist yet (we're copying TO it), so resolve its parent instead.
		DataElementPath toDep = DataElementPath.create( toPath );
		DataElementPath parentDep = toDep.getParentPath();
		if ( parentDep == null || parentDep.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "toPath must include a parent folder" );
		McpEnvelope toParent = resolve( parentDep.toString() );
		if ( !toParent.isOk() )
			return toParent;
		ru.biosoft.server.servlets.webservices.providers.WebProvider provider = McpProviderSupport.provider( "folder" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: folder" );
		String jobID = java.util.UUID.randomUUID().toString();
		// CopyFolderProvider reads a literal "path" key (not the "de" element-path key), so build a
		// raw request map and bypass buildRequest's path→de promotion.
		Map<String, String> request = new LinkedHashMap<String, String>();
		request.put( "action", "copy" );
		request.put( "path", fromPath );
		request.put( "newPath", toPath );
		request.put( "jobID", jobID );
		McpEnvelope env = McpProviderSupport.invokeRaw( provider, "folder", "copy", request );
		if ( !env.isOk() )
			return env;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "jobID", jobID );
		return McpEnvelope.ok( m );
	}

	/**
	 * Report the status of an async job started by a job-based provider (e.g. {@link #copyFolder}) —
	 * the headless equivalent of the web UI's progress dialog. Delegates to the {@code jobcontrol}
	 * provider, which returns {@code {status: <int>, percent: 0-100, values: [message], results: [...]}}.
	 * The response is re-shaped to {@code {jobID, status, progress, message, completed}}.
	 *
	 * @param jobID the job ID returned by the start action (e.g. {@link #copyFolder})
	 * @return an envelope whose data is the re-shaped status.
	 */
	public static McpEnvelope jobStatus( String jobID )
	{
		ru.biosoft.server.servlets.webservices.providers.WebProvider provider = McpProviderSupport.provider( "jobcontrol" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: jobcontrol" );
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "jobID", jobID );
		Map<String, Object> resp = McpProviderSupport.responseMap( (ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport) provider, "jobcontrol", null, params );
		if ( resp == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no job named: " + jobID );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "jobID", jobID );
		Object status = resp.get( JSONResponse.ATTR_STATUS );
		m.put( "status", status instanceof Number ? Integer.valueOf( ( (Number) status ).intValue() ) : null );
		Object percent = resp.get( JSONResponse.ATTR_PERCENT );
		m.put( "progress", percent instanceof Number ? Integer.valueOf( ( (Number) percent ).intValue() ) : null );
		Object values = resp.get( "values" );
		Object message = ( values instanceof java.util.List && ! ( (java.util.List<?>) values ).isEmpty() )
				? ( (java.util.List<?>) values ).get( 0 ) : null;
		m.put( "message", message );
		m.put( "completed", status instanceof Number && ( (Number) status ).intValue() == ru.biosoft.jobcontrol.JobControl.COMPLETED );
		return McpEnvelope.ok( m );
	}

	/**
	 * Fetch the current session's server log tail — the headless equivalent of the web UI's "Logs"
	 * view pane. Delegates to the {@code log} provider ({@code get} action), which returns the
	 * session's in-memory {@code java.util.logging} capture (a bounded, SEVERE-by-default buffer of
	 * everything the JVM logged under this session — not a single job's log, but cross-job server
	 * output such as unhandled exceptions). The provider sends the text via {@code sendString}, so it
	 * arrives under the {@code values} field of the response.
	 *
	 * <p>This is a diagnostic tool: the log is only populated if the experimental logging feature was
	 * enabled at server boot ({@code WebLogProvider.initWebLogger}); otherwise the provider returns an
	 * error ("Log is disabled"), which is surfaced as a non-ok envelope.</p>
	 *
	 * @return an envelope whose data is {@code {log}} (the accumulated text) on success.
	 */
	public static McpEnvelope getLog()
	{
		ru.biosoft.server.servlets.webservices.providers.WebProvider provider = McpProviderSupport.provider( "log" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: log" );
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		Map<String, Object> resp = McpProviderSupport.responseMap( (ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport) provider, "log", "get", params );
		if ( resp == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "log is unavailable for this session (the experimental logging feature may not be enabled)" );
		Object values = resp.get( JSONResponse.ATTR_VALUES );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "log", values );
		return McpEnvelope.ok( m );
	}

	/**
	 * Run a script inline (any type the script provider supports: JS, R, Java, ...) as an async job —
	 * the headless equivalent of the web UI's "Run script". Delegates to the {@code script} provider
	 * ({@code runInline} action). Returns a {@code jobID} immediately; poll {@link #jobStatus(String)}
	 * until it completes, then fetch the output with {@link #scriptResult(String)}.
	 *
	 * @param script the script source text
	 * @param type   the script type (one of {@link #scriptTypes})
	 * @return an envelope whose data is {@code {jobID}} on success.
	 */
	public static McpEnvelope runScript( String script, String type )
	{
		if ( script == null || script.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "script must be a non-empty string" );
		if ( type == null || type.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "type must be a non-empty string" );
		ru.biosoft.server.servlets.webservices.providers.WebProvider provider = McpProviderSupport.provider( "script" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: script" );
		String jobID = java.util.UUID.randomUUID().toString();
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "script", script );
		params.put( "type", type );
		params.put( "jobID", jobID );
		McpEnvelope env = McpProviderSupport.invoke( provider, "script", "runInline", params );
		if ( !env.isOk() )
			return env;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "jobID", jobID );
		return McpEnvelope.ok( m );
	}

	/**
	 * Fetch the output of a script job started by {@link #runScript} — the headless equivalent of the
	 * web UI's script result pane. Delegates to the {@code script} provider ({@code environment} action),
	 * which returns the job's printed buffer, tables, images, and HTML.
	 *
	 * @param jobID the job ID returned by {@link #runScript}
	 * @return an envelope whose data is the script output structure.
	 */
	public static McpEnvelope scriptResult( String jobID )
	{
		ru.biosoft.server.servlets.webservices.providers.WebProvider provider = McpProviderSupport.provider( "script" );
		if ( provider == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: script" );
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put( "jobID", jobID );
		return McpProviderSupport.invoke( provider, "script", "environment", params );
	}

	/**
	 * List the top-level registered collections (roots).
	 */
	public static McpEnvelope collections()
	{
		Collection<String> roots = CollectionFactory.getRootNames();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for ( String name : roots )
		{
			DataElement de = CollectionFactory.getDataElement( name );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "name", name );
			m.put( "path", name );
			m.put( "isCollection", Boolean.valueOf( de instanceof DataCollection ) );
			if ( de instanceof DataCollection )
				m.put( "size", Integer.valueOf( ((DataCollection<?>) de).getSize() ) );
			list.add( m );
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "count", Integer.valueOf( list.size() ) );
		result.put( "collections", list );
		return McpEnvelope.ok( result );
	}

	/**
	 * Re-initialize a collection that previously failed to load — the headless equivalent of the web
	 * UI's "Retry"/"Reinitialize" menu item ({@code ReinitializeAction}), whose perform path is just
	 * {@code ((DataCollection)de).reinitialize()} with no UI. Useful after a transient failure (e.g. a
	 * missing dependency at boot) to force a reload.
	 */
	public static McpEnvelope reinitialize( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		if ( !( de instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a collection: " + path );
		try
		{
			( (DataCollection<?>) de ).reinitialize();
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not reinitialize '" + path + "': " + e.getMessage() );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "reinitialized", path );
		m.put( "valid", Boolean.valueOf( ( (DataCollection<?>) de ).isValid() ) );
		return McpEnvelope.ok( m );
	}

	/**
	 * List the export formats available for a repository element (in accept-priority order), so a
	 * caller can discover what {@link #export(String, String, String)} can produce for it.
	 */
	public static McpEnvelope exportFormats( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		List<String> formats = ru.biosoft.access.DataElementExporterRegistry.getExporterFormats( de );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "count", Integer.valueOf( formats.size() ) );
		m.put( "formats", formats );
		return McpEnvelope.ok( m );
	}

	/**
	 * Read the text content of a document element (script, notebook, text file, ...). Delegates to
	 * the platform's {@code doc} provider ({@code getcontent} action).
	 * @param path full repository path of the text element
	 * @return an envelope whose data is {@code {path, content}}.
	 */
	public static McpEnvelope documentContent( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		McpEnvelope env = McpProviderSupport.invoke( "doc", "getcontent", oneDe( path ) );
		if ( !env.isOk() )
			return env;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "content", env.getData() == null ? "" : String.valueOf( env.getData() ) );
		return McpEnvelope.ok( m );
	}

	/**
	 * Write the text content of a document element. Delegates to the {@code doc} provider
	 * ({@code savecontent} action).
	 * @param path    full repository path of the text element
	 * @param content the new content
	 * @return an envelope whose data is {@code {path, saved: true}}.
	 */
	public static McpEnvelope saveDocumentContent( String path, String content )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		Map<String, Object> params = oneDe( path );
		params.put( "newPath", path );
		params.put( "content", content == null ? "" : content );
		McpEnvelope env = McpProviderSupport.invoke( "doc", "savecontent", params );
		if ( !env.isOk() )
			return env;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "saved", Boolean.TRUE );
		return McpEnvelope.ok( m );
	}

	/** A parameter map with a single element-path entry ({@code de}). */
	private static Map<String, Object> oneDe( String path )
	{
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put( McpProviderSupport.KEY_DE, path );
		return p;
	}

	/**
	 * Detect the omics type (Transcriptomics, Genomics, ...) of a data element. Delegates to the
	 * platform's {@code omicsType} provider ({@code detect} action).
	 * @param path full repository path of the element
	 * @return an envelope whose data is {@code {path, omicsType}} (omicsType may be null/empty).
	 */
	public static McpEnvelope detectOmicsType( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		McpEnvelope env = McpProviderSupport.invoke( "omicsType", "detect", oneDe( path ) );
		if ( !env.isOk() )
			return env;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "omicsType", env.getData() == null ? null : String.valueOf( env.getData() ) );
		return McpEnvelope.ok( m );
	}

	/**
	 * Set the omics type of a data element. Delegates to the {@code omicsType} provider ({@code set}
	 * action).
	 * @param path       full repository path of the element
	 * @param omicsType  the omics type name (e.g. "Transcriptomics", "Genomics")
	 * @return an envelope whose data is {@code {path, omicsType, saved: true}}.
	 */
	public static McpEnvelope setOmicsType( String path, String omicsType )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		Map<String, Object> params = oneDe( path );
		params.put( "omicsType", omicsType );
		McpEnvelope env = McpProviderSupport.invoke( "omicsType", "set", params );
		if ( !env.isOk() )
			return env;
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "omicsType", omicsType );
		m.put( "saved", Boolean.TRUE );
		return McpEnvelope.ok( m );
	}

	/**
	 * Report whether a collection has git version-control enabled (and git is available). Delegates to
	 * the platform's {@code git} provider ({@code isEnabled} action).
	 * @param path full repository path of the collection
	 * @return an envelope whose data is {@code {path, enabled: bool}}.
	 */
	public static McpEnvelope gitEnabled( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		McpEnvelope env = McpProviderSupport.invoke( "git", "isEnabled", oneDe( path ) );
		if ( !env.isOk() )
			return env;
		String raw = env.getData() == null ? "" : String.valueOf( env.getData() );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "path", path );
		m.put( "enabled", "ok".equalsIgnoreCase( raw ) );
		return McpEnvelope.ok( m );
	}

	/**
	 * Read the current session's preferences. Delegates to the platform's {@code preferences} provider
	 * (no action — it returns the preferences bean structure).
	 * @return an envelope whose data is the preferences structure.
	 */
	public static McpEnvelope preferences()
	{
		// The provider only special-cases action "add"; any other action returns the structure.
		return McpProviderSupport.invoke( "preferences", "get", new LinkedHashMap<String, Object>() );
	}

	/**
	 * Export a repository element to a file on the server's local filesystem, in the given format —
	 * the headless equivalent of the web UI's "Export" menu item ({@code ExportElementAction}, which
	 * only wraps a file-chooser dialog around this same registry call). Mirrors the web
	 * {@code ExportProvider}: resolve the element, pick the exporter for the format, and run
	 * {@code doExport}.
	 *
	 * @param path       full repository path of the element to export
	 * @param format     the export format (one of {@link #exportFormats}); may be null/empty to use
	 *                   the highest-priority available format
	 * @param targetDir  server-local directory for the output file (null/empty = current dir); the
	 *                   file name is the element name + the format's suffix
	 * @return an envelope whose data is {@code {file, format, bytes}} on success.
	 */
	public static McpEnvelope export( String path, String format, String targetDir )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		try
		{
			List<String> formats = ru.biosoft.access.DataElementExporterRegistry.getExporterFormats( de );
			if ( formats.isEmpty() )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "no export format available for: " + path );
			String fmt = ( format == null || format.isEmpty() ) ? formats.get( 0 ) : format;
			ru.biosoft.access.DataElementExporterRegistry.ExporterInfo[] infos =
					ru.biosoft.access.DataElementExporterRegistry.getExporterInfo( fmt, de );
			if ( infos == null || infos.length == 0 )
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
						"format not supported for this element: " + format + " (available: " + formats + ")" );
			ru.biosoft.access.DataElementExporterRegistry.ExporterInfo info = infos[ 0 ];
			String suffix = info.getSuffix();
			if ( suffix.indexOf( '.' ) == -1 )
				suffix = "." + suffix;
			File dir = ( targetDir == null || targetDir.isEmpty() ) ? new File( "." ) : new File( targetDir );
			if ( !dir.isDirectory() && !dir.mkdirs() )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "target directory does not exist and could not be created: " + dir );
			File file = new File( dir, de.getName() + suffix );
			ru.biosoft.access.DataElementExporter exporter = info.cloneExporter();
			exporter.doExport( de, file );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "file", file.getAbsolutePath() );
			m.put( "format", info.getFormat() );
			m.put( "bytes", Long.valueOf( file.length() ) );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"export failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Import a file on the server's local filesystem into a target collection — the headless equivalent
	 * of the web UI's "Import document" menu item ({@code ImportElementAction}, which only wraps a
	 * file-chooser + format dropdown around this same importer-registry call). Mirrors the web
	 * {@code ImportElementDialog} minus all UI: resolve the parent, pick the importer for the format
	 * (or autodetect), and run {@code doImport}.
	 *
	 * <p>Unlike the dialog, there is no property-input sub-dialog and no post-import "open in GUI".
	 * If the chosen importer requires property input (its {@code getProperties} returns an
	 * {@code Option} bean, which the dialog would present as a second modal), this returns a
	 * structured {@code invalid_params} error naming the importer rather than blocking on a dialog —
	 * most importers (DML, etc.) do not require property input, so this is usually a no-op.</p>
	 *
	 * @param parentPath full repository path of the target collection (must be a mutable
	 *                   {@code DataCollection} with at least one importer available for it)
	 * @param file       server-local path to the file to import
	 * @param format     the import format (one of {@link #importFormats(String)}); may be null/empty
	 *                   to autodetect. Autodetect that matches more than one importer is a clean
	 *                   error listing the candidates (mirrors the dialog's ambiguity handling).
	 * @param name       name for the imported element (null/empty = the file name without extension)
	 * @return an envelope whose data is {@code {imported, format}} on success.
	 */
	public static McpEnvelope importElement( String parentPath, String file, String format, String name )
	{
		McpEnvelope pe = resolve( parentPath );
		if ( !pe.isOk() )
			return pe;
		Object parentObj = pe.getData();
		if ( !( parentObj instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "target is not a collection: " + parentPath );
		@SuppressWarnings( "unchecked" )
		DataCollection<?> parent = (DataCollection<?>) parentObj;
		if ( !parent.isMutable() )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "target collection is read-only: " + parentPath );

		File f = new File( file == null ? "" : file );
		if ( !f.exists() )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no file at: " + file );

		try
		{
			// Resolve the format: autodetect (highest-priority importer, or a clean error if
			// ambiguous) or an explicit format that has a registered importer.
			String fmt;
			if ( format == null || format.isEmpty() || ru.biosoft.access.DataElementImporterRegistry.AUTODETECT.equals( format ) )
			{
				ru.biosoft.access.DataElementImporterRegistry.ImporterInfo[] infos =
						ru.biosoft.access.DataElementImporterRegistry.getAutoDetectImporter( f, parent, false );
				if ( infos == null || infos.length == 0 )
					return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
							"no importer available for '" + file + "' into " + parentPath );
				if ( infos.length > 1 )
				{
					List<String> candidates = new ArrayList<String>();
					for ( ru.biosoft.access.DataElementImporterRegistry.ImporterInfo info : infos )
						candidates.add( info.getFormat() );
					return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
							"more than one importer matches '" + file + "'; specify format explicitly: " + candidates );
				}
				fmt = infos[ 0 ].getFormat();
			}
			else
			{
				if ( ru.biosoft.access.DataElementImporterRegistry.getImporter( f, format, parent ) == null )
					return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS,
							"no importer for format '" + format + "' into " + parentPath );
				fmt = format;
			}

			// Stage the local file into the session's upload store so the import provider's
			// getUploadedFile(fileID) can find it (the provider is designed for multipart web uploads).
			String staged = stageUploadFile( f );
			if ( staged == null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "could not stage file for import: " + file );
			String fileID = staged;

			ru.biosoft.server.servlets.webservices.providers.WebProvider provider = McpProviderSupport.provider( "import" );
			if ( provider == null )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no provider registered for prefix: import" );
			String jobID = java.util.UUID.randomUUID().toString();
			// The import provider reads a literal "de" for the target collection; build a raw request.
			Map<String, String> request = new LinkedHashMap<String, String>();
			request.put( "action", "import" );
			request.put( "de", parentPath );
			request.put( "type", "import" );
			request.put( "format", fmt );
			request.put( "fileID", fileID );
			request.put( "jobID", jobID );
			McpEnvelope env = McpProviderSupport.invokeRaw( provider, "import", "import", request );
			if ( !env.isOk() )
				return env;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "jobID", jobID );
			m.put( "format", fmt );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"import failed: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Stage a local file into the session's upload store so {@code WebServicesServlet.getUploadedFile}
	 * can find it. Copies the file to {@code UPLOAD_DIRECTORY/upload_<sessionID>_<fileID><suffix>} and
	 * records the original name + suffix in the current {@code WebSession}.
	 * @return the generated {@code fileID} on success, or {@code null} on failure.
	 */
	private static String stageUploadFile( File f )
	{
		try
		{
			ru.biosoft.server.servlets.webservices.WebSession session =
					ru.biosoft.server.servlets.webservices.WebSession.getCurrentSession();
			if ( session == null )
				return null;
			String sessionID = session.getSessionId();
			String fileID = java.util.UUID.randomUUID().toString().replaceAll( "\\W", "" );
			String suffix = f.getName().contains( "." ) ? "." + f.getName().substring( f.getName().lastIndexOf( '.' ) + 1 ) : "";
			File destination = new File( ru.biosoft.server.servlets.webservices.WebServicesServlet.UPLOAD_DIRECTORY,
					"upload_" + sessionID + "_" + fileID + suffix );
			copyFile( f, destination );
			session.putValue( "uploadedFile_" + fileID, f.getName() );
			session.putValue( "uploadedFileSuffix_" + fileID, suffix );
			return fileID;
		}
		catch ( Exception e )
		{
			log.log( java.util.logging.Level.WARNING, "Could not stage upload file: " + f, e );
			return null;
		}
	}

	private static void copyFile( File src, File dst ) throws Exception
	{
		try ( java.io.InputStream in = new java.io.FileInputStream( src );
				java.io.OutputStream out = new java.io.FileOutputStream( dst ) )
		{
			byte[] buf = new byte[ 8192 ];
			int n;
			while ( ( n = in.read( buf ) ) != -1 )
				out.write( buf, 0, n );
		}
	}

	/**
	 * List the import formats available for a target collection (in accept-priority order), so a
	 * caller can discover what {@link #importElement(String, String, String, String)} can read into
	 * it. The first entry is always {@code autodetect}.
	 */
	public static McpEnvelope importFormats( String parentPath )
	{
		McpEnvelope pe = resolve( parentPath );
		if ( !pe.isOk() )
			return pe;
		Object parentObj = pe.getData();
		if ( !( parentObj instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "target is not a collection: " + parentPath );
		@SuppressWarnings( "unchecked" )
		DataCollection<?> parent = (DataCollection<?>) parentObj;
		try
		{
			List<String> formats = new ArrayList<String>();
			formats.add( ru.biosoft.access.DataElementImporterRegistry.AUTODETECT );
			for ( ru.biosoft.access.DataElementImporterRegistry.ImporterInfo info : ru.biosoft.access.DataElementImporterRegistry.importers() )
			{
				try
				{
					if ( info.getImporter().accept( parent, null ) > ru.biosoft.access.core.DataElementImporter.ACCEPT_UNSUPPORTED )
						formats.add( info.getFormat() );
				}
				catch ( Exception e )
				{
					// A throwing importer must not break enumeration of the rest.
				}
			}
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "path", parentPath );
			m.put( "count", Integer.valueOf( formats.size() ) );
			m.put( "formats", formats );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not list import formats: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * List the script types that can be created in a target collection — one row per
	 * {@code ru.biosoft.access.scriptType} extension whose product is available on this server. The
	 * {@code type} values are what {@link #createScript(String, String, String, String)} takes. This
	 * is the headless equivalent of the "New JS script" / "New R script" / "New Java code" menu items
	 * (each of which just prompts for a name and creates the matching script element).
	 */
	public static McpEnvelope scriptTypes()
	{
		// Delegates to the platform's `script` provider (`types` action), which returns a
		// {<type-id>: <type-title>} object.
		McpEnvelope env = McpProviderSupport.invoke( "script", "types", new LinkedHashMap<String, Object>() );
		if ( !env.isOk() )
			return env;
		@SuppressWarnings( "unchecked" )
		Map<String, Object> types = (Map<String, Object>) env.getData();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		if ( types != null )
		{
			for ( Map.Entry<String, Object> e : types.entrySet() )
			{
				Map<String, Object> m = new LinkedHashMap<String, Object>();
				m.put( "type", e.getKey() );
				m.put( "title", e.getValue() == null ? null : String.valueOf( e.getValue() ) );
				list.add( m );
			}
		}
		Collections.sort( list, ( a, b ) -> String.valueOf( a.get( "title" ) ).compareTo( String.valueOf( b.get( "title" ) ) ) );
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put( "count", Integer.valueOf( list.size() ) );
		result.put( "types", list );
		return McpEnvelope.ok( result );
	}

	/**
	 * Create a new script element (JS, R, Java, ...) in a target collection — the headless equivalent
	 * of the web UI's "New JS script" / "New R script" / "New Java code" menu items
	 * ({@code AddJSAction} / {@code AddRScriptAction} / {@code AddJavaElementAction}), each of which
	 * only prompts for a name and then creates the matching element via the same
	 * {@link ru.biosoft.access.script.ScriptTypeRegistry} this uses. Mirrors the web
	 * {@code WebScriptsProvider.createScript}: build the element for the type, then save it.
	 *
	 * @param parentPath full repository path of the target collection (must be a mutable
	 *                   {@code DataCollection})
	 * @param type       the script type (one of {@link #scriptTypes()}); must be non-empty
	 * @param name       name for the new element (must be non-empty; must not already exist)
	 * @param content    initial script content (may be null/empty for a blank script)
	 * @return an envelope whose data is {@code {created, type}} on success.
	 */
	public static McpEnvelope createScript( String parentPath, String type, String name, String content )
	{
		if ( type == null || type.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "script type must be a non-empty string" );
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "script name must be a non-empty string" );
		McpEnvelope resolved = resolve( parentPath );
		if ( !resolved.isOk() )
			return resolved;
		DataElement parent = (DataElement) resolved.getData();
		if ( !( parent instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "parent is not a collection: " + parentPath );
		DataCollection<?> dc = (DataCollection<?>) parent;
		if ( !dc.isMutable() )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "target collection is read-only: " + parentPath );
		DataElementPath path = DataElementPath.create( dc, name );
		if ( path.exists() )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "an element already exists at: " + path );
		try
		{
			ru.biosoft.access.script.ScriptDataElement scriptElement =
					ru.biosoft.access.script.ScriptTypeRegistry.createScript( type, path, content == null ? "" : content );
			if ( scriptElement == null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "script type produced no element: " + type );
			path.save( scriptElement );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "created", path.toString() );
			m.put( "type", type );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not create script '" + name + "': " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * The kinds of element the {@link #newElement(String, String, String, String)} tool can create,
	 * keyed by the {@code kind} string. Every one of these is the headless equivalent of a "New X"
	 * menu item in the web UI (table, test, workflow, research, Jupyter notebook, or any script type
	 * from {@link #scriptTypes()}).
	 */
	private static final String KIND_TABLE = "table";
	private static final String KIND_TEST = "test";
	private static final String KIND_WORKFLOW = "workflow";
	private static final String KIND_RESEARCH = "research";
	private static final String KIND_NOTEBOOK = "notebook";

	/**
	 * Create a new element in a target collection — the headless equivalent of the web UI's "New X"
	 * menu items ("New table", "New test", "New workflow", "New research", "New Jupyter file", and
	 * "New JS script"/"New R script"/"New Java code"/"New nextflow script"/"New WDL script"/"New math
	 * script"). Each of those actions only prompts for a name and then creates the matching element;
	 * this takes the name (and optional content) directly.
	 *
	 * @param parentPath full repository path of the target collection (must be a mutable
	 *                   {@code DataCollection}; a "workflow"/"research" element additionally requires
	 *                   the parent to be inside a research module)
	 * @param kind       one of {@code table}, {@code test}, {@code workflow}, {@code research},
	 *                   {@code notebook}, or a script {@code type} from {@link #scriptTypes()}
	 *                   (e.g. {@code js}, {@code R}, {@code Java}, {@code Nextflow}, {@code WDL},
	 *                   {@code math})
	 * @param name       name for the new element (must be non-empty; must not already exist)
	 * @param content    initial content for script/notebook kinds (may be null/empty); ignored for
	 *                   table/test/workflow/research kinds
	 * @return an envelope whose data is {@code {created, kind}} on success.
	 */
	public static McpEnvelope newElement( String parentPath, String kind, String name, String content )
	{
		if ( kind == null || kind.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "kind must be a non-empty string" );
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "name must be a non-empty string" );
		McpEnvelope resolved = resolve( parentPath );
		if ( !resolved.isOk() )
			return resolved;
		DataElement parent = (DataElement) resolved.getData();
		if ( !( parent instanceof DataCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "parent is not a collection: " + parentPath );
		DataCollection<?> dc = (DataCollection<?>) parent;
		if ( !dc.isMutable() )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "target collection is read-only: " + parentPath );
		DataElementPath path = DataElementPath.create( dc, name );
		if ( path.exists() )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "an element already exists at: " + path );
		try
		{
			DataElement created;
			if ( KIND_TABLE.equals( kind ) )
			{
				created = newTable( dc, name );
			}
			else if ( KIND_TEST.equals( kind ) )
			{
				created = newTest( dc, name );
			}
			else if ( KIND_WORKFLOW.equals( kind ) )
			{
				created = newWorkflow( dc, name );
			}
			else if ( KIND_RESEARCH.equals( kind ) )
			{
				created = newResearch( dc, name );
			}
			else if ( KIND_NOTEBOOK.equals( kind ) )
			{
				created = newNotebook( dc, name, content );
			}
			else
			{
				// Treat the kind as a script type (js, R, Java, Nextflow, WDL, math, ...).
				created = ru.biosoft.access.script.ScriptTypeRegistry.createScript( kind, path, content == null ? "" : content );
			}
			if ( created == null )
				return McpEnvelope.error( McpConstants.CODE_INTERNAL, "kind '" + kind + "' produced no element" );
			DataElementPath.create( created ).save( created );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "created", path.toString() );
			m.put( "kind", kind );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not create " + kind + " element '" + name + "': " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/** Headless "New table": {@code TableDataCollectionUtils.createTableDataCollection}. */
	private static DataElement newTable( DataCollection<?> parent, String name )
	{
		return ru.biosoft.table.TableDataCollectionUtils.createTableDataCollection( parent, name );
	}

	/** Headless "New test": a {@code TestModel} with the default (empty) options. */
	@SuppressWarnings( "unchecked" )
	private static DataElement newTest( DataCollection<?> parent, String name )
	{
		biouml.plugins.test.access.NewTestDocumentAction.TestOptions options =
				new biouml.plugins.test.access.NewTestDocumentAction.TestOptions( name, null );
		return new biouml.plugins.test.TestModel( (DataCollection<?>) parent, name, options.getPath() );
	}

	/** Headless "New workflow": a {@code WorkflowDiagramType} diagram. */
	private static DataElement newWorkflow( DataCollection<?> parent, String name ) throws Exception
	{
		return new biouml.plugins.research.workflow.WorkflowDiagramType().createDiagram( parent, name, null );
	}

	/** Headless "New research": a {@code ResearchDiagramType} diagram. */
	private static DataElement newResearch( DataCollection<?> parent, String name ) throws Exception
	{
		return new biouml.plugins.research.research.ResearchDiagramType().createDiagram( parent, name, null );
	}

	/**
	 * Headless "New Jupyter file": an {@code IPythonElement} with an empty notebook for the kernel.
	 * Mirrors the web {@code JupyterProvider} create action ({@code constructEmptyFile} +
	 * {@code new IPythonElement(name, parent, content, file)}).
	 */
	private static DataElement newNotebook( DataCollection<?> parent, String name, String content )
	{
		String json = ( content == null || content.isEmpty() ) ? emptyNotebook( "python3" ) : content;
		File file = ru.biosoft.access.DataCollectionUtils.getChildFile( parent, name );
		return new biouml.plugins.jupyter.access.IPythonElement( name, parent, json, file.getAbsolutePath() );
	}

	/** A minimal empty Jupyter notebook (nbformat 4) for the given kernel (python3 by default). */
	private static String emptyNotebook( String kernel )
	{
		StringBuilder sb = new StringBuilder( "{\"cells\":[{\"cell_type\":\"code\",\"execution_count\":null,\"metadata\":{},\"outputs\":[],\"source\":[]}]," );
		switch ( kernel )
		{
			case "r":
				sb.append( "\"metadata\":{\"kernelspec\":{\"display_name\":\"R\",\"language\":\"R\",\"name\":\"ir\"},\"language_info\":{\"codemirror_mode\":\"r\",\"file_extension\":\".r\",\"mimetype\":\"text/x-r-source\",\"name\":\"R\",\"pygments_lexer\":\"r\",\"version\":\"3.6.1\"}}," );
				break;
			case "python3":
			default:
				sb.append( "\"metadata\":{\"kernelspec\":{\"display_name\":\"Python 3\",\"language\":\"python\",\"name\":\"python3\"},\"language_info\":{\"codemirror_mode\":{\"name\":\"ipython\",\"version\":3},\"name\":\"python\"}}," );
				break;
		}
		sb.append( "\"nbformat\":4,\"nbformat_minor\":2}" );
		return sb.toString();
	}

	/**
	 * Change the element type of a file inside a {@link ru.biosoft.fs.FileSystemCollection} — the
	 * headless equivalent of the web UI's "Change element type" menu item
	 * ({@code ChangeElementTypeAction}, which only wraps {@code FileSystemCollection.setElementType}
	 * in a properties dialog to pick the type). The available types are listed by
	 * {@link #filesystemElementTypes(String)}.
	 *
	 * @param path   full repository path of the element (inside a file-system collection)
	 * @param newType the target element type (one of {@link #filesystemElementTypes})
	 * @return an envelope whose data is {@code {changed, path, type}} on success.
	 */
	public static McpEnvelope setFilesystemType( String path, String newType )
	{
		if ( newType == null || newType.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "type must be a non-empty string" );
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		Object origin = de.getOrigin();
		if ( !( origin instanceof ru.biosoft.fs.FileSystemCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not in a file-system collection: " + path );
		ru.biosoft.fs.FileSystemCollection fsc = (ru.biosoft.fs.FileSystemCollection) origin;
		try
		{
			fsc.setElementType( de.getName(), newType );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "changed", path );
			m.put( "type", newType );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not change element type: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * List the element types available for a file inside a {@link ru.biosoft.fs.FileSystemCollection}
	 * (what {@link #setFilesystemType} can change it to).
	 */
	public static McpEnvelope filesystemElementTypes( String path )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		Object origin = de.getOrigin();
		if ( !( origin instanceof ru.biosoft.fs.FileSystemCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not in a file-system collection: " + path );
		try
		{
			List<String> types = ru.biosoft.fs.FileSystemCollection.getAvailableTypes( de ).sorted().toList();
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "path", path );
			m.put( "count", Integer.valueOf( types.size() ) );
			m.put( "types", types );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not list element types: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Log into a credentials-protected collection — the headless equivalent of the web UI's "Login"
	 * menu item ({@code LoginModuleAction}, which only wraps {@code CredentialsCollection
	 * .processCredentialsBean} in a credentials dialog). The collection's credentials bean is obtained
	 * via {@code getCredentialsBean()} and filled from the supplied fields (matched case-
	 * insensitively against the bean's bean-property names, e.g. {@code user}/{@code password});
	 * {@code processCredentialsBean} is then invoked, retrying once on a login failure.
	 *
	 * @param path   full repository path of the collection (must implement {@code CredentialsCollection}
	 *               and report {@code needCredentials()})
	 * @param fields a flat map of credential field name -> value (e.g. {"user":"...","password":"..."})
	 * @return an envelope whose data is {@code {loggedIn, path}} on success.
	 */
	public static McpEnvelope login( String path, Map<String, String> fields )
	{
		McpEnvelope resolved = resolve( path );
		if ( !resolved.isOk() )
			return resolved;
		DataElement de = (DataElement) resolved.getData();
		if ( !( de instanceof ru.biosoft.access.security.CredentialsCollection ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "element is not a credentials collection: " + path );
		ru.biosoft.access.security.CredentialsCollection cc = (ru.biosoft.access.security.CredentialsCollection) de;
		if ( !cc.needCredentials() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "collection does not need credentials: " + path );
		Object bean = cc.getCredentialsBean();
		if ( bean == null )
			return McpEnvelope.error( McpConstants.CODE_INTERNAL, "no credentials bean available for: " + path );
		try
		{
			fillCredentialsBean( bean, fields );
			cc.processCredentialsBean( bean );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "loggedIn", path );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"login failed for '" + path + "': " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Fill a credentials bean's bean-properties from the supplied field map, matching property names
	 * case-insensitively (so "user" fills a "user" or "User" property). Unknown field names are
	 * ignored. Uses the bean's public set-property methods (the same mechanism the web
	 * {@code PropertiesDialog} uses to populate the bean).
	 */
	private static void fillCredentialsBean( Object bean, Map<String, String> fields ) throws Exception
	{
		if ( fields == null || fields.isEmpty() )
			return;
		java.beans.BeanInfo info = java.beans.Introspector.getBeanInfo( bean.getClass() );
		for ( java.beans.PropertyDescriptor pd : info.getPropertyDescriptors() )
		{
			if ( pd.getWriteMethod() == null || "class".equals( pd.getName() ) )
				continue;
			for ( Map.Entry<String, String> e : fields.entrySet() )
			{
				if ( pd.getName().equalsIgnoreCase( e.getKey() ) )
				{
					Object value = e.getValue();
					Class<?> paramType = pd.getWriteMethod().getParameterTypes()[ 0 ];
					pd.getWriteMethod().invoke( bean, convertForBean( value, paramType ) );
				}
			}
		}
	}

	/** Convert a string field value to the target bean-property type where trivially possible. */
	private static Object convertForBean( Object value, Class<?> target )
	{
		if ( value == null )
			return null;
		if ( target.isInstance( value ) )
			return value;
		if ( target == String.class )
			return String.valueOf( value );
		if ( target == Integer.class || target == int.class )
			return Integer.valueOf( String.valueOf( value ) );
		if ( target == Boolean.class || target == boolean.class )
			return Boolean.valueOf( String.valueOf( value ) );
		return value;
	}
}
