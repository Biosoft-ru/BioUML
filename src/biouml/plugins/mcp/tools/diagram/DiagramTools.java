package biouml.plugins.mcp.tools.diagram;

import java.util.Map;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpDiagramSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * Phase-4 diagram MCP tools. Registered into a {@link McpToolCatalog}.
 *
 * <p>These tools let an agent create and edit BioUML diagrams headlessly — create a diagram,
 * inspect its elements, add/remove/move/rename nodes and edges, and save/import/export to DML
 * files — without opening the desktop UI.</p>
 */
public final class DiagramTools
{
	private DiagramTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.register( "biouml_diagram_create",
				"Create a new diagram in a target collection. type is 'math' (default) or 'pathway'. Returns the new diagram's path.",
				"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"name\"]}",
				( ex, args ) -> McpDiagramSupport.create( str( args, "parentPath" ), str( args, "name" ), str( args, "type" ) ) );

		catalog.register( "biouml_diagram_describe",
				"Describe a diagram: element count, per-element summaries (id, name, kind, position, node type) and — when a dynamic model is present — its variables and equation count.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> McpDiagramSupport.describe( str( args, "path" ) ) );

		catalog.register( "biouml_diagram_node_types",
				"List the node types that biouml_diagram_add_node accepts.",
				"{}",
				( ex, args ) -> McpEnvelope.ok( McpDiagramSupport.nodeTypes() ) );

		catalog.register( "biouml_diagram_add_node",
				"Add a node to a diagram. Returns the assigned element id. nodeType is one of the keys from biouml_diagram_node_types (default 'Stub').",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"nodeType\":{\"type\":\"string\"},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"name\"]}",
				( ex, args ) -> McpDiagramSupport.addNode( str( args, "diagramPath" ), str( args, "name" ), str( args, "nodeType" ),
						optInt( args, "x" ), optInt( args, "y" ) ) );

		catalog.register( "biouml_diagram_add_edge",
				"Add an edge between two nodes. Returns the assigned edge id.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"sourceId\":{\"type\":\"string\"},\"targetId\":{\"type\":\"string\"},\"edgeType\":{\"type\":\"string\"},\"label\":{\"type\":\"string\"}},\"required\":[\"diagramPath\",\"sourceId\",\"targetId\"]}",
				( ex, args ) -> McpDiagramSupport.addEdge( str( args, "diagramPath" ), str( args, "sourceId" ), str( args, "targetId" ),
						str( args, "edgeType" ), str( args, "label" ) ) );

		catalog.register( "biouml_diagram_remove_element",
				"Remove an element (node or edge) from a diagram by its id.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"}},\"required\":[\"diagramPath\",\"elementId\"]}",
				( ex, args ) -> McpDiagramSupport.removeElement( str( args, "diagramPath" ), str( args, "elementId" ) ) );

		catalog.register( "biouml_diagram_move_element",
				"Move a node to a new (x, y) position.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"}},\"required\":[\"diagramPath\",\"elementId\",\"x\",\"y\"]}",
				( ex, args ) -> {
					Integer x = optInt( args, "x" );
					Integer y = optInt( args, "y" );
					if ( x == null || y == null )
						return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "x and y must be integers" );
					return McpDiagramSupport.moveElement( str( args, "diagramPath" ), str( args, "elementId" ), x, y );
				} );

		catalog.register( "biouml_diagram_rename_element",
				"Rename an element's display title.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"},\"newName\":{\"type\":\"string\"}},\"required\":[\"diagramPath\",\"elementId\",\"newName\"]}",
				( ex, args ) -> McpDiagramSupport.renameElement( str( args, "diagramPath" ), str( args, "elementId" ), str( args, "newName" ) ) );

		catalog.register( "biouml_diagram_save",
				"Save a diagram to a DML file on disk (in targetDir, or the current directory). Returns the file path and size.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"targetDir\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpDiagramSupport.save( str( args, "diagramPath" ), str( args, "targetDir" ) ) );

		catalog.register( "biouml_diagram_export",
				"Export a diagram to a file (DML by default). Returns the file path and size.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"targetDir\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> McpDiagramSupport.exportDiagram( str( args, "diagramPath" ), str( args, "targetDir" ), str( args, "format" ) ) );

		catalog.register( "biouml_diagram_import",
				"Import a diagram file (DML by default) into a target collection. Returns the new element's path.",
				"{\"type\":\"object\",\"properties\":{\"parentPath\":{\"type\":\"string\"},\"file\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"}},\"required\":[\"parentPath\",\"file\"]}",
				( ex, args ) -> McpDiagramSupport.importDiagram( str( args, "parentPath" ), str( args, "file" ), str( args, "format" ), str( args, "name" ) ) );
	}

	private static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v == null ? null : String.valueOf( v );
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
