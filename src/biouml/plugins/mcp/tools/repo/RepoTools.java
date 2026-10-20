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
				"List the children of a repository collection. Project contents may change in a parallel web UI session; if an element under a project is missing, re-list it (and the projects with biouml_project op list) instead of relying on an earlier listing.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpRepositorySupport.list( McpArgs.str( args, "path" ) );
				} );

		catalog.register( "biouml_repo_describe",
				"Describe a repository element (name, class, child count, children). Project contents may change in a parallel web UI session; if an element under a project is missing, re-list it (and the projects with biouml_project op list) instead of relying on an earlier listing.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpRepositorySupport.describe( McpArgs.str( args, "path" ) );
				} );

		catalog.registerHidden( "biouml_repo_search.search",
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

		catalog.registerHidden( "biouml_repo_search_async",
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

		catalog.registerHidden( "biouml_repo_search_status",
				"{\"type\":\"object\",\"properties\":{\"taskId\":{\"type\":\"string\"}},\"required\":[\"taskId\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "taskId" );
					if ( v != null )
						return v;
					return McpRepositorySupport.searchStatus( McpArgs.str( args, "taskId" ) );
				} );

		catalog.registerHidden( "biouml_repo_get_actions",
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

		catalog.registerHidden( "biouml_repo_run_action",
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

		catalog.registerHidden( "biouml_repo_create_folder",
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

		catalog.registerHidden( "biouml_repo_copy_element",
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

		catalog.registerHidden( "biouml_repo_copy_folder",
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

			catalog.register( "biouml_repo_job_status",
					"ASYNC poll — Report the progress of a background job started by an async start-tool (biouml_repo_copy op folder, biouml_repo_script op run, biouml_repo_import). Call it REPEATEDLY (with a short delay between calls) until the returned 'completed' field is true; the work is not done until then. The 'message' field is the job's accumulated log up to this moment — it grows as the job runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's jobcontrol provider. jobID is the id returned by the start tool. Returns {jobID, status, progress, message, completed}.",
					"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "jobID" );
						if ( v != null )
							return v;
						return McpRepositorySupport.jobStatus( McpArgs.str( args, "jobID" ) );
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

			catalog.registerHidden( "biouml_repo_export_formats",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.exportFormats( McpArgs.str( args, "path" ) );
					} );

			catalog.registerHidden( "biouml_repo_export.export",
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

			catalog.registerHidden( "biouml_repo_download",
					"{\"type\":\"object\",\"properties\":{\"downloadId\":{\"type\":\"string\"},\"offset\":{\"type\":\"integer\"},\"length\":{\"type\":\"integer\"}},\"required\":[\"downloadId\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "downloadId" );
						if ( v == null )
							v = McpArgs.intArg( args, "offset", 0 );
						if ( v == null )
							v = McpArgs.intArg( args, "length", 0 );
						if ( v != null )
							return v;
						return McpRepositorySupport.download( McpArgs.str( args, "downloadId" ),
								args.get( "offset" ) == null ? null : Integer.valueOf( McpArgs.intVal( args, "offset", 0 ) ),
								args.get( "length" ) == null ? null : Integer.valueOf( McpArgs.intVal( args, "length", 0 ) ) );
					} );

			catalog.registerHidden( "biouml_repo_document_content",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.documentContent( McpArgs.str( args, "path" ) );
					} );

			catalog.registerHidden( "biouml_repo_save_document_content",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},\"required\":[\"path\",\"content\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						McpEnvelope vc = McpArgs.requiredString( args, "content" );
						if ( vc != null )
							return vc;
						return McpRepositorySupport.saveDocumentContent( McpArgs.str( args, "path" ), McpArgs.str( args, "content" ) );
					} );

			catalog.registerHidden( "biouml_repo_run_script",
					"{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"}},\"required\":[\"script\",\"type\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "script" );
						if ( v != null )
							return v;
						McpEnvelope vt = McpArgs.requiredString( args, "type" );
						if ( vt != null )
							return vt;
						return McpRepositorySupport.runScript( McpArgs.str( args, "script" ), McpArgs.str( args, "type" ) );
					} );

			catalog.registerHidden( "biouml_repo_script_result",
					"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "jobID" );
						if ( v != null )
							return v;
						return McpRepositorySupport.scriptResult( McpArgs.str( args, "jobID" ) );
					} );

			catalog.registerHidden( "biouml_repo_detect_omics_type",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.detectOmicsType( McpArgs.str( args, "path" ) );
					} );

			catalog.registerHidden( "biouml_repo_set_omics_type",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"omicsType\":{\"type\":\"string\"}},\"required\":[\"path\",\"omicsType\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						McpEnvelope vt = McpArgs.requiredString( args, "omicsType" );
						if ( vt != null )
							return vt;
						return McpRepositorySupport.setOmicsType( McpArgs.str( args, "path" ), McpArgs.str( args, "omicsType" ) );
					} );

			catalog.register( "biouml_repo_git_enabled",
					"Report whether a collection has git version-control enabled (and git is available on the server). Delegates to the platform's git provider.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.gitEnabled( McpArgs.str( args, "path" ) );
					} );

			catalog.register( "biouml_repo_preferences",
					"Read the current session's preferences. Delegates to the platform's preferences provider.",
					"{}",
					( ex, args ) -> McpRepositorySupport.preferences() );

			catalog.registerHidden( "biouml_repo_import_formats",
					"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"}},\"required\":[\"parentPath\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "parentPath" );
						if ( v != null )
							return v;
						return McpRepositorySupport.importFormats( McpArgs.str( args, "parentPath" ) );
					} );

			catalog.registerHidden( "biouml_repo_import.import",
					"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"file\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"file\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "parentPath" );
						if ( v != null )
							return v;
						McpEnvelope vf = McpArgs.requiredString( args, "file" );
						if ( vf != null )
							return vf;
						McpEnvelope vfmt = McpArgs.optionalString( args, "format" );
						if ( vfmt != null )
							return vfmt;
						McpEnvelope vn = McpArgs.optionalString( args, "name" );
						if ( vn != null )
							return vn;
						return McpRepositorySupport.importElement( McpArgs.str( args, "parentPath" ), McpArgs.str( args, "file" ), McpArgs.str( args, "format" ), McpArgs.str( args, "name" ) );
					} );

			catalog.registerHidden( "biouml_repo_script_types",
					"{}",
					( ex, args ) -> McpRepositorySupport.scriptTypes() );

			catalog.registerHidden( "biouml_repo_new_script",
					"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"type\",\"name\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "parentPath" );
						if ( v != null )
							return v;
						McpEnvelope vt = McpArgs.requiredString( args, "type" );
						if ( vt != null )
							return vt;
						McpEnvelope vn = McpArgs.requiredString( args, "name" );
						if ( vn != null )
							return vn;
						McpEnvelope vc = McpArgs.optionalString( args, "content" );
						if ( vc != null )
							return vc;
						return McpRepositorySupport.createScript( McpArgs.str( args, "parentPath" ), McpArgs.str( args, "type" ), McpArgs.str( args, "name" ), McpArgs.str( args, "content" ) );
					} );

			catalog.register( "biouml_repo_new_element",
					"Create a new element in a collection — the headless equivalent of the web UI's 'New X' menu items. kind is one of: 'folder', 'table', 'test', 'workflow', 'research', 'notebook' (Jupyter), or a script type from biouml_repo_script op types (e.g. 'js', 'R', 'Java', 'Nextflow', 'WDL', 'math'). parentPath is the target collection; name is the new element's name; content is optional initial content (used for scripts/notebooks, ignored otherwise). Returns {created, kind}.",
					"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"kind\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"kind\",\"name\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "parentPath" );
						if ( v != null )
							return v;
						McpEnvelope vk = McpArgs.requiredString( args, "kind" );
						if ( vk != null )
							return vk;
						McpEnvelope vn = McpArgs.requiredString( args, "name" );
						if ( vn != null )
							return vn;
						McpEnvelope vc = McpArgs.optionalString( args, "content" );
						if ( vc != null )
							return vc;
						if ( "folder".equalsIgnoreCase( McpArgs.str( args, "kind" ) ) )
							return McpRepositorySupport.createFolder( McpArgs.str( args, "parentPath" ), McpArgs.str( args, "name" ) );
						return McpRepositorySupport.newElement( McpArgs.str( args, "parentPath" ), McpArgs.str( args, "kind" ), McpArgs.str( args, "name" ), McpArgs.str( args, "content" ) );
					} );

			catalog.registerHidden( "biouml_repo_filesystem_types",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.filesystemElementTypes( McpArgs.str( args, "path" ) );
					} );

			catalog.registerHidden( "biouml_repo_set_filesystem_type",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"}},\"required\":[\"path\",\"type\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						McpEnvelope vt = McpArgs.requiredString( args, "type" );
						if ( vt != null )
							return vt;
						return McpRepositorySupport.setFilesystemType( McpArgs.str( args, "path" ), McpArgs.str( args, "type" ) );
					} );

			catalog.register( "biouml_repo_login",
					"Log into a credentials-protected collection — the headless equivalent of the web UI's 'Login' menu item (CredentialsCollection.processCredentialsBean). path is the collection; fields is a flat map of credential field name -> value (e.g. {\"user\":\"...\",\"password\":\"...\"}) matched case-insensitively to the collection's credentials bean. Returns {loggedIn, path}.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"fields\":{\"type\":\"object\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						Map<String, String> fields = McpArgs.stringMap( args, "fields" );
						return McpRepositorySupport.login( McpArgs.str( args, "path" ), fields );
					} );

			catalog.register( "biouml_repo_log",
					"Fetch the current session's server log tail — the headless equivalent of the web UI's 'Logs' view pane. Returns the in-memory java.util.logging capture for this session (a bounded, SEVERE-by-default buffer of everything the JVM logged under it — cross-job server output such as unhandled exceptions, NOT a single job's log). Diagnostic only: returns an error if the experimental logging feature was not enabled at server boot. Delegates to the platform's log provider.",
					"{}",
					( ex, args ) -> McpRepositorySupport.getLog() );

		catalog.registerOps( "biouml_repo_search",
				"Search element names (case-insensitive substring) under scope (default: all roots). op search (default): return the matches; on a large repository it can exceed the client's timeout — then use op start, which runs the same search in the background and returns a taskId at once, poll op status (taskId: queued/running/done/cancelled/error) until 'done', and call op search again with the same query and scope: the warmed caches make it quick.",
				"{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"},\"scope\":{\"type\":\"string\"},\"taskId\":{\"type\":\"string\"}}}",
				"search",
				McpToolCatalog.op( "search", "biouml_repo_search.search" ),
				McpToolCatalog.op( "start", "biouml_repo_search_async" ),
				McpToolCatalog.op( "status", "biouml_repo_search_status" ) );

		catalog.registerOps( "biouml_repo_action",
				"The context-menu actions of the element path. op list: the actions available (key, name, description, headlessSafe). op run: run action (a key from op list) headlessly; interactive-only actions return requires_interactive_ui.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"action\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				null,
				McpToolCatalog.op( "list", "biouml_repo_get_actions" ),
				McpToolCatalog.op( "run", "biouml_repo_run_action" ) );

		catalog.registerOps( "biouml_repo_copy",
				"Copy srcPath to destPath, the full path of the copy (its parent must exist and be writable; the last segment is the new name); the source is left untouched. op element: one element (file, table, diagram, any cloneable element) — the web UI's 'Save a copy'; done when the call returns. op folder: a folder with its whole subtree; ASYNC — returns a jobID at once, the copy is done when biouml_repo_job_status reports 'completed' true.",
				"{\"type\":\"object\",\"properties\":{\"srcPath\":{\"type\":\"string\"},\"destPath\":{\"type\":\"string\"}},\"required\":[\"srcPath\",\"destPath\"]}",
				null,
				McpToolCatalog.op( "element", "biouml_repo_copy_element" ),
				McpToolCatalog.op( "folder", "biouml_repo_copy_folder", "srcPath>fromPath", "destPath>toPath" ) );

		catalog.registerOps( "biouml_repo_export",
				"Export a repository element to a file on the server — the web UI's 'Export'. op formats: the formats available for path, in priority order (they follow the perspective in effect, see biouml_perspective_select). op export (default): write path in format (one of op formats; default the first) into targetDir (a server-local directory; default a new temporary one), named after the element with the format's suffix; returns {file, format, bytes, sha256, downloadId}. op download: the exported file downloadId in base64 chunks of length bytes (default 262144, max 1048576) from offset (default 0); returns {name, offset, bytes, totalBytes, sha256, base64, done, nextOffset} — call again with offset=nextOffset until done, join the decoded chunks and check sha256. A downloadId lasts 24 h and works only for the user who exported.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"},\"targetDir\":{\"type\":\"string\"},\"downloadId\":{\"type\":\"string\"},\"offset\":{\"type\":\"integer\"},\"length\":{\"type\":\"integer\"}}}",
				"export",
				McpToolCatalog.op( "formats", "biouml_repo_export_formats" ),
				McpToolCatalog.op( "export", "biouml_repo_export.export" ),
				McpToolCatalog.op( "download", "biouml_repo_download" ) );

		catalog.registerOps( "biouml_repo_import",
				"Import a file from the server's filesystem into the collection parentPath. op formats: the formats available for parentPath, in priority order, the first being 'autodetect' (they follow the perspective in effect, see biouml_perspective_select). op import (default): ASYNC — returns a jobID at once; the import is done when biouml_repo_job_status reports 'completed' true, and its final message is the path of the imported element. file is a server-local path; format is one of op formats (omit to autodetect; an ambiguous autodetect is an error listing the candidates); name defaults to the file name without extension.",
				"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"file\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"}},\"required\":[\"parentPath\"]}",
				"import",
				McpToolCatalog.op( "formats", "biouml_repo_import_formats" ),
				McpToolCatalog.op( "import", "biouml_repo_import.import" ) );

		catalog.registerOps( "biouml_repo_document",
				"The text of the document element path (script, notebook, text file, ...). op read: return it. op write: replace it with content.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				null,
				McpToolCatalog.op( "read", "biouml_repo_document_content" ),
				McpToolCatalog.op( "write", "biouml_repo_save_document_content" ) );

		catalog.registerOps( "biouml_repo_script",
				"Run scripts (JS, R, Java, ...). op types: the script types available on this server (type id, title, element class). op run: ASYNC — run the source text script of type (an id from op types); returns a jobID at once; when biouml_repo_job_status reports 'completed' true, op result (jobID) returns the output: the printed buffer, tables, images and HTML. A script element is created with biouml_repo_new_element.",
				"{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"},\"jobID\":{\"type\":\"string\"}}}",
				null,
				McpToolCatalog.op( "types", "biouml_repo_script_types" ),
				McpToolCatalog.op( "run", "biouml_repo_run_script" ),
				McpToolCatalog.op( "result", "biouml_repo_script_result" ) );

		catalog.registerOps( "biouml_repo_omics_type",
				"The omics type (Transcriptomics, Genomics, ...) of the data element path. op detect: detect it. op set: set it to omicsType.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"omicsType\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				null,
				McpToolCatalog.op( "detect", "biouml_repo_detect_omics_type" ),
				McpToolCatalog.op( "set", "biouml_repo_set_omics_type" ) );

		catalog.registerOps( "biouml_repo_filesystem_type",
				"The element type of the file path in a file-system collection — the web UI's 'Change element type'. op types: the types available ({path, count, types}). op set: change it to type, one of op types; returns {changed, type}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				null,
				McpToolCatalog.op( "types", "biouml_repo_filesystem_types" ),
				McpToolCatalog.op( "set", "biouml_repo_set_filesystem_type" ) );
	}
}
