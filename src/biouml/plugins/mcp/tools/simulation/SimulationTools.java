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
				"ASYNC — Start a diagram simulation as a background job. This returns IMMEDIATELY with a jobID; the simulation runs in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_simulation_status (with a short delay) until 'completed' is true, (3) then call biouml_simulation_result to fetch the time series. The diagram must have a dynamic model with at least one rate (ODE) equation. Delegates to the platform's simulation provider.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpSimulationSupport.startSimulation( str( args, "diagramPath" ) ) );

		catalog.register( "biouml_simulation_status",
				"ASYNC poll — Report the progress of a simulation job started by biouml_simulation_start. Call it REPEATEDLY (with a short delay between calls) until the returned 'completed' field is true; the simulation is not done until then. The 'message' field is the job's accumulated log up to this moment — it grows as the simulation runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {jobID, status, progress, message, completed}.",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.simulationStatus( str( args, "jobID" ) ) );

		catalog.register( "biouml_simulation_result",
				"ASYNC result — Fetch the <em>raw time-series</em> result of a completed simulation job. Call it only AFTER biouml_simulation_status reports 'completed' is true. WARNING: this returns the full numeric series, which for a non-trivial simulation is a LARGE amount of data that can exhaust your context — to merely <em>see</em> the result, prefer biouml_simulation_plot (a compact image + tiny metadata summary instead). Use this tool only when you specifically need the raw numbers (e.g. to compute a statistic). Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {vars, times, values} (and Q1/Q2/Q3 for stochastic results).",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.simulationResult( str( args, "jobID" ) ) );

		catalog.register( "biouml_simulation_plot",
				"Render a PLOT of a completed simulation's result ON THE SERVER and return it as a PNG image plus a tiny metadata summary — the headless equivalent of the web UI's 'New plot' / '+' action. PREFERRED over biouml_simulation_result for simply inspecting a result: the full time series stays on the server and only an image + {variables, points, timeRange, finals} come back, so a large result does not blow your context. Call it AFTER biouml_simulation_status reports 'completed'. jobID is the id returned by biouml_simulation_start. Optionally pass 'variables' (subset of the result's variable names) and 'width'/'height' (px); by default every non-time variable is plotted. Returns {jobID, plot, variables, points, timeRange, finals, image(base64 PNG), imageFormat, width, height}.",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"},\"variables\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"width\":{\"type\":\"integer\"},\"height\":{\"type\":\"integer\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.plotSimulationResult(
						str( args, "jobID" ),
						strArray( args, "variables" ),
						optInt( args, "width" ),
						optInt( args, "height" ) ) );
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
