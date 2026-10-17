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
 * (repository roots and entry points, the start page's steps and presets, a guide for agents), switch the
 * perspective for the MCP session — which changes the import/export formats the platform offers — and read the
 * web UI's help pages.</p>
 */
public final class PerspectiveTools
{
	private PerspectiveTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_perspective_list",
				"List the perspectives available to the current user — BioUML's task-oriented entry points (e.g. GTRD, Microbiome, Regulatory mutations) — and the one in effect for this session. Each has a description (declared, or taken from the perspective's start page), its repository roots, entry points (the analyses, workflows and collections work in that perspective usually starts from; pass their paths to biouml_analysis_describe or biouml_repo_describe), and guide:true when biouml_perspective_select returns a step-by-step guide for doing the perspective's work with MCP tools. The perspective in effect decides which import/export formats are offered; switch it with biouml_perspective_select.",
				"{}",
				( ex, args ) -> McpPerspectiveSupport.list() );

		catalog.register( "biouml_perspective_select",
				"Switch the perspective for this MCP session only (the user's saved web-UI perspective is not changed) and learn how work in it is done. name is one of biouml_perspective_list. From then on biouml_repo_import_formats, biouml_repo_export_formats and import autodetection follow the perspective's importer/exporter rules. Returns the perspective's description, repository roots, entry points and allow/deny rules; guide, when there is one: how to do with MCP tools what the perspective's start page does (read it first); and — when the web UI files are found on the server — an intro block with what the start page offers users: for a wizard (a page with step-id elements) its steps (title, a short guide, helpPage — read the whole help page with biouml_perspective_page), more entry points (with an exists flag), analysisPresets (the analyses the page starts, with the parameter values it passes, checked against the analysis — pass them to biouml_analysis_run — and chosenByUser, the parameters the user sets), pathTemplates (repository paths the page builds, e.g. 'data/Projects/{currentProject}/Data'), exampleProjects (projects the page lists as examples, e.g. demo projects with results to learn from), importTargets (folders the page imports into), help links and a page summary.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Perspective name from biouml_perspective_list\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "name" );
					if ( v != null )
						return v;
					return McpPerspectiveSupport.select( McpArgs.str( args, "name" ) );
				} );

		catalog.register( "biouml_perspective_page",
				"Read a page of the web UI as text: a wizard step's help page (the helpPage of a step in biouml_perspective_select), a perspective's start page or a user guide included in them. page is its path in the web application, e.g. 'wizard/help/step-start.shtml'; server-side includes are expanded. Returns {page, title, text (headings as '## ...', one paragraph, list item or table row per line), totalChars, nextOffset (when more follows: call again with offset=nextOffset), links (repository paths from '#de=' links, and web links)}.",
				"{\"type\":\"object\",\"properties\":{\"page\":{\"type\":\"string\",\"description\":\"Path of the page in the web application, e.g. wizard/help/step-start.shtml\"},\"offset\":{\"type\":\"integer\",\"description\":\"First character to return (default 0)\"}},\"required\":[\"page\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "page" );
					if ( v == null )
						v = McpArgs.intArg( args, "offset", 0 );
					if ( v != null )
						return v;
					return McpPerspectiveSupport.page( McpArgs.str( args, "page" ), McpArgs.intVal( args, "offset", 0 ) );
				} );
	}
}
