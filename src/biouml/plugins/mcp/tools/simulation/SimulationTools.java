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
		catalog.register( "biouml_simulation_run",
				"Run a headless ODE simulation of a diagram and return its time series. The diagram must have a dynamic model with at least one rate (ODE) equation; otherwise a missing_dynamic_model error is returned. Times and the solver are optional.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"initialTime\":{\"type\":\"number\"},\"completionTime\":{\"type\":\"number\"},\"timeIncrement\":{\"type\":\"number\"},\"solver\":{\"type\":\"string\"},\"maxPoints\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpSimulationSupport.run(
						str( args, "diagramPath" ),
						optNum( args, "initialTime" ),
						optNum( args, "completionTime" ),
						optNum( args, "timeIncrement" ),
						str( args, "solver" ),
						optInt( args, "maxPoints" ) ) );

		catalog.register( "biouml_simulation_list_solvers",
				"List the ODE solvers available to the simulation engine (name, type, implementation class).",
				"{}",
				( ex, args ) -> McpSimulationSupport.listSolvers() );

		catalog.register( "biouml_simulation_start",
				"ASYNC — Start a diagram simulation as a background job (the provider-based counterpart of biouml_simulation_run). This returns IMMEDIATELY with a jobID; the simulation runs in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_simulation_status (with a short delay) until 'completed' is true, (3) then call biouml_simulation_result to fetch the time series. The diagram must have a dynamic model with at least one rate (ODE) equation. Delegates to the platform's simulation provider.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpSimulationSupport.startSimulation( str( args, "diagramPath" ) ) );

		catalog.register( "biouml_simulation_status",
				"ASYNC poll — Report the progress of a simulation job started by biouml_simulation_start. Call it REPEATEDLY (with a short delay between calls) until the returned 'completed' field is true; the simulation is not done until then. The 'message' field is the job's accumulated log up to this moment — it grows as the simulation runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {jobID, status, progress, message, completed}.",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.simulationStatus( str( args, "jobID" ) ) );

		catalog.register( "biouml_simulation_result",
				"ASYNC result — Fetch the time-series result of a completed simulation job. Call it only AFTER biouml_simulation_status reports 'completed' is true. Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {vars, times, values} (and Q1/Q2/Q3 for stochastic results).",
				"{\"type\":\"object\",\"properties\":{\"jobID\":{\"type\":\"string\"}},\"required\":[\"jobID\"]}",
				( ex, args ) -> McpSimulationSupport.simulationResult( str( args, "jobID" ) ) );
	}

	private static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v == null ? null : String.valueOf( v );
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
