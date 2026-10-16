package biouml.plugins.mcp.tools.perspective;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpEnvelope;
import biouml.plugins.mcp.support.McpPerspectiveSupport;

/**
 * Perspective MCP tools. Registered into a {@link McpToolCatalog}.
 *
 * <p>Perspectives are the task-oriented entry points users pick in the web UI (GTRD, Microbiome, Regulatory
 * mutations, ...). These tools let an agent see which ones the user may open, where work in each starts
 * (repository roots and entry points), and switch the perspective for the MCP session — which changes the
 * import/export formats the platform offers.</p>
 */
public final class PerspectiveTools
{
	private PerspectiveTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_perspective_list",
				"List the perspectives available to the current user — BioUML's task-oriented entry points (e.g. GTRD, Microbiome, Regulatory mutations) — and the one in effect for this session. Each has a description (declared, or taken from the perspective's start page), its repository roots, and entry points: the analyses, workflows and collections work in that perspective usually starts from (pass their paths to biouml_analysis_describe or biouml_repo_describe). The perspective in effect decides which import/export formats are offered; switch it with biouml_perspective_select.",
				"{}",
				( ex, args ) -> McpPerspectiveSupport.list() );

		catalog.register( "biouml_perspective_select",
				"Switch the perspective for this MCP session only (the user's saved web-UI perspective is not changed). name is one of biouml_perspective_list. From then on biouml_repo_import_formats, biouml_repo_export_formats and import autodetection follow the perspective's importer/exporter rules. Returns the perspective's description, repository roots, entry points and allow/deny rules, and — when the web UI files are found on the server — an intro block with what the perspective's start page offers users: more entry points (with an exists flag), analysisPresets (analysis path + parameter values to pass to biouml_analysis_run), importTargets (folders the page imports into), help links and a page summary.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Perspective name from biouml_perspective_list\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpPerspectiveSupport.select( McpArgs.str( args, "name" ) );
				} );
	}
}
