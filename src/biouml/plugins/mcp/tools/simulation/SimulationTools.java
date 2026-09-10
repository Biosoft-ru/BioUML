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
