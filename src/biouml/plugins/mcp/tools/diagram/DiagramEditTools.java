package biouml.plugins.mcp.tools.diagram;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpDiagramEditSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * MCP tools for headless diagram *editing* — the layout operations (align/distribute) and the
 * sub-model operations that the web UI offers in the diagram context menus. Each is a
 * {@code BackgroundDynamicAction} whose {@code getJobControl(...).run()} does the work with no UI;
 * the "selection" a live editor would supply is passed explicitly as a list of element paths.
 */
public final class DiagramEditTools
{
	private DiagramEditTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_diagram_layout",
				"Apply a layout operation to the selected diagram nodes — the headless equivalent of the web UI's 'Align ...' / 'Distribute ...' diagram context-menu items. path is the diagram; op is one of align-up, align-down, align-left, align-right, align-centerx, align-centery, distribute-hor, distribute-ver; nodes is the list of node paths to lay out (>=2 for align, >=3 for distribute). The diagram view is regenerated headlessly and the nodes moved via the diagram's semantic controller. Returns {applied, op, count}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"op\":{\"type\":\"string\"},\"nodes\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"path\",\"op\",\"nodes\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vo = McpArgs.requiredString( args, "op" );
					if ( vo != null )
						return vo;
					McpEnvelope vn = McpArgs.stringArray( args, "nodes" );
					if ( vn != null )
						return vn;
					return McpDiagramEditSupport.layout( McpArgs.str( args, "path" ), McpArgs.str( args, "op" ), McpArgs.strArray( args, "nodes" ) );
				} );

		catalog.register( "biouml_diagram_update_submodel",
				"Update the sub-diagrams of a composite diagram — the headless equivalent of the web UI's 'Update submodel' menu item (UpdateSubModelAction). Recursively re-reads each sub-diagram's current diagram and clears its cached view so the composite reflects the latest sub-models. path is the composite diagram. Returns {applied, status}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpDiagramEditSupport.updateSubmodel( McpArgs.str( args, "path" ) );
				} );
	}
}
