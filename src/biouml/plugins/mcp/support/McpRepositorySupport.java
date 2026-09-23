package biouml.plugins.mcp.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.McpConstants;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
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
	 * Queue a folder (subtree) copy as an async analysis task — the headless equivalent of the web UI's
	 * "Copy folder" menu item (web provider {@code folder/copy} → {@code CopyFolderAnalysis}). Returns a
	 * taskId immediately; poll {@link #taskStatus(String)} (the generic analysis task-status tool) until it
	 * completes, since copying a large folder can exceed a client's request timeout.
	 *
	 * @param fromPath full repository path of the folder to copy
	 * @param toPath   full repository path of the destination (parent must exist and be writable)
	 * @return an envelope whose data is {@code {taskId, status}} on success.
	 */
	public static McpEnvelope copyFolder( String fromPath, String toPath )
	{
		McpEnvelope from = resolve( fromPath );
		if ( !from.isOk() )
			return from;
		McpEnvelope to = resolve( toPath );
		if ( !to.isOk() )
			return to;
		try
		{
			ru.biosoft.analysis.CopyFolderAnalysis analysis =
					ru.biosoft.analysiscore.AnalysisMethodRegistry.getAnalysisMethod( ru.biosoft.analysis.CopyFolderAnalysis.class );
			ru.biosoft.analysis.CopyFolderAnalysis.CopyFolderAnalysisParameters parameters = analysis.getParameters();
			parameters.setWriteAnalysisInfo( false );
			parameters.setFromFolder( DataElementPath.create( fromPath ) );
			parameters.setToFolder( DataElementPath.create( toPath ) );
			TaskInfo info = TaskManager.getInstance().addAnalysisTask( analysis, true );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "taskId", info.getName() );
			m.put( "status", "queued" );
			return McpEnvelope.ok( m );
		}
		catch ( Exception e )
		{
			return McpEnvelope.error( McpConstants.CODE_INTERNAL,
					"could not queue folder copy: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
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
}
