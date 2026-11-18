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
				"The catalog of what can be run: analysis methods by group and workflows (ready-made pipelines of analyses) by folder, each with a one-line summary of what it is for, and topics, the research questions biouml_analysis_guide answers. query (optional) keeps the analyses and workflows whose name, group or guide has all its words, e.g. 'enrichment', 'master regulator', 'vcf'; group (optional) lists one analyses group or workflow folder. Prefer a workflow that fits over chaining analyses by hand. Not sure which analysis answers a research question: start with biouml_analysis_guide.",
				"{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"words to find in names and guides\"},\"group\":{\"type\":\"string\",\"description\":\"an analyses group or a workflow folder\"}}}",
				( ex, args ) -> McpAnalysisSupport.listMethods( str( args, "query" ), str( args, "group" ) ) );

		catalog.register( "biouml_analysis_guide",
				"Research guidance for someone who is not a bioinformatician: from a research question to the analyses and workflows that answer it. Without arguments: the topics (id, title, summary), e.g. what a list of genes has in common, which transcription factors and master regulators control them, differential expression from a table of expression values or from raw files, ChIP-seq peaks, variants, drug targets. With topic (an id): the steps to take with these tools — which workflow or analyses to run and in what order, what data and identifiers they need, how to read the results, what to tell the user. Read the topic before choosing an analysis for a question it covers.",
				"{\"type\":\"object\",\"properties\":{\"topic\":{\"type\":\"string\",\"description\":\"a topic id from the topic list\"}}}",
				( ex, args ) -> McpAnalysisSupport.guide( str( args, "topic" ) ) );

		catalog.register( "biouml_analysis_describe",
				"Describe an analysis method or a workflow (name: an analysis name, or the path of a workflow from biouml_analysis_list): its description (and help, the text of its help page when it has one), guide (written for agents: summary, use — when to pick it, input — what it needs, output — how to read the result, next — what usually follows, tips), its parameter input schema (name, type, default, description — from the guide where the analysis declares none or only repeats the name, hint — what the guide adds to a declared description: meaning and sensible values, and for choice parameters allowedValues — at most 200, with allowedValuesCount when there are more), input names, and output names. The fields of a nested bean and of each item of an array of beans are listed with the keys biouml_analysis_run takes ('expertSettings/cutoffFC', 'conditions/0/condition'). params (optional, as for biouml_analysis_run) are bound first: the choices of some parameters depend on others — e.g. the conditions of 'Generate workflow from annotation diagram' are the category titles of the annotationDiagram passed — and array items appear once the array is set. filter (optional) keeps only the allowed values that contain it, case-insensitively (matchingCount tells how many match), e.g. filter 'carcinoma' to find a disease among thousands; without properties it lists only the choice parameters with matches. properties (optional) lists only the parameters named (with their fields and items, e.g. ['conditions']). With filter or properties the help text and the guide are left out. For a workflow: kind 'workflow', its parameters with their role (input, output, setting; the default of an output is computed from the inputs passed in params) and steps, the analyses it runs.",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"params\":{\"type\":\"object\",\"description\":\"parameter values to bind before describing, as for biouml_analysis_run\"},\"filter\":{\"type\":\"string\",\"description\":\"list only the allowed values containing this text\"},\"properties\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"description\":\"list only these parameters\"}},\"required\":[\"name\"]}",
				( ex, args ) -> McpAnalysisSupport.describeMethod( str( args, "name" ),
						args.get( "params" ) instanceof Map ? (Map<String, Object>) args.get( "params" ) : null, str( args, "filter" ), strings( args, "properties" ) ) );

		catalog.register( "biouml_analysis_run",
				"ASYNC — Run an analysis or a workflow (name: an analysis name, or the path of a workflow; a workflow always runs in the background and returns {taskId, outputs}). By default (sync=false, the default) this returns IMMEDIATELY with {taskId, status}; the analysis runs in the background and is NOT done when this call returns. To finish: (1) call this to get the taskId, (2) poll biouml_task with op status using increasing delays (15s, 30s, 1min, 2min, 5min, 10min, then every 30min for long analyses) until its status is 'done'/'error', (3) then read the results. Set sync=true to instead run inline and wait for the result in one call (only for quick analyses). params is a map of bean-property values: nested keys use '/' separators (or pass a JSON object), array items are addressed by index ('conditions/0/condition') or passed as a JSON list ('conditions': [{'condition': '...'}]); path-typed properties take repository path strings. Choice parameters list their accepted values in biouml_analysis_describe (allowedValues).",
				"{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"params\":{\"type\":\"object\"},\"originPath\":{\"type\":\"string\"},\"sync\":{\"type\":\"boolean\"}},\"required\":[\"name\"]}",
				( ex, args ) -> {
					Map<String, Object> params = args.get( "params" ) instanceof Map ? (Map<String, Object>) args.get( "params" ) : new java.util.LinkedHashMap<String, Object>();
					Boolean sync = args.get( "sync" ) instanceof Boolean ? (Boolean) args.get( "sync" ) : Boolean.FALSE;
					return McpAnalysisSupport.runAnalysis( str( args, "name" ), params, str( args, "originPath" ), sync );
				} );

		catalog.registerHidden( "biouml_task_status",
				"{\"type\":\"object\",\"properties\":{\"taskId\":{\"type\":\"string\"}},\"required\":[\"taskId\"]}",
				( ex, args ) -> McpAnalysisSupport.taskStatus( str( args, "taskId" ) ) );

		catalog.registerHidden( "biouml_task_cancel",
				"{\"type\":\"object\",\"properties\":{\"taskId\":{\"type\":\"string\"}},\"required\":[\"taskId\"]}",
				( ex, args ) -> McpAnalysisSupport.cancelTask( str( args, "taskId" ) ) );

		catalog.registerHidden( "biouml_task_list",
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
				"Read an analysis output: for a table, column names/types, rows (from offset, default 0; at most limit, default 50, max 500; truncated tells whether more follow) and their row ids (rowIds, aligned with rows — the table's key, e.g. a gene or drug id); columns (optional) names the columns to return, in that order — wide tables such as drug lists with synonyms are much smaller with only the columns needed; for a track (e.g. an imported VCF), its sites the same way: rowIds are the site ids, columns chromosome, from, to, strand, type and the site properties (a VCF's RefAllele, AltAllele, genotype fields such as Format_GT_<sample>, ...); for a folder, its children; for a simulation result (e.g. the output of 'Simulation analysis'), the time series ({vars, times, values}, plus Q1/Q2/Q3 for stochastic results).",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"offset\":{\"type\":\"integer\",\"description\":\"first table row to return (default 0)\"},\"limit\":{\"type\":\"integer\",\"description\":\"rows to return (default 50, max 500)\"},\"columns\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"description\":\"table columns to return (default all)\"}},\"required\":[\"path\"]}",
				( ex, args ) -> McpAnalysisSupport.getAnalysisResult( str( args, "path" ), integer( args, "offset" ), integer( args, "limit" ), strings( args, "columns" ) ) );

		catalog.registerOps( "biouml_task",
				"Background tasks, e.g. of biouml_analysis_run. op list: active and recent tasks (id, type, status). op status: ASYNC poll — the status of task taskId (queued/running/paused/done/error/cancelled; status is the numeric job status, state its name), progress (0-100) and result paths. Poll with increasing delays: check after 15s, then 30s, 60s, 120s, 300s, 600s, then every 30 minutes until done (long analyses like Genome Enhancer can take hours). It also returns the log's warnings and errors grouped by message ignoring numbers (problems: the latest 20 distinct ones with their counts; problemCount: all problem lines; problemKinds: all distinct messages), e.g. skipped workflow steps, and the log's last lines (logTail; lines that differ only in numbers, such as progress lines, are shown once with [×N]). op cancel: cancel a queued or running task.",
				"{\"type\":\"object\",\"properties\":{\"taskId\":{\"type\":\"string\"}}}",
				null,
				McpToolCatalog.op( "list", "biouml_task_list" ),
				McpToolCatalog.op( "status", "biouml_task_status" ),
				McpToolCatalog.op( "cancel", "biouml_task_cancel" ) );
	}

	/** A list-of-strings argument; a single string may also list them comma-separated. */
	private static java.util.List<String> strings( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		java.util.List<String> out = new java.util.ArrayList<String>();
		if ( v instanceof java.util.List )
		{
			for ( Object o : (java.util.List<?>) v )
				if ( o != null )
					out.add( String.valueOf( o ) );
		}
		else if ( v != null )
		{
			for ( String s : String.valueOf( v ).split( "," ) )
				if ( !s.trim().isEmpty() )
					out.add( s.trim() );
		}
		return out;
	}

	private static Integer integer( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		if ( v instanceof Number )
			return Integer.valueOf( ( (Number) v ).intValue() );
		try
		{
			return v == null ? null : Integer.valueOf( String.valueOf( v ).trim() );
		}
		catch ( NumberFormatException e )
		{
			return null;
		}
	}

	private static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v == null ? null : String.valueOf( v );
	}
}
