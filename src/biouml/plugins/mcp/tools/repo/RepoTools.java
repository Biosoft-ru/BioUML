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
				"Search element names (case-insensitive substring) under a scope (default: all roots).",
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
	}
}
