package biouml.plugins.mcp.tools.analysis;

import java.util.Map;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpAnalysisSupport;

/**
 * Phase-3 analysis + task MCP tools. Registered into a {@link McpToolCatalog}.
 *
 * <p>These tools expose the platform's core value — discovering, configuring, running, and
 * monitoring analyses (BioUML and any registry-contributed analyses) — as MCP-callable actions.</p>
 */
public final class AnalysisTools
{
	private AnalysisTools()
	{
	}

	@SuppressWarnings( "unchecked" )
	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_analysis_list",
				"List all available analysis methods grouped by their analyses group.",
				"{}",
				( ex, args ) -> McpAnalysisSupport.listMethods() );

		catalog.register( "biouml_analysis_describe",
				"Describe an analysis method: its parameter input schema (name, type, default, description), input names, and output names.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}",
				( ex, args ) -> McpAnalysisSupport.describeMethod( str( args, "name" ) ) );

		catalog.register( "biouml_analysis_run",
				"Run an analysis. params is a flat map of bean-property values (nested keys use '/' separators; path-typed properties take repository path strings). sync=true runs inline and returns the result; otherwise it is queued and returns {taskId,status}.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"params\":{\"type\":\"object\"},\"originPath\":{\"type\":\"string\"},\"sync\":{\"type\":\"boolean\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					Map<String, Object> params = args.get( "params" ) instanceof Map ? (Map<String, Object>) args.get( "params" ) : new java.util.LinkedHashMap<String, Object>();
					Boolean sync = args.get( "sync" ) instanceof Boolean ? (Boolean) args.get( "sync" ) : Boolean.FALSE;
					return McpAnalysisSupport.runAnalysis( str( args, "name" ), params, str( args, "originPath" ), sync );
				} );

		catalog.register( "biouml_task_status",
				"Report a task's status (queued/running/paused/done/error/cancelled), progress (0-100), and result paths.",
				"{\"type\":\"object\",\"properties\":{\"taskId\":{\"type\":\"string\"}},\"required\":[\"taskId\"]}",
				( ex, args ) -> McpAnalysisSupport.taskStatus( str( args, "taskId" ) ) );

		catalog.register( "biouml_task_cancel",
				"Cancel a queued or running task.",
				"{\"type\":\"object\",\"properties\":{\"taskId\":{\"type\":\"string\"}},\"required\":[\"taskId\"]}",
				( ex, args ) -> McpAnalysisSupport.cancelTask( str( args, "taskId" ) ) );

		catalog.register( "biouml_task_list",
				"List active and recent tasks (id, type, status).",
				"{}",
				( ex, args ) -> McpAnalysisSupport.listTasks() );

		catalog.register( "biouml_analysis_repeat",
				"Re-run a completed analysis from its stored output collection (reads the stored analysisName + parameters and runs again).",
				"{\"type\":\"object\",\"properties\":{\"resultPath\":{\"type\":\"string\"},\"sync\":{\"type\":\"boolean\"}},\"required\":[\"resultPath\"]}",
				( ex, args ) -> {
					Boolean sync = args.get( "sync" ) instanceof Boolean ? (Boolean) args.get( "sync" ) : Boolean.TRUE;
					return McpAnalysisSupport.repeatAnalysis( str( args, "resultPath" ), sync );
				} );

		catalog.register( "biouml_analysis_get_result",
				"Read an analysis output collection: for a table, column names/types and the first N rows; for a folder, its children.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> McpAnalysisSupport.getAnalysisResult( str( args, "path" ) ) );
	}

	private static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v == null ? null : String.valueOf( v );
	}
}
