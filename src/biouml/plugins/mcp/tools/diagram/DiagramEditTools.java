package biouml.plugins.mcp.tools.diagram;

import biouml.plugins.mcp.server.McpToolCatalog;
import biouml.plugins.mcp.support.McpArgs;
import biouml.plugins.mcp.support.McpDiagramActionSupport;
import biouml.plugins.mcp.support.McpDiagramEditSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * MCP tools for headless diagram *editing* — the layout operations (align/distribute) and the
 * sub-model operations that the web UI offers in the diagram context menus. Each is a
 * {@code BackgroundDynamicAction} whose {@code getJobControl(...).run()} does the work with no UI;
 * the "selection" a live editor would supply is passed explicitly as a list of element paths.
 * The edited diagram is saved, like after the diagram tools' add/remove operations.
 */
public final class DiagramEditTools
{
	/** The arguments of biouml_diagram_edit's align/distribute ops, renamed to those of biouml_diagram_layout. */
	private static final String[] LAYOUT_ARGS = { "diagramPath>path", "elements>nodes" };

	private DiagramEditTools()
	{
	}

	public static void registerAll( McpToolCatalog catalog )
	{
		catalog.registerHidden( "biouml_diagram_layout",
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

		catalog.registerHidden( "biouml_diagram_update_submodel",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "path" );
					if ( v != null )
						return v;
					return McpDiagramEditSupport.updateSubmodel( McpArgs.str( args, "path" ) );
				} );

