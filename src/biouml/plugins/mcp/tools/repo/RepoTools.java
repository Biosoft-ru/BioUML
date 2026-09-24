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
				"ASYNC — Copy a folder (its whole subtree) to a new location. This returns IMMEDIATELY with a jobID; the copy continues in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_repo_job_status with the jobID (with a short delay between calls) until its 'completed' field is true, (3) the copy is then done. Delegates to the platform's folder provider.",
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
					"ASYNC poll — Report the progress of a background job started by an async start-tool (e.g. biouml_repo_copy_folder, biouml_repo_run_script, biouml_repo_import). Call it REPEATEDLY (with a short delay between calls) until the returned 'completed' field is true; the work is not done until then. Delegates to the platform's jobcontrol provider. jobID is the id returned by the start tool. Returns {jobID, status, progress, message, completed}.",
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

			catalog.register( "biouml_repo_document_content",
					"Read the text content of a document element (script, notebook, text file, ...). Delegates to the platform's document provider.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.documentContent( McpArgs.str( args, "path" ) );
					} );

			catalog.register( "biouml_repo_save_document_content",
					"Write the text content of a document element (script, notebook, text file, ...). Delegates to the platform's document provider. path is the element to update; content is the new text.",
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

			catalog.register( "biouml_repo_run_script",
					"ASYNC — Run a script (JS, R, Java, ...). This returns IMMEDIATELY with a jobID; the script runs in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_repo_job_status (with a short delay) until 'completed' is true, (3) then call biouml_repo_script_result to fetch the output. Delegates to the platform's script provider. script is the source text; type is the script type (one of biouml_repo_script_types).",
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

			catalog.register( "biouml_repo_script_result",
					"ASYNC result — Fetch the output of a script job started by biouml_repo_run_script. Call it only AFTER biouml_repo_job_status reports 'completed' is true. Delegates to the platform's script provider. jobID is the id returned by biouml_repo_run_script. Returns the job's printed buffer, tables, images, and HTML.",
					"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "jobID" );
						if ( v != null )
							return v;
						return McpRepositorySupport.scriptResult( McpArgs.str( args, "jobID" ) );
					} );

			catalog.register( "biouml_repo_detect_omics_type",
					"Detect the omics type (Transcriptomics, Genomics, ...) of a data element. Delegates to the platform's omicsType provider.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.detectOmicsType( McpArgs.str( args, "path" ) );
					} );

			catalog.register( "biouml_repo_set_omics_type",
					"Set the omics type of a data element (e.g. Transcriptomics, Genomics). Delegates to the platform's omicsType provider.",
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

			catalog.register( "biouml_repo_import_formats",
					"List the import formats available for a target collection (in accept-priority order). Use this to discover what biouml_repo_import can read into the collection before calling it. The first entry is always 'autodetect'.",
					"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"}},\"required\":[\"parentPath\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "parentPath" );
						if ( v != null )
							return v;
						return McpRepositorySupport.importFormats( McpArgs.str( args, "parentPath" ) );
					} );

			catalog.register( "biouml_repo_import",
					"ASYNC — Import a file from the server's local filesystem into a target collection. This returns IMMEDIATELY with a jobID; the import continues in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_repo_job_status (with a short delay) until 'completed' is true — the final status message is the path of the imported element. Delegates to the platform's import provider. parentPath is the target collection; file is a server-local file path; format is one of biouml_repo_import_formats (omit to autodetect — an ambiguous autodetect is a clean error listing the candidates); name is optional (defaults to the file name without extension).",
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

			catalog.register( "biouml_repo_script_types",
					"List the script types that can be created in a collection (JS, R, Java, ...) — the headless equivalent of the 'New JS script' / 'New R script' / 'New Java code' menu items. Returns one row per type with its 'type' id (what biouml_repo_new_script takes), title, and element class. Only types whose product is available on this server are listed.",
					"{}",
					( ex, args ) -> McpRepositorySupport.scriptTypes() );

			catalog.register( "biouml_repo_new_script",
					"Create a new script element (JS, R, Java, ...) in a target collection — the headless equivalent of the web UI's 'New JS script' / 'New R script' / 'New Java code' menu items. parentPath is the target collection; type is one of biouml_repo_script_types; name is the new element's name; content is the initial script text (omit for a blank script). Returns {created, type}.",
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
					"Create a new element in a collection — the headless equivalent of the web UI's 'New X' menu items. kind is one of: 'table', 'test', 'workflow', 'research', 'notebook' (Jupyter), or a script type from biouml_repo_script_types (e.g. 'js', 'R', 'Java', 'Nextflow', 'WDL', 'math'). parentPath is the target collection; name is the new element's name; content is optional initial content (used for scripts/notebooks, ignored otherwise). Returns {created, kind}.",
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
						return McpRepositorySupport.newElement( McpArgs.str( args, "parentPath" ), McpArgs.str( args, "kind" ), McpArgs.str( args, "name" ), McpArgs.str( args, "content" ) );
					} );

			catalog.register( "biouml_repo_filesystem_types",
					"List the element types available for a file inside a file-system collection — the headless equivalent of the web UI's 'Change element type' menu item. path is the element. Returns {path, count, types}.",
					"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
					( ex, args ) -> {
						McpEnvelope v = McpArgs.requiredString( args, "path" );
						if ( v != null )
							return v;
						return McpRepositorySupport.filesystemElementTypes( McpArgs.str( args, "path" ) );
					} );

			catalog.register( "biouml_repo_set_filesystem_type",
					"Change the element type of a file inside a file-system collection — the headless equivalent of the web UI's 'Change element type' menu item (FileSystemCollection.setElementType). path is the element; type is the target type (one of biouml_repo_filesystem_types). Returns {changed, type}.",
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
	}
}
