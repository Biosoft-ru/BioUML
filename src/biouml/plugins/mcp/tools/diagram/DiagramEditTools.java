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

		catalog.register( "biouml_diagram_split",
				"Split the selected elements of a composite diagram into a new diagram — the headless equivalent of the web UI's 'Split diagram' menu item (SplitDiagramAction). path is the composite diagram; elements are the node/edge paths to move to the new diagram (>=1); name is the new diagram's name (default 'Module'); targetCollection is the collection to create it in (default: the source's); autoIncludeReactions pulls in every reaction whose participants are all selected; addModule embeds the new diagram back as a sub-diagram module with connection ports. Returns {applied, status, path, created}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"name\":{\"type\":\"string\"},\"targetCollection\":{\"type\":\"string\"},\"autoIncludeReactions\":{\"type\":\"boolean\"},\"addModule\":{\"type\":\"boolean\"}},\"required\":[\"path\",\"elements\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope ve = McpArgs.stringArray( args, "elements" );
					if ( ve != null )
						return ve;
					McpEnvelope vb1 = McpArgs.boolArg( args, "autoIncludeReactions", false );
					if ( vb1 != null )
						return vb1;
					McpEnvelope vb2 = McpArgs.boolArg( args, "addModule", false );
					if ( vb2 != null )
						return vb2;
					return McpDiagramEditSupport.splitDiagram( McpArgs.str( args, "path" ), McpArgs.strArray( args, "elements" ),
							McpArgs.str( args, "name" ), McpArgs.str( args, "targetCollection" ),
							McpArgs.bool( args, "autoIncludeReactions", false ), McpArgs.bool( args, "addModule", false ) );
				} );

		catalog.register( "biouml_diagram_clone_node",
				"Clone a pathway node — the headless equivalent of the web UI's 'Clone node' menu item (CloneNodeAction). path is the pathway diagram; node is the name of the node to clone (must carry a variable role); cloneName is the new node's name (default auto-generated); separateClones creates one clone per reaction (named cloneName_reaction); reactions are the names of the reactions whose edges are redirected to the clone (default: none). Returns {applied, status, clone}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"node\":{\"type\":\"string\"},\"cloneName\":{\"type\":\"string\"},\"separateClones\":{\"type\":\"boolean\"},\"reactions\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"path\",\"node\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vn = McpArgs.requiredString( args, "node" );
					if ( vn != null )
						return vn;
					McpEnvelope vb = McpArgs.boolArg( args, "separateClones", false );
					if ( vb != null )
						return vb;
					return McpDiagramEditSupport.cloneNode( McpArgs.str( args, "path" ), McpArgs.str( args, "node" ),
							McpArgs.str( args, "cloneName" ), McpArgs.bool( args, "separateClones", false ), McpArgs.strArray( args, "reactions" ) );
				} );

		catalog.register( "biouml_diagram_change_subdiagram",
				"Change a sub-diagram's target diagram — the headless equivalent of the web UI's 'Change subdiagram' menu item (ChangeSubdiagramAction). path is the composite diagram containing the sub-diagram; subdiagram is the path of the sub-diagram to replace; target is the path of the new diagram the sub-diagram should point at. The existing sub-diagram is replaced by a fresh one preserving the node name and rewiring the ports. Returns {applied, status, subdiagram, target}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"subdiagram\":{\"type\":\"string\"},\"target\":{\"type\":\"string\"}},\"required\":[\"path\",\"subdiagram\",\"target\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vs = McpArgs.requiredString( args, "subdiagram" );
					if ( vs != null )
						return vs;
					McpEnvelope vt = McpArgs.requiredString( args, "target" );
					if ( vt != null )
						return vt;
					return McpDiagramEditSupport.changeSubdiagram( McpArgs.str( args, "path" ), McpArgs.str( args, "subdiagram" ), McpArgs.str( args, "target" ) );
				} );

		catalog.register( "biouml_diagram_change_port",
				"Change a pathway port's type — the headless equivalent of the web UI's 'Change port type' menu item (ChangePortTypeAction). path is the pathway diagram; port is the name of the port node; portType is the new type (input, output, or contact). The port is recreated with the new type and its edges are rewired. Returns {applied, status, port, portType}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"port\":{\"type\":\"string\"},\"portType\":{\"type\":\"string\"}},\"required\":[\"path\",\"port\",\"portType\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vp = McpArgs.requiredString( args, "port" );
					if ( vp != null )
						return vp;
					McpEnvelope vt = McpArgs.requiredString( args, "portType" );
					if ( vt != null )
						return vt;
					return McpDiagramEditSupport.changePortType( McpArgs.str( args, "path" ), McpArgs.str( args, "port" ), McpArgs.str( args, "portType" ) );
				} );

		catalog.register( "biouml_diagram_merge_clone",
				"Merge a cloned pathway node back into its original — the headless equivalent of the web UI's 'Merge clone' menu item (MergeCloneAction). path is the pathway diagram; clone is the name of the clone node to merge away (must be a variable-role node whose variable references a different original element). Its edges are redirected to the original and the clone is removed. Returns {applied, status, merged}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"clone\":{\"type\":\"string\"}},\"required\":[\"path\",\"clone\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope vc = McpArgs.requiredString( args, "clone" );
					if ( vc != null )
						return vc;
					return McpDiagramEditSupport.mergeClone( McpArgs.str( args, "path" ), McpArgs.str( args, "clone" ) );
				} );

		catalog.register( "biouml_diagram_save_subset",
				"Save a subset of a diagram's elements to a new diagram — the headless equivalent of the web UI's 'Save subset' menu item (SaveDiagramSubsetAction). path is the source diagram; elements are the node/edge paths to keep (>=1); name is the new diagram's name (default '<source> subset'); targetCollection is the collection to create it in (default: the source's). The new diagram contains only the selected elements (plus the compartments/edges that hold them). Returns {applied, status, path, created, kept}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"name\":{\"type\":\"string\"},\"targetCollection\":{\"type\":\"string\"}},\"required\":[\"path\",\"elements\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope ve = McpArgs.stringArray( args, "elements" );
					if ( ve != null )
						return ve;
					return McpDiagramEditSupport.saveSubset( McpArgs.str( args, "path" ), McpArgs.strArray( args, "elements" ),
							McpArgs.str( args, "name" ), McpArgs.str( args, "targetCollection" ) );
				} );

		catalog.register( "biouml_diagram_add_from_search",
				"Add upstream/downstream neighbours from a graph search to a diagram — the headless equivalent of the web UI's 'Add upstream elements' / 'Add downstream elements' menu items (AddFromSearchUpAction / AddFromSearchDownAction). path is the diagram to add elements to; elements are the node paths to expand (>=1); direction is 'up' or 'down'. Each selected element is queried in the BioHub for linked elements in the given direction and they are added to the diagram. Requires a BioHub query engine for the element's source. Returns {applied, status, direction, count}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"direction\":{\"type\":\"string\"}},\"required\":[\"path\",\"elements\",\"direction\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					McpEnvelope ve = McpArgs.stringArray( args, "elements" );
					if ( ve != null )
						return ve;
					McpEnvelope vd = McpArgs.requiredString( args, "direction" );
					if ( vd != null )
						return vd;
					return McpDiagramEditSupport.addFromSearch( McpArgs.str( args, "path" ), McpArgs.strArray( args, "elements" ), McpArgs.str( args, "direction" ) );
				} );
	}
}