		catalog.registerHidden( "biouml_diagram_split",
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

		catalog.registerHidden( "biouml_diagram_clone_node",
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

		catalog.registerHidden( "biouml_diagram_change_subdiagram",
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

		catalog.registerHidden( "biouml_diagram_change_port",
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

		catalog.registerHidden( "biouml_diagram_merge_clone",
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

		catalog.registerHidden( "biouml_diagram_save_subset",
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
				"Add upstream/downstream neighbours from a graph search to a diagram — the headless equivalent of the web UI's 'Add upstream elements' / 'Add downstream elements' menu items (AddFromSearchUpAction / AddFromSearchDownAction). path is the diagram to add elements to; elements are the node paths to expand (>=1); direction is 'up' or 'down'. Each selected element is queried in the BioHub for linked elements in the given direction and they are added to the diagram. Requires a BioHub query engine for the element's source. Returns {applied, status, direction, count}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version.",
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

		catalog.registerOps( "biouml_diagram_edit",
				"Change existing elements of a diagram as the diagram editor does; elementId is a name or a path such as 'default.s1'. op move: move node elementId to (x, y); a compartment takes its content along and attached edges are re-routed; returns {id, x, y}. op shift: shift the nodes elements by (dx, dy), e.g. a column of equations; a listed node inside a listed compartment moves once, with it; returns {moved, movedWithCompartment, dx, dy}. Both warn when a node ends up at a negative x or y (beyond the left or top edge). op resize: set width and height (pixels) of elementId, e.g. enlarge a compartment; returns {id, width, height}. op rename: set the display title of elementId to newName. op remove: remove elementId (node or edge) with the edges and reaction participants that depend on it, and the rate variables of removed reactions (removedVariables). op align-up, align-down, align-left, align-right, align-centerx, align-centery, distribute-hor, distribute-ver: the web UI's 'Align ...' / 'Distribute ...' on the nodes elements (>=2 to align, >=3 to distribute); returns {applied, op, count}.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"elementId\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"x\":{\"type\":\"integer\"},\"y\":{\"type\":\"integer\"},\"dx\":{\"type\":\"integer\"},\"dy\":{\"type\":\"integer\"},\"width\":{\"type\":\"integer\"},\"height\":{\"type\":\"integer\"},\"newName\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				null,
				McpToolCatalog.op( "move", "biouml_diagram_move_element" ),
				McpToolCatalog.op( "shift", "biouml_diagram_move_elements" ),
				McpToolCatalog.op( "resize", "biouml_diagram_resize_element" ),
				McpToolCatalog.op( "rename", "biouml_diagram_rename_element" ),
				McpToolCatalog.op( "remove", "biouml_diagram_remove_element" ),
				McpToolCatalog.op( "align-up", "biouml_diagram_layout", LAYOUT_ARGS ),
				McpToolCatalog.op( "align-down", "biouml_diagram_layout", LAYOUT_ARGS ),
				McpToolCatalog.op( "align-left", "biouml_diagram_layout", LAYOUT_ARGS ),
				McpToolCatalog.op( "align-right", "biouml_diagram_layout", LAYOUT_ARGS ),
				McpToolCatalog.op( "align-centerx", "biouml_diagram_layout", LAYOUT_ARGS ),
				McpToolCatalog.op( "align-centery", "biouml_diagram_layout", LAYOUT_ARGS ),
				McpToolCatalog.op( "distribute-hor", "biouml_diagram_layout", LAYOUT_ARGS ),
				McpToolCatalog.op( "distribute-ver", "biouml_diagram_layout", LAYOUT_ARGS ) );

		catalog.registerOps( "biouml_diagram_extract",
				"Create a new diagram from the elements (node/edge paths, >=1) of the diagram path: name is the new diagram's name, targetCollection where to create it (default: the source's collection). op subset: the web UI's 'Save subset' — a new diagram with only the elements (plus the compartments and edges that hold them), name default '<source> subset'; returns {applied, status, path, created, kept}. op split: the web UI's 'Split diagram' of a composite diagram — the elements move to the new diagram (name default 'Module'); autoIncludeReactions pulls in every reaction whose participants are all selected; addModule embeds the new diagram back as a sub-diagram module with connection ports, changing and saving the source too; returns {applied, status, path, created}.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"name\":{\"type\":\"string\"},\"targetCollection\":{\"type\":\"string\"},\"autoIncludeReactions\":{\"type\":\"boolean\"},\"addModule\":{\"type\":\"boolean\"}},\"required\":[\"path\",\"elements\"]}",
				null,
				McpToolCatalog.op( "subset", "biouml_diagram_save_subset" ),
				McpToolCatalog.op( "split", "biouml_diagram_split" ) );

		catalog.registerOps( "biouml_diagram_submodel",
				"Edit the sub-models of the composite or pathway diagram path. op update: the web UI's 'Update submodel' — re-read every sub-diagram's diagram (recursively) so the composite shows the latest sub-models; returns {applied, status}. op change_subdiagram: point the sub-diagram subdiagram (its path) at the diagram target, keeping the node name and rewiring the ports; returns {applied, status, subdiagram, target}. op change_port: change the type of the port node port to portType (input, output or contact), rewiring its edges; returns {applied, status, port, portType}. The diagram is saved; if the edit or the save fails, it is re-read from its last saved version.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"subdiagram\":{\"type\":\"string\"},\"target\":{\"type\":\"string\"},\"port\":{\"type\":\"string\"},\"portType\":{\"type\":\"string\"}},\"required\":[\"path\"]}",
				null,
				McpToolCatalog.op( "update", "biouml_diagram_update_submodel" ),
				McpToolCatalog.op( "change_subdiagram", "biouml_diagram_change_subdiagram" ),
				McpToolCatalog.op( "change_port", "biouml_diagram_change_port" ) );

		catalog.registerOps( "biouml_diagram_clone",
				"Clones of a node of the pathway diagram path. op clone: the web UI's 'Clone node' — clone node (a node with a variable role); cloneName defaults to a generated name; reactions are the reactions whose edges are redirected to the clone (default none); separateClones makes one clone per reaction (named cloneName_reaction); returns {applied, status, clone}. op merge: the web UI's 'Merge clone' — merge the clone node back into its original: its edges are redirected to the original and it is removed; returns {applied, status, merged}. The diagram is saved; if the edit or the save fails, it is re-read from its last saved version.",
				"{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"},\"node\":{\"type\":\"string\"},\"cloneName\":{\"type\":\"string\"},\"separateClones\":{\"type\":\"boolean\"},\"reactions\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},\"required\":[\"path\",\"node\"]}",
				null,
				McpToolCatalog.op( "clone", "biouml_diagram_clone_node" ),
				McpToolCatalog.op( "merge", "biouml_diagram_merge_clone", "node>clone" ) );

		catalog.registerHidden( "biouml_diagram_list_actions",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"}},\"required\":[\"diagramPath\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v != null )
						return v;
					return McpDiagramActionSupport.list( McpArgs.str( args, "diagramPath" ) );
				} );

		catalog.registerHidden( "biouml_diagram_run_action",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"action\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"properties\":{\"type\":\"object\"}},\"required\":[\"diagramPath\",\"action\"]}",
				( ex, args ) -> {
					McpEnvelope v = McpArgs.requiredString( args, "diagramPath" );
					if ( v != null )
						return v;
					McpEnvelope va = McpArgs.requiredString( args, "action" );
					if ( va != null )
						return va;
					McpEnvelope ve = McpArgs.stringArray( args, "elements" );
					if ( ve != null )
						return ve;
					return McpDiagramActionSupport.run( McpArgs.str( args, "diagramPath" ), McpArgs.str( args, "action" ),
							McpArgs.strArray( args, "elements" ), McpArgs.stringMap( args, "properties" ) );
				} );

		catalog.registerOps( "biouml_diagram_action",
				"The diagram's own toolbar actions — the buttons the web UI shows for an open diagram, which plugins add for their diagram types, e.g. on a Genome Enhancer annotation diagram 'Experiment control template' (an Experiment and a Control category), 'One category', 'Multiple category template' (numRow x numCol categories), 'Logical category template'. op list: the actions of the diagram diagramPath that the current perspective allows, each with its name, description, the selection it needs and its properties with their defaults (what its dialog asks for). op run: run the action named action on diagramPath as the web UI does; elements are the selected elements (names or paths such as 'Experiment', when the action needs a selection); properties is an object {name: value} of the action's properties (the others keep their defaults). The diagram is saved; returns {action, added[{id, path, name, kind, x, y, nodeType}], removed[ids]}. If the action fails, the diagram is re-read from its last saved version.",
				"{\"type\":\"object\",\"properties\":{\"diagramPath\":{\"type\":\"string\"},\"action\":{\"type\":\"string\"},\"elements\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"properties\":{\"type\":\"object\"}},\"required\":[\"diagramPath\"]}",
				null,
				McpToolCatalog.op( "list", "biouml_diagram_list_actions" ),
				McpToolCatalog.op( "run", "biouml_diagram_run_action" ) );
	}
}
