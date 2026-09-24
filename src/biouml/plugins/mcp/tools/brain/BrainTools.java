package biouml.plugins.mcp.tools.brain;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpBrainSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * MCP tools for headless brain-model generation — the headless equivalents of the web UI's
 * "Generate ..." context-menu items on a brain diagram. Each is a {@code BackgroundDynamicAction}
 * whose {@code getJobControl(diagram).run()} deploys the regional/cellular/receptor models into new
 * diagrams and saves them, with no UI.
 */
public final class BrainTools
{
	private BrainTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		String diag = "{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}";

		catalog.register( "biouml_brain_generate_equations",
				"Generate the brain-model equations from a brain diagram — the headless equivalent of the web UI's 'Generate equations' menu item. path is the brain diagram. Deploys the regional/cellular/receptor model equations into new diagrams and saves them. Returns {operation, path, status, completed}.",
				diag,
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpBrainSupport.generateEquations( McpArgs.str( args, "path" ) );
				} );

		catalog.register( "biouml_brain_generate_composite_diagram",
				"Generate the brain composite diagram from a brain diagram — the headless equivalent of the web UI's 'Generate composite diagram' menu item. path is the brain diagram. Builds a composite diagram wiring the cellular+regional submodels and saves it. Returns {operation, path, status, completed}.",
				diag,
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpBrainSupport.generateCompositeDiagram( McpArgs.str( args, "path" ) );
				} );

		catalog.register( "biouml_brain_generate_multilevel_model",
				"Generate the brain multi-level model from a brain diagram — the headless equivalent of the web UI's 'Generate multi-level model' menu item. path is the brain diagram. Deploys the submodels and builds the multi-level agent-model diagram. Returns {operation, path, status, completed}.",
				diag,
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpBrainSupport.generateMultilevelModel( McpArgs.str( args, "path" ) );
				} );
	}
}
