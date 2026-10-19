package biouml.plugins.mcp.tools.simulation;

import java.util.Map;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpSimulationSupport;

/**
 * Phase-4 simulation MCP tools. Registered into a {@link McpToolCatalog}.
 *
 * <p>These tools let an agent run a headless ODE simulation of a diagram that carries a dynamic
 * model, and inspect the available solvers.</p>
 */
public final class SimulationTools
{
	private SimulationTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_simulation_list_solvers",
				"List the ODE solvers available to the simulation engine (name, type, implementation class).",
				"{}",
				( ex, args ) -> McpSimulationSupport.listSolvers() );

		catalog.register( "biouml_simulation_start",
				"ASYNC — Start a diagram simulation as a background job. This returns IMMEDIATELY with a jobID; the simulation runs in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_simulation_status (with a short delay) until 'completed' is true, (3) then call biouml_simulation_output to plot or fetch the result. The diagram must have a dynamic model with at least one rate (ODE) equation. Delegates to the platform's simulation provider.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpSimulationSupport.startSimulation( str( args, "diagramPath" ) ) );

		catalog.register( "biouml_simulation_status",
				"ASYNC poll — Report the progress of a simulation job started by biouml_simulation_start. Call it REPEATEDLY until the returned 'completed' field is true; the simulation is not done until then. ⏱️ Do NOT poll faster than every 10 seconds — each poll is a full server request and polling every 1–2 seconds wastes server resources and adds latency for no benefit; wait ~10 s between polls (longer, e.g. 30 s, for big simulations). The 'message' field is the job's accumulated log up to this moment — it grows as the simulation runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {jobID, status, progress, message, completed}.",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.simulationStatus( str( args, "jobID" ) ) );

		catalog.registerHidden( "biouml_simulation_result",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.simulationResult( str( args, "jobID" ) ) );

		catalog.registerHidden( "biouml_simulation_plot",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"},\"variables\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"width\":{\"type\":\"integer\"},\"height\":{\"type\":\"integer\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.plotSimulationResult(
						str( args, "jobID" ),
						strArray( args, "variables" ),
						optInt( args, "width" ),
						optInt( args, "height" ) ) );

		catalog.registerHidden( "biouml_simulation_save_result",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"},\"destinationPath\":{\"type\":\"string\"}},\"required\":[\"jobID\",\"destinationPath\"]}",
				( ex, args ) -> McpSimulationSupport.saveSimulationResult(
						str( args, "jobID" ),
						str( args, "destinationPath" ) ) );

		catalog.registerOps( "biouml_simulation_output",
				"The result of the simulation job jobID (from biouml_simulation_start), once biouml_simulation_status reports 'completed'. op plot (default): render it as a plot on the server — the web UI's 'New plot' — and return a PNG image plus {jobID, plot, variables, points, timeRange, finals, imageFormat, width, height}; variables picks the result variables (default: all but time); width/height in px. The series stays on the server, so use this to look at a result. op data: the raw time series {vars, times, values} (plus Q1/Q2/Q3 for stochastic results) — every number, which for any non-trivial simulation can exhaust your context; only when you need the numbers. op save: store the result in the repository at destinationPath (the full path of the new element; its parent must exist and be writable) — the web UI's 'Save result'; otherwise a result lives in memory only and is lost when the server restarts.",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"},\"variables\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"width\":{\"type\":\"integer\"},\"height\":{\"type\":\"integer\"},\"destinationPath\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
				"plot",
				McpToolCatalog.op( "plot", "biouml_simulation_plot" ),
				McpToolCatalog.op( "data", "biouml_simulation_result" ),
				McpToolCatalog.op( "save", "biouml_simulation_save_result" ) );
	}

	private static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v == null ? null : String.valueOf( v );
	}

	private static String[] strArray( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		if ( v instanceof java.util.List )
		{
			java.util.List<?> list = (java.util.List<?>) v;
			String[] out = new String[ list.size() ];
			for ( int i = 0; i < list.size(); i++ )
				out[ i ] = list.get( i ) == null ? null : String.valueOf( list.get( i ) );
			return out;
		}
		if ( v instanceof String[] )
			return (String[]) v;
		if ( v instanceof String && ! ( (String) v ).isEmpty() )
			return ( (String) v ).split( "," );
		return null;
	}

	private static Double optNum( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		if ( v == null )
			return null;
		try
		{
			return Double.valueOf( Double.parseDouble( String.valueOf( v ).trim() ) );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	private static Integer optInt( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		if ( v == null )
			return null;
		try
		{
			return Integer.valueOf( Integer.parseInt( String.valueOf( v ).trim() ) );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
}
