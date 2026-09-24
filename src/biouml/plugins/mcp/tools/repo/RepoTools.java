package biouml.plugins.mcp.tools.repo;

import java.util.List;
import java.util.Map;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpActionInvoker;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpRepositorySupport;

import ru.biosoft.access.core.DataElement;

/**
 * Phase-2 repository MCP tools. Registered into a {@link McpToolCatalog}.
 *
 * <p>These tools expose the repository model and the same actions the user sees in the right-click
 * context menus, addressed by {@code DataElementPath} strings. Every handler validates its
 * arguments at the boundary ({@link McpArgs}) and returns structured {@code invalid_params} /
 * {@code not_found} / {@code path_escape} envelopes — never a raw stack trace.</p>
 */
public final class RepoTools
{
	private RepoTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_repo_collections",
				"List the top-level registered repository collections (databases, data, analyses, ...).",
				"{}",
				( ex, args ) -> McpRepositorySupport.collections() );

		catalog.register( "biouml_repo_list",
				"List the children of a repository collection.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpRepositorySupport.list( McpArgs.str( args, "path" ) );
				} );

		catalog.register( "biouml_repo_describe",
				"Describe a repository element (name, class, child count, children).",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpRepositorySupport.describe( McpArgs.str( args, "path" ) );
				} );

		catalog.register( "biouml_repo_search",
				"Search element names (case-insensitive substring) under a scope (default: all roots). Synchronous; returns fast on a responsive server but can exceed the client timeout on a large repository — for large or slow repositories use biouml_repo_search_async and poll biouml_repo_search_status.",
				"{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"},\"scope\":{\"type\":\"string\"}},\"required\":[\"query\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "query" );
					if ( v != null )
						return v;
					McpEnvelope vs = McpArgs.optionalString( args, "scope" );
					if ( vs != null )
						return vs;
					return McpRepositorySupport.search( McpArgs.str( args, "query" ), McpArgs.str( args, "scope" ) );
				} );

		catalog.register( "biouml_repo_search_async",
				"Run a repository search (same as biouml_repo_search) in the background and return a taskId immediately, instead of blocking. On a large/slow repository a synchronous search can exceed the client's request timeout, so: (1) call this to start the search, (2) poll biouml_repo_search_status with the taskId until status is 'done', (3) call biouml_repo_search again with the same query/scope to read the matches — by then the search has warmed the repository caches so it returns quickly. The background run itself does not store its matches.",
				"{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"},\"scope\":{\"type\":\"string\"}},\"required\":[\"query\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "query" );
					if ( v != null )
						return v;
					McpEnvelope vs = McpArgs.optionalString( args, "scope" );
					if ( vs != null )
						return vs;
					return McpRepositorySupport.searchAsync( McpArgs.str( args, "query" ), McpArgs.str( args, "scope" ) );
				} );

		catalog.register( "biouml_repo_search_status",
				"Report the status (queued/running/done/cancelled/error) of a background repository search queued via biouml_repo_search_async.",
				"{\"type\":\"object\",\"properties\":{\"taskId\":{\"type\":\"string\"}},\"required\":[\"taskId\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "taskId" );
					if ( v != null )
						return v;
					return McpRepositorySupport.searchStatus( McpArgs.str( args, "taskId" ) );
				} );

		catalog.register( "biouml_repo_get_actions",
				"List the context-menu actions available for an element (key, name, description, headlessSafe).",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope r = McpRepositorySupport.resolve( McpArgs.str( args, "path" ) );
					if ( !r.isOk() )
						return r;
					List<Map<String, Object>> actions = McpActionInvoker.actionsFor( (DataElement) r.getData() );
					return McpEnvelope.ok( actions );
				} );

		catalog.register( "biouml_repo_run_action",
				"Run a context-menu action (by ActionCommandKey) on an element headlessly. Interactive-only actions return requires_interactive_ui.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"action\":{\"type\":\"string\"}},\"required\":[\"path\",\"action\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope va = McpArgs.requiredString( args, "action" );
					if ( va != null )
						return va;
					McpEnvelope r = McpRepositorySupport.resolve( McpArgs.str( args, "path" ) );
					if ( !r.isOk() )
						return r;
					return McpActionInvoker.perform( (DataElement) r.getData(), McpArgs.str( args, "action" ) );
				} );

		catalog.register( "biouml_repo_create_folder",
				"Create a new folder inside a folder-collection parent.",
				"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "parentPath" );
					if ( v != null )
						return v;
					McpEnvelope vn = McpArgs.requiredString( args, "name" );
					if ( vn != null )
						return vn;
					return McpRepositorySupport.createFolder( McpArgs.str( args, "parentPath" ), McpArgs.str( args, "name" ) );
				} );

		catalog.register( "biouml_repo_remove",
				"Remove an element by path. dryRun=true reports what would be removed without changing anything.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"dryRun\":{\"type\":\"boolean\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vb = McpArgs.boolArg( args, "dryRun", false );
					if ( vb != null )
						return vb;
					return McpRepositorySupport.remove( McpArgs.str( args, "path" ), McpArgs.bool( args, "dryRun", false ) );
				} );

		catalog.register( "biouml_repo_copy_element",
				"Copy a single element (file, table, diagram, or any cloneable data element) to a new location — the headless equivalent of the web UI's 'Save a copy'. destPath is the full path of the new copy (its parent must exist and be writable); the last path segment is the new name. The source is left untouched.",
				"{\"type\":\"object\",\"properties\":{\"srcPath\":{\"type\":\"string\"},\"destPath\":{\"type\":\"string\"}},\"required\":[\"srcPath\",\"destPath\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "srcPath" );
					if ( v != null )
						return v;
					McpEnvelope vd = McpArgs.requiredString( args, "destPath" );
					if ( vd != null )
						return vd;
					return McpRepositorySupport.copyElement( McpArgs.str( args, "srcPath" ), McpArgs.str( args, "destPath" ) );
				} );

		catalog.register( "biouml_repo_copy_folder",
				"Copy a folder (its whole subtree) to a new location — the headless equivalent of the web UI's 'Copy folder'. Runs asynchronously as a background task and returns a taskId immediately; poll biouml_task_status with the taskId until it completes (a large folder copy can exceed the client's request timeout).",
				"{\"type\":\"object\",\"properties\":{\"fromPath\":{\"type\":\"string\"},\"toPath\":{\"type\":\"string\"}},\"required\":[\"fromPath\",\"toPath\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "fromPath" );
					if ( v != null )
						return v;
					McpEnvelope vd = McpArgs.requiredString( args, "toPath" );
					if ( vd != null )
						return vd;
					return McpRepositorySupport.copyFolder( McpArgs.str( args, "fromPath" ), McpArgs.str( args, "toPath" ) );
				} );

			catalog.register( "biouml_repo_reinitialize",
					"Re-initialize a collection that previously failed to load — the headless equivalent of the web UI's 'Retry'/'Reinitialize' menu item. Returns the path and whether it is now valid.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.reinitialize( McpArgs.str( args, "path" ) );
					} );

			catalog.register( "biouml_repo_export_formats",
					"List the export formats available for a repository element (in accept-priority order). Use this to discover what biouml_repo_export can produce for the element before calling it.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.exportFormats( McpArgs.str( args, "path" ) );
					} );

			catalog.register( "biouml_repo_export",
					"Export a repository element to a file on the server's local filesystem — the headless equivalent of the web UI's 'Export' menu item. format is one of biouml_repo_export_formats (omit to use the highest-priority format); targetDir is a server-local directory (omit for the current dir) — the output file is the element name + the format suffix. Returns {file, format, bytes}.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"},\"targetDir\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						McpEnvelope vf = McpArgs.optionalString( args, "format" );
						if ( vf != null )
							return vf;
						McpEnvelope vt = McpArgs.optionalString( args, "targetDir" );
						if ( vt != null )
							return vt;
						return McpRepositorySupport.export( McpArgs.str( args, "path" ), McpArgs.str( args, "format" ), McpArgs.str( args, "targetDir" ) );
					} );
	}
}
