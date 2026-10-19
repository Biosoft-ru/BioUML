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

		catalog.registerHidden( "biouml_brain_generate_equations",
				diag,
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpBrainSupport.generateEquations( McpArgs.str( args, "path" ) );
				} );

		catalog.registerHidden( "biouml_brain_generate_composite_diagram",
				diag,
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpBrainSupport.generateCompositeDiagram( McpArgs.str( args, "path" ) );
				} );

		catalog.registerHidden( "biouml_brain_generate_multilevel_model",
				diag,
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpBrainSupport.generateMultilevelModel( McpArgs.str( args, "path" ) );
				} );

		catalog.registerOps( "biouml_brain_generate",
				"Generate models from the brain diagram path — the web UI's 'Generate ...' menu items; the new diagrams are saved. op equations: deploy the regional/cellular/receptor model equations into new diagrams. op composite_diagram: build a composite diagram wiring the cellular and regional submodels. op multilevel_model: deploy the submodels and build the multi-level agent-model diagram. Returns {operation, path, status, completed}.",
				diag,
				null,
				McpToolCatalog.op( "equations", "biouml_brain_generate_equations" ),
				McpToolCatalog.op( "composite_diagram", "biouml_brain_generate_composite_diagram" ),
				McpToolCatalog.op( "multilevel_model", "biouml_brain_generate_multilevel_model" ) );
	}
}
